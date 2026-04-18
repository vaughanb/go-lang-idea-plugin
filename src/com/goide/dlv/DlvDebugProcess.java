/*
 * Copyright 2013-2016 Sergey Ignatov, Alexander Zolotov, Florin Patan
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.goide.dlv;

import com.goide.GoConstants;
import com.goide.GoFileType;
import com.goide.dlv.breakpoint.DlvBreakpointProperties;
import com.goide.dlv.breakpoint.DlvBreakpointType;
import com.goide.dlv.protocol.DlvRequest;
import com.goide.util.GoUtil;
import com.intellij.execution.ExecutionResult;
import com.intellij.execution.ui.ExecutionConsole;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.PlainTextLanguage;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.util.io.socketConnection.ConnectionStatus;
import com.intellij.xdebugger.XDebugProcess;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.XSourcePosition;
import com.intellij.xdebugger.breakpoints.XBreakpoint;
import com.intellij.xdebugger.breakpoints.XBreakpointHandler;
import com.intellij.xdebugger.breakpoints.XLineBreakpoint;
import com.intellij.xdebugger.evaluation.XDebuggerEditorsProviderBase;
import com.intellij.xdebugger.frame.XSuspendContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.concurrency.Promise;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.goide.dlv.protocol.DlvApi.*;

public final class DlvDebugProcess extends XDebugProcess implements Disposable {
  public static final boolean IS_DLV_DISABLED = !GoConstants.AMD64.equals(GoUtil.systemArch());

  private static final Logger LOG = Logger.getInstance(DlvDebugProcess.class);
  private final AtomicBoolean breakpointsInitiated = new AtomicBoolean();
  private final AtomicBoolean connectedListenerAdded = new AtomicBoolean();

  @NotNull private final DlvRemoteVmConnection myConnection;
  @Nullable private final ExecutionResult myExecutionResult;

  private void handleDebuggerState(@NotNull DebuggerState o) {
    if (o.exited) {
      stop();
      return;
    }

    XBreakpoint<DlvBreakpointProperties> find = findBreak(o.breakPoint);
    send(new DlvRequest.StacktraceGoroutine()).onSuccess(locations -> {
      DlvSuspendContext context = new DlvSuspendContext(DlvDebugProcess.this, o.currentThread.id, locations, getProcessor());
      XDebugSession session = getSession();
      if (find == null) {
        session.positionReached(context);
      }
      else {
        session.breakpointReached(find, null, context);
      }
    });
  }

  @Nullable
  private XBreakpoint<DlvBreakpointProperties> findBreak(@Nullable Breakpoint point) {
    return point == null ? null : breakpoints.get(point.id);
  }

  @NotNull
  <T> Promise<T> send(@NotNull DlvRequest<T> request) {
    return send(request, getProcessor());
  }

  @NotNull
  static <T> Promise<T> send(@NotNull DlvRequest<T> request, @NotNull DlvCommandProcessor processor) {
    return processor.send(request).onError(t -> LOG.info(t));
  }

  @NotNull
  DlvCommandProcessor getProcessor() {
    DlvVm vm = myConnection.getVm();
    if (vm == null) throw new IllegalStateException("DlvVm is not initialized");
    return vm.getCommandProcessor();
  }

  public DlvDebugProcess(@NotNull XDebugSession session, @NotNull DlvRemoteVmConnection connection, @Nullable ExecutionResult er) {
    super(session);
    myConnection = connection;
    myExecutionResult = er;
  }

  @NotNull
  @Override
  public XBreakpointHandler<?>[] getBreakpointHandlers() {
    return new XBreakpointHandler[]{new MyBreakpointHandler()};
  }

  @NotNull
  @Override
  public ExecutionConsole createConsole() {
    return myExecutionResult == null ? super.createConsole() : myExecutionResult.getExecutionConsole();
  }

  @Override
  public void dispose() {
    myConnection.close();
  }

  @NotNull
  @Override
  public com.intellij.xdebugger.evaluation.XDebuggerEditorsProvider getEditorsProvider() {
    return new MyEditorsProvider();
  }

  @Override
  public boolean checkCanInitBreakpoints() {
    if (myConnection.getStatus() == ConnectionStatus.CONNECTED) {
      return initBreakpointHandlersAndSetBreakpoints(false);
    }

    if (connectedListenerAdded.compareAndSet(false, true)) {
      myConnection.addListener(status -> {
        if (status == ConnectionStatus.CONNECTED) {
          initBreakpointHandlersAndSetBreakpoints(true);
        }
      });
    }
    return false;
  }

  private boolean initBreakpointHandlersAndSetBreakpoints(boolean setBreakpoints) {
    if (!breakpointsInitiated.compareAndSet(false, true)) return false;

    if (setBreakpoints) {
      doSetBreakpoints();
      command(CONTINUE);
    }

    return true;
  }

  private void doSetBreakpoints() {
    ReadAction.run(() -> getSession().initBreakpoints());
  }

  private void command(@NotNull String name) {
    send(new DlvRequest.Command(name)).onSuccess(this::handleDebuggerState);
  }

  @Override
  public void startStepOver(@Nullable XSuspendContext context) {
    command(NEXT);
  }

  @Override
  public void startStepInto(@Nullable XSuspendContext context) {
    command(STEP);
  }

  @Override
  public void resume(@Nullable XSuspendContext context) {
    command(CONTINUE);
  }

  @Override
  public void runToPosition(@NotNull XSourcePosition position, @Nullable XSuspendContext context) {
    // todo
  }

  @Override
  public void stop() {
    DlvVm vm = myConnection.getVm();
    if (vm != null) {
      send(new DlvRequest.Detach(true));
    }
    getSession().stop();
  }

  private static class MyEditorsProvider extends XDebuggerEditorsProviderBase {
    @NotNull
    @Override
    public FileType getFileType() {
      return GoFileType.INSTANCE;
    }

    @Override
    protected PsiFile createExpressionCodeFragment(@NotNull Project project,
                                                   @NotNull String text,
                                                   @Nullable PsiElement context,
                                                   boolean isPhysical) {
      return PsiFileFactory.getInstance(project).createFileFromText("dlv-debug.txt", PlainTextLanguage.INSTANCE, text);
    }
  }

  private static final Key<Integer> ID = Key.create("DLV_BP_ID");
  private final Map<Integer, XBreakpoint<DlvBreakpointProperties>> breakpoints = new ConcurrentHashMap<>();

  private class MyBreakpointHandler extends XBreakpointHandler<XLineBreakpoint<DlvBreakpointProperties>> {

    public MyBreakpointHandler() {
      super(DlvBreakpointType.class);
    }

    @Override
    public void registerBreakpoint(@NotNull XLineBreakpoint<DlvBreakpointProperties> breakpoint) {
      XSourcePosition breakpointPosition = breakpoint.getSourcePosition();
      if (breakpointPosition == null) return;
      VirtualFile file = breakpointPosition.getFile();
      int line = breakpointPosition.getLine();
      send(new DlvRequest.CreateBreakpoint(file.getPath(), line + 1))
        .onSuccess(b -> {
          breakpoint.putUserData(ID, b.id);
          breakpoints.put(b.id, breakpoint);
          getSession().updateBreakpointPresentation(breakpoint, AllIcons.Debugger.Db_verified_breakpoint, null);
        })
        .onError(t -> {
          String message = t == null ? null : t.getMessage();
          getSession().updateBreakpointPresentation(breakpoint, AllIcons.Debugger.Db_invalid_breakpoint, message);
        });
    }

    @Override
    public void unregisterBreakpoint(@NotNull XLineBreakpoint<DlvBreakpointProperties> breakpoint, boolean temporary) {
      XSourcePosition breakpointPosition = breakpoint.getSourcePosition();
      if (breakpointPosition == null) return;
      Integer id = breakpoint.getUserData(ID);
      if (id == null) return;
      breakpoint.putUserData(ID, null);
      breakpoints.remove(id);
      send(new DlvRequest.ClearBreakpoint(id));
    }
  }
}

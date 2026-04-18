/*
 * Copyright 2013-2015 Sergey Ignatov, Alexander Zolotov, Florin Patan
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

import com.goide.dlv.protocol.DlvRequest;
import com.goide.dlv.protocol.DlvResponse;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.concurrency.AsyncPromise;
import org.jetbrains.concurrency.Promise;
import org.jetbrains.jsonProtocol.Request;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class DlvCommandProcessor {
  private static final Logger LOG = Logger.getInstance(DlvCommandProcessor.class);

  private final AtomicInteger nextId = new AtomicInteger();
  private final Map<Integer, PendingRequest<?>> pendingRequests = new ConcurrentHashMap<>();

  public abstract boolean write(@NotNull Request message) throws IOException;

  @NotNull
  public <T> Promise<T> send(@NotNull DlvRequest<T> message) {
    int id = nextId.incrementAndGet();
    message.finalize(id);
    AsyncPromise<T> promise = new AsyncPromise<>();
    pendingRequests.put(id, new PendingRequest<>(promise, message.getMethodName()));
    try {
      if (!write(message)) {
        pendingRequests.remove(id);
        promise.setError("Failed to write message");
      }
    }
    catch (IOException e) {
      pendingRequests.remove(id);
      promise.setError(e);
    }
    return promise;
  }

  public void processIncomingJson(@NotNull JsonReaderEx reader) {
    DlvResponse response = new DlvResponse.CommandResponseImpl(reader, null);
    int id = response.id();
    PendingRequest<?> pending = pendingRequests.remove(id);
    if (pending == null) {
      LOG.error("No pending request for response id " + id);
      return;
    }
    dispatchResponse(response, pending);
  }

  @SuppressWarnings("unchecked")
  private <T> void dispatchResponse(@NotNull DlvResponse response, @NotNull PendingRequest<T> pending) {
    if (response.result() != null) {
      try {
        T result = readResult(pending.methodName, response);
        pending.promise.setResult(result);
      }
      catch (Exception e) {
        pending.promise.setError(e);
      }
    }
    else {
      pending.promise.setError(createMessage(response));
    }
  }

  @NotNull
  private static String createMessage(@NotNull DlvResponse r) {
    DlvResponse.ErrorInfo e = r.error();
    if (e == null) return "Internal messaging error";
    List<String> data = e.data();
    String message = e.message();
    if (ContainerUtil.isEmpty(data)) return StringUtil.defaultIfEmpty(message, "<null>");
    List<String> list = new SmartList<>(message);
    list.addAll(data);
    return list.toString();
  }

  @NotNull
  @SuppressWarnings("unchecked")
  private <T> T readResult(@NotNull String method, @NotNull DlvResponse successResponse) {
    JsonReaderEx result = successResponse.result();
    assert result != null : "success result should be not null";
    JsonReader reader = result.asGson();
    Object o = new GsonBuilder().create().fromJson(reader, getResultType(method.replaceFirst("RPCServer\\.", "")));
    return (T)o;
  }

  @NotNull
  private static Type getResultType(@NotNull String method) {
    for (Class<?> c : DlvRequest.class.getDeclaredClasses()) {
      if (method.equals(c.getSimpleName())) {
        Type s = c.getGenericSuperclass();
        assert s instanceof ParameterizedType : c.getCanonicalName() + " should have a generic parameter for correct callback processing";
        Type[] arguments = ((ParameterizedType)s).getActualTypeArguments();
        assert arguments.length == 1 : c.getCanonicalName() + " should have only one generic argument for correct callback processing";
        return arguments[0];
      }
    }
    LOG.error("Unknown response " + method + ", please register an appropriate request into com.goide.dlv.protocol.DlvRequest");
    return Object.class;
  }

  private static class PendingRequest<T> {
    @NotNull final AsyncPromise<T> promise;
    @NotNull final String methodName;

    PendingRequest(@NotNull AsyncPromise<T> promise, @NotNull String methodName) {
      this.promise = promise;
      this.methodName = methodName;
    }
  }
}

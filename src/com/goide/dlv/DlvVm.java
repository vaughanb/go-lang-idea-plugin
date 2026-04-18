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

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.vfs.CharsetToolkit;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jsonProtocol.Request;

import java.io.IOException;

public class DlvVm {
  private static final Logger LOG = Logger.getInstance(DlvVm.class);

  @NotNull private final DlvCommandProcessor commandProcessor;
  @NotNull private final Channel channel;

  public DlvVm(@NotNull Channel channel) {
    this.channel = channel;

    commandProcessor = new DlvCommandProcessor() {
      @Override
      public boolean write(@NotNull Request message) throws IOException {
        ByteBuf content = message.getBuffer();
        LOG.info("OUT: " + content.toString(CharsetToolkit.UTF8_CHARSET));
        return channel.writeAndFlush(content).awaitUninterruptibly().isSuccess();
      }
    };
  }

  @NotNull
  public DlvCommandProcessor getCommandProcessor() {
    return commandProcessor;
  }
}

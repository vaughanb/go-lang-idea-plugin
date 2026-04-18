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

import com.intellij.openapi.Disposable;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.vfs.CharsetToolkit;
import com.intellij.util.io.socketConnection.ConnectionStatus;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.json.JsonObjectDecoder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.InetSocketAddress;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class DlvRemoteVmConnection implements Disposable {
  private static final Logger LOG = Logger.getInstance(DlvRemoteVmConnection.class);

  @Nullable private DlvVm myVm;
  @Nullable private Channel myChannel;
  @Nullable private EventLoopGroup myGroup;
  @NotNull private ConnectionStatus myStatus = ConnectionStatus.NOT_CONNECTED;
  private final CopyOnWriteArrayList<Consumer<ConnectionStatus>> myListeners = new CopyOnWriteArrayList<>();

  public void open(@NotNull InetSocketAddress address) {
    myGroup = new NioEventLoopGroup(1);
    new Bootstrap()
      .group(myGroup)
      .channel(NioSocketChannel.class)
      .handler(new ChannelInitializer<SocketChannel>() {
        @Override
        protected void initChannel(@NotNull SocketChannel channel) {
          myChannel = channel;
          myVm = new DlvVm(channel);
          channel.pipeline().addLast(new JsonObjectDecoder(), new SimpleChannelInboundHandler<ByteBuf>() {
            @Override
            protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) {
              String text = msg.toString(CharsetToolkit.UTF8_CHARSET);
              LOG.info("IN: " + text);
              myVm.getCommandProcessor().processIncomingJson(new JsonReaderEx(text));
            }
          });
          setStatus(ConnectionStatus.CONNECTED);
        }
      })
      .connect(address)
      .addListener((ChannelFutureListener) future -> {
        if (!future.isSuccess()) {
          LOG.warn("Failed to connect to " + address, future.cause());
          setStatus(ConnectionStatus.DISCONNECTED);
        }
      });
  }

  private void setStatus(@NotNull ConnectionStatus status) {
    myStatus = status;
    for (Consumer<ConnectionStatus> listener : myListeners) {
      listener.accept(status);
    }
  }

  public void addListener(@NotNull Consumer<ConnectionStatus> listener) {
    myListeners.add(listener);
  }

  @NotNull
  public ConnectionStatus getStatus() {
    return myStatus;
  }

  @Nullable
  public DlvVm getVm() {
    return myVm;
  }

  public void close() {
    if (myChannel != null) {
      myChannel.close();
    }
    if (myGroup != null) {
      myGroup.shutdownGracefully();
    }
    setStatus(ConnectionStatus.DISCONNECTED);
  }

  @Override
  public void dispose() {
    close();
  }
}

/*
 * Copyright (c) 2008 - 2013 MongoDB Inc. <http://10gen.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mongodb.connection;

import org.bson.ByteBuf;
import org.mongodb.MongoInterruptedException;
import reactor.core.Environment;
import reactor.core.composable.Promise;
import reactor.function.Consumer;
import reactor.function.Function;
import reactor.io.Buffer;
import reactor.tcp.TcpClient;
import reactor.tcp.TcpConnection;
import reactor.tcp.encoding.Codec;
import reactor.tcp.netty.NettyTcpClient;
import reactor.tcp.spec.TcpClientSpec;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class TcpClientStream implements Stream {
    private final TcpClient<Buffer, ByteBuf> tcpClient;
    private final ServerAddress address;
    private final SocketSettings settings;
    private TcpConnection<Buffer, ByteBuf> tcpConnection;
    private volatile boolean isClosed;

    public TcpClientStream(final ServerAddress address, final SocketSettings settings, final SSLSettings sslSettings) {
        this.address = address;
        this.settings = settings;
        TcpClientSpec<Buffer, ByteBuf> tcpClientSpec = new TcpClientSpec<Buffer, ByteBuf>(NettyTcpClient.class)
                                                       .env(new Environment())
                                                       .codec(new FixedLengthCodec())
                                                       .connect(address.getHost(), address.getPort());
        tcpClient = tcpClientSpec.get();
        try {
            Promise<TcpConnection<Buffer, ByteBuf>> connectionPromise = tcpClient.open();
            final CountDownLatch latch = new CountDownLatch(1);
            connectionPromise.consume(new Consumer<TcpConnection<Buffer, ByteBuf>>() {
                @Override
                public void accept(final TcpConnection<Buffer, ByteBuf> connection) {
                    tcpConnection = connection;
                    latch.countDown();
                }
            });
            latch.await();
        } catch (InterruptedException e) {
            throw new MongoInterruptedException("Interrupted while opening TCP connection", e);
        }
    }

    @Override
    public void write(final List<ByteBuf> buffers) throws IOException {
        for (ByteBuf cur : buffers) {
            try {
                tcpConnection.send(cur).await();
            } catch (InterruptedException e) {
                throw new MongoInterruptedException("Interrupted while writing to TCP connection", e);
            }
        }
    }

    @Override
    public void read(final ByteBuf buffer) throws IOException {
        final CountDownLatch latch = new CountDownLatch(1);
        tcpConnection.in().consume(new Consumer<Buffer>() {
            @Override
            public void accept(final Buffer bytes) {
                buffer.put(bytes.asBytes(), bytes.position(), bytes.limit());
                latch.countDown();
            }
        });
        try {
            latch.await();
        } catch (InterruptedException e) {
            throw new MongoInterruptedException("Interrupted reading from connection", e);
        }
    }

    @Override
    public void writeAsync(final List<ByteBuf> buffers, final AsyncCompletionHandler handler) {
        throw new UnsupportedOperationException("Not implemented yet!");
    }

    @Override
    public void readAsync(final ByteBuf buffer, final AsyncCompletionHandler handler) {
        throw new UnsupportedOperationException("Not implemented yet!");
    }

    @Override
    public ServerAddress getAddress() {
        return address;
    }

    @Override
    public void close() {
        tcpClient.close();
        isClosed = true;
    }

    @Override
    public boolean isClosed() {
        return isClosed;
    }

    private static class FixedLengthCodec implements Codec<Buffer, Buffer, ByteBuf> {
        @Override
        public Function<Buffer, Buffer> decoder(final Consumer<Buffer> next) {
            return new Function<Buffer, Buffer>() {
                @Override
                public Buffer apply(final Buffer bytes) {
                    Buffer buffer = new Buffer(bytes);
                    next.accept(buffer);
                    return null;
                }
            };
        }

        @Override
        public Function<ByteBuf, Buffer> encoder() {
            return new Function<ByteBuf, Buffer>() {
                @Override
                public Buffer apply(final ByteBuf byteBuf) {
                    Buffer buffer = new Buffer();
                    buffer.append(byteBuf.asNIO());
                    return buffer.flip();
                }
            };
        }
    }
}

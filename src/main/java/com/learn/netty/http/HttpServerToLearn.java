package com.learn.netty.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.handler.codec.http.HttpContent;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Objects;
import java.util.UUID;
import java.util.function.BiFunction;

/**
 * @author Mr.M
 */
public class HttpServerToLearn {

    public static void main(String[] args) {
        Path file = Paths.get("");
        DisposableServer httpServer =
                HttpServer.create()
                        .host("127.0.0.1")
                        .port(8190)
                        // 设置Http解码器
                        .httpRequestDecoder(httpRequestDecoderSpec -> {
                            return httpRequestDecoderSpec.maxChunkSize(1024 * 10);
                        })
                        // 路由Http
                        .route(routes ->
                                routes.post("/getValue",
                                        (httpRequest, httpResponse) -> {
                                            Flux<HttpContent> flux = httpRequest.receiveContent();
                                            Flux<String> stringFlux =  flux.flatMap(f -> {
                                                byte[] bytes = new byte[f.content().readableBytes()];
                                                f.content().readBytes(bytes);
                                                return Flux.just(new String(bytes));
                                            });
                                            return httpResponse.status(200)
                                                    .sendString(stringFlux);
                                        }))
                        // 使用SSE
                        .route(routes -> {
                            routes.get("/sse", serveSse());
                        })
                        // 处理静态资源
                        .route(routes -> routes.file("/index.html", file))
                        // 写数据和添加响应头，响应状态等方法基本同普通处理一样，在此不再描述
                        // 开启响应压缩，有三个变种方法
                        .compress(true)
                        // 消费请求数据也和Tcp差不多，也不再描述
                        .bindNow();
        httpServer.onDispose()
                .block();
    }

    /**
     * Prepares SSE response
     * The "Content-Type" is "text/event-stream"
     * The flushing strategy is "flush after every element" emitted by the provided Publisher
     */
    private static BiFunction<HttpServerRequest, HttpServerResponse, Publisher<Void>> serveSse() {
        Flux<Long> flux = Flux.interval(Duration.ofMillis(10));
        return (request, response) ->
                response.sse()
                        .send(flux.map(in -> toByteBuf(UUID.randomUUID().toString())), b -> true);
    }

    /**
     * Transforms the Object to ByteBuf following the expected SSE format.
     */
    private static ByteBuf toByteBuf(Object any) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            out.write("data: ".getBytes(Charset.defaultCharset()));
            MAPPER.writeValue(out, any);
            out.write("\n\n".getBytes(Charset.defaultCharset()));
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
        return ByteBufAllocator.DEFAULT
                .buffer()
                .writeBytes(out.toByteArray());
    }

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static void main0(String[] args) {
        DisposableServer server =
                HttpServer.create()
                        // 还可以设置协议为Http2.0，具体见文档，在此不再叙述
                        .route(routes ->
                                routes.get("/hello",
                                        (request, response) -> response.sendString(Mono.just("Hello World!")))
                                        .post("/echo",
                                                (request, response) -> response.send(request.receive().retain()))
                                        // 路径参数形式
                                        .get("/path/{param}",
                                                (request, response) -> response.sendString(Mono.just(Objects.requireNonNull(request.param("param")))))
                                        // 获取远程地址
                                        .get("/remote", ((httpServerRequest, httpServerResponse) -> httpServerResponse
                                                .status(200).sendString(Mono.just(Objects.requireNonNull(httpServerRequest.remoteAddress()).getAddress().toString()))))
                                        .ws("/ws",
                                                (wsInbound, wsOutbound) -> wsOutbound.send(wsInbound.receive().retain())))
                        .bindNow();

        server.onDispose()
                .block();
    }
}

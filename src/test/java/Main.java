import io.netty.handler.codec.http.HttpContent;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;

/**
 * @author Mr.M
 */
public class Main {

    public static void main(String[] args) {
        DisposableServer httpServer =
                HttpServer.create()
                        .host("127.0.0.1")
                        .port(8190)
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
                        .bindNow();
        httpServer.onDispose()
                .block();
    }

    public static void main0(String[] args) {
        DisposableServer server =
                HttpServer.create()
                        .route(routes ->
                                routes.get("/hello",
                                        (request, response) -> response.sendString(Mono.just("Hello World!")))
                                        .post("/echo",
                                                (request, response) -> response.send(request.receive().retain()))
                                        .get("/path/{param}",
                                                (request, response) -> response.sendString(Mono.just(request.param("param"))))
                                        .ws("/ws",
                                                (wsInbound, wsOutbound) -> wsOutbound.send(wsInbound.receive().retain())))
                        .bindNow();

        server.onDispose()
                .block();
    }
}

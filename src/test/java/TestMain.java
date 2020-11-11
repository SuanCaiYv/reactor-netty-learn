import reactor.netty.DisposableServer;
import reactor.netty.tcp.TcpServer;

/**
 * @author Mr.M
 */
public class TestMain {

    public static void main(String[] args) {
        DisposableServer tcpServer = TcpServer.create().host("127.0.0.1")
                .port(8190)
                .bindNow();
    }
}

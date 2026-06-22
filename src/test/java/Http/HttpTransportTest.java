package Http;

import MessagePackage.OnionPacket;
import MessagePackage.OnionPacketDto;
import NodeMaterials.NodeConfig;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HttpTransportTest {
    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void sendPacketShouldPostPacketJsonAndDecodeBase64Response() throws Exception {
        byte[] responseMessage = "Delivered response".getBytes(StandardCharsets.UTF_8);
        AtomicReference<String> requestBody = new AtomicReference<>();
        startServer(exchange -> {
            requestBody.set(readRequestBody(exchange));
            writeResponse(exchange, 200, Base64.getEncoder().encodeToString(responseMessage));
        });
        OnionPacket packet = createPacket();
        NodeConfig destination = new NodeConfig("node-1", "localhost", server.getAddress().getPort());

        byte[] result = new HttpTransport().sendPacket(destination, packet);

        assertArrayEquals(responseMessage, result);
        assertEquals(packet, OnionPacketDto.fromJson(requestBody.get()).toPacket());
    }

    @Test
    void sendPacketShouldRejectNullInputs() {
        HttpTransport transport = new HttpTransport();
        NodeConfig destination = new NodeConfig("node-1", "localhost", 4444);
        OnionPacket packet = createPacket();

        assertThrows(
                IllegalArgumentException.class,
                () -> transport.sendPacket(null, packet)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> transport.sendPacket(destination, null)
        );
    }

    @Test
    void sendPacketShouldRejectNonSuccessfulStatus() throws Exception {
        startServer(exchange -> writeResponse(exchange, 500, "server failed"));
        NodeConfig destination = new NodeConfig("node-1", "localhost", server.getAddress().getPort());

        assertThrows(
                IllegalStateException.class,
                () -> new HttpTransport().sendPacket(destination, createPacket())
        );
    }

    private void startServer(ExchangeHandler handler) throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/packet", exchange -> {
            if (!"POST".equals(exchange.getRequestMethod())) {
                writeResponse(exchange, 405, "method not allowed");
                return;
            }

            handler.handle(exchange);
        });
        server.setExecutor(Executors.newSingleThreadExecutor());
        server.start();
    }

    private String readRequestBody(HttpExchange exchange) throws IOException {
        return new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
    }

    private void writeResponse(
            HttpExchange exchange,
            int statusCode,
            String responseBody
    ) throws IOException {
        byte[] responseBytes = responseBody.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        exchange.getResponseBody().write(responseBytes);
        exchange.close();
    }

    private OnionPacket createPacket() {
        return new OnionPacket(
                new byte[]{1, 2, 3},
                new byte[12],
                new byte[]{4, 5, 6}
        );
    }

    private interface ExchangeHandler {
        void handle(HttpExchange exchange) throws IOException;
    }
}

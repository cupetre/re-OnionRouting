package Http;

import MessagePackage.DecryptedLayer;
import MessagePackage.OnionPacket;
import MessagePackage.OnionPacketDto;
import Logs.LogLevel;
import Logs.Logger;
import NodeMaterials.MixnetNode;
import NodeMaterials.NodeConfig;
import NodeMaterials.NodeDirectory;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Objects;

public class HttpMixnetNodeServer {
    private final MixnetNode node;
    private final NodeDirectory nodeDirectory;
    private final HttpTransport transport;
    private final HttpServer server;
    private static final String PACKET_ENDPOINT = "/packet";

    public HttpMixnetNodeServer(
            MixnetNode node,
            NodeDirectory nodeDirectory,
            HttpTransport transport) throws IOException {
        this.node = Objects.requireNonNull(node);
        this.nodeDirectory = Objects.requireNonNull(nodeDirectory);
        this.transport = Objects.requireNonNull(transport);


        InetSocketAddress address =
                new InetSocketAddress(
                        node.getConfig().getHost(),
                        node.getConfig().getPort()
                );

        this.server = HttpServer.create(address, 0);
        this.server.createContext(PACKET_ENDPOINT, this::handlePacket);

    }

    public void start() {
        server.start();
        Logger.log(
                node.getNodeId() + " listening on http://" +
                        node.getConfig().getHost() +
                        ":" +
                        node.getConfig().getPort() +
                        PACKET_ENDPOINT,
                LogLevel.Status
        );
    }
    
    public void stop() {
        server.stop(0);
        Logger.log("Stopped HTTP server for " + node.getNodeId(), LogLevel.Info);
    }

    private void handlePacket(HttpExchange exchange) throws IOException {
        try {
            if (!"POST".equals(exchange.getRequestMethod())) {
                writeResponse(exchange, 405, "Only POST is allowed");
                return;
            }

            String json = readRequestBody(exchange);

            OnionPacketDto dto =
                    OnionPacketDto.fromJson(json);

            OnionPacket packet =
                    dto.toPacket();

            DecryptedLayer layer =
                    node.decryptedLayer(packet);

            if (layer.isDeliver()) {
                Logger.log(node.getNodeId() + " received final delivery over HTTP", LogLevel.Success);
                byte[] finalMessage =
                        layer.getFinalMessage();

                String responseBody =
                        Base64.getEncoder().encodeToString(finalMessage);

                writeResponse(exchange, 200, responseBody);
                return;
            }

            Logger.log(node.getNodeId() + " forwarding HTTP packet to " + layer.getNextNodeID(), LogLevel.Status);
            NodeConfig nextConfig =
                    nodeDirectory.getConfig(layer.getNextNodeID());

            byte[] response =
                    transport.sendPacket(
                            nextConfig,
                            layer.getInnerPacket()
                    );

            String responseBody =
                    Base64.getEncoder().encodeToString(response);

            writeResponse(exchange, 200, responseBody);

        } catch (Exception exception) {
            Logger.log(node.getNodeId() + " failed to handle HTTP packet: " + exception.getMessage(), LogLevel.Error);
            writeResponse(
                    exchange,
                    500,
                    exception.getMessage() == null ? "Internal server error" : exception.getMessage()
            );
        }
    }

    private void writeResponse(
            HttpExchange exchange,
            int statusCode,
            String body
    ) throws IOException {
        if (body == null) {
            body = "";
        }

        byte[] responseBytes =
                body.getBytes(StandardCharsets.UTF_8);

        exchange.sendResponseHeaders(
                statusCode,
                responseBytes.length
        );

        exchange.getResponseBody().write(responseBytes);
        exchange.close();
    }

    private String readRequestBody(HttpExchange httpExchange) throws IOException {
        return new String(
                httpExchange.getRequestBody().readAllBytes(),
                StandardCharsets.UTF_8
        );
    }
}

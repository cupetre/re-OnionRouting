package Http;

import CryptoUtil.KeyRegister;
import CryptoUtil.RsaEncryption;
import MessagePackage.OnionMessageBuilder;
import MessagePackage.OnionPacket;
import MessagePackage.OnionPacketDto;
import NodeMaterials.MixnetNode;
import NodeMaterials.NodeConfig;
import NodeMaterials.NodeDirectory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HttpMixnetNodeServerTest {
    private final List<HttpMixnetNodeServer> servers = new ArrayList<>();

    @AfterEach
    void stopServers() {
        for (HttpMixnetNodeServer server : servers) {
            server.stop();
        }
        servers.clear();
    }

    @Test
    void singleHttpNodeShouldDeliverMessage() throws Exception {
        int port = findFreePort();
        KeyPair nodeKeys = RsaEncryption.generateKeyPair();
        NodeConfig config = new NodeConfig("node-1", "localhost", port);
        MixnetNode node = new MixnetNode(config, nodeKeys);
        NodeDirectory directory = new NodeDirectory();
        directory.register(config);
        HttpMixnetNodeServer server = startServer(node, directory);
        byte[] message = "HTTP delivery".getBytes(StandardCharsets.UTF_8);
        OnionPacket packet = OnionMessageBuilder.buildDelivery(
                message,
                nodeKeys.getPublic()
        );

        byte[] response = new HttpTransport().sendPacket(config, packet);

        assertArrayEquals(message, response);
    }

    @Test
    void threeHttpNodesShouldRelayAndDeliverMessage() throws Exception {
        KeyPair node1Keys = RsaEncryption.generateKeyPair();
        KeyPair node2Keys = RsaEncryption.generateKeyPair();
        KeyPair node3Keys = RsaEncryption.generateKeyPair();
        NodeConfig node1Config = new NodeConfig("node-1", "localhost", findFreePort());
        NodeConfig node2Config = new NodeConfig("node-2", "localhost", findFreePort());
        NodeConfig node3Config = new NodeConfig("node-3", "localhost", findFreePort());
        MixnetNode node1 = new MixnetNode(node1Config, node1Keys);
        MixnetNode node2 = new MixnetNode(node2Config, node2Keys);
        MixnetNode node3 = new MixnetNode(node3Config, node3Keys);
        NodeDirectory directory = createDirectory(node1Config, node2Config, node3Config);
        startServer(node1, directory);
        startServer(node2, directory);
        startServer(node3, directory);
        KeyRegister keyRegister = new KeyRegister();
        keyRegister.register("node-1", node1Keys.getPublic());
        keyRegister.register("node-2", node2Keys.getPublic());
        keyRegister.register("node-3", node3Keys.getPublic());
        byte[] message = "HTTP through three nodes".getBytes(StandardCharsets.UTF_8);
        OnionPacket packet = OnionMessageBuilder.buildOnion(
                message,
                List.of("node-1", "node-2", "node-3"),
                keyRegister
        );

        byte[] response = new HttpTransport().sendPacket(node1Config, packet);

        assertArrayEquals(message, response);
    }

    @Test
    void packetEndpointShouldRejectGetRequests() throws Exception {
        int port = findFreePort();
        KeyPair nodeKeys = RsaEncryption.generateKeyPair();
        NodeConfig config = new NodeConfig("node-1", "localhost", port);
        MixnetNode node = new MixnetNode(config, nodeKeys);
        startServer(node, new NodeDirectory());

        HttpResponse<String> response = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/packet"))
                        .GET()
                        .build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        );

        assertEquals(405, response.statusCode());
    }

    @Test
    void packetEndpointShouldRejectBadJson() throws Exception {
        int port = findFreePort();
        KeyPair nodeKeys = RsaEncryption.generateKeyPair();
        NodeConfig config = new NodeConfig("node-1", "localhost", port);
        MixnetNode node = new MixnetNode(config, nodeKeys);
        startServer(node, new NodeDirectory());

        HttpResponse<String> response = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/packet"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString("{bad json", StandardCharsets.UTF_8))
                        .build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        );

        assertEquals(500, response.statusCode());
    }

    @Test
    void relayShouldFailWhenNextNodeIsMissingFromDirectory() throws Exception {
        KeyPair node1Keys = RsaEncryption.generateKeyPair();
        KeyPair node2Keys = RsaEncryption.generateKeyPair();
        NodeConfig node1Config = new NodeConfig("node-1", "localhost", findFreePort());
        NodeConfig node2Config = new NodeConfig("node-2", "localhost", findFreePort());
        MixnetNode node1 = new MixnetNode(node1Config, node1Keys);
        NodeDirectory directory = new NodeDirectory();
        directory.register(node1Config);
        startServer(node1, directory);
        KeyRegister keyRegister = new KeyRegister();
        keyRegister.register("node-1", node1Keys.getPublic());
        keyRegister.register("node-2", node2Keys.getPublic());
        OnionPacket packet = OnionMessageBuilder.buildOnion(
                "missing node".getBytes(StandardCharsets.UTF_8),
                List.of("node-1", "node-2"),
                keyRegister
        );

        assertThrows(
                IllegalStateException.class,
                () -> new HttpTransport().sendPacket(node1Config, packet)
        );
    }

    private HttpMixnetNodeServer startServer(
            MixnetNode node,
            NodeDirectory directory
    ) throws IOException {
        HttpMixnetNodeServer server = new HttpMixnetNodeServer(
                node,
                directory,
                new HttpTransport()
        );
        server.start();
        servers.add(server);
        return server;
    }

    private NodeDirectory createDirectory(NodeConfig... configs) {
        NodeDirectory directory = new NodeDirectory();
        for (NodeConfig config : configs) {
            directory.register(config);
        }
        return directory;
    }

    private int findFreePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }
}

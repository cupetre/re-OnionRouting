package org.example;

import CryptoUtil.KeyRegister;
import CryptoUtil.RsaEncryption;
import Http.HttpMixnetNodeServer;
import Http.HttpTransport;
import Logs.LogLevel;
import Logs.Logger;
import MessagePackage.OnionMessageBuilder;
import MessagePackage.OnionPacket;
import MessagePackage.Router;
import NodeMaterials.MixnetNode;
import NodeMaterials.NodeConfig;
import NodeMaterials.NodeDirectory;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;

public class Main {
    public static void main(String[] args) throws Exception {
        String appMode = System.getenv("APP_MODE");
        boolean modeFromEnvironment = appMode != null && !appMode.isBlank();
        String mode = modeFromEnvironment ? appMode : args.length == 0 ? "memory-demo" : args[0];
        int valueStartIndex = modeFromEnvironment ? 0 : 1;

        if ("http-demo".equalsIgnoreCase(mode)) {
            runHttpDemo(readMessage(args, valueStartIndex));
            return;
        }

        if ("node".equalsIgnoreCase(mode)) {
            runDockerNode(args, valueStartIndex);
            return;
        }

        if ("client".equalsIgnoreCase(mode)) {
            runDockerClient(readMessage(args, valueStartIndex));
            return;
        }

        runMemoryDemo();
    }

    private static void runMemoryDemo() throws Exception {
        Logger.log("Starting in-memory onion routing demo", LogLevel.Status);

        KeyPair node1Keys = RsaEncryption.generateKeyPair();
        KeyPair node2Keys = RsaEncryption.generateKeyPair();
        KeyPair node3Keys = RsaEncryption.generateKeyPair();

        MixnetNode node1 = new MixnetNode(
                new NodeConfig("node-1", "localhost", 4444),
                node1Keys
        );
        MixnetNode node2 = new MixnetNode(
                new NodeConfig("node-2", "localhost", 4445),
                node2Keys
        );
        MixnetNode node3 = new MixnetNode(
                new NodeConfig("node-3", "localhost", 4446),
                node3Keys
        );

        KeyRegister keyRegister = new KeyRegister();
        keyRegister.register(node1.getNodeId(), node1.getPublicKey());
        keyRegister.register(node2.getNodeId(), node2.getPublicKey());
        keyRegister.register(node3.getNodeId(), node3.getPublicKey());

        Router router = new Router();
        router.registerNode(node1);
        router.registerNode(node2);
        router.registerNode(node3);

        List<String> route = List.of("node-1", "node-2", "node-3");
        byte[] message = "Hello professor, this message travelled through onion layers.".getBytes(StandardCharsets.UTF_8);

        Logger.log("Demo route: " + String.join(" -> ", route), LogLevel.Info);
        Logger.log("Building encrypted onion packet", LogLevel.Status);
        OnionPacket onionPacket = OnionMessageBuilder.buildOnion(
                message,
                route,
                keyRegister
        );

        Logger.log("Routing encrypted onion packet", LogLevel.Status);
        byte[] deliveredMessage = router.route(route.get(0), onionPacket);

        Logger.log("Delivered message: " + new String(deliveredMessage, StandardCharsets.UTF_8), LogLevel.Success);
        Logger.log("In-memory onion routing demo finished", LogLevel.Success);
    }

    private static void runHttpDemo(String userMessage) throws Exception {
        Logger.log("Starting HTTP onion routing demo", LogLevel.Status);

        KeyPair node1Keys = RsaEncryption.generateKeyPair();
        KeyPair node2Keys = RsaEncryption.generateKeyPair();
        KeyPair node3Keys = RsaEncryption.generateKeyPair();

        MixnetNode node1 = new MixnetNode(
                new NodeConfig("node-1", "localhost", 4444),
                node1Keys
        );
        MixnetNode node2 = new MixnetNode(
                new NodeConfig("node-2", "localhost", 4445),
                node2Keys
        );
        MixnetNode node3 = new MixnetNode(
                new NodeConfig("node-3", "localhost", 4446),
                node3Keys
        );

        KeyRegister keyRegister = new KeyRegister();
        keyRegister.register(node1.getNodeId(), node1.getPublicKey());
        keyRegister.register(node2.getNodeId(), node2.getPublicKey());
        keyRegister.register(node3.getNodeId(), node3.getPublicKey());

        NodeDirectory nodeDirectory = new NodeDirectory();
        nodeDirectory.register(node1.getConfig());
        nodeDirectory.register(node2.getConfig());
        nodeDirectory.register(node3.getConfig());

        HttpTransport transport = new HttpTransport();
        HttpMixnetNodeServer server1 = new HttpMixnetNodeServer(node1, nodeDirectory, transport);
        HttpMixnetNodeServer server2 = new HttpMixnetNodeServer(node2, nodeDirectory, transport);
        HttpMixnetNodeServer server3 = new HttpMixnetNodeServer(node3, nodeDirectory, transport);

        try {
            Logger.log("Starting HTTP node servers", LogLevel.Status);
            server1.start();
            server2.start();
            server3.start();

            List<String> route = List.of("node-1", "node-2", "node-3");
            byte[] message = userMessage.getBytes(StandardCharsets.UTF_8);

            Logger.log("HTTP demo route: " + String.join(" -> ", route), LogLevel.Info);
            Logger.log("User message accepted, preparing HTTP onion request", LogLevel.Info);
            Logger.log("Building encrypted onion packet for HTTP route", LogLevel.Status);
            OnionPacket onionPacket = OnionMessageBuilder.buildOnion(
                    message,
                    route,
                    keyRegister
            );

            Logger.log("Client sending onion packet to http://localhost:4444/packet", LogLevel.Status);
            byte[] deliveredMessage = transport.sendPacket(
                    nodeDirectory.getConfig(route.get(0)),
                    onionPacket
            );

            Logger.log("HTTP delivered message: " + new String(deliveredMessage, StandardCharsets.UTF_8), LogLevel.Success);
            Logger.log("HTTP onion routing demo finished", LogLevel.Success);
        } finally {
            Logger.log("Stopping HTTP node servers", LogLevel.Status);
            server1.stop();
            server2.stop();
            server3.stop();
        }
    }

    private static void runDockerNode(String[] args, int valueStartIndex) throws Exception {
        String nodeID = readNodeID(args, valueStartIndex);
        DockerDemoConfig dockerDemoConfig = new DockerDemoConfig();

        MixnetNode node = new MixnetNode(
                dockerDemoConfig.createServerConfig(nodeID),
                dockerDemoConfig.keyPairFor(nodeID)
        );

        HttpTransport transport = new HttpTransport();
        HttpMixnetNodeServer server = new HttpMixnetNodeServer(
                node,
                dockerDemoConfig.createNodeDirectory(),
                transport
        );

        Logger.log("Starting Docker node mode for " + nodeID, LogLevel.Status);
        server.start();
        new CountDownLatch(1).await();
    }

    private static void runDockerClient(String userMessage) throws Exception {
        DockerDemoConfig dockerDemoConfig = new DockerDemoConfig();
        List<String> route = dockerDemoConfig.getRoute();

        Logger.log("Starting Docker client mode", LogLevel.Status);
        Logger.log("Docker route: " + String.join(" -> ", route), LogLevel.Info);

        byte[] message = userMessage.getBytes(StandardCharsets.UTF_8);
        OnionPacket onionPacket = OnionMessageBuilder.buildOnion(
                message,
                route,
                dockerDemoConfig.createKeyRegister()
        );

        HttpTransport transport = new HttpTransport();
        byte[] deliveredMessage = transport.sendPacket(
                dockerDemoConfig.createNodeDirectory().getConfig(route.get(0)),
                onionPacket
        );

        Logger.log(
                "Docker delivered message: " + new String(deliveredMessage, StandardCharsets.UTF_8),
                LogLevel.Success
        );
    }

    private static String readNodeID(String[] args, int valueStartIndex) {
        if (args.length > valueStartIndex && !args[valueStartIndex].isBlank()) {
            return args[valueStartIndex];
        }

        String nodeID = System.getenv("NODE_ID");

        if (nodeID == null || nodeID.isBlank()) {
            throw new IllegalArgumentException("Node id must be passed as an argument or NODE_ID env variable");
        }

        return nodeID;
    }

    private static String readMessage(String[] args, int valueStartIndex) {
        if (args.length > valueStartIndex) {
            StringBuilder messageBuilder = new StringBuilder();

            for (int i = valueStartIndex; i < args.length; i++) {
                if (!messageBuilder.isEmpty()) {
                    messageBuilder.append(" ");
                }

                messageBuilder.append(args[i]);
            }

            String message = messageBuilder.toString();

            if (!message.isBlank()) {
                return message;
            }
        }

        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter message to send through the HTTP onion route: ");

        String message = scanner.nextLine();

        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("Message cannot be empty");
        }

        return message;
    }
}

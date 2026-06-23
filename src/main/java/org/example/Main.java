package org.example;

import Http.HttpMixnetNodeServer;
import Http.HttpTransport;
import Logs.LogLevel;
import Logs.Logger;
import MessagePackage.OnionMessageBuilder;
import MessagePackage.OnionPacket;
import NodeMaterials.MixnetNode;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;

public class Main {
    public static void main(String[] args) throws Exception {
        String nodeID = System.getenv("NODE_ID");

        if (nodeID != null && !nodeID.isBlank()) {
            runNode(nodeID);
            return;
        }

        if (args.length > 0) {
            runClient(messageFromArguments(args));
            return;
        }

        runInteractiveClient();
    }

    private static void runNode(String nodeID) throws Exception {
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

        Logger.log("Starting mixnet node " + nodeID, LogLevel.Status);
        server.start();
        new CountDownLatch(1).await();
    }

    private static void runClient(String userMessage) throws Exception {
        DockerDemoConfig dockerDemoConfig = new DockerDemoConfig();
        List<String> route = dockerDemoConfig.getRoute();

        Logger.log("Starting mixnet client", LogLevel.Status);
        Logger.log("Route: " + String.join(" -> ", route), LogLevel.Info);

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
                "Delivered message: " + new String(deliveredMessage, StandardCharsets.UTF_8),
                LogLevel.Success
        );
    }

    private static void runInteractiveClient() throws Exception {
        Scanner scanner = new Scanner(System.in);
        Logger.log("Interactive client started. Type exit to stop.", LogLevel.Status);

        while (true) {
            System.out.print("Enter message: ");

            if (!scanner.hasNextLine()) {
                return;
            }

            String message = scanner.nextLine();

            if ("exit".equalsIgnoreCase(message.trim())) {
                Logger.log("Stopping interactive client", LogLevel.Status);
                return;
            }

            if (message.isBlank()) {
                System.out.println("Message cannot be empty.");
                continue;
            }

            runClient(message);
        }
    }

    private static String messageFromArguments(String[] args) {
        StringBuilder messageBuilder = new StringBuilder();

        for (String argument : args) {
            if (!messageBuilder.isEmpty()) {
                messageBuilder.append(" ");
            }

            messageBuilder.append(argument);
        }

        String message = messageBuilder.toString();

        if (message.isBlank()) {
            throw new IllegalArgumentException("Message cannot be empty");
        }

        return message;
    }
}

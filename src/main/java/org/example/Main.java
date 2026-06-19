package org.example;

import CryptoUtil.KeyRegister;
import CryptoUtil.RsaEncryption;
import Logs.LogLevel;
import Logs.Logger;
import MessagePackage.OnionMessageBuilder;
import MessagePackage.OnionPacket;
import MessagePackage.Router;
import NodeMaterials.MixnetNode;
import NodeMaterials.NodeConfig;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.util.List;

public class Main {
    public static void main(String[] args) throws Exception {
        Logger.log("Starting onion routing demo", LogLevel.Status);

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
        Logger.log("Onion routing demo finished", LogLevel.Success);
    }
}

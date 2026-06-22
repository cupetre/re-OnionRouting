package Http;

import MessagePackage.OnionPacket;
import MessagePackage.OnionPacketDto;
import NodeMaterials.NodeConfig;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

public class HttpTransport {
    private static final String PACKET_ENDPOINT = "/packet";
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);

    public byte[] sendPacket(
            NodeConfig destination,
            OnionPacket packet
    ) throws IOException, InterruptedException {
        if (destination == null) {
            throw new IllegalArgumentException("destination is null ,fnd error in packet or destination node entrance");
        }

        if (packet == null) {
            throw new IllegalArgumentException("Packet cannot be null");
        }

        // first we convert it
        OnionPacketDto dto = OnionPacketDto.fromPacket(packet);

        // now in json style
        String json = dto.toJson();

        URI uri = URI.create(
                "http://" + destination.getHost() + ":" + destination.getPort() + PACKET_ENDPOINT
        );

        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(REQUEST_TIMEOUT)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();

        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> response =
                client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        if ( response.statusCode() != 200 ) {
            throw new IllegalStateException(" http request is failing with smth other than 200 status " +
                    response.statusCode() + " that being this code " + response.body());
        }

        return Base64.getDecoder().decode(response.body());
    }

}


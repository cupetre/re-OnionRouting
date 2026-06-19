package MessagePackage;

public class OnionPacketDto {
    private final String encryptedAesKey;
    private final String iv;
    private final String encryptedPayload;

    public OnionPacketDto(String encryptedAesKey, String iv, String encryptedPayload) {
        this.encryptedAesKey = encryptedAesKey;
        this.iv = iv;
        this.encryptedPayload = encryptedPayload;
    }

    public static OnionPacketDto fromPacket(OnionPacket packet) {

    }

    public OnionPacket toPacket() {
        
    }
}

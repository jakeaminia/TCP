import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class Packet {

    private int sequenceNumber;
    private int acknowledgment;
    private long timestamp;
    private int length;
    private boolean SYN;
    private boolean FIN;
    private boolean ACK;
    private short checksum;
    private byte[] data;

    public Packet(int sequenceNumber, int acknowledgment, long timestamp, int length, boolean SYN, boolean FIN,
            boolean ACK, byte[] data) {
        this.sequenceNumber = sequenceNumber;
        this.acknowledgment = acknowledgment;
        this.timestamp = timestamp;
        this.length = length;
        this.SYN = SYN;
        this.FIN = FIN;
        this.ACK = ACK;
        this.data = data;
    }

    public Packet(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        buffer.order(ByteOrder.BIG_ENDIAN);

        this.sequenceNumber = buffer.getInt();
        this.acknowledgment = buffer.getInt();
        this.timestamp = buffer.getLong();

        int flagsEncoded = buffer.getInt();
        this.length = flagsEncoded >>> 3;
        this.SYN = (flagsEncoded & 0b100) != 0;
        this.FIN = (flagsEncoded & 0b010) != 0;
        this.ACK = (flagsEncoded & 0b001) != 0;

        buffer.getShort(); // 0s before checksum
        this.checksum = buffer.getShort();

        if (this.length > 0) {
            this.data = new byte[this.length];
            buffer.get(this.data, 0, this.length);
        }
    }

    public byte[] toBytes() {
        int headerSide = 4 + 4 + 8 + 4 + 4;
        int totalSize = headerSide + (this.data != null ? this.data.length : 0);

        ByteBuffer buffer = ByteBuffer.allocate(totalSize);
        buffer.order(ByteOrder.BIG_ENDIAN);

        buffer.putInt(this.sequenceNumber);
        buffer.putInt(this.acknowledgment);
        buffer.putLong(this.timestamp);

        int flagsEncoded = (this.length << 3);
        if (this.SYN)
            flagsEncoded |= 0b100;
        if (this.FIN)
            flagsEncoded |= 0b010;
        if (this.ACK)
            flagsEncoded |= 0b001;
        buffer.putInt(flagsEncoded);

        buffer.putInt(0); // blank checksum field before calculation

        if (this.data != null) {
            buffer.put(this.data);
        }

        this.checksum = this.computeChecksum(buffer.array());
        buffer.putShort(22, this.checksum); // at index 22 and not 20 because of 0s padding

        return buffer.array();
    }

    public short computeChecksum(byte[] bytes) {
        int sum = 0;

        for (int i = 0; i < bytes.length; i += 2) {
            int high = (bytes[i] & 0xFF);
            int low = (i + 1 < bytes.length) ? (bytes[i + 1] & 0xFF) : 0;
            sum += (high << 8) | low;
            if ((sum & 0xFFFF0000) != 0) {
                sum = (sum & 0xFFFF) + 1;
            }
        }

        return (short) ~(sum & 0xFFFF);
    }
}

import java.net.DatagramPacket;

public class Receiver extends Host {

    public Receiver(int port, int maxTransmitUnits, int slidingWindowSize, String fileName) {
        super(port, maxTransmitUnits, slidingWindowSize, fileName);
    }

    public boolean listen() {
        try {
            // Wait for SYN 0
            this.receive(Packet.HEADER_SIZE);

            // Send SYN 0 ACK 1
            byte[] synAckBytes = this.syn().toBytes();
            this.socket.send(
                    new DatagramPacket(synAckBytes, Packet.HEADER_SIZE, this.remoteIP, this.remotePort));

            // Wait for ACK 1
            this.receive(Packet.HEADER_SIZE);

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }
}

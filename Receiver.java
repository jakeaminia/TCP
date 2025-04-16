import java.net.DatagramPacket;

public class Receiver extends Host {

    public Receiver(int port, int maxTransmitUnits, int slidingWindowSize, String fileName) {
        super(port, maxTransmitUnits, slidingWindowSize, fileName);
    }

    public void listen() {

    }

    public boolean connect() {
        if (this.isConnected()) {
            return true;
        }

        try {
            // Wait for SYN 0
            this.receive(Packet.HEADER_SIZE);

            this.socket.connect(this.remoteIP, this.remotePort);

            // Send SYN 0 ACK 1
            this.socket.send(new DatagramPacket(syn().toBytes(), Packet.HEADER_SIZE, this.remoteIP, this.remotePort));

            // Wait for ACK 1
            this.receive(Packet.HEADER_SIZE);

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        this.setConnected(true);
        return true;
    }

    public boolean disconnect() {
        if (this.isConnected()) {
            return true;
        }

        try {
            // Wait for FIN
            this.receive(Packet.HEADER_SIZE);

            // Send ACK
            this.socket.send(new DatagramPacket(ack().toBytes(), Packet.HEADER_SIZE, this.remoteIP, this.remotePort));

            // Send FIN
            this.socket.send(new DatagramPacket(fin().toBytes(), Packet.HEADER_SIZE, this.remoteIP, this.remotePort));

            // Wait for ACK
            this.receive(Packet.HEADER_SIZE);

            // Disconnect
            this.socket.disconnect();
            this.setConnected(false);
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}

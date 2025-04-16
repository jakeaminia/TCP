import java.net.DatagramPacket;
import java.net.InetAddress;

public class Sender extends Host {

    public Sender(int port, String remoteIP, int remotePort, String fileName, int maxTransmitUnits,
            int slidingWindowSize) {
        super(port, maxTransmitUnits, slidingWindowSize, fileName);
        this.remotePort = remotePort;

        try {
            this.remoteIP = InetAddress.getByName(remoteIP);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void run() {

    }

    /**
     * Conducts the sender side of the TCP 3-way handshake
     * 
     * @return Success of connection
     */
    public boolean connect() {
        if (this.isConnected()) {
            return true;
        }

        try {

            this.socket.connect(this.remoteIP, this.remotePort);

            // Send SYN 0
            this.socket.send(new DatagramPacket(syn().toBytes(), Packet.HEADER_SIZE, this.remoteIP, this.remotePort));

            // Wait for ACK 1, SYN 0
            this.receive(Packet.HEADER_SIZE);

            // Send ACK 1
            this.socket.send(new DatagramPacket(ack().toBytes(), Packet.HEADER_SIZE, this.remoteIP, this.remotePort));

        } catch (Exception e) {
            this.socket.disconnect();
            e.printStackTrace();
            return false;
        }

        this.setConnected(true);
        return true;
    }

    public boolean disconnect() {
        if (!this.isConnected) {
            return true;
        }

        try {
            // Send FIN
            this.socket.send(new DatagramPacket(fin().toBytes(), Packet.HEADER_SIZE, this.remoteIP, this.remotePort));

            // Wait for ACK
            this.receive(Packet.HEADER_SIZE);

            // Wait for FIN
            this.receive(Packet.HEADER_SIZE);

            // Send ACK
            this.socket.send(new DatagramPacket(ack().toBytes(), Packet.HEADER_SIZE, this.remoteIP, this.remotePort));

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

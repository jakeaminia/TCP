import java.net.DatagramPacket;
import java.net.InetAddress;

public class Sender extends Host {
    private int remotePort;
    private InetAddress remoteIP;

    public InetAddress getRemoteIP() {
        return remoteIP;
    }

    public int getRemotePort() {
        return remotePort;
    }

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
            byte[] synBytes = syn(0, 0).toBytes();
            this.socket.send(new DatagramPacket(synBytes, Packet.HEADER_SIZE, this.remoteIP, this.remotePort));

            // Wait for ACK 1, SYN 0
            DatagramPacket synAckDatagram = new DatagramPacket(new byte[Packet.HEADER_SIZE], Packet.HEADER_SIZE);
            this.socket.receive(synAckDatagram);
            Packet synAck = new Packet(synAckDatagram.getData());

            // Send ACK 1
            byte[] ackBytes = ack(synAck.getSequenceNumber() + 1).toBytes();
            this.socket.send(new DatagramPacket(ackBytes, Packet.HEADER_SIZE, this.remoteIP, this.remotePort));

        } catch (Exception e) {
            this.socket.disconnect();
            e.printStackTrace();
            return false;
        }

        this.setConnected(true);
        return true;
    }
}

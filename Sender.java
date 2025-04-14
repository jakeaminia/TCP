import java.io.IOException;
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

    public boolean connect() {
        if (this.isConnected()) {
            return true;
        }

        this.socket.connect(this.remoteIP, this.remotePort);

        // 3-way handshake
        // Send SYN 0
        byte[] synBytes = syn(0, 0).toBytes();
        try {
            this.socket.send(new DatagramPacket(synBytes, synBytes.length, this.remoteIP, this.remotePort));
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        // Wait for ACK 1, SYN 0
        // Send ACK 1

        this.setConnected(true);
        return true;
    }
}

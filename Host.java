import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public abstract class Host {

    protected int port;
    protected int maxTransmitUnits;
    protected int slidingWindowSize;
    protected String fileName;
    protected DatagramSocket socket;
    protected boolean isConnected;
    protected int remotePort;
    protected InetAddress remoteIP;

    protected int sequenceNumber;
    protected int acknowledgment;

    public InetAddress getRemoteIP() {
        return remoteIP;
    }

    public int getRemotePort() {
        return remotePort;
    }

    public int getPort() {
        return port;
    }

    public int getMaxTransmitUnits() {
        return maxTransmitUnits;
    }

    public int getSlidingWindowSize() {
        return slidingWindowSize;
    }

    public String getFileName() {
        return fileName;
    }

    public DatagramSocket getSocket() {
        return socket;
    }

    public boolean isConnected() {
        return isConnected;
    }

    public void setConnected(boolean isConnected) {
        this.isConnected = isConnected;
    }

    protected Host(int port, int maxTransmitUnits, int slidingWindowSize, String fileName) {
        this.port = port;
        this.fileName = fileName;
        this.maxTransmitUnits = maxTransmitUnits;
        this.slidingWindowSize = slidingWindowSize;
        this.sequenceNumber = 0;
        this.acknowledgment = 0;

        try {
            this.socket = new DatagramSocket(this.port);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public Packet syn() {
        Packet packet = new Packet(this.sequenceNumber, this.acknowledgment, System.nanoTime(), 0, true, false,
                acknowledgment != 0, null);
        this.sequenceNumber += 1;
        return packet;
    }

    public Packet fin() {
        return new Packet(this.sequenceNumber, this.acknowledgment, System.nanoTime(), 0, false, true,
                acknowledgment != 0, null);
    }

    public Packet ack() {
        return new Packet(0, this.acknowledgment, System.nanoTime(), 0, false, false, true, null);
    }

    public Packet receive(int length) throws IOException {
        DatagramPacket datagram = new DatagramPacket(new byte[length], length);
        this.socket.receive(datagram);
        Packet result = new Packet(datagram.getData());
        this.remoteIP = datagram.getAddress();
        this.remotePort = datagram.getPort();
        this.acknowledgment = result.getSequenceNumber() + result.getLength() + 1; // TODO: Use buffer
        return result;
    }
}
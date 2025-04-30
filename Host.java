import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

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

    protected byte[] file;
    protected FileOutputStream output;

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

    protected void loadFile() {
        try {
            this.file = Files.readAllBytes(Paths.get(fileName));
        } catch (IOException e) {
            System.err.println("Could not read file: " + fileName);
        }
    }
    
    protected void openOutput() {
        try {
            this.output = new FileOutputStream(fileName);
        } catch (IOException e) {
            System.err.println("Could not open file for writing: " + fileName);
        }
    }
    
    protected void write(byte[] data) {
        try {
            if (output != null) output.write(data);
        } catch (IOException e) {
            e.printStackTrace();
        }
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
        this.acknowledgment = result.getSequenceNumber() + result.getLength(); // TODO: Use buffer
        return result;
    }


    public void log(String action, Packet p) {
        String flags = (p.isSYN() ? "S" : "-") +
                       (p.isFIN() ? "F" : "-") +
                       (p.isACK() ? "A" : "-") +
                       ((p.getLength() > 0) ? "D" : "-");
        double now = System.nanoTime() / 1_000_000_000.0;
        int len = p.getLength();
        System.out.printf("%s %.3f %s %d %d %d\n", action, now, flags,
                          p.getSequenceNumber(), len, p.getAcknowledgment());
    }
    
}

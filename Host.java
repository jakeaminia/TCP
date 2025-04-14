import java.net.DatagramSocket;

public abstract class Host {

    private int port;
    private int maxTransmitUnits;
    private int slidingWindowSize;
    private String fileName;
    protected DatagramSocket socket;
    private boolean isConnected;

    protected int sequenceNumber;
    protected int acknowledgment;

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

    public static Packet syn(int sequenceNumber, int acknowledgment) {
        return new Packet(sequenceNumber, acknowledgment, System.nanoTime(), 0, true, false, acknowledgment != 0, null);
    }

    public static Packet fin(int sequenceNumber, int acknowledgment) {
        return new Packet(sequenceNumber, acknowledgment, System.nanoTime(), 0, false, true, acknowledgment != 0, null);
    }

    public static Packet ack(int acknowledgment) {
        return new Packet(0, acknowledgment, System.nanoTime(), 0, false, false, true, null);
    }
}
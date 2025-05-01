import java.net.DatagramPacket;
import java.util.Map;
import java.util.TreeMap;

public class Receiver extends Host {

    private int expectedSeq = 1;
    private int bytesReceived = 0;
    private int outOfOrderCount = 0;
    private int badChecksumCount = 0;
    private Map<Integer, Packet> buffer = new TreeMap<>();

    public Receiver(int port, int maxTransmitUnits, int slidingWindowSize, String fileName) {
        super(port, maxTransmitUnits, slidingWindowSize, fileName);
    }

    public void listen() {
        while (true) {
            try {
                Packet packet = this.receive(this.maxTransmitUnits + Packet.HEADER_SIZE);
                this.log("rcv", packet);
    
                // End on FIN but only after processing all data
                if (packet.isFIN()) {
                    this.send(ack());
                    this.setConnected(false);
                    break;
                }
    
                if (!packet.isValidChecksum()) {
                    badChecksumCount++;
                    continue;
                }
    
                if (packet.getSequenceNumber() > expectedSeq) {
                    buffer.put(packet.getSequenceNumber(), packet);
                    outOfOrderCount++;
                } else if (packet.getSequenceNumber() == expectedSeq) {
                    this.write(packet.getData());
                    bytesReceived += packet.getLength();
                    expectedSeq += packet.getLength();
    
                    while (buffer.containsKey(expectedSeq)) {
                        Packet next = buffer.remove(expectedSeq);
                        this.write(next.getData());
                        bytesReceived += next.getLength();
                        expectedSeq += next.getLength();
                    }
                }
    
                this.send(new Packet(0, expectedSeq, packet.getTimestamp(), 0, false, false, true, null));
    
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    
        try {
            if (this.output != null) this.output.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    
        printStats();
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
        this.openOutput();
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

    private void printStats() {
        System.out.println("Data received: " + bytesReceived + " bytes");
        System.out.println("Out-of-order packets discarded: " + outOfOrderCount);
        System.out.println("Packets with bad checksum: " + badChecksumCount);
    }
}

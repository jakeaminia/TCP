import java.net.DatagramPacket;
import java.util.Map;
import java.util.TreeMap;
import java.io.IOException;

public class Receiver extends Host {

    private int expectedSeq = 0;
    private int bytesReceived = 0;
    private int outOfOrderCount = 0;
    private int badChecksumCount = 0;
    private int packetsReceived = 0;
    private Map<Integer, Packet> buffer = new TreeMap<>();

    public Receiver(int port, int maxTransmitUnits, int slidingWindowSize, String fileName) {
        super(port, maxTransmitUnits, slidingWindowSize, fileName);
    }

    public void listen() {
        while (true) {
            try {
                Packet packet = this.receive(this.maxTransmitUnits + Packet.HEADER_SIZE);
                this.log("rcv", packet);
    
                if (!packet.isValidChecksum()) {
                    badChecksumCount++;
                    continue;
                }

                packetsReceived++;
    
                // System.out.println("RCV_RAW: seq=" + packet.getSequenceNumber() +
                //                    " ack=" + packet.getAcknowledgment() +
                //                    " FIN=" + packet.isFIN() + " SYN=" + packet.isSYN() +
                //                    " ACK=" + packet.isACK() + " len=" + packet.getLength());
    
                // System.out.println("Received packet with seq=" + packet.getSequenceNumber() +
                //                    ", ack=" + packet.getAcknowledgment() +
                //                    ", FIN=" + packet.isFIN());
    
                // End on FIN but only after processing all data
                if (packet.isFIN()) {
                    // System.out.println("Receiver: Received FIN.");
    
                    this.send(ack());
                    Packet ourFin = fin();
                    this.send(ourFin);    
                    Packet finalAck = this.receive(Packet.HEADER_SIZE);
                    this.log("rcv", finalAck);
                    this.socket.disconnect();
                    this.setConnected(false);
                    // System.out.println("Receiver: Connection closed.");
                    break;
                }
    
                if (packet.getSequenceNumber() > expectedSeq) {
                    buffer.put(packet.getSequenceNumber(), packet);
                    outOfOrderCount++;
                } else if (packet.getSequenceNumber() == expectedSeq) {
                    // System.out.println("Attempting to write data of length: " + packet.getLength());
                    this.write(packet.getData());
                    // System.out.println("Write completed");
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
        // System.out.println("Receiver.connect(): fileName = '" + this.fileName + "'");
        try {
            this.receive(Packet.HEADER_SIZE);

            this.socket.connect(this.remoteIP, this.remotePort);

            this.socket.send(new DatagramPacket(syn().toBytes(), Packet.HEADER_SIZE, this.remoteIP, this.remotePort));

            this.receive(Packet.HEADER_SIZE);

            this.openOutput();

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        this.setConnected(true);
        return true;
    }


    private void printStats() {
        System.out.println("Data received: " + bytesReceived + " bytes");
        System.out.println("Packets received: " + packetsReceived);
        System.out.println("Out-of-order packets discarded: " + outOfOrderCount);
        System.out.println("Packets with bad checksum: " + badChecksumCount);
    }
}

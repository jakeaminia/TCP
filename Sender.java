import java.net.DatagramPacket;
import java.net.InetAddress;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

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
        this.loadFile(); // Load input file to byte array

        int fileLen = file.length;
        int baseSeq = 1;
        int nextSeq = 1;
        int mtu = maxTransmitUnits;

        long estimatedRTT = 5_000_000_000L;
        long devRTT = 0;
        long timeout = 2 * estimatedRTT;

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        Map<Integer, ScheduledFuture<?>> timers = new ConcurrentHashMap<>();
        Map<Integer, Packet> unacked = new ConcurrentHashMap<>();

        int dupAckCount = 0;
        int lastAck = 0;
        int bytesSent = 0;
        int retransmits = 0;
        int dupAcks = 0;

        try {
            while (baseSeq < fileLen || !unacked.isEmpty()) {

                // Send while inside sliding window
                while (nextSeq < fileLen && (nextSeq - baseSeq) / mtu < slidingWindowSize) {
                    int chunkSize = Math.min(mtu, fileLen - nextSeq);
                    byte[] chunk = Arrays.copyOfRange(file, nextSeq, nextSeq + chunkSize);

                    Packet pkt = new Packet(nextSeq, 0, System.nanoTime(), chunkSize, false, false, true, chunk);
                    this.send(pkt);
                    this.log("snd", pkt);

                    unacked.put(pkt.getSequenceNumber(), pkt);

                    int seq = pkt.getSequenceNumber();
                    ScheduledFuture<?> task = scheduler.schedule(() -> {
                        try {
                            System.out.println("Timeout: retransmitting seq " + seq);
                            this.send(pkt);
                            this.log("snd", pkt);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }, timeout, TimeUnit.NANOSECONDS);

                    ScheduledFuture<?> prev = timers.put(seq, task);
                    if (prev != null) prev.cancel(false);

                    nextSeq += chunkSize;
                    bytesSent += chunkSize;
                }

                // Wait for ACK
                Packet ackPkt = this.receive(Packet.HEADER_SIZE);
                this.log("rcv", ackPkt);
                int ackNum = ackPkt.getAcknowledgment();

                // RTT estimate if new ACK
                if (ackNum > lastAck) {
                    long sampleRTT = System.nanoTime() - ackPkt.getTimestamp();
                    long diff = Math.abs(sampleRTT - estimatedRTT);
                    estimatedRTT = (long) (0.875 * estimatedRTT + 0.125 * sampleRTT);
                    devRTT = (long) (0.75 * devRTT + 0.25 * diff);
                    timeout = estimatedRTT + 4 * devRTT;

                    lastAck = ackNum;
                    dupAckCount = 0;

                    // Remove acked packets
                    Iterator<Integer> it = unacked.keySet().iterator();
                    while (it.hasNext()) {
                        int seq = it.next();
                        Packet p = unacked.get(seq);
                        if (seq + p.getLength() <= ackNum) {
                            ScheduledFuture<?> t = timers.remove(seq);
                            if (t != null) t.cancel(false);
                            it.remove();
                        }
                    }

                    baseSeq = ackNum;

                } else if (ackNum == lastAck) {
                    dupAckCount++;
                    if (dupAckCount == 3 && unacked.containsKey(ackNum)) {
                        System.out.println("Fast retransmit for seq " + ackNum);
                        this.send(unacked.get(ackNum));
                        this.log("snd", unacked.get(ackNum));
                        retransmits++;
                        dupAcks++;
                        dupAckCount = 0;
                    }
                }
            }

            scheduler.shutdownNow();
            System.out.println("File transfer complete.");
            System.out.println("Total bytes sent: " + bytesSent);
            System.out.println("Retransmissions: " + retransmits);
            System.out.println("Duplicate ACKs: " + dupAcks);

            this.disconnect();

        } catch (Exception e) {
            e.printStackTrace();
            scheduler.shutdownNow();
        }
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
            System.out.println("Sender: Sending FIN...");
    
            // Step 1: Build and send FIN properly
            Packet finPkt = fin();
            byte[] finBytes = finPkt.toBytes();
            DatagramPacket dp = new DatagramPacket(finBytes, finBytes.length, this.remoteIP, this.remotePort);
            this.socket.send(dp);
            this.log("snd", finPkt);  // log the sent FIN
    
            // Step 2: Wait for ACK
            Packet ackPkt = this.receive(Packet.HEADER_SIZE + 100);  // 100 extra in case it's padded
            this.log("rcv", ackPkt);
    
            // Step 3: Wait for FIN from receiver
            Packet theirFin = this.receive(Packet.HEADER_SIZE + 100);
            this.log("rcv", theirFin);
    
            // Step 4: Send final ACK
            this.send(ack());
            this.log("snd", ack());
    
            this.socket.disconnect();
            this.setConnected(false);
            return true;
    
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

}

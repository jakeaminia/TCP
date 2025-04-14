public class Receiver extends Host {

    public Receiver(int port, int maxTransmitUnits, int slidingWindowSize, String fileName) {
        super(port, maxTransmitUnits, slidingWindowSize, fileName);
    }

    public void listen() {
    }
}

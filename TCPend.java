public class TCPend {
    public static void main(String[] args) {
        if (checkSenderArgs(args)) {

        Sender sender = new Sender(Integer.parseInt(args[1]), args[3], Integer.parseInt(args[5]), args[7],
            Integer.parseInt(args[9]), Integer.parseInt(args[11]));

        if (sender.connect()) {
            sender.run();
        } else {
            System.err.println("Connection failed.");
        }

        } else if (checkReceiverArgs(args)) {

            Receiver receiver = new Receiver(Integer.parseInt(args[1]), Integer.parseInt(args[3]),
            Integer.parseInt(args[5]), args[7]);

            if (receiver.connect()) {
                receiver.listen();
            } else {
                System.err.println("Receiver failed to connect.");
            }

        } else {
            System.err.println("Improper argument format.");
        }
    }

    public static boolean checkSenderArgs(String[] args) {
        return args.length == 12 && args[0].equals("-p") && args[2].equals("-s") && args[4].equals("-a")
                && args[6].equals("-f") && args[8].equals("-m") && args[10].equals("-c");
    }

    public static boolean checkReceiverArgs(String[] args) {
        return args.length == 8 && args[0].equals("-p") && args[2].equals("-m") && args[4].equals("-c")
                && args[6].equals("-f");
    };

}
package printer;

public class NetworkLabelPrinter implements LabelPrinter {

    private final String ipAddress;
    private final int port;

    public NetworkLabelPrinter(String ipAddress) {
        this.ipAddress = ipAddress;
        this.port = 9100;
    }

    public NetworkLabelPrinter(String ipAddress, int port) {
        this.ipAddress = ipAddress;
        this.port = port;
    }

}

package printer;

import com.zebra.sdk.comm.ConnectionException;

public class Base64ContentPrinter extends Thread {

    private final ZebraLabelPrinter printer;
    private final String content;

    public Base64ContentPrinter(ZebraLabelPrinter printer, String base64content) {
        this.printer = printer;
        this.content = base64content;
    }

    @Override
    public void run() {
        synchronized (this.printer) {
            try {
                this.printer.printBase64Content(this.content);
            } catch (ConnectionException e) {
                e.printStackTrace();
            }
        }
    }
}

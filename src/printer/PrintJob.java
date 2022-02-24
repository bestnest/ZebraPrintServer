package printer;

import java.util.Objects;
import com.zebra.sdk.comm.ConnectionException;


public class PrintJob extends Thread {

    private final ZebraLabelPrinter printer;
    private final String printCode;
    private final String mode;

    /***
     * Creates a PrintJob with a specified mode. The available modes are "base64" and "raw".
     * A job with "base64" mode will decode the labelCode string and then send the commands to the printer.
     * A job with "raw" mode will send the labelCode straight to the printer.
     * @param printer The printer object to use
     * @param labelCode the base64 encoded or plain text printer code to print
     * @param mode The mode
     */
    public PrintJob(ZebraLabelPrinter printer, String labelCode, String mode) {
        this.printer = printer;
        this.printCode = labelCode;
        this.mode = mode;
    }

    @Override
    public void run() {
        synchronized (this.printer) {
            try {
                if (Objects.equals(this.mode, "base64")) {
                    this.printer.printBase64Content(this.printCode);
                } else if (Objects.equals(this.mode, "raw")) {
                    this.printer.print(this.printCode);
                }
            } catch (ConnectionException e) {
                e.printStackTrace();
            }
        }
    }
}

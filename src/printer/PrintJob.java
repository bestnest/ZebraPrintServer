package printer;

import java.util.Objects;
import java.io.IOException;
import com.zebra.sdk.comm.ConnectionException;
import com.zebra.sdk.printer.ZebraPrinterLanguageUnknownException;


public class PrintJob {

    private final String mode;
    private final String name;
    private final String printCode;
    private final LabelPrinter printer;

    /***
     * Creates a PrintJob with a specified mode. The available modes are "base64", "raw", and "image".
     * A job with "base64" mode will decode the labelCode string and then send the commands to the printer.
     * A job with "raw" mode will send the labelCode straight to the printer.
     * A job in "image" mode will decode the printData and send it as an image to the printer.
     * @param printer The printer object to use
     * @param printData the base64 encoded or plain text printer code to print
     * @param mode The mode. Options are "base64", "raw", and "image"
     */
    public PrintJob(LabelPrinter printer, String printData, String mode, String name) {
        this.mode = mode;
        this.name = name;
        this.printer = printer;
        this.printCode = printData;
    }

    public void doittoit() {
        try {
            System.out.println(this.name + " job is printing now.");
            if (Objects.equals(this.mode, "base64")) {
                this.printer.printBase64Content(this.printCode);
            } else if (Objects.equals(this.mode, "raw")) {
                this.printer.print(this.printCode);
            } else if (Objects.equals(this.mode, "image")) {
                this.printer.printImage(this.printCode);
            }
        } catch (ConnectionException | ZebraPrinterLanguageUnknownException | IOException e) {
            e.printStackTrace();
        }
    }

}

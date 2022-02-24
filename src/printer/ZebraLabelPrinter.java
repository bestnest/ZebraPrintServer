package printer;


import java.io.InputStream;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import com.zebra.sdk.comm.Connection;
import java.nio.charset.StandardCharsets;
import com.zebra.sdk.graphics.ZebraImageI;
import com.zebra.sdk.printer.ZebraPrinter;
import com.zebra.sdk.comm.ConnectionException;
import org.apache.commons.codec.binary.Base64;
import com.zebra.sdk.graphics.ZebraImageFactory;
import com.zebra.sdk.printer.ZebraPrinterFactory;
import com.zebra.sdk.printer.ZebraPrinterLanguageUnknownException;


public class ZebraLabelPrinter {

    private final Connection connection;

    public ZebraLabelPrinter(Connection printerConnection) {
        this.connection = printerConnection;
    }

    public void print(byte[] data) throws ConnectionException {
        this.connection.open();
        this.connection.write(data);
        this.connection.close();
    }

    public void print(String data) throws ConnectionException {
        this.print(data.getBytes(StandardCharsets.UTF_8));
    }

    public void printBase64Content(String base64encoded) throws ConnectionException {
        byte[] decoded = Base64.decodeBase64(base64encoded);
        this.print(decoded);
    }

    public void printImage(String base64ImageData) throws ZebraPrinterLanguageUnknownException, ConnectionException, IOException {
        // Get a Zebra compatible image
        InputStream rawImageData = new ByteArrayInputStream(Base64.decodeBase64(base64ImageData));
        ZebraImageI image = ZebraImageFactory.getImage(rawImageData);

        // Yeet it to the printer
        this.connection.open();
        ZebraPrinter imagePrinter = ZebraPrinterFactory.getInstance(this.connection);
        imagePrinter.printImage(image, 0, 0, 0, 0, false);
        this.connection.close();
    }

}

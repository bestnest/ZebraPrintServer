package printer;


import java.io.IOException;
import com.zebra.sdk.comm.Connection;
import java.nio.charset.StandardCharsets;
import com.zebra.sdk.comm.ConnectionException;
import com.zebra.sdk.printer.ZebraPrinterLanguageUnknownException;
import org.apache.commons.codec.binary.Base64;


public abstract class LabelPrinter {

    protected final Connection connection;

    protected LabelPrinter(Connection connection) {
        this.connection = connection;
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

    abstract void printImage(String base64ImageData) throws ZebraPrinterLanguageUnknownException, ConnectionException, IOException;

}

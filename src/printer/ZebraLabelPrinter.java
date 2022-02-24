package printer;

import com.zebra.sdk.comm.Connection;
import java.nio.charset.StandardCharsets;
import com.zebra.sdk.comm.ConnectionException;
import org.apache.commons.codec.binary.Base64;


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

}

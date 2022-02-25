package printer;

import com.zebra.sdk.comm.ConnectionException;


public interface LabelPrinter {

    public default void print(byte[] data) throws ConnectionException {}

    public default void print(String data) throws ConnectionException {}

    public default void printBase64Content(String base64encoded) throws ConnectionException {}

}

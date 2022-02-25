package printer;

import java.io.IOException;
import com.zebra.sdk.comm.ConnectionException;
import com.zebra.sdk.printer.ZebraPrinterLanguageUnknownException;


public interface LabelPrinter {

    default void print(byte[] data) throws ConnectionException {}

    default void print(String data) throws ConnectionException {}

    default void printBase64Content(String base64encoded) throws ConnectionException {}

    default void printImage(String base64ImageData) throws ZebraPrinterLanguageUnknownException, ConnectionException, IOException {}

}

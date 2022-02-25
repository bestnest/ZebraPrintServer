package printer;


// Import time, baby!
import java.io.*;
import java.util.UUID;
import java.nio.file.Files;
import java.awt.image.BufferedImage;
import com.zebra.sdk.comm.Connection;
import java.nio.charset.StandardCharsets;
import com.zebra.sdk.graphics.ZebraImageI;
import com.zebra.sdk.printer.ZebraPrinter;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.commons.codec.binary.Base64;
import com.zebra.sdk.comm.ConnectionException;
import org.apache.pdfbox.rendering.PDFRenderer;
import com.zebra.sdk.graphics.ZebraImageFactory;
import com.zebra.sdk.printer.ZebraPrinterFactory;
import org.apache.pdfbox.tools.imageio.ImageIOUtil;
import com.zebra.sdk.printer.ZebraPrinterLanguageUnknownException;


public class ZebraLabelPrinter implements LabelPrinter {

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
        InputStream rawImageData;

        // Check if image is a PDF
        byte[] imageBytes = Base64.decodeBase64(base64ImageData);
        if (imageBytes[0] == 0x25 && imageBytes[1] == 0x50 && imageBytes[2] == 0x44 && imageBytes[3] == 0x46 && imageBytes[4] == 0x2D) {
            // This is a PDF, we need to convert it
            UUID fileID = UUID.randomUUID();
            String pdfFilePath = "target/images/" + fileID + ".pdf";
            File pdfFile = new File(pdfFilePath);
            Files.write(pdfFile.toPath(), imageBytes);
            ByteArrayOutputStream convertedPNG = generateImageFromPDF(pdfFile, "PNG");
            rawImageData = new ByteArrayInputStream(convertedPNG.toByteArray());
        } else {
            rawImageData = new ByteArrayInputStream(imageBytes);
        }

        // Get a Zebra compatible image
        ZebraImageI image = ZebraImageFactory.getImage(rawImageData);

        // Yeet it to the printer
        this.connection.open();
        ZebraPrinter imagePrinter = ZebraPrinterFactory.getInstance(this.connection);
        System.out.println(imagePrinter.getCurrentStatus().labelLengthInDots);
        imagePrinter.printImage(image, 0, 0, 0, 0, false);
        this.connection.close();

    }

    private ByteArrayOutputStream generateImageFromPDF(File pdfFile, String extension) throws IOException {
        PDDocument document = PDDocument.load(pdfFile);
        PDFRenderer pdfRenderer = new PDFRenderer(document);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        BufferedImage bim = pdfRenderer.renderImageWithDPI(0, 300, ImageType.GRAY);
        ImageIOUtil.writeImage(bim, extension, out);

        document.close();
        return out;
    }

}

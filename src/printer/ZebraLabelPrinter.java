package printer;


// Import time, baby!
import java.io.*;
import java.util.UUID;
import java.nio.file.Files;
import java.awt.image.BufferedImage;
import com.zebra.sdk.comm.Connection;
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


public class ZebraLabelPrinter extends LabelPrinter {

    public ZebraLabelPrinter(Connection printerConnection) {
        super(printerConnection);
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
            ByteArrayOutputStream convertedPNG = generatePNGFromPDF(pdfFile);
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

    private ByteArrayOutputStream generatePNGFromPDF(File pdfFile) throws IOException {
        PDDocument document = PDDocument.load(pdfFile);
        PDFRenderer pdfRenderer = new PDFRenderer(document);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        BufferedImage bim = pdfRenderer.renderImageWithDPI(0, 300, ImageType.GRAY);
        ImageIOUtil.writeImage(bim, "PNG", out);

        document.close();
        return out;
    }

}

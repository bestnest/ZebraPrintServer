package api;


import dict.KeyError;
import dict.Dictionary;
import static spark.Spark.*;

import printer.ZebraLabelPrinter;
import org.json.simple.JSONObject;
import printer.Base64ContentPrinter;
import org.json.simple.parser.JSONParser;
import com.zebra.sdk.comm.ConnectionException;
import com.zebra.sdk.printer.discovery.UsbDiscoverer;
import com.zebra.sdk.printer.discovery.ZebraPrinterFilter;
import com.zebra.sdk.printer.discovery.DiscoveredUsbPrinter;

import java.util.ArrayList;


public class Server {

    private int port = 9100;
    private final Dictionary printerIndex = new Dictionary();
    private final Dictionary printers = new Dictionary();
    private final ArrayList<Thread> printThreads = new ArrayList<Thread>();

    // Constructors
    public Server() throws KeyError, ConnectionException {
        this.loadPrinters();
    }

    public Server(int port) throws KeyError, ConnectionException {
        this.port = port;
        this.loadPrinters();
    }

    /***
     * Starts the API server and declares all the routes
     */
    public void startServer() {
        port(this.port);

        // Set up cors
        options("/*", (request, response) -> {

            String accessControlRequestHeaders = request.headers("Access-Control-Request-Headers");
            if (accessControlRequestHeaders != null) {
                response.header("Access-Control-Allow-Headers", accessControlRequestHeaders);
            }

            String accessControlRequestMethod = request.headers("Access-Control-Request-Method");
            if (accessControlRequestMethod != null) {
                response.header("Access-Control-Allow-Methods", accessControlRequestMethod);
            }

            return "OK";
        });

        after((request, response) -> {
            response.header("Access-Control-Allow-Origin", "*");
            response.header("Access-Control-Request-Method", "GET, POST");
            response.header("Access-Control-Allow-Headers", "Content-Type");
        });

        // Get the status of the print server
        get("/status.json", (request, response) -> {
            response.type("application/json");
            response.body("{\"status\": \"ready\"}");
            return response.body();
        });

        // Get the list of available printers
        get("/printers.json", (request, response) -> {
            this.loadPrinters();
            response.type("application/json");
            response.body(this.printerIndex.toJSON());
            return response.body();
        });

        // test printing something
        path("/print", () -> {
            after("/*", (req, res) -> {
                System.out.println("Received call to print from " + req.ip());
                res.header("content-type", "application/json");
            });

            // test path
            post("/test", (request, response) -> {
                ZebraLabelPrinter p = (ZebraLabelPrinter) this.printers.get(0);
                p.print("^XA^FO20,20^A0N,25,25^FDThe printer is working successfully.^FS^XZ");
                return makeJSONResponse("success", "Test page printing...");
            });

            // base64 content with no splitting
            post("/base64LabelCode", (request, response) -> {
                JSONParser parser = new JSONParser();
                JSONObject json;
                try {
                    json = (JSONObject) parser.parse(request.body());
                } catch (Exception ignored) {
                    return makeJSONResponse("failure", "Invalid JSON request body. Please try again, homie.");
                }
                String printerAddress = (String) json.get("printer");
                String base64content = (String) json.get("printerCode");
                Base64ContentPrinter printThread = new Base64ContentPrinter((ZebraLabelPrinter) this.printers.get(printerAddress), base64content);
                this.printThreads.add(printThread);
                printThread.start();
                return makeJSONResponse("success", "Successfully queued label code");
            });
        });

    }

    /***
     * Stops the server from running
     */
    public void stopServer() {
        stop();
    }

    private void loadPrinters() throws ConnectionException, KeyError {
        for (DiscoveredUsbPrinter printer : UsbDiscoverer.getZebraUsbPrinters(new ZebraPrinterFilter())) {
            Dictionary printerInfo = new Dictionary();
            String name = printer.address.split("#model_")[1];
            printerInfo.set("address", printer.address);
            printerInfo.set("name", name);
            this.printerIndex.set(printer.address, printerInfo);
            this.printers.set(printer.address, new ZebraLabelPrinter(printer.getConnection()));
        }
    }

    private String makeJSONResponse(String message, String details) throws KeyError {
        Dictionary response = new Dictionary();
        response.set("message", message);
        response.set("details", details);
        return response.toJSON();
    }
}

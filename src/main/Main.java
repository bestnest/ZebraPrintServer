package main;


import api.Server;
import com.zebra.sdk.comm.ConnectionException;
import com.zebra.sdk.printer.discovery.DiscoveryException;


public class Main {

    public static void main(String[] args) throws ConnectionException, DiscoveryException {
        Server apiServer = new Server(9100);
        apiServer.startServer();
    }

}

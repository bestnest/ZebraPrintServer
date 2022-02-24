package main;


import api.Server;
import dict.KeyError;
import com.zebra.sdk.comm.ConnectionException;


public class Main {

    public static void main(String[] args) throws ConnectionException, KeyError {
        Server apiServer = new Server(9100);
        apiServer.startServer();
    }

}

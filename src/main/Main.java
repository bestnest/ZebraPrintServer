package main;


import api.Server;
import java.util.Queue;
import printer.PrintJob;
import java.util.LinkedList;
import com.zebra.sdk.comm.ConnectionException;
import com.zebra.sdk.printer.discovery.DiscoveryException;


public class Main {

    public static void main(String[] args) throws ConnectionException, DiscoveryException, InterruptedException {
        Queue<PrintJob> printQueue = new LinkedList<PrintJob>();
        Server apiServer = new Server(printQueue, 9100);
        apiServer.startServer();
        while (true) {
            PrintJob currentJob = printQueue.poll();
            if (currentJob != null) {
                currentJob.start();
                synchronized (currentJob) {
                    currentJob.wait();
                }
            }
            Thread.sleep(500);
        }
    }

}

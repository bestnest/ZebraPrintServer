package main;


import api.Server;
import java.util.Queue;
import printer.PrintJob;
import java.util.LinkedList;
import com.zebra.sdk.comm.ConnectionException;
import java.util.concurrent.locks.ReentrantLock;
import com.zebra.sdk.printer.discovery.DiscoveryException;


public class Main {

    public static void main(String[] args) throws ConnectionException, DiscoveryException {
        Queue<PrintJob> printQueue = new LinkedList<>();
        final ReentrantLock lock = new ReentrantLock();
        Server apiServer = new Server(printQueue, 9100, lock);
        apiServer.startServer();
        try {
            for (;;) {
                lock.lock();
                PrintJob currentJob = printQueue.poll();
                lock.unlock();
                if (currentJob != null) {
                    currentJob.doittoit();
                }
                // Keep from DOS-ing on the queue
                Thread.sleep(200);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

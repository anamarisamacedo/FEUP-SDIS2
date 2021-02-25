package models;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import communicationChannels.centralizedChordManager.server.*;

public class ServerRunnable implements Runnable {
    ServerChannel server;
    private int port;
    private String address;
    private CentralizedChordManager chordManager;
    static ScheduledExecutorService threadPoolExecutor;

    public ServerRunnable(int port, String address, CentralizedChordManager chordManager) {
        this.port = port;
        this.address = address;
        this.chordManager = chordManager;
    }

    @Override
    public void run() {
        try {
            server = new ServerChannel("TLSv1.2", "localhost", port, chordManager);
            threadPoolExecutor = Executors.newScheduledThreadPool(100);
            threadPoolExecutor.execute(server);
            // server.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * @return the address
     */
    public String getAddress() {
        return address;
    }

    /**
     * @param address the address to set
     */
    public void setAddress(String address) {
        this.address = address;
    }
}
package models;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.Iterator;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

public class CentralizedChordManager {
    static ServerRunnable serverRunnable;

    static public int byteNumber;
    public static int numPeers;
    static public int numDHT;
    private static Peer[] nodeList;
    static private CopyOnWriteArrayList<Integer> nodeIDList = new CopyOnWriteArrayList<Integer>();
    static private CentralizedChordManager chordManager;

    public CentralizedChordManager(int maxNumPeers) {
        byteNumber = (int) Math.ceil(Math.log(maxNumPeers) / Math.log(2));
        numDHT = (int) Math.pow(2, byteNumber);

        nodeList = new Peer[numDHT];
    }

    /**
     * @param nodeIP
     * @param receivedPort
     * @return
     */
    public String registerPeer(String nodeIP, int receivedPort) {
        synchronized (this) {
            int nodeID = 0;
            String initInfo = "";
            String nodePort = String.valueOf(receivedPort);
            numPeers++;
            MessageDigest md;
            try {
                md = MessageDigest.getInstance("SHA1");
                md.reset();
                String hashString = nodeIP + nodePort;
                md.update(hashString.getBytes());
                byte[] hashBytes = md.digest();
                BigInteger hashNum = new BigInteger(1, hashBytes);

                nodeID = Math.abs(hashNum.intValue()) % numDHT;

                System.out.println("Generated ID: " + nodeID + " for requesting Peer");

                while (nodeList[nodeID] != null) { // ID Collision
                    md.reset();
                    md.update(hashBytes);
                    hashBytes = md.digest();
                    hashNum = new BigInteger(1, hashBytes);
                    nodeID = Math.abs(hashNum.intValue()) % numDHT;
                    System.out.println("ID Collision, new ID: " + nodeID);
                }

                if (nodeList[nodeID] == null) {
                    nodeList[nodeID] = new Peer(nodeID, nodeIP, receivedPort);
                    nodeIDList.add(nodeID);
                    System.out.println("New peer added ... ");
                }

                Collections.sort(nodeIDList, Collections.reverseOrder());

                int predID = nodeID;
                Iterator<Integer> iterator = nodeIDList.iterator();
                while (iterator.hasNext()) {
                    int next = iterator.next();
                    if (next < predID) {
                        predID = next;
                        break;
                    }
                }
                if (predID == nodeID)
                    predID = Collections.max(nodeIDList);

                initInfo = nodeID + "/" + predID + "/" + nodeList[predID].address + "/"
                        + nodeList[predID].getSslEnginePort();

            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
            return initInfo;
        }
    }

    /**
     * @param id
     */
    public void finishJoining(int id) {
        System.out.println("*** Post Initiation Call: Peer " + id + " is in the DHT.");
        System.out.println("Current number of peers = " + numPeers + "\n");
    }

    public static void main(final String[] args) { // 0 - sslEnginePort, 1 - IP, 2 - max peers
        int port = Integer.parseInt(args[0]);
        String IP = args[1];
        int maxPeers = Integer.parseInt(args[2]);
        chordManager = new CentralizedChordManager(maxPeers);
        serverRunnable = new ServerRunnable(port, IP, chordManager);
        Thread server = new Thread(serverRunnable);
        server.start();
    }
}
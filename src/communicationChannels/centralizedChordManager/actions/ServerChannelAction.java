package communicationChannels.centralizedChordManager.actions;

import java.io.IOException;
import java.rmi.NotBoundException;

import models.CentralizedChordManager;
import protocolActions.Send;

public class ServerChannelAction {

    public final static String REGISTER_PEER = "REGISTER_PEER";
    public final static String FINISH_JOINING = "FINISH_JOINING";

    public static Send parseMessage(Send received2, CentralizedChordManager chordManager)
            throws NotBoundException, NumberFormatException, IOException, InterruptedException {
        String operation = received2.operation;
        String message = received2.message;
        Send ret = new Send(Send.CONFIRMATION);
        switch (operation) {
            case REGISTER_PEER:
                synchronized (ServerChannelAction.class) {
                    String address = message.split(";")[0];
                    int nodePort = Integer.parseInt(message.split(";")[1]);
                    ret.message = chordManager.registerPeer(address, nodePort);
                    break;
                }
            case FINISH_JOINING:
                System.out.println("Current number of nodes = " + CentralizedChordManager.numPeers + "\n");
                ret.message = "Current number of nodes = " + CentralizedChordManager.numPeers + "\n";
                break;
            case Send.ENOUGH_NODES:
                System.out.println(received2.desiredReplicationDegree + " Desired");
                int nodesAvailable = CentralizedChordManager.numPeers - 1;
                if (nodesAvailable >= received2.desiredReplicationDegree) {
                    ret.enough = true;
                } else {
                    ret.enough = false;
                }
                System.out.println("Enough: " + ret.enough);
                break;
        }
        return ret;
    }

}

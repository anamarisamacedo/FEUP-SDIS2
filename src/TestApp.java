import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import rmi_interface.RMI_Interface;

public class TestApp {
    protected final static String HOST = "127.0.0.1";
    protected final static String BACKUP = "BACKUP";
    protected final static String RESTORE = "RESTORE";
    protected final static String DELETE = "DELETE";
    protected final static String RECLAIM = "RECLAIM";
    protected final static String STATE = "STATE";

    
    /** 
     * @param args
     */
    public static void main(String[] args) {
        try {
            if (args.length < 2) {
                System.out.println("The testing application should be invoked as follows:\n"
                        + "java TestApp <peer_ap> <sub_protocol> <opnd_1> <opnd_2>");
            }
            String peer_ap = args[0]; // e.g 1
            String operation = args[1]; // BACKUP, RESTORE, RECLAIM, DELETE, RESTORE
            String opnd_1 = ""; // file or number to RECLAIM
            if (args.length > 2) {
                opnd_1 = args[2];
            }
            String opnd_2 = ""; // replication degree
            if (args.length > 3) {
                opnd_2 = args[3];
            }
            Registry registry = LocateRegistry.getRegistry("Localhost");
            RMI_Interface peer = (RMI_Interface) registry.lookup(peer_ap);
            switch (operation) {
                case BACKUP:
                    if (opnd_2 == null) {
                        System.out.println("Replication degree missing");
                    } else {
                        try {
                            peer.backup(opnd_1, Integer.parseInt(opnd_2), peer_ap);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    break;
                case RESTORE:
                    peer.restore(opnd_1, peer_ap);
                    break;
                case DELETE:
                    peer.delete(opnd_1, peer_ap);
                    break;
                case RECLAIM:
                    peer.reclaim(Integer.parseInt(opnd_1), peer_ap);
                    break;
                // case STATE:
                // peer.state();
                // break;
            }

        } catch (NotBoundException | RemoteException e) {
            System.out.println("Peer could not be found");
        }
    }

}
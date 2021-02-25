package chord;

import java.io.Serializable;

import communicationChannels.peer.actions.PeerMessageAction;
import models.Peer;
import protocolActions.Send;

public class ChordManager implements Serializable, Runnable {
    /**
     *
     */
    private static final long serialVersionUID = -4080367458897356248L;
    public static Peer thisPeer;
    public static Peer me;
    public static Peer pred;
    public static FingerTable[] finger;
    public static int numDHT;
    public static int maxPeersBytes;

    public ChordManager(Peer p) {
        ChordManager.thisPeer = p;
        me = p.me;
        pred = p.pred;
        finger = Peer.finger;
        numDHT = Peer.numDHT;
        maxPeersBytes = p.maxPeersBytes;
        bootChord();
    }

    /**
     * * Notify other nodes to update their predecessors and finger table
     */
    public static void update_others() {
        Peer p;
        for (int i = 1; i <= maxPeersBytes; i++) {
            int id = me.getPeerId() - (int) Math.pow(2, i - 1) + 1;
            if (id < 0)
                id = id + numDHT;

            p = find_predecessor(id);

            Send request = new Send(Send.UPDATE_FINGER_TABLE,
                    me.getPeerId() + "/" + me.address + "/" + me.getSslEnginePort() + "/" + i);
            try {
                query(p.address, p.getSslEnginePort(), request);
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

    /**
     * @param id
     * @return Peer
     */
    private static Peer find_predecessor(int id) {
        try {
            Peer n = me;
            int myID = n.getPeerId();
            int succID = finger[1].getSuccessor().getPeerId();
            int normalInterval = 1;
            if (myID >= succID)
                normalInterval = 0;

            while ((normalInterval == 1 && (id <= myID || id > succID))
                    || (normalInterval == 0 && (id <= myID && id > succID))) {

                Send request = new Send(Send.CLOSEST_PREDECESSOR, String.valueOf(id));
                n = query(n.address, n.getSslEnginePort(), request).peer;

                myID = n.getPeerId();

                Send request2 = new Send(Send.GET_SUCESSOR, "FROM PEER " + thisPeer.getPeerId());

                succID = query(n.address, n.getSslEnginePort(), request2).peer.getPeerId();

                if (myID >= succID)
                    normalInterval = 0;
                else
                    normalInterval = 1;
            }

            return n;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null; // making compiler happy
    }

    public void bootChord() {
        System.out.println("Building Finger table ... ");
        for (int i = 1; i <= thisPeer.maxPeersBytes; i++) {
            finger[i] = new FingerTable();
            finger[i].setStart((me.getPeerId() + (int) Math.pow(2, i - 1)) % numDHT);
        }
        for (int i = 1; i < thisPeer.maxPeersBytes; i++) {
            finger[i].setInterval(finger[i].getStart(), finger[i + 1].getStart());
        }
        finger[maxPeersBytes].setInterval(finger[maxPeersBytes].getStart(), finger[1].getStart() - 1);

        for (int i = 1; i <= thisPeer.maxPeersBytes; i++) {
            finger[i].setSuccessor(me);
        }

        if (pred.getPeerId() == me.getPeerId()) { // if predcessor is same as my ID -> only peer in DHT
            System.out.println("First Peer on the DHT");
        } else {
            try {
                init_finger_table(pred, finger);
                System.out.println("Initiated Finger Table!");
                update_others();
                System.out.println("Other peers updated");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        try {
            // SEND FINISH JOINING
            Send request = new Send(Send.FINISH_JOINING, me.getPeerId() + "");
            query(thisPeer.centralizedChordManagerAddress, thisPeer.centralizedChordManagerPort, request);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * @param ip
     * @param port
     * @param send
     * @return Send
     */
    public static Send query(String ip, int port, Send send) {
        if (me.address.equals(ip) && me.getSslEnginePort() == (port)) {
            Send response = null;
            try {
                response = PeerMessageAction.parseMessage(send, thisPeer);
            } catch (NumberFormatException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return response;
        } else {
            return send.writeAndRead(send, thisPeer, Peer.PROTOCOL, ip, port);
        }
    }

    /**
     * @param n
     * @param finger
     * @throws Exception
     */
    public static void init_finger_table(Peer n, FingerTable[] finger) throws Exception {
        int myID, nextID;

        Send send = new Send(Send.FIND_SUCESSOR, "" + finger[1].getStart());
        Peer result = query(n.address, n.getSslEnginePort(), send).peer;

        finger[1].setSuccessor(result);

        Send send2 = new Send(Send.GET_PREDECESSOR);
        Peer result2 = query(finger[1].getSuccessor().address, finger[1].getSuccessor().getSslEnginePort(), send2).peer;
        pred = result2;

        Send send3 = new Send(Send.SET_PREDECESSOR, me.getPeerId() + "/" + me.address + "/" + me.getSslEnginePort());
        query(finger[1].getSuccessor().address, finger[1].getSuccessor().getSslEnginePort(), send3);
        int normalInterval = 1;
        for (int i = 1; i <= maxPeersBytes - 1; i++) {

            myID = me.getPeerId();
            nextID = finger[i].getSuccessor().getPeerId();

            if (myID >= nextID)
                normalInterval = 0;
            else
                normalInterval = 1;

            if ((normalInterval == 1 && (finger[i + 1].getStart() >= myID && finger[i + 1].getStart() <= nextID))
                    || (normalInterval == 0
                            && (finger[i + 1].getStart() >= myID || finger[i + 1].getStart() <= nextID))) {

                finger[i + 1].setSuccessor(finger[i].getSuccessor());
            } else {

                Send request4 = new Send(Send.FIND_SUCESSOR, "" + finger[i + 1].getStart());
                Peer result4 = query(n.address, n.getSslEnginePort(), request4).peer;

                int fiStart = finger[i + 1].getStart();
                int succ = result4.getPeerId();
                int fiSucc = finger[i + 1].getSuccessor().getPeerId();
                if (fiStart > succ)
                    succ = succ + numDHT;
                if (fiStart > fiSucc)
                    fiSucc = fiSucc + numDHT;

                if (fiStart <= succ && succ <= fiSucc) {
                    finger[i + 1].setSuccessor(result4);
                }
            }
        }
    }

    /**
     * @param newPeer
     */
    public static void setPredecessor(Peer newPeer) {
        pred = newPeer;
    }

    /**
     * @return Peer
     */
    public static Peer getPredecessor() {
        return pred;
    }

    /**
     * @param id
     * @return Peer
     * @throws Exception
     */
    public static Peer find_successor(int id) throws Exception {
        Peer n;
        n = find_predecessor(id);
        Send request = new Send(Send.GET_SUCESSOR, "FROM PEER " + thisPeer.getPeerId());
        return query(n.address, n.getSslEnginePort(), request).peer;
    }

    /**
     * @param id
     * @return Peer
     */
    public static Peer closest_preceding_finger(int id) {
        int normalInterval = 1;
        int myID = me.getPeerId();
        if (myID >= id) {
            normalInterval = 0;
        }

        for (int i = maxPeersBytes; i >= 1; i--) {
            int nodeID = finger[i].getSuccessor().getPeerId();
            if (normalInterval == 1) {
                if (nodeID > myID && nodeID < id)
                    return finger[i].getSuccessor();
            } else {
                if (nodeID > myID || nodeID < id)
                    return finger[i].getSuccessor();
            }
        }
        return me;
    }

    /**
     * @param newPeer
     * @param i
     */
    public static void update_finger_table(Peer newPeer, int i) {
        Peer p;
        int normalInterval = 1;
        int myID = me.getPeerId();
        int nextID = finger[i].getSuccessor().getPeerId();
        if (myID >= nextID)
            normalInterval = 0;
        else
            normalInterval = 1;

        if (((normalInterval == 1 && (newPeer.getPeerId() >= myID && newPeer.getPeerId() < nextID))
                || (normalInterval == 0 && (newPeer.getPeerId() >= myID || newPeer.getPeerId() < nextID)))
                && (me.getPeerId() != newPeer.getPeerId())) {

            finger[i].setSuccessor(newPeer);
            p = pred;

            Send request = new Send(Send.UPDATE_FINGER_TABLE,
                    newPeer.getPeerId() + "/" + newPeer.address + "/" + newPeer.getSslEnginePort() + "/" + i);
            try {
                query(p.address, p.getSslEnginePort(), request);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * @param key
     * @return Peer
     * @throws Exception
     */
    public Peer lookup(int key) throws Exception {
        Peer successorPeer = find_successor(key);
        Send request = new Send(Send.LOOKUP);
        return query(successorPeer.address, successorPeer.getSslEnginePort(), request).peer;
    }

    public static void Stabilize() {
        Send send = new Send(Send.GET_PREDECESSOR);
        Peer x;
        try {
            try {
                x = query(finger[1].getSuccessor().address, finger[1].getSuccessor().getSslEnginePort(), send).peer;
            } catch (Exception e) {
                x = null;
            }
            Peer p = finger[1].getSuccessor();
            if (x != null) {
                if (thisPeer.getPeerId() < p.getPeerId()) {
                    if (x.getPeerId() > thisPeer.getPeerId() && x.getPeerId() < p.getPeerId()) {
                        finger[1].setSuccessor(x);
                    }
                } else if (thisPeer.getPeerId() == p.getPeerId()) {
                    finger[1].setSuccessor(x);
                } else {
                    if (x.getPeerId() > thisPeer.getPeerId() || x.getPeerId() < finger[1].getSuccessor().getPeerId()) {
                        finger[1].setSuccessor(x);
                    }
                }
            } else {
                finger[1].setSuccessor(p);
            }
            Send send2 = new Send(Send.NOTIFY);
            send2.peer = new Peer(thisPeer.getPeerId(), thisPeer.address, thisPeer.getSslEnginePort());
            query(finger[1].getSuccessor().address, finger[1].getSuccessor().getSslEnginePort(), send2);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * @param s
     */
    public static void notify(Peer s) {
        Peer predecessor = pred;
        if (predecessor != null) {
            if (predecessor.getPeerId() < thisPeer.getPeerId()) {
                if (s.getPeerId() > predecessor.getPeerId() && s.getPeerId() < thisPeer.getPeerId()) {
                    predecessor = s;
                }
            } else if (predecessor.getPeerId() == thisPeer.getPeerId()) {
                predecessor = s;
            } else {
                if (s.getPeerId() > predecessor.getPeerId() || s.getPeerId() < thisPeer.getPeerId()) {
                    predecessor = s;
                }
            }
        } else {
            predecessor = s;
        }
    }

    public static void fix_fingers() {
        for (int i = 1; i <= maxPeersBytes; i++) {
            try {
                finger[i].setSuccessor(find_successor(finger[i].getStart()));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * @return Peer
     */
    public static Peer getSucessor() // throws RemoteException
    {
        return finger[1].getSuccessor();
    }

    @Override
    public void run() {
        Stabilize();
        fix_fingers();
    }
}
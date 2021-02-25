
package threads;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import chord.ChordManager;
import models.FileModel;
import models.Peer;
import protocolActions.Send;

public class BackupThread implements Runnable {
    Peer p;
    String path;
    int replicationDegree;
    String senderId;
    boolean enhanced;

    public BackupThread(Peer p, String path, int replicationDegree, String senderId, boolean enhanced) {
        this.p = p;
        this.path = path;
        this.replicationDegree = replicationDegree;
        this.senderId = senderId;
        this.enhanced = enhanced;
    }

    @Override
    public void run() {
        // CHECK IF THERE ARE ENOUGH NODES
        Send enoughPeers = new Send(Send.ENOUGH_NODES, replicationDegree);
        Send ret = enoughPeers.writeAndRead(enoughPeers, p, Peer.PROTOCOL, p.centralizedChordManagerAddress,
                p.centralizedChordManagerPort);
        // CHECK IF THERE ARE ENOUGH NODES
        if (ret.enough) {
            MessageDigest md3 = null;
            try {
                md3 = MessageDigest.getInstance("SHA1");
            } catch (NoSuchAlgorithmException e2) {
                e2.printStackTrace();
            }
            md3.reset();
            md3.update(path.getBytes());
            byte[] hashBytes3 = md3.digest();
            BigInteger hashNum3 = new BigInteger(1, hashBytes3);
            int key3 = Math.abs(hashNum3.intValue()) % Peer.numDHT;
            System.out.println("Generated key " + key3 + " for file: " + path);
            FileModel file = new FileModel(path, replicationDegree, p.me);
            file.loadFileContent();
            p.initiatorPeerFiles.add(file);
            if (file.fileData.length != 0) {
                Peer destination = null;
                try {
                    destination = ChordManager.find_successor(key3);
                    // if dest is myself then i increase the retries by 1 because I'm sending the
                    // file to myself first
                    if (destination.getSslEnginePort() == p.getSslEnginePort()) {
                        file.retries += 1;
                    }
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
                SendFileThread thread = new SendFileThread(p.initiatorPeerFiles.get(p.initiatorPeerFiles.indexOf(file)),
                        p, destination);
                thread.start();

                try {
                    thread.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (!file.replicationDegreeAchieved())
                    System.out.println("File " + path + " was backed up with lower repDegree: "
                            + file.perceivedReplicationDegree + " of: " + file.perceivedReplicationDegree);
                else {
                    System.out.println("File " + path + " was backed up with repDegree wanted");
                }
            } else {
                System.err.println("File " + path + " was not backed up");
            }
        } else {
            System.err.println("File " + path + " was not backed up replication degree of : " + replicationDegree
                    + " could not be achived due to less number of peers available");
        }
    }

    protected class SendFileThread extends Thread {
        Peer p;
        FileModel file;
        Peer destination;

        public SendFileThread(FileModel file, Peer p, Peer destination) {
            this.file = file;
            this.p = p;
            this.destination = destination;
        }

        @Override
        public void run() {
            try {
                Send send = new Send(Send.BACKUP, file, p.me);
                send.writeFile(Send.BACKUP, p.me, Peer.PROTOCOL, destination.address, destination.getSslEnginePort());
                file.fileData = new byte[0]; // clear data
                System.out.println("Initiated back up");
                synchronized (p.initiatorPeerFiles.stream().filter(f -> f.fileId.equals(file.fileId)).findFirst()
                        .get()) {
                    p.initiatorPeerFiles.stream().filter(f -> f.fileId.equals(file.fileId)).findFirst().get().wait();
                }
                return;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
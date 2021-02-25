package threads;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import chord.ChordManager;
import models.FileModel;
import models.Peer;
import protocolActions.Send;

public class RestoreThread implements Runnable {
    Peer p;
    String path;
    String senderId;
    FileModel file;

    public RestoreThread(Peer p, String path, String senderId, FileModel file) {
        this.p = p;
        this.path = path;
        this.senderId = senderId;
        this.file = file;
    }

    @Override
    public void run() {
        MessageDigest md3 = null;
        file.retries = 5;
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
        Send restoreSend = new Send(Send.RESTORE, file, p.me);
        restoreSend.write(restoreSend, Peer.PROTOCOL, destination.address, destination.getSslEnginePort());
    }

}
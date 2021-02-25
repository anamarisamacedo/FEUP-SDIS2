package threads;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import chord.ChordManager;
import models.FileModel;
import models.Peer;
import protocolActions.Send;

public class DeleteThread extends Thread {
    Peer p;
    String path;
    List<FileModel> fileModelsToDelete;
    String senderId;

    public DeleteThread(Peer p, List<FileModel> fileModelsToDelete, String path, String senderId) {
        this.p = p;
        this.fileModelsToDelete = fileModelsToDelete;
        this.path = path;
        this.senderId = senderId;
    }

    @Override
    public void run() {
        List<DeleteFileThread> threads = new ArrayList<>();
        for (FileModel file : this.fileModelsToDelete) {
            file.retries = 5;
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
            DeleteFileThread thread = new DeleteFileThread(file, p, destination);
            threads.add(thread);
            thread.start();
        }
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        this.p.initiatorPeerFiles.removeAll(this.fileModelsToDelete);
    }

    protected class DeleteFileThread extends Thread {
        Peer p;
        FileModel file;
        Peer destination;

        public DeleteFileThread(FileModel file, Peer p, Peer destination) {
            this.file = file;
            this.p = p;
            this.destination = destination;
        }

        @Override
        public void run() {
            try {
                Send send = new Send(Send.DELETE, file, p.me);
                send.write(send, Peer.PROTOCOL, destination.address, destination.getSslEnginePort());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
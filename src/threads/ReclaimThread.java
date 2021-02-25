package threads;

import models.Peer;

public class ReclaimThread implements Runnable {
    int spaceToReclaim;
    String senderId;
    Peer p;

    public ReclaimThread(Peer p, int spaceToReclaim, String senderId) {
        this.p = p;
        this.spaceToReclaim = spaceToReclaim;
        this.senderId = senderId;
    }

    @Override
    public void run() {
        p.storage.setMaxStorage(this.spaceToReclaim * 1000, p); // *1000 to convert from KB to B
    }

}
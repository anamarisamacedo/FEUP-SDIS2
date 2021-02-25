package models;

import java.io.Serializable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class SaveablePeer implements Serializable {
    public SaveablePeer(CopyOnWriteArrayList<FileModel> files, Storage storage,
            ConcurrentHashMap<String, CopyOnWriteArrayList<Chunk>> peerRepliesMap) {
        this.files = files;
        this.storage = storage;
        this.peerRepliesMap = peerRepliesMap;
    }

    /**
     *
     */
    private static final long serialVersionUID = -4779751253303066746L;
    CopyOnWriteArrayList<FileModel> files;
    Storage storage;
    ConcurrentHashMap<String, CopyOnWriteArrayList<Chunk>> peerRepliesMap;

}
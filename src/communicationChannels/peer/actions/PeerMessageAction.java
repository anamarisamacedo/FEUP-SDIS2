package communicationChannels.peer.actions;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.NoSuchElementException;
import java.util.concurrent.CopyOnWriteArrayList;

import chord.ChordManager;
import models.Chunk;
import models.FileModel;
import models.Peer;
import protocolActions.Send;

public class PeerMessageAction {
    static CopyOnWriteArrayList<FileModel> receivedFilesBackup = new CopyOnWriteArrayList<>();
    static CopyOnWriteArrayList<FileModel> receivedFilesRestore = new CopyOnWriteArrayList<>();

    public static Send parseMessage(Send received, Peer p) throws NumberFormatException, Exception {
        String operation = received.operation;
        Peer peer;
        String body[] = null;
        Send response = new Send(Send.CONFIRMATION);
        switch (operation) {
            case Send.SET_PREDECESSOR:
                synchronized (PeerMessageAction.class) {
                    body = received.message.split("/");
                    peer = new Peer(Integer.parseInt(body[0]), body[1], Integer.parseInt(body[2]));
                    ChordManager.setPredecessor(peer);
                    response = new Send(Send.CONFIRMATION, "Successfully set");
                    break;
                }
            case Send.GET_PREDECESSOR:
                peer = ChordManager.getPredecessor();
                response = new Send(Send.PEER, new Peer(peer.getPeerId(), peer.address, peer.getSslEnginePort()));
                break;
            case Send.FIND_SUCESSOR:
                body = received.message.split("/");
                peer = ChordManager.find_successor(Integer.parseInt(body[0]));
                response = new Send(Send.PEER, new Peer(peer.getPeerId(), peer.address, peer.getSslEnginePort()));
                break;
            case Send.GET_SUCESSOR:
                peer = ChordManager.getSucessor();
                response = new Send(Send.PEER, new Peer(peer.getPeerId(), peer.address, peer.getSslEnginePort()));
                break;
            case Send.CLOSEST_PREDECESSOR:
                int id = Integer.parseInt(received.message);
                peer = ChordManager.closest_preceding_finger(id);
                response = new Send(Send.PEER, new Peer(peer.getPeerId(), peer.address, peer.getSslEnginePort()));
                break;
            case Send.UPDATE_FINGER_TABLE:
                synchronized (PeerMessageAction.class) {
                    body = received.message.split("/");
                    peer = new Peer(Integer.parseInt(body[0]), body[1], Integer.parseInt(body[2]));

                    ChordManager.update_finger_table(peer, Integer.parseInt(body[3]));
                    response = new Send(Send.CONFIRMATION, "Update finger" + body[3]);
                    break;
                }
            case Send.LOOKUP:
                body = received.message.split("/");
                for (int i = 0; i < body.length; i++) {
                    System.out.println("Body has this: " + body[1]);
                    System.out.println(" ");
                }
                // newPeer = p.chord.lookup(Integer.parseInt(body[0]));
                break;
            case Send.NOTIFY:
                peer = received.peer;
                ChordManager.notify(peer);
                break;
            case Send.BACKUP:
                if (groupChunksToFile(received, receivedFilesBackup)) {
                    FileModel file = null;
                    try {
                        file = p.initiatorPeerFiles.stream().filter(f -> f.fileId.equals(received.file.fileId))
                                .findFirst().get();
                    } catch (NoSuchElementException e) {
                    }
                    if (file != null) {
                        // I'm an initiator :-(
                        // p.initiatorPeerFiles.set(p.initiatorPeerFiles.indexOf(file), received.file);
                        received.file.retries -= 1;
                        if (received.file.retries == 0) {
                            synchronized (p.initiatorPeerFiles.stream()
                                    .filter(f -> f.fileId.equals(received.file.fileId)).findFirst().get()) {
                                p.initiatorPeerFiles.stream().filter(f -> f.fileId.equals(received.file.fileId))
                                        .findFirst().get().notifyAll();
                                int oldRetries = received.file.retries;
                                if (!received.file.updateReplication) {
                                    received.file.updateReplication = true;
                                    received.file.retries = 1;
                                    // Update others replicationDegrees
                                    Peer next = ChordManager.getSucessor();
                                    Send newSend = new Send(Send.BACKUP, received.file, received.peer);
                                    newSend.writeFile(Send.BACKUP, p.me, Peer.PROTOCOL, next.address,
                                            next.getSslEnginePort());
                                    System.out.println("Updating replication degree on other peers");
                                } else {
                                    System.out.println("Replication degree updated on other peers");
                                }
                                // Update others replicationDegrees
                                file.retries = oldRetries;
                                file.perceivedReplicationDegree = received.file.perceivedReplicationDegree;
                            }
                        } else {
                            Peer sucessor = ChordManager.getSucessor();
                            Send newSend = new Send(Send.BACKUP, received.file, received.peer);
                            newSend.writeFile(newSend.operation, received.peer, Peer.PROTOCOL, sucessor.address,
                                    sucessor.getSslEnginePort());
                        }
                    } else {
                        FileModel file6 = null;
                        Peer dest = null;
                        try {
                            file6 = p.storage.storage.stream().filter(f -> f.fileId.equals(received.file.fileId))
                                    .findFirst().get();
                        } catch (NoSuchElementException e) {

                        }
                        Send newSend = new Send(Send.BACKUP, received.file, received.peer);
                        if (file6 == null) {
                            // I'm not an initiator :-D
                            // Check if I have storage
                            if (p.storage.addFileToStorage(received.file, p)) {
                                received.file.perceivedReplicationDegree += 1;
                                dest = ChordManager.getSucessor();
                                if (received.file.replicationDegreeAchieved()) {
                                    dest = received.peer;
                                    newSend.operation = Send.CONFIRM_BACKUP;
                                    System.out.println("Replication degree achieved for file: " + received.file.fileId);
                                }
                            } else {
                                dest = ChordManager.getSucessor();
                            }
                        } else {
                            file6.perceivedReplicationDegree = received.desiredReplicationDegree;
                            dest = ChordManager.getSucessor();
                            System.out.println("Updating replication degree");
                        }
                        newSend.writeFile(newSend.operation, received.peer, Peer.PROTOCOL, dest.address,
                                dest.getSslEnginePort());
                    }
                }
                break;
            case Send.CONFIRM_BACKUP:
                if (groupChunksToFile(received, receivedFilesBackup)) {
                    FileModel file2 = p.initiatorPeerFiles.stream().filter(f -> f.fileId.equals(received.file.fileId))
                            .findFirst().get();
                    if (file2 != null) {
                        synchronized (p.initiatorPeerFiles.stream().filter(f -> f.fileId.equals(received.file.fileId))
                                .findFirst().get()) {
                            p.initiatorPeerFiles.stream().filter(f -> f.fileId.equals(received.file.fileId)).findFirst()
                                    .get().notifyAll();
                            int oldRetries = received.file.retries;
                            if (!received.file.updateReplication) {
                                received.file.updateReplication = true;
                                received.file.retries = 1;
                                // Update others replicationDegrees
                                Peer next = ChordManager.getSucessor();
                                Send newSend = new Send(Send.BACKUP, received.file, received.peer);
                                newSend.writeFile(Send.BACKUP, p.me, Peer.PROTOCOL, next.address,
                                        next.getSslEnginePort());
                                System.out.println("Updating replication degree on other peers");
                            } else {
                                System.out.println("Replication degree updated on other peers");
                            }
                            // Update others replicationDegrees
                            file2.retries = oldRetries;
                            file2.perceivedReplicationDegree = received.file.perceivedReplicationDegree;
                        }
                    }
                }
                break;
            case Send.RESTORE:
                FileModel file3 = null;
                Peer dest = null;
                try {
                    file3 = p.storage.storage.stream().filter(f -> f.fileId.equals(received.file.fileId)).findFirst()
                            .get();
                } catch (NoSuchElementException e) {

                }
                if (received.file.retries > 0) {
                    if (file3 == null) {
                        dest = ChordManager.getSucessor();
                        Send newSend = new Send(Send.RESTORE, received.file, received.peer);
                        newSend.write(newSend, Peer.PROTOCOL, dest.address, dest.getSslEnginePort());
                        return response;
                    } else {
                        dest = received.peer;
                        Send send = new Send(Send.CONFIRM_RESTORE, file3, received.peer);
                        send.writeFile(Send.CONFIRM_RESTORE, received.peer, Peer.PROTOCOL, dest.address,
                                dest.getSslEnginePort());
                    }
                }
                if (received.peer.getPeerId() == p.getPeerId()) {
                    received.file.retries -= 1;
                }
                break;
            case Send.CONFIRM_RESTORE:
                if (groupChunksToFile(received, receivedFilesRestore)) {
                    p.writeFileModelToDirectory(received.file);
                    System.out.println("File: " + received.file.filePath + " was restored");
                }
                break;
            case Send.DELETE:
                Peer destDPeer = null;
                Send newSend = new Send(Send.DELETE, received.file, received.peer);
                if (received.peer.getPeerId() == p.getPeerId()) {
                    received.file.retries -= 1;
                }
                if (received.file.retries > 0) {
                    if (p.storage.removeFileFromStorage(received.file, p)) {
                        received.file.perceivedReplicationDegree -= 1;
                        newSend.file.perceivedReplicationDegree = received.file.perceivedReplicationDegree;
                    }
                    if (received.file.perceivedReplicationDegree > 0) {
                        destDPeer = ChordManager.getSucessor();
                        newSend.file.perceivedReplicationDegree = received.file.perceivedReplicationDegree;
                        newSend.write(newSend, Peer.PROTOCOL, destDPeer.address, destDPeer.getSslEnginePort());
                    }
                }
                break;
            case Send.REMOVED:
                FileModel file4 = null;
                Peer dest1 = null;
                try {
                    file4 = p.initiatorPeerFiles.stream().filter(f -> f.fileId.equals(received.file.fileId)).findFirst()
                            .get();
                } catch (NoSuchElementException e) {

                }
                if (file4 != null) {
                    // I'm an initiator
                    file4.perceivedReplicationDegree -= 1;
                    if (!file4.replicationDegreeAchieved()) {
                        Send newBackup = new Send(Send.NEW_BACKUP, file4, p.me);
                        dest1 = ChordManager.getSucessor();
                        newBackup.write(newBackup, Peer.PROTOCOL, dest1.address, dest1.getSslEnginePort());
                    }
                } else {
                    if (received.peer.getPeerId() == p.getPeerId()) {
                        received.file.retries -= 1;
                    }
                    if (received.file.retries > 0) {
                        dest1 = ChordManager.getSucessor();
                        received.write(received, Peer.PROTOCOL, dest1.address, dest1.getSslEnginePort());
                    }
                }
                break;
            case Send.NEW_BACKUP:
                FileModel file5 = null;
                Peer dest2 = null;
                try {
                    // I saved this file, commencing new backup
                    // Here we go again
                    file5 = p.storage.storage.stream().filter(f -> f.fileId.equals(received.file.fileId)).findFirst()
                            .get();
                } catch (NoSuchElementException e) {

                }
                if (file5 != null) {
                    received.file.perceivedReplicationDegree = -1;
                    file5.perceivedReplicationDegree = received.file.perceivedReplicationDegree;
                    file5.retries = 5;
                    file5.updateReplication = false;
                    Send backup = new Send(Send.BACKUP, file5, file5.peer);
                    dest2 = ChordManager.getSucessor();
                    backup.writeFile(backup.operation, p.me, Peer.PROTOCOL, dest2.address, dest2.getSslEnginePort());
                    System.out.println("RepDegree of file: " + file5.fileId + " is lower, sending new backup");
                } else {
                    dest2 = ChordManager.getSucessor();
                    received.write(received, Peer.PROTOCOL, dest2.address, dest2.getSslEnginePort());
                }
                break;
        }
        // System.out.println("outResponse for " + tokens[0] + ": " + outResponse);
        return response;

    }

    private static boolean groupChunksToFile(Send received, CopyOnWriteArrayList<FileModel> receivedFiles)
            throws IOException {
        synchronized (PeerMessageAction.class) {
            FileModel isInList = null;
            try {
                isInList = receivedFiles.stream().filter(f -> f.fileId.equals(received.file.fileId)).findFirst().get();
            } catch (NoSuchElementException e) {
            }
            if (isInList == null) {
                isInList = received.file;
                receivedFiles.add(isInList);
            }
            isInList.receivedChunks.add(new Chunk(received.file.fileId, received.file.chunkId, received.file.fileData));
            if (isInList.receivedChunks.size() == received.file.noOfChunks) {
                // Reconstruct
                Collections.sort(isInList.receivedChunks, new Chunk());
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                for (Chunk c : isInList.receivedChunks) {
                    output.write(c.getChunkInfo());
                }
                isInList.receivedChunks.clear();
                received.file.fileData = output.toByteArray();
                // delete isInList
                receivedFiles.remove(isInList);
                System.out
                        .println("Reconstructed file: " + received.file.filePath + " going to: " + received.operation);
                return true;
            }
            return false;
        }
    }

}
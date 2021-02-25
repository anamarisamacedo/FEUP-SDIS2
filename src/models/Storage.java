/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package models;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.math.BigInteger;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.NoSuchElementException;
import java.util.concurrent.CopyOnWriteArrayList;

import chord.ChordManager;
import protocolActions.Send;

/**
 *
 * @author guilh
 */
public class Storage implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = 8832855660897142073L;

    // Maximum Storage Considered Is 10 Megabytes
    private int maxStorage = 1000 * 1000 * 10;

    // Storage keeper
    public CopyOnWriteArrayList<FileModel> storage = new CopyOnWriteArrayList<>();

    public boolean continueReclaiming(int chunkSize) {
        if (this.storageValue() > maxStorage) {
            return true;
        }
        return false;
    }

    public Integer storageValue() {
        int currentSize = 0;
        for (FileModel file : storage) {
            currentSize += file.fileData.length;
        }
        return currentSize;
    }

    public Integer storageValue(CopyOnWriteArrayList<FileModel> storage) {
        int currentSize = 0;
        for (FileModel file : storage) {
            currentSize += file.fileData.length;
        }
        return currentSize;
    }

    private boolean fileCanBeInStorage(FileModel fileModel) {
        int currentSize = 0;
        for (FileModel file : storage) {
            currentSize += file.fileData.length;
        }
        return currentSize + fileModel.fileData.length <= maxStorage;
    }

    private FileModel isFileInStorage(FileModel file) {
        FileModel returnedFile = null;
        try {
            returnedFile = this.storage.stream().filter(f -> f.fileId.equals(file.fileId)).findFirst().get();
        } catch (NoSuchElementException e) {
        }
        if (returnedFile != null) {
            return returnedFile;
        }
        return null;
    }

    private void writeToFile(Peer p, FileModel received) {
        System.out.println(received.filePath);
        final String pathName = "tmp/" + p.getPeerAp() + "/files/";
        Path path = Paths.get(pathName + "storage/");
        try {
            Files.createDirectories(path);
        } catch (FileAlreadyExistsException e) {
            // the directory already exists.
        } catch (IOException e) {
            e.printStackTrace();
        }
        String filePathName = pathName + "/storage/" + received.fileId;
        ObjectOutputStream out;
        try {
            out = new ObjectOutputStream(new FileOutputStream(filePathName));
            out.write(received.fileData);
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean addFileToStorage(FileModel fileModel, Peer p) {
        if (fileCanBeInStorage(fileModel)) {
            storage.add(fileModel);
            this.writeToFile(p, fileModel);
            System.out.println("Saved file: " + fileModel.fileId);
            return true;
        } else {
            return false;
        }
    }

    public boolean removeFileFromStorage(FileModel fileModel, Peer p) {
        FileModel isInStorage = isFileInStorage(fileModel);
        if (isInStorage != null) {
            this.storage.remove(isInStorage);
            this.deleteFileFromMemory(p, fileModel);
            System.out.println("File " + fileModel.filePath + " deleted from storage");
            return true;
        }
        return false;
    }

    private void deleteFileFromMemory(Peer p, FileModel fileModel) {
        String directory = "tmp/" + p.getPeerAp() + "/files/storage/" + fileModel.fileId;
        Path path = Paths.get(directory);
        try {
            Files.delete(path);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public CopyOnWriteArrayList<FileModel> getStorage() {
        return storage;
    }

    public void removeFileFromStorage(String fileId) {
        for (FileModel file : storage) {
            if (file.getFilePath() == fileId) {
                storage.remove(file);
                break;
            }
        }
    }

    /**
     * @return the maxStorage
     */
    public int getMaxStorage() {
        return maxStorage;
    }

    /**
     * @param maxStorage the maxStorage to set
     */
    public void setMaxStorage(int maxStorage, Peer p) {
        this.maxStorage = maxStorage;
        storage.sort((s1, s2) -> s1.perceivedReplicationDegree - s2.perceivedReplicationDegree); // sort by
                                                                                                 // perceivedReplicationDegree
        CopyOnWriteArrayList<FileModel> filesToRemove = new CopyOnWriteArrayList<>();
        synchronized (storage) {
            int deleting = storageValue();
            for (FileModel saved : storage) {
                System.out.println("checking: " + saved.fileId);
                System.out.println("checking: " + this.maxStorage + " < " + deleting);
                if (this.maxStorage < deleting) {
                    filesToRemove.add(saved);
                }
                deleting -= saved.fileData.length;
            }
            storage.removeAll(filesToRemove);
            for (FileModel saved : filesToRemove) {
                FileModel dummy = new FileModel();
                dummy.fileId = saved.fileId;
                dummy.peer = saved.peer;
                dummy.retries = 5; // 5 actually
                dummy.filePath = saved.filePath;
                dummy.perceivedReplicationDegree = saved.perceivedReplicationDegree;
                dummy.replicationDegree = saved.replicationDegree;
                Send remove = new Send(Send.REMOVED, dummy, p.me);
                MessageDigest md3 = null;
                try {
                    md3 = MessageDigest.getInstance("SHA1");
                } catch (NoSuchAlgorithmException e2) {
                    e2.printStackTrace();
                }
                md3.reset();
                md3.update(saved.filePath.getBytes());
                byte[] hashBytes3 = md3.digest();
                BigInteger hashNum3 = new BigInteger(1, hashBytes3);
                int key3 = Math.abs(hashNum3.intValue()) % Peer.numDHT;
                System.out.println("Generated key " + key3 + " for file: " + dummy.fileId);
                Peer destination = null;
                try {
                    destination = ChordManager.find_successor(key3);
                    // if dest is myself then i increase the retries by 1 because I'm sending the
                    // file to myself first
                    if (destination.getSslEnginePort() == p.getSslEnginePort()) {
                        dummy.retries += 1;
                    }
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
                System.out.println("Going to delete: " + saved.fileId);
                deleteFileFromMemory(p, saved);
                remove.write(remove, Peer.PROTOCOL, destination.address, destination.getSslEnginePort());
            }
        }
        System.out.println("Successfully updated space to: " + maxStorage / 1000 + " KB");
    }
}
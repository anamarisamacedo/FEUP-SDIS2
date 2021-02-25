/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package models;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.CopyOnWriteArrayList;

import utils.FileIdEncrypter;

/**
 *
 * @author guilh
 */
public class FileModel implements Serializable {

	/**
	 *
	 */
	private static final long serialVersionUID = -4033707300739810457L;
	public String filePath;
	public int replicationDegree;
	public int perceivedReplicationDegree;
	public byte[] fileData;
	public String fileId;
	public int retries = 5;
	public boolean toWait = true;
	public int noOfChunks;
	public int chunkId;
	public boolean updateReplication = false;
	public Peer peer;

	public CopyOnWriteArrayList<Chunk> receivedChunks = new CopyOnWriteArrayList<>();

	public FileModel(String filePath, int replicationDegree, Peer p) {
		this.filePath = filePath;
		this.replicationDegree = replicationDegree;
		this.perceivedReplicationDegree = 0;
		this.fileId = FileIdEncrypter
				.getSha256(this.filePath + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date()));
		this.peer = p;
	}

	public FileModel() {
	}

	public boolean replicationDegreeAchieved() {
		return perceivedReplicationDegree == replicationDegree;
	}

	public void loadFileContent() {
		File file = new File(this.filePath);
		try {
			fileData = Files.readAllBytes(file.toPath());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * @return the fileId
	 */
	public String getFilePath() {
		return filePath;
	}

	/**
	 * @param fileId the fileId to set
	 */
	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}

	/**
	 * @return the replicationDegree
	 */
	public int getReplicationDegree() {
		return replicationDegree;
	}

	/**
	 * @param replicationDegree the replicationDegree to set
	 */
	public void setReplicationDegree(int replicationDegree) {
		this.replicationDegree = replicationDegree;
	}

}
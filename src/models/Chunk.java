/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package models;

import java.io.Serializable;
import java.util.Comparator;

/**
 *
 * @author guilh
 */
public class Chunk implements Serializable, Comparator<Chunk> {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
	private String fileId;
	private int chunkNumber;
	private int chunkSize;
	private int chunkReplicationDegree;
	private String senderPeer;
	private byte[] chunkInfo = new byte[64000];

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}

		if (!Chunk.class.isAssignableFrom(obj.getClass())) {
			return false;
		}

		final Chunk other = (Chunk) obj;
		if (this.chunkNumber != other.chunkNumber) {
			return false;
		}
		if (!this.fileId.equals(other.fileId)) {
			return false;
		}
		return true;
	}

	public Chunk(String fileId, int chunkNumber, int chunkSize, int chunkReplicationDegree, byte[] chunkInfo) {
		this.fileId = fileId;
		this.chunkNumber = chunkNumber;
		this.chunkSize = chunkSize;
		this.chunkReplicationDegree = chunkReplicationDegree;
		this.chunkInfo = chunkInfo;
	}

	public Chunk(String fileId, int chunkNo, byte[] chunkInfo) {
		this.fileId = fileId;
		this.chunkNumber = chunkNo;
		this.chunkInfo = chunkInfo;
	}

	public Chunk(String fileId, int chunkNo, String senderPeer) {
		this.fileId = fileId;
		this.chunkNumber = chunkNo;
		this.senderPeer = senderPeer;
	}

	public Chunk() {
	}

	/**
	 * @return the chunkNumber
	 */
	public int getChunkNumber() {
		return chunkNumber;
	}

	/**
	 * @param chunkNumber the chunkNumber to set
	 */
	public void setChunkNumber(int chunkNumber) {
		this.chunkNumber = chunkNumber;
	}

	/**
	 * @return the chunkSize
	 */
	public int getChunkSize() {
		return chunkSize;
	}

	/**
	 * @param chunkSize the chunkSize to set
	 */
	public void setChunkSize(int chunkSize) {
		this.chunkSize = chunkSize;
	}

	/**
	 * @return the chunkInfo
	 */
	public byte[] getChunkInfo() {
		return chunkInfo;
	}

	/**
	 * @param chunkInfo the chunkInfo to set
	 */
	public void setChunkInfo(byte[] chunkInfo) {
		this.chunkInfo = chunkInfo;
	}

	/**
	 * @return the fileId
	 */
	public String getFileId() {
		return fileId;
	}

	/**
	 * @param fileId the fileId to set
	 */
	public void setFileId(String fileId) {
		this.fileId = fileId;
	}

	/**
	 * @return the chunkReplicationDegree
	 */
	public int getChunkReplicationDegree() {
		return chunkReplicationDegree;
	}

	/**
	 * @param chunkReplicationDegree the chunkReplicationDegree to set
	 */
	public void setChunkReplicationDegree(int chunkReplicationDegree) {
		this.chunkReplicationDegree = chunkReplicationDegree;
	}

	@Override
	public int compare(Chunk o1, Chunk o2) {
		if (o1.getChunkNumber() < o2.getChunkNumber()) {
			return -1;
		} else if (o1.getChunkNumber() > o2.getChunkNumber()) {
			return 1;
		}
		return 0;
	}

	/**
	 * @return the senderPeer
	 */
	public String getSenderPeer() {
		return senderPeer;
	}

	/**
	 * @param senderPeer the senderPeer to set
	 */
	public void setSenderPeer(String senderPeer) {
		this.senderPeer = senderPeer;
	}

}
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package protocolActions;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.math.BigInteger;
import java.util.Arrays;

import communicationChannels.client.ClientChannel;
import models.FileModel;
import models.Peer;

public class Send implements Serializable {

	/**
	 *
	 */
	private static final long serialVersionUID = -5413808701020710014L;
	public final static String CONFIRMATION = "CONFIRMATION";
	public static final String PEER = "PEER";
	public static final String BACKUP = "BACKUP";
	public static final String CONFIRM_BACKUP = "CONFIRM_BACKUP";
	public static final String RESTORE = "RESTORE";
	public static final String CONFIRM_RESTORE = "CONFIRM_RESTORE";
	public static final String DELETE = "DELETE";
	public static final String REMOVED = "REMOVED";
	public static final String NEW_BACKUP = "NEW_BACKUP";
	public static final String RECLAIMED = "RECLAIMED";
	public final static String REGISTER_PEER = "REGISTER_PEER";
	public final static String FINISH_JOINING = "FINISH_JOINING";
	public final static String SET_PREDECESSOR = "SET_PREDECESSOR";
	public final static String GET_PREDECESSOR = "GET_PREDECESSOR";
	public final static String FIND_SUCESSOR = "FIND_SUCESSOR";
	public final static String GET_SUCESSOR = "GET_SUCESSOR";
	public final static String CLOSEST_PREDECESSOR = "CLOSEST_PREDECESSOR";
	public final static String UPDATE_FINGER_TABLE = "UPDATE_FINGER_TABLE";
	public final static String LOOKUP = "LOOKUP";
	public final static String LENGTH_SEPARATOR = " " + (char) 0x0D + (char) 0x0A + (char) 0x0D + (char) 0x0A
			+ (char) 0x0D + (char) 0x0A + (char) 0x0D + (char) 0x0A;
	public static final String NOTIFY = "NOTIFY";
	public static final String ENOUGH_NODES = "ENOUGH_NODES";
	public String operation;
	public String message = null;
	public FileModel file = null;
	public Peer peer = null;
	public boolean enough = false;
	public int desiredReplicationDegree = 0;
	public int MAX_FILE_DATA = 1024 * 12; // 12 KB

	// private String senderId;
	public Send(String operation, int desiredReplicationDegree) {
		this.operation = operation;
		this.desiredReplicationDegree = desiredReplicationDegree;
	}

	public Send(final String operation, final String message) {
		this.operation = operation;
		this.message = message;
	}

	public Send(String operation, Peer peer) {
		this.operation = operation;
		this.peer = peer;
	}

	public Send(String operation, FileModel file, Peer p) {
		this.operation = operation;
		this.file = file;
		this.peer = p;
	}

	public Send(String operation) {
		this.operation = operation;
	}

	public Send() {
	}

	/**
	 * @return String
	 */
	public String toString() {
		return "Operation: " + operation + (message != null ? " Message: " + message : "")
				+ (file != null ? " File: " + file.getFilePath() + " size: " + file.fileData.length : "")
				+ (peer != null
						? String.format(" Peer id:%d, address:%s, port:%d ", peer.getPeerId(), peer.address,
								peer.getSslEnginePort())
						: "");
	}

	/**
	 * @param send
	 * @return byte[]
	 */
	protected byte[] appendFinalInfo(final byte[] send) {
		final int sendLength = send.length;
		final byte[] ret = new byte[send.length + LENGTH_SEPARATOR.getBytes().length
				+ BigInteger.valueOf(sendLength).toByteArray().length + LENGTH_SEPARATOR.getBytes().length];
		System.arraycopy(send, 0, ret, 0, send.length);
		System.arraycopy(LENGTH_SEPARATOR.getBytes(), 0, ret, send.length, LENGTH_SEPARATOR.getBytes().length);
		System.arraycopy(BigInteger.valueOf(sendLength).toByteArray(), 0, ret,
				send.length + LENGTH_SEPARATOR.getBytes().length, BigInteger.valueOf(sendLength).toByteArray().length);
		System.arraycopy(LENGTH_SEPARATOR.getBytes(), 0, ret,
				send.length + LENGTH_SEPARATOR.getBytes().length + BigInteger.valueOf(sendLength).toByteArray().length,
				LENGTH_SEPARATOR.getBytes().length);
		return ret;
	}

	/**
	 * @return byte[]
	 */
	public byte[] appendFinalInfo() {
		try {
			final ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
			final ObjectOutputStream objStream = new ObjectOutputStream(byteStream);
			objStream.writeObject(this);
			byte[] send = byteStream.toByteArray();
			final int sendLength = send.length;
			final byte[] ret = new byte[send.length + LENGTH_SEPARATOR.getBytes().length
					+ BigInteger.valueOf(sendLength).toByteArray().length + LENGTH_SEPARATOR.getBytes().length];
			System.arraycopy(send, 0, ret, 0, send.length);
			System.arraycopy(LENGTH_SEPARATOR.getBytes(), 0, ret, send.length, LENGTH_SEPARATOR.getBytes().length);
			System.arraycopy(BigInteger.valueOf(sendLength).toByteArray(), 0, ret,
					send.length + LENGTH_SEPARATOR.getBytes().length,
					BigInteger.valueOf(sendLength).toByteArray().length);
			System.arraycopy(LENGTH_SEPARATOR.getBytes(), 0, ret, send.length + LENGTH_SEPARATOR.getBytes().length
					+ BigInteger.valueOf(sendLength).toByteArray().length, LENGTH_SEPARATOR.getBytes().length);
			return ret;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null; // to make the compiler happy
	}

	/**
	 * @param send
	 * @param protocol
	 * @param hostAdress
	 * @param hostPort
	 */
	public void write(final Send send, final String protocol, final String hostAdress, final int hostPort) {
		try {
			ClientChannel clientChannel = new ClientChannel(protocol, hostAdress, hostPort);
			ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
			ObjectOutputStream objStream = new ObjectOutputStream(byteStream);
			objStream.writeObject(send);
			objStream.flush();
			clientChannel.write(appendFinalInfo(byteStream.toByteArray()));
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * @param send
	 * @param peer
	 * @param protocol
	 * @param hostAdress
	 * @param hostPort
	 * @return Send
	 */
	public Send writeAndRead(final Send send, final Peer peer, final String protocol, final String hostAdress,
			final int hostPort) {
		try {
			ClientChannel clientChannel = new ClientChannel(protocol, hostAdress, hostPort);
			if (clientChannel.engine == null) {
				return null;
			}
			ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
			ObjectOutputStream objStream = new ObjectOutputStream(byteStream);
			objStream.writeObject(send);
			objStream.flush();
			return clientChannel.writeAndRead(appendFinalInfo(byteStream.toByteArray()));
		} catch (final Exception e) {
			// e.printStackTrace();
		}
		return null; // to make to compiler happy
	}

	/**
	 * @param operation
	 * @param peer
	 * @param protocol
	 * @param hostAdress
	 * @param hostPort
	 */
	public void writeFile(String operation, Peer peer, String protocol, String hostAdress, int hostPort) {
		byte[][] ret = new byte[(int) Math.ceil(this.file.fileData.length / (double) MAX_FILE_DATA)][];
		int start = 0;
		for (int i = 0; i < ret.length; i++) {
			FileModel dummy = new FileModel();
			dummy.filePath = this.file.filePath;
			dummy.replicationDegree = this.file.replicationDegree;
			dummy.perceivedReplicationDegree = this.file.perceivedReplicationDegree;
			dummy.fileId = file.fileId;
			dummy.retries = file.retries;
			dummy.toWait = file.toWait;
			dummy.noOfChunks = ret.length;
			dummy.chunkId = i;
			dummy.updateReplication = file.updateReplication;
			if (start + MAX_FILE_DATA > this.file.fileData.length) {
				ret[i] = new byte[this.file.fileData.length - start];
				System.arraycopy(this.file.fileData, start, ret[i], 0, this.file.fileData.length - start);
				dummy.fileData = ret[i];
				Send send = new Send(operation, dummy, peer);
				write(send, protocol, hostAdress, hostPort);
			} else {
				ret[i] = new byte[MAX_FILE_DATA];
				System.arraycopy(this.file.fileData, start, ret[i], 0, MAX_FILE_DATA);
				dummy.fileData = ret[i];
				Send send = new Send(operation, dummy, peer);
				write(send, protocol, hostAdress, hostPort);
			}
			start += MAX_FILE_DATA;
		}
	}

	/**
	 * @param message
	 * @return Send
	 */
	public static Send removeFinalInfo(final byte[] message) {
		final int length = message.length;
		int pos = 0;
		while (pos < length) {
			if (message[pos] == 13 && message[pos + 1] == 10 && message[pos + 2] == 13 && message[pos + 3] == 10
					&& message[pos + 4] == 13 && message[pos + 5] == 10 && message[pos + 6] == 13
					&& message[pos + 7] == 10) {
				break;
			}
			pos++;
		}
		final byte[] sendArray = Arrays.copyOfRange(message, 0, pos - 1);
		final ByteArrayInputStream byteInput = new ByteArrayInputStream(sendArray);
		ObjectInputStream is;
		try {
			is = new ObjectInputStream(byteInput);
			final Send send = (Send) is.readObject();
			return send;
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return null; // to make the compiler happy
	}
}
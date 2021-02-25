/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package models;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import chord.ChordManager;
import chord.FingerTable;
import communicationChannels.peer.server.ServerChannel;
import protocolActions.Send;
import rmi_interface.RMI_Interface;
// import threads.BackupThread;
// import threads.DeleteThread;
import threads.ReclaimThread;
// import threads.RestoreThread;
// import threads.SendSingleChunkThread;
import threads.BackupThread;
import threads.RestoreThread;
import threads.DeleteThread;

/**
 *
 * @author guilh
 */
public class Peer implements RMI_Interface, Serializable {

	/**
	 *
	 */
	public static final String PROTOCOL = "TLSv1.2";
	private static final long serialVersionUID = -4714405236610413456L;
	private int peerId;
	private static ServerChannel mainChannel;
	private ScheduledExecutorService threadPoolExecutor;
	private static final int sizeOfPool = Runtime.getRuntime().availableProcessors() + 1;
	public Storage storage;
	public CopyOnWriteArrayList<FileModel> initiatorPeerFiles;
	private static boolean enhanced;
	public ConcurrentHashMap<String, CopyOnWriteArrayList<Chunk>> peerRepliesMap;
	public Deque<Peer> nodesChord;
	public int sslEnginePort;
	public String address;
	public String centralizedChordManagerAddress;
	public int centralizedChordManagerPort;
	public int maxPeers;
	public Peer me, pred;
	public int maxPeersBytes;
	public static FingerTable[] finger;
	public static int numDHT;
	public ChordManager chord;

	public Peer(final int peerId, final int sslEnginePort, String centralizedChordManagerAddress,
			int centralizedChordManagerPort, String address) throws IOException {
		this.peerId = peerId;
		this.sslEnginePort = sslEnginePort;
		try {
			mainChannel = new ServerChannel(this, "TLSv1.2", address, sslEnginePort);
			// Register this peer on Centralized server
		} catch (Exception e) {
			e.printStackTrace();

		}
		this.threadPoolExecutor = Executors.newScheduledThreadPool(100);
		this.storage = new Storage();
		this.initiatorPeerFiles = new CopyOnWriteArrayList<FileModel>();
		this.peerRepliesMap = new ConcurrentHashMap<>();
		this.nodesChord = new ArrayDeque<>();
		this.address = address; // CHANGE TO OTHER IP IF NEEDED
		this.centralizedChordManagerAddress = centralizedChordManagerAddress;
		this.centralizedChordManagerPort = centralizedChordManagerPort;
	}

	public Peer(final int peerId, String address, final int sslEnginePort) {
		this.peerId = peerId;
		this.sslEnginePort = sslEnginePort;
		this.address = address;
		// this.nodesChord = new ArrayDeque<>();

	}

	public static void main(final String[] args) {
		try {
			if (args.length < 7) {
				System.out.println("Not a sufficient number of arguments. Please fix this and run the program again.");
				return;
			}

			final String peerId = args[0];
			final String access_point = args[1];
			final String serverPort = args[2];
			final String centralizedChordManagerAddress = args[3];
			final int centralizedChordManagerPort = Integer.parseInt(args[4]);
			String address = args[6];
			final Peer peer = new Peer(Integer.parseInt(peerId), Integer.parseInt(serverPort),
					centralizedChordManagerAddress, centralizedChordManagerPort, address);
			// Cleans this peer info on peer.bin file
			Runtime.getRuntime().addShutdownHook(new Thread("app-shutdown-hook") {
				public void run() {
					peer.getThreadPoolExecutor().shutdownNow();
					try {
						peer.savePeer();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			});
			peer.getThreadPoolExecutor().execute(mainChannel);

			// CENTRALIZED CHORD MANAGER

			int maxNumPeers = Integer.parseInt(args[5]);
			peer.maxPeersBytes = (int) Math.ceil(Math.log(maxNumPeers) / Math.log(2));
			finger = new FingerTable[peer.maxPeersBytes + 1];
			numDHT = (int) Math.pow(2, peer.maxPeersBytes);
			System.out.println("Registering to Centralized Chord Manager");

			Send send = new Send(Send.REGISTER_PEER, peer.address + ";" + peer.sslEnginePort);
			String initInfo = send.writeAndRead(send, peer, Peer.PROTOCOL, centralizedChordManagerAddress,
					centralizedChordManagerPort).message;
			String[] tokens = initInfo.split("/");
			peer.me = new Peer(Integer.parseInt(tokens[0]), peer.address, Integer.parseInt(serverPort));
			peer.pred = new Peer(Integer.parseInt(tokens[1]), tokens[2], Integer.parseInt(tokens[3]));
			peer.peerId = peer.me.getPeerId();

			System.out.println("Peer ID" + peer.me.getPeerId() + ". Predecessor ID: " + peer.pred.getPeerId());

			// CENTRALIZED CHORD MANAGER

			// INITIATE CHORD MANAGER

			peer.chord = new ChordManager(peer);

			// INITIATE CHORD MANAGER
			final RMI_Interface stub = (RMI_Interface) UnicastRemoteObject.exportObject(peer, 0);
			final Registry registry = LocateRegistry.getRegistry();
			registry.bind(access_point, stub);
			peer.createDirectories();
			peer.loadPeer();
			System.err.println("Peer with ID of " + peer.peerId + " created");

			peer.getThreadPoolExecutor().scheduleAtFixedRate(peer.chord, 0, 5,
			TimeUnit.SECONDS);
		} catch (final Exception e) {
			System.err.println("Client exception: " + e.toString());
			e.printStackTrace();
		}
	}

	private void createDirectories() {
		String pathName = "tmp/" + this.getPeerAp() + "/data/";
		File directory = new File(pathName);
		if (!directory.exists()) {
			directory.mkdirs();
		}
		pathName = "tmp/" + this.getPeerAp() + "/files/";
		directory = new File(pathName);
		if (!directory.exists()) {
			directory.mkdirs();
		}
		pathName = "tmp/" + this.getPeerAp() + "/files/restored/";
		directory = new File(pathName);
		if (!directory.exists()) {
			directory.mkdirs();
		}
		pathName = "tmp/" + this.getPeerAp() + "/files/storage/";
		directory = new File(pathName);
		if (!directory.exists()) {
			directory.mkdirs();
		}
	}

	protected void savePeer() throws FileNotFoundException, IOException {
		final String pathName = "tmp/" + this.getPeerAp() + "/data/";
		final File directory = new File(pathName);
		if (!directory.exists()) {
			directory.mkdirs();

		}
		final ObjectOutputStream out = new ObjectOutputStream(
				new FileOutputStream(pathName + this.getPeerAp() + ".bin"));
		try {
			SaveablePeer save = new SaveablePeer(this.initiatorPeerFiles, this.storage, this.peerRepliesMap);
			out.writeObject(save);
		} finally {
			out.close();
		}
	}

	private void loadPeer() throws FileNotFoundException, IOException, ClassNotFoundException {
		final String pathName = "tmp/" + this.getPeerAp() + "/data/" + this.getPeerAp() + ".bin";
		final File file = new File(pathName);
		if (file.exists()) {
			// loads
			final ObjectInputStream in = new ObjectInputStream(new FileInputStream(pathName));
			try {
				SaveablePeer p = (SaveablePeer) in.readObject();
				// Peer has been successfully created if files object is not null
				if (p.files != null) {
					this.initiatorPeerFiles = p.files;
					this.storage = p.storage;
					this.peerRepliesMap = p.peerRepliesMap;
				}
			} finally {
				in.close();
			}
		}

	}

	// Cleans this peer info on peers.txt file
	public static void receiveMessage(final byte[] messageReceived) {
		// console.lo
		// p.backup
	}

	public synchronized void backup(final String path, final int replicationDegree, final String senderId)
			throws Exception {
		// Send putchunk message to BackupThread
		File directory = new File(path);
		if (!directory.exists()) {
			System.out.println("Could not find file " + path);
			return;
		}
		final BackupThread backup = new BackupThread(this, path, replicationDegree, senderId, enhanced);
		this.getThreadPoolExecutor().execute(backup);
	}

	public synchronized void restore(final String path, final String senderId) throws RemoteException {
		// Check if initiator per of restore matches the one used on backup
		for (FileModel file : initiatorPeerFiles) {
			if (file.getFilePath().equals(path)) {
				final RestoreThread restore = new RestoreThread(this, path, senderId, file);
				this.getThreadPoolExecutor().execute(restore);
				break;
			}
		}
	}

	public synchronized void delete(final String path, final String senderId) {
		List<FileModel> fileModelsToRemove = new ArrayList<>();
		for (FileModel file : this.initiatorPeerFiles) {
			if (file.getFilePath().equals(path)) {
				fileModelsToRemove.add(file);
			}
		}
		if (!fileModelsToRemove.isEmpty()) {
			final DeleteThread delete = new DeleteThread(this, fileModelsToRemove, path, senderId);
			this.getThreadPoolExecutor().execute(delete);
		}
	}

	public synchronized void reclaim(int spaceToReclaim, String senderId) throws RemoteException {
		final ReclaimThread reclaim = new ReclaimThread(this, spaceToReclaim, senderId);
		this.getThreadPoolExecutor().execute(reclaim);
	}

	/**
	 * @return the peerId
	 */
	public int getPeerId() {
		return peerId;
	}

	public synchronized String getPeerAp() {
		return "Peer" + peerId;
	}

	/**
	 * @return the threadPoolExecutor
	 */
	public ScheduledExecutorService getThreadPoolExecutor() {
		return threadPoolExecutor;
	}

	/**
	 * @return the files
	 */
	public CopyOnWriteArrayList<FileModel> getFiles() {
		return initiatorPeerFiles;
	}

	/**
	 * @param files the files to set
	 */
	public void setFiles(CopyOnWriteArrayList<FileModel> files) {
		this.initiatorPeerFiles = files;
	}

	public boolean isEnhanced() {
		return enhanced;
	}

	public void setEnhanced(boolean enhanced) {
		Peer.enhanced = enhanced;
	}

	/**
	 * @return the peerResponseMap
	 */
	public ConcurrentHashMap<String, CopyOnWriteArrayList<Chunk>> getPeerResponseMap() {
		return peerRepliesMap;
	}

	/**
	 * @param peerResponseMap the peerResponseMap to set
	 */
	public void setPeerResponseMap(ConcurrentHashMap<String, CopyOnWriteArrayList<Chunk>> peerRepliesMap) {
		this.peerRepliesMap = peerRepliesMap;
	}

	public static ServerChannel getMainChannel() {
		return mainChannel;
	}

	/**
	 * @return the sslEnginePort
	 */
	public int getSslEnginePort() {
		return sslEnginePort;
	}

	/**
	 * @param sslEnginePort the sslEnginePort to set
	 */
	public void setSslEnginePort(int sslEnginePort) {
		this.sslEnginePort = sslEnginePort;
	}

	/**
	 * @return the sizeofpool
	 */
	public static int getSizeofpool() {
		return sizeOfPool;
	}

	public void writeFileModelToDirectory(FileModel file) {
		FileOutputStream fos = null;
		String directory_builder = "tmp/" + this.getPeerAp() + "/files/" + "restored/";
		String[] directories = file.filePath.split("\\/");
		if (directories.length > 0) {
			for (int i = 0; i < directories.length - 1; i++) {
				directory_builder += directories[i] += "\\/";
			}
			File directory;
			directory = new File(directory_builder);
			if (!directory.exists()) {
				directory.mkdirs();
			}
		}
		try {
			fos = new FileOutputStream(directory_builder + directories[directories.length - 1]);
			fos.write(file.fileData);
			if (fos != null) {
				fos.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

}
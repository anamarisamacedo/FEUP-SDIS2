package communicationChannels.centralizedChordManager.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.rmi.NotBoundException;
import java.security.SecureRandom;
import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLException;

import communicationChannels.Channel;
import communicationChannels.centralizedChordManager.actions.ServerChannelAction;
import models.CentralizedChordManager;
import protocolActions.Send;

/**
 */
public class ServerChannel extends Channel implements Runnable {

    public static void main(final String args[]) throws IOException {
        if (args.length < 1) {
            System.out.println("Usage: java SSLServer <port> <cypher-suite>*");
            return;
        }
    }

    private boolean active;

    private SSLContext context;
    private static CopyOnWriteArrayList<String> connectedPeers = new CopyOnWriteArrayList<>();

    private Selector selector;
    private int port;
    private ServerSocketChannel serverSocketChannel;
    private CentralizedChordManager chordManager;

    /**
     * @throws Exception
     */
    public ServerChannel(String protocol, String hostAddress, int port, CentralizedChordManager chordManager)
            throws Exception {
        this.port = port;
        this.chordManager = chordManager;
        context = SSLContext.getInstance(protocol);
        context.init(createKeyManagers("communicationChannels/server.keys", "123456", "123456"),
                createTrustManagers("communicationChannels/truststore", "123456"), new SecureRandom());

        selector = SelectorProvider.provider().openSelector();
        this.serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.socket().bind(new InetSocketAddress(hostAddress, port));
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

        active = true;
        System.out.println("SSLEngine server opened at port: " + port);
    }

    /**
     *
     * @throws Exception
     */
    public void start() {
        myAppData = ByteBuffer.allocate(64000 + 700);
        myNetData = ByteBuffer.allocate(64000 + 700);
        peerAppData = ByteBuffer.allocate(64000 + 700);
        peerNetData = ByteBuffer.allocate(64000 + 700);
        while (true) {

            try {
                selector.select();
                Iterator<SelectionKey> selectedKeys = selector.selectedKeys().iterator();
                while (selectedKeys.hasNext()) {
                    SelectionKey key = selectedKeys.next();
                    selectedKeys.remove();
                    if (!key.isValid()) {
                        continue;
                    }
                    if (key.isAcceptable()) {
                        if (!accept(key))
                            continue;
                    } else if (key.isReadable()) {
                        read((SocketChannel) key.channel(), (SSLEngine) key.attachment());
                    }

                }
            } catch (Exception e) {
                // e.printStackTrace();
            }

        }
    }

    /**
     * @throws Exception
     */
    private boolean accept(SelectionKey key) throws Exception {

        SocketChannel socketChannel = ((ServerSocketChannel) key.channel()).accept();
        if (socketChannel == null) {
            return false;
        }
        socketChannel.configureBlocking(false);

        SSLEngine engine = context.createSSLEngine();
        engine.setUseClientMode(false);
        engine.beginHandshake();

        if (doHandshake(socketChannel, engine)) {
            socketChannel.register(selector, SelectionKey.OP_READ, engine);
            // received_keys.add(socketChannel);
        } else {
            socketChannel.close();
        }
        return true;
    }

    /**
    
     */
    @Override
    protected void read(SocketChannel socketChannel, SSLEngine engine) {
        peerNetData.clear();
        int bytesRead;
        try {
            bytesRead = socketChannel.read(peerNetData);
            if (bytesRead > 0) {
                peerNetData.flip();
                while (peerNetData.hasRemaining()) {
                    peerAppData.clear();
                    SSLEngineResult result = engine.unwrap(peerNetData, peerAppData);
                    switch (result.getStatus()) {
                        case OK:
                            peerAppData.flip();
                            break;
                        case BUFFER_OVERFLOW:
                            peerAppData = enlargeApplicationBuffer(engine, peerAppData);
                            break;
                        case BUFFER_UNDERFLOW:
                            peerNetData = handleBufferUnderflow(engine, peerNetData);
                            break;
                        case CLOSED:
                            closeConnection(socketChannel, engine);
                            return;
                        default:
                            throw new IllegalStateException("Invalid SSL status: " + result.getStatus());
                    }
                }
                Send received = Send.removeFinalInfo(peerAppData.array());
                write(socketChannel, engine, received);
            } else if (bytesRead < 0) {
                handleEndOfStream(socketChannel, engine);
            }
        } catch (IOException e) {
            // e.printStackTrace();
        }

    }

    /**
     */
    @Override
    protected void write(SocketChannel channel, SSLEngine engine, Send received) {
        synchronized (this) {
            try {
                byte[] message = ServerChannelAction.parseMessage(received, this.chordManager).appendFinalInfo();
                myAppData.clear();
                myAppData.put(message);
                myAppData.flip();
                while (myAppData.hasRemaining()) {
                    // The loop has a meaning for (outgoing) messages larger than 16KB.
                    // Every wrap call will remove 16KB from the original message and send it to the
                    // remote peer.
                    myNetData.clear();
                    SSLEngineResult result;
                    try {
                        result = engine.wrap(myAppData, myNetData);
                        switch (result.getStatus()) {
                            case OK:
                                myNetData.flip();
                                while (myNetData.hasRemaining()) {
                                    channel.write(myNetData);
                                }
                                break;
                            case BUFFER_OVERFLOW:
                                myNetData = enlargePacketBuffer(engine, myNetData);
                                break;
                            case BUFFER_UNDERFLOW:
                                throw new SSLException(
                                        "Buffer underflow occured after a wrap. I don't think we should ever get here.");
                            case CLOSED:
                                closeConnection(channel, engine);
                                return;
                            default:
                                throw new IllegalStateException("Invalid SSL status: " + result.getStatus());
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

            } catch (NumberFormatException | NotBoundException | IOException | InterruptedException e1) {
                e1.printStackTrace();
            }

        }
    }

    public void write(byte[] nothing) throws IOException {

    }

    @Override
    public void run() {
        try {
            start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void write(SocketChannel socketChannel, SSLEngine engine, byte[] message) throws Exception {
        // TODO Auto-generated method stub
    }

    /**
     * @return the active
     */
    public boolean isActive() {
        return active;
    }

    /**
     * @param active the active to set
     */
    public void setActive(boolean active) {
        this.active = active;
    }

    /**
     * @return the connectedPeers
     */
    public static CopyOnWriteArrayList<String> getConnectedPeers() {
        return connectedPeers;
    }

    /**
     * @param connectedPeers the connectedPeers to set
     */
    public static void setConnectedPeers(CopyOnWriteArrayList<String> connectedPeers) {
        ServerChannel.connectedPeers = connectedPeers;
    }

    /**
     * @return the port
     */
    public int getPort() {
        return port;
    }

    /**
     * @param port the port to set
     */
    public void setPort(int port) {
        this.port = port;
    }

}

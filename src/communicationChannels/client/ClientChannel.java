package communicationChannels.client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.security.SecureRandom;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLException;

import communicationChannels.Channel;
import protocolActions.Send;

/**
 */
public class ClientChannel extends Channel implements Runnable {

    private String remoteAddress;

    private int port;

    public SSLEngine engine;

    private SocketChannel socketChannel;

    private String protocol;
    private byte[] message;
    private Send received = new Send();

    /**
     * @throws Exception
     */
    public ClientChannel(String protocol, String remoteAddress, int port) {
        try {
            this.protocol = protocol;
            this.remoteAddress = remoteAddress;
            this.port = port;
            SSLContext context;
            context = SSLContext.getInstance(protocol);
            context.init(createKeyManagers("communicationChannels/client.keys", "123456", "123456"),
                    createTrustManagers("communicationChannels/truststore", "123456"), new SecureRandom());

            engine = context.createSSLEngine(remoteAddress, port);

            engine.setUseClientMode(true);

            myAppData = ByteBuffer.allocate(1000 * 20);
            myNetData = ByteBuffer.allocate(1000 * 20);
            peerAppData = ByteBuffer.allocate(1000 * 20);
            peerNetData = ByteBuffer.allocate(1000 * 20);
            connect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            write(message);
            read();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public Send writeAndRead(byte[] message) {
        try {
            write(message);
            read();
            shutdown();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return received;
    }

    /**
     * @throws Exception
     */
    protected boolean connect() {
        try {
            socketChannel = SocketChannel.open();
            socketChannel.configureBlocking(false);
            socketChannel.connect(new InetSocketAddress(remoteAddress, port));
            while (!socketChannel.finishConnect()) {
            }
            engine.beginHandshake();
            return doHandshake(socketChannel, engine);
            // return true;
        } catch (Exception e) {
            // e.printStackTrace();
            engine = null;
        }
        return false; // make the compiler happy
    }

    /**
     * Public method to send a message to the server.
     *
     * @param message - message to be sent to the server.
     * @throws IOException if an I/O error occurs to the socket channel.
     */
    public void write(byte[] message) throws IOException {
        write(socketChannel, engine, message);
    }

    /**
     */
    @Override
    protected void write(SocketChannel socketChannel, SSLEngine engine, byte[] message) throws IOException {
        myAppData.clear();
        myAppData.put(message);
        myAppData.flip();
        while (myAppData.hasRemaining()) {
            // The loop has a meaning for (outgoing) messages larger than 16KB.
            // Every wrap call will remove 16KB from the original message and send it to the
            // remote peer.
            myNetData.clear();
            SSLEngineResult result = engine.wrap(myAppData, myNetData);
            switch (result.getStatus()) {
                case OK:
                    myNetData.flip();
                    while (myNetData.hasRemaining()) {
                        socketChannel.write(myNetData);
                    }
                    break;
                case BUFFER_OVERFLOW:
                    myNetData = enlargePacketBuffer(engine, myNetData);
                    break;
                case BUFFER_UNDERFLOW:
                    throw new SSLException(
                            "Buffer underflow occured after a wrap. I don't think we should ever get here.");
                case CLOSED:
                    closeConnection(socketChannel, engine);
                    return;
                default:
                    throw new IllegalStateException("Invalid SSL status: " + result.getStatus());
            }
        }
    }

    /**
     * Public method to try to read from the server.
     *
     * @throws Exception
     */
    public void read() {
        read(socketChannel, engine);
    }

    /**
     * @throws Exception
     */
    @Override
    protected void read(SocketChannel socketChannel, SSLEngine engine) {
        peerNetData.clear();
        boolean exitReadLoop = false;
        while (!exitReadLoop) {
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
                                received = Send.removeFinalInfo(peerAppData.array());
                                exitReadLoop = true;
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
                } else if (bytesRead < 0) {
                    handleEndOfStream(socketChannel, engine);
                    return;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     */
    public void shutdown() throws IOException {
        closeConnection(socketChannel, engine);
        executor.shutdown();
    }

    @Override
    protected void write(SocketChannel socketChannel, SSLEngine engine, Send message) throws Exception {
        // TODO Auto-generated method stub

    }

    /**
     * @return the protocol
     */
    public String getProtocol() {
        return protocol;
    }

    /**
     * @param protocol the protocol to set
     */
    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }
}

package communicationChannels;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.security.KeyStore;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import protocolActions.Send;

/**
 */
public abstract class Channel {

    protected ByteBuffer myAppData;

    protected ByteBuffer myNetData;

    protected ByteBuffer peerAppData;

    protected ByteBuffer peerNetData;

    protected ExecutorService executor = Executors.newSingleThreadExecutor();

    /**
     * @param socketChannel
     * @param socketChannel
     * @param engine
     * @param socketChannel
     * @param engine
     * @param socketChannel
     * @param engine
     * @throws IOException
     */
    protected abstract void read(SocketChannel socketChannel, SSLEngine engine) throws Exception;

    /**
     * @param socketChannel
     * @param engine
     * @param socketChannel
     * @param engine
     * @param socketChannel
     * @param engine
     * @throws IOException
     */
    protected abstract void write(SocketChannel socketChannel, SSLEngine engine, byte[] message) throws Exception;

    /**
     * @param socketChannel
     * @param engine
     * @param socketChannel
     * @param engine
     * @throws IOException
     */
    protected abstract void write(SocketChannel socketChannel, SSLEngine engine, Send message) throws Exception;

    /**
     * @throws IOException - if an error occurs during read/write to the socket
     *                     channel.
     */
    protected boolean doHandshake(SocketChannel socketChannel, SSLEngine engine) throws IOException {

        SSLEngineResult result;
        HandshakeStatus handshakeStatus;

        int appBufferSize = engine.getSession().getApplicationBufferSize();
        ByteBuffer myAppData = ByteBuffer.allocate(appBufferSize);
        ByteBuffer peerAppData = ByteBuffer.allocate(appBufferSize);
        myNetData.clear();
        peerNetData.clear();

        handshakeStatus = engine.getHandshakeStatus();
        while (handshakeStatus != SSLEngineResult.HandshakeStatus.FINISHED
                && handshakeStatus != SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING) {
            switch (handshakeStatus) {
                case NEED_UNWRAP:
                    if (socketChannel.read(peerNetData) < 0) {
                        if (engine.isInboundDone() && engine.isOutboundDone()) {
                            return false;
                        }
                        try {
                            engine.closeInbound();
                        } catch (SSLException e) {

                        }
                        engine.closeOutbound();
                        handshakeStatus = engine.getHandshakeStatus();
                        break;
                    }
                    peerNetData.flip();
                    try {
                        result = engine.unwrap(peerNetData, peerAppData);
                        peerNetData.compact();
                        handshakeStatus = result.getHandshakeStatus();
                    } catch (SSLException sslException) {

                        engine.closeOutbound();
                        handshakeStatus = engine.getHandshakeStatus();
                        break;
                    }
                    switch (result.getStatus()) {
                        case OK:
                            break;
                        case BUFFER_OVERFLOW:
                            peerAppData = enlargeApplicationBuffer(engine, peerAppData);
                            break;
                        case BUFFER_UNDERFLOW:
                            peerNetData = handleBufferUnderflow(engine, peerNetData);
                            break;
                        case CLOSED:
                            if (engine.isOutboundDone()) {
                                return false;
                            } else {
                                engine.closeOutbound();
                                handshakeStatus = engine.getHandshakeStatus();
                                break;
                            }
                        default:
                            throw new IllegalStateException("Invalid SSL status: " + result.getStatus());
                    }
                    break;
                case NEED_WRAP:
                    myNetData.clear();
                    try {
                        result = engine.wrap(myAppData, myNetData);
                        handshakeStatus = result.getHandshakeStatus();
                    } catch (SSLException sslException) {

                        engine.closeOutbound();
                        handshakeStatus = engine.getHandshakeStatus();
                        break;
                    }
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
                            try {
                                myNetData.flip();
                                while (myNetData.hasRemaining()) {
                                    socketChannel.write(myNetData);
                                }
                                peerNetData.clear();
                            } catch (Exception e) {

                                handshakeStatus = engine.getHandshakeStatus();
                            }
                            break;
                        default:
                            throw new IllegalStateException("Invalid SSL status: " + result.getStatus());
                    }
                    break;
                case NEED_TASK:
                    Runnable task;
                    while ((task = engine.getDelegatedTask()) != null) {
                        executor.execute(task);
                    }
                    handshakeStatus = engine.getHandshakeStatus();
                    break;
                case FINISHED:
                    break;
                case NOT_HANDSHAKING:
                    break;
                default:
                    throw new IllegalStateException("Invalid SSL status: " + handshakeStatus);
            }
        }

        return true;

    }

    /**
     * @param engine
     * @param buffer
     * @return ByteBuffer
     */
    protected ByteBuffer enlargePacketBuffer(SSLEngine engine, ByteBuffer buffer) {
        return enlargeBuffer(buffer, engine.getSession().getPacketBufferSize());
    }

    /**
     * @param engine
     * @param buffer
     * @return ByteBuffer
     */
    protected ByteBuffer enlargeApplicationBuffer(SSLEngine engine, ByteBuffer buffer) {
        return enlargeBuffer(buffer, engine.getSession().getApplicationBufferSize());
    }

    /**
     * @return A new buffer with a larger capacity.
     */
    protected ByteBuffer enlargeBuffer(ByteBuffer buffer, int sessionProposedCapacity) {
        if (sessionProposedCapacity > buffer.capacity()) {
            buffer = ByteBuffer.allocate(sessionProposedCapacity);
        } else {
            buffer = ByteBuffer.allocate(buffer.capacity() * 2);
        }
        return buffer;
    }

    /**
     * @throws Exception
     */
    protected ByteBuffer handleBufferUnderflow(SSLEngine engine, ByteBuffer buffer) {
        if (engine.getSession().getPacketBufferSize() < buffer.limit()) {
            return buffer;
        } else {
            ByteBuffer replaceBuffer = enlargePacketBuffer(engine, buffer);
            buffer.flip();
            replaceBuffer.put(buffer);
            return replaceBuffer;
        }
    }

    /**
     */
    protected void closeConnection(SocketChannel socketChannel, SSLEngine engine) throws IOException {
        engine.closeOutbound();
        doHandshake(socketChannel, engine);
        socketChannel.close();
    }

    /**
     */
    protected void handleEndOfStream(SocketChannel socketChannel, SSLEngine engine) throws IOException {
        try {
            engine.closeInbound();
        } catch (Exception e) {

        }
        closeConnection(socketChannel, engine);
    }

    /**
     * @throws Exception
     */
    protected KeyManager[] createKeyManagers(String filepath, String keystorePassword, String keyPassword)
            throws Exception {
        KeyStore keyStore = KeyStore.getInstance("JKS");
        InputStream keyStoreIS = new FileInputStream(filepath);
        try {
            keyStore.load(keyStoreIS, keystorePassword.toCharArray());
        } finally {
            if (keyStoreIS != null) {
                keyStoreIS.close();
            }
        }
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keyStore, keyPassword.toCharArray());
        return kmf.getKeyManagers();
    }

    /**
     * @throws Exception
     */
    protected TrustManager[] createTrustManagers(String filepath, String keystorePassword) throws Exception {
        KeyStore trustStore = KeyStore.getInstance("JKS");
        InputStream trustStoreIS = new FileInputStream(filepath);
        try {
            trustStore.load(trustStoreIS, keystorePassword.toCharArray());
        } finally {
            if (trustStoreIS != null) {
                trustStoreIS.close();
            }
        }
        TrustManagerFactory trustFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustFactory.init(trustStore);
        return trustFactory.getTrustManagers();
    }

}

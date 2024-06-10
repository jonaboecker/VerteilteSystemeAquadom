package aqua.blatt1.common;

import aqua.blatt1.common.msgtypes.KeyExchangeMessage;
import messaging.Endpoint;
import messaging.Message;

import javax.crypto.*;
import java.io.*;
import java.net.InetSocketAddress;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class SecureEndpoint extends Endpoint {

    private KeyPair keyPair;

    {
        try {
            keyPair = KeyPairGenerator.getInstance("RSA").generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private Map<InetSocketAddress, PublicKey> commPartner = new HashMap<>();

    private Cipher encrypt;
    private Cipher decrypt;

    {
        try {
            encrypt = Cipher.getInstance("RSA");
            decrypt = Cipher.getInstance("RSA");
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new RuntimeException(e);
        }
    }

    public SecureEndpoint() {
        super();
        try {
            decrypt.init(Cipher.DECRYPT_MODE, keyPair.getPrivate());
        } catch (InvalidKeyException e) {
            throw new RuntimeException(e);
        }
    }

    public SecureEndpoint(int port) {
        super(port);
        try {
            decrypt.init(Cipher.DECRYPT_MODE, keyPair.getPrivate());
        } catch (InvalidKeyException e) {
            throw new RuntimeException(e);
        }
    }

    private byte[] serialize(Serializable msg) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(msg);
            return bos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Object deserialize(byte[] msg) {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(msg);
             ObjectInputStream in = new ObjectInputStream(bis)) {
            return in.readObject();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private byte[] compress(byte[] data) throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream(data.length);
             GZIPOutputStream gzip = new GZIPOutputStream(bos)) {
            gzip.write(data);
            gzip.finish();
            return bos.toByteArray();
        }
    }

    private byte[] decompress(byte[] data) throws IOException {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(data);
             GZIPInputStream gis = new GZIPInputStream(bis);
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[1024];
            int len;
            while ((len = gis.read(buffer)) != -1) {
                bos.write(buffer, 0, len);
            }
            return bos.toByteArray();
        }
    }

    private class Sender implements Runnable {

        private final InetSocketAddress receiver;
        private final Serializable payload;

        public Sender(InetSocketAddress receiver, Serializable payload) {
            this.receiver = receiver;
            this.payload = payload;
        }

        @Override
        public void run() {
            while (!commPartner.containsKey(receiver)) {
                // Wait until the key exchange is completed
            }
            send(receiver, payload);
        }
    }

    @Override
    public void send(InetSocketAddress receiver, Serializable payload) {
        if (!commPartner.containsKey(receiver)) {
            super.send(receiver, new KeyExchangeMessage(keyPair.getPublic()));
            Thread t = new Thread(new Sender(receiver, payload));
            t.start();
            return;
        }

        try {
            byte[] data = serialize(payload);
            data = compress(data);

            encrypt.init(Cipher.ENCRYPT_MODE, commPartner.get(receiver));

            if (data.length > 245) { // 245 bytes is a safe limit for RSA with 2048-bit key
                throw new RuntimeException("Data must not be longer than 245 bytes after compression");
            }

            byte[] encryptedData = encrypt.doFinal(data);
            super.send(receiver, encryptedData);
        } catch (IOException | IllegalBlockSizeException | BadPaddingException | InvalidKeyException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public messaging.Message blockingReceive() {
        messaging.Message msg = super.blockingReceive();
        if (msg.getPayload() instanceof KeyExchangeMessage) {
            PublicKey tempKey = ((KeyExchangeMessage) msg.getPayload()).getKey();
            commPartner.put(msg.getSender(), tempKey);
            return null;
        } else {
            try {
                decrypt.init(Cipher.DECRYPT_MODE, keyPair.getPrivate());
                byte[] decryptedData = decrypt.doFinal((byte[]) msg.getPayload());
                byte[] decompressedData = decompress(decryptedData);
                Serializable payload = (Serializable) deserialize(decompressedData);
                return new Message(payload, msg.getSender());
            } catch (IllegalBlockSizeException | BadPaddingException | InvalidKeyException | IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public messaging.Message nonBlockingReceive() {
        messaging.Message msg = super.nonBlockingReceive();
        if (msg.getPayload() instanceof KeyExchangeMessage) {
            PublicKey tempKey = ((KeyExchangeMessage) msg.getPayload()).getKey();
            commPartner.put(msg.getSender(), tempKey);
            return null;
        } else {
            try {
                decrypt.init(Cipher.DECRYPT_MODE, keyPair.getPrivate());
                byte[] decryptedData = decrypt.doFinal((byte[]) msg.getPayload());
                byte[] decompressedData = decompress(decryptedData);
                Serializable payload = (Serializable) deserialize(decompressedData);
                return new Message(payload, msg.getSender());
            } catch (IllegalBlockSizeException | BadPaddingException | InvalidKeyException | IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}

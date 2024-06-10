package aqua.blatt1.common;

import messaging.Endpoint;
import messaging.Message;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;


public class SecureEndpoint extends Endpoint {

    private Key key = new SecretKeySpec("CAFEBABECAFEBABE".getBytes(), "AES");

    private Cipher encrypt;
    private Cipher decrypt;

    {
        try {
            encrypt = Cipher.getInstance("AES");
            decrypt =  Cipher.getInstance("AES");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (NoSuchPaddingException e) {
            throw new RuntimeException(e);
        }
    }

    public SecureEndpoint() {
        super();
        try {
            encrypt.init(Cipher.ENCRYPT_MODE, key);
            decrypt.init(Cipher.DECRYPT_MODE, key);
        } catch (InvalidKeyException e) {
            throw new RuntimeException(e);
        }
    }
    public SecureEndpoint(int port) {
        super(port);
        try {
            encrypt.init(Cipher.ENCRYPT_MODE, key);
            decrypt.init(Cipher.DECRYPT_MODE, key);
        } catch (InvalidKeyException e) {
            throw new RuntimeException(e);
        }
    }

    private byte [] serialize (Serializable msg) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = null;
        try {
            oos = new ObjectOutputStream(bos);
            oos.writeObject(msg);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return bos.toByteArray();
    }

    private Object deserialize (byte [] msg) {
        ByteArrayInputStream bis = new ByteArrayInputStream(msg);
        try (ObjectInput in = new ObjectInputStream(bis)) {
            return in.readObject();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void send (java.net.InetSocketAddress receiver, java.io.Serializable payload) {

        byte [] msg;

        try {
            msg = encrypt.doFinal(serialize(payload));
        } catch (IllegalBlockSizeException e) {
            throw new RuntimeException(e);
        } catch (BadPaddingException e) {
            throw new RuntimeException(e);
        }

        super.send(receiver, msg);
    }

    @Override
    public messaging.Message blockingReceive(){
        messaging.Message msg = super.blockingReceive();
        try {
            return new Message((Serializable) deserialize(decrypt.doFinal((byte[]) msg.getPayload())), msg.getSender());
        } catch (IllegalBlockSizeException e) {
            throw new RuntimeException(e);
        } catch (BadPaddingException e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public messaging.Message nonBlockingReceive(){
        messaging.Message msg = super.nonBlockingReceive();
        try {
            return new Message((Serializable) deserialize(decrypt.doFinal((byte[]) msg.getPayload())), msg.getSender());
        } catch (IllegalBlockSizeException e) {
            throw new RuntimeException(e);
        } catch (BadPaddingException e) {
            throw new RuntimeException(e);
        }
    }
}
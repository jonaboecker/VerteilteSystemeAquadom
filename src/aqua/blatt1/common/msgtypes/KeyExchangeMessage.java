package aqua.blatt1.common.msgtypes;

import java.io.Serializable;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;

public class KeyExchangeMessage implements Serializable {
        private final byte[] keyBytes;

        public KeyExchangeMessage(PublicKey key) {
            this.keyBytes = key.getEncoded();
        }

        public PublicKey getKey() {
            try {
                KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                return keyFactory.generatePublic(new X509EncodedKeySpec(keyBytes));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
}

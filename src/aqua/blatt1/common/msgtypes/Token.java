package aqua.blatt1.common.msgtypes;

import java.io.Serializable;
import java.net.InetSocketAddress;

@SuppressWarnings("serial")
public final class Token implements Serializable {
	private final String token = "This is a Token";

    public Token() {}
}

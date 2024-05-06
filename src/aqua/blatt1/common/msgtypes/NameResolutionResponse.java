package aqua.blatt1.common.msgtypes;

import java.io.Serializable;
import java.net.InetSocketAddress;

public class NameResolutionResponse implements Serializable {
    private final InetSocketAddress tankAdress;
    private final String requestId;

    public NameResolutionResponse(InetSocketAddress tankAdress, String requestId) {
        this.tankAdress = tankAdress;
        this.requestId = requestId;
    }

    public InetSocketAddress getTankId() {
        return tankAdress;
    }

    public String getRequestId() {
        return requestId;
    }
}

package aqua.blatt1.common.msgtypes;

import java.io.Serializable;

public class NameResolutionRequest implements Serializable {
    private final String tankId;
    private final String requestId;

    public NameResolutionRequest(String tankId, String requestId) {
        this.tankId = tankId;
        this.requestId = requestId;
    }

    public String getTankId() {
        return tankId;
    }

    public String getRequestId() {
        return requestId;
    }
}

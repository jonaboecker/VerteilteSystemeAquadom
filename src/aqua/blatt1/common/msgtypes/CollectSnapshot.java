package aqua.blatt1.common.msgtypes;

import java.io.Serializable;

public class CollectSnapshot implements Serializable {
    public int fishies = 0;

    public CollectSnapshot (int fishies) {
        this.fishies = fishies;
    }
}

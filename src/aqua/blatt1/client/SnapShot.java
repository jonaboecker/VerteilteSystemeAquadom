package aqua.blatt1.client;

import java.util.ArrayList;

public class SnapShot {
    protected int fishiesCount;
    protected int fishiesLeft;
    protected int fishiesRight;
    //protected ArrayList<Integer> messagesLeft;
    //protected ArrayList<Integer> messagesRight;

    public SnapShot(int fishiesCount) {
        this.fishiesCount = fishiesCount;
        fishiesLeft = 0;
        fishiesRight = 0;
    }

    public int sumFishies() {
        return fishiesCount + fishiesRight + fishiesLeft;
    }
}

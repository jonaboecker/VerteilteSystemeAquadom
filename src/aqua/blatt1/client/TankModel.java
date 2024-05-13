package aqua.blatt1.client;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import aqua.blatt1.common.Aufzeichnungsmodus;
import aqua.blatt1.common.Direction;
import aqua.blatt1.common.FishModel;
import aqua.blatt1.common.msgtypes.CollectSnapshot;

public class TankModel extends Observable implements Iterable<FishModel> {

    public static final int WIDTH = 600;
    public static final int HEIGHT = 350;
    protected static final int MAX_FISHIES = 5;
    protected static final Random rand = new Random();
    protected volatile String id;
    protected final Set<FishModel> fishies;
    protected int fishCounter = 0;
    protected final ClientCommunicator.ClientForwarder forwarder;
    protected InetSocketAddress leftNeighbor;
    protected InetSocketAddress rightNeighbor;
    protected boolean token = false;
    protected Timer timer = new Timer();

    protected int fishiesInTank = 0;
    protected SnapShot snapShot;
    protected Aufzeichnungsmodus aufzeichnungsmodus = Aufzeichnungsmodus.IDLE;
    private Boolean snapShotInitiator = false;
    public int globalSnapshot = -1;

    protected Map<String, InetSocketAddress> homeAgent = new HashMap<>();

    public TankModel(ClientCommunicator.ClientForwarder forwarder) {
        this.fishies = Collections.newSetFromMap(new ConcurrentHashMap<FishModel, Boolean>());
        this.forwarder = forwarder;
    }

    synchronized void onRegistration(String id, int leaseTime) {
        this.id = id;
        newFish(WIDTH - FishModel.getXSize(), rand.nextInt(HEIGHT - FishModel.getYSize()));
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                forwarder.register();
            }
        };
        timer.schedule(task, leaseTime / 2, leaseTime / 2);
    }

    synchronized void onNeighborUpdate(InetSocketAddress leftNeighbor, InetSocketAddress rightNeighbor) {
        this.leftNeighbor = leftNeighbor;
        this.rightNeighbor = rightNeighbor;
    }

    synchronized void onReceiveToken() {
        token = true;

        timer.schedule(new TokenTimer(this), 2000);
    }

    public void locateFishGlobally(String fishId) {
        if (homeAgent.get(fishId) == null)
            locateFishLocally(fishId);
        else
            forwarder.sendLocationRequest(homeAgent.get(fishId), fishId);

    }

    public void locateFishLocally(String fishId) {
        for (FishModel fish : fishies) {
            if (fish.getId().equals(fishId)) {
                fish.toggle();
                return;
            }
        }
    }

    private static class TokenTimer extends java.util.TimerTask {
        private final TankModel tankModel;

        public TokenTimer(TankModel tankModel) {
            this.tankModel = tankModel;
        }

        @Override
        public void run() {
            tankModel.token = false;
            tankModel.forwarder.handOffToken(tankModel.leftNeighbor);
        }
    }

    public boolean hasToken() {
        return token;
    }

    public synchronized void newFish(int x, int y) {
        if (fishies.size() < MAX_FISHIES) {
            x = x > WIDTH - FishModel.getXSize() - 1 ? WIDTH - FishModel.getXSize() - 1 : x;
            y = y > HEIGHT - FishModel.getYSize() ? HEIGHT - FishModel.getYSize() : y;

            FishModel fish = new FishModel("fish" + (++fishCounter) + "@" + getId(), x, y,
                    rand.nextBoolean() ? Direction.LEFT : Direction.RIGHT);

            fishiesInTank++;
            fishies.add(fish);
            homeAgent.put(fish.getId(), null);
        }
    }

    synchronized void receiveFish(FishModel fish, InetSocketAddress sender) {
        fish.setToStart();
        fishiesInTank++;
        if (aufzeichnungsmodus != Aufzeichnungsmodus.IDLE) {
            if (sender == leftNeighbor && (aufzeichnungsmodus == Aufzeichnungsmodus.LEFT || aufzeichnungsmodus == Aufzeichnungsmodus.BOTH)) {
                snapShot.fishiesLeft++;
            }
            if (sender == rightNeighbor && (aufzeichnungsmodus == Aufzeichnungsmodus.RIGHT || aufzeichnungsmodus == Aufzeichnungsmodus.BOTH)) {
                snapShot.fishiesRight++;
            }
        }
        fishies.add(fish);
        if (homeAgent.containsKey(fish.getId())) {
            homeAgent.put(fish.getId(), null);
        } else {
            forwarder.sendNameResolutionRequest(fish.getTankId(), fish.getId());
        }
    }

    public void sendLocationUpdate(String fishId, InetSocketAddress tankAdress) {
        forwarder.sendLocationUpdate(fishId, tankAdress);
    }

    public void onLocationUpdate(String fishId, InetSocketAddress tankAdress) {
        homeAgent.put(fishId, tankAdress);
    }

    public String getId() {
        return id;
    }

    public synchronized int getFishCounter() {
        return fishCounter;
    }

    public synchronized Iterator<FishModel> iterator() {
        return fishies.iterator();
    }

    private synchronized void updateFishies() {
        for (Iterator<FishModel> it = iterator(); it.hasNext(); ) {
            FishModel fish = it.next();

            fish.update();

            if (fish.hitsEdge()) {
                if (!token) {
                    fish.reverse();
                } else if (fish.getDirection() == Direction.LEFT && leftNeighbor != null) {
                    forwarder.handOff(fish, leftNeighbor);
                    fishiesInTank--;
                } else if (fish.getDirection() == Direction.RIGHT && rightNeighbor != null) {
                    forwarder.handOff(fish, rightNeighbor);
                    fishiesInTank--;
                } else {
                    throw new IllegalStateException("no Neighbor to handoff to!");
                }
            }


            if (fish.disappears())
                it.remove();
        }
    }

    private synchronized void update() {
        updateFishies();
        setChanged();
        notifyObservers();
    }

    protected void run() {
        forwarder.register();

        try {
            while (!Thread.currentThread().isInterrupted()) {
                update();
                TimeUnit.MILLISECONDS.sleep(10);
            }
        } catch (InterruptedException consumed) {
            // allow method to terminate
        }
    }

    public synchronized void finish() {
        if (token) {
            timer.cancel();
            token = false;
            forwarder.handOffToken(leftNeighbor);
        }
        forwarder.deregister(id);
    }

    public synchronized void initiateSnapshot() {
        initSnapShot();
        snapShotInitiator = true;
    }

    private synchronized void initSnapShot() {
        snapShot = new SnapShot(fishiesInTank);
        aufzeichnungsmodus = Aufzeichnungsmodus.BOTH;
        forwarder.sendSnapshotMarker(leftNeighbor);
        forwarder.sendSnapshotMarker(rightNeighbor);
    }

    public synchronized void createSnapshot(InetSocketAddress sender) {
        if (aufzeichnungsmodus == Aufzeichnungsmodus.IDLE) {
            initSnapShot();
            if (sender.equals(leftNeighbor)) {
                aufzeichnungsmodus = Aufzeichnungsmodus.RIGHT;
            } else {
                aufzeichnungsmodus = Aufzeichnungsmodus.LEFT;
            }
        } else {
            if (aufzeichnungsmodus == Aufzeichnungsmodus.BOTH) {
                if (sender.equals(leftNeighbor)) {
                    aufzeichnungsmodus = Aufzeichnungsmodus.RIGHT;
                } else {
                    aufzeichnungsmodus = Aufzeichnungsmodus.LEFT;
                }
            } else {
                aufzeichnungsmodus = Aufzeichnungsmodus.IDLE;
                if (snapShotInitiator) {
                    forwarder.handOffCollectSnapshotToken(leftNeighbor, new CollectSnapshot(snapShot.sumFishies()));
                }
            }
        }
    }

    public synchronized void addSnapshotToToken(CollectSnapshot token) {
        if (snapShotInitiator) {
            globalSnapshot = token.fishies;
            snapShotInitiator = false;
        } else {
            token.fishies += snapShot.sumFishies();
            forwarder.handOffCollectSnapshotToken(leftNeighbor, token);
        }
    }

}
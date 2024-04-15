package aqua.blatt1.client;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.Iterator;
import java.util.Observable;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.Timer;

import aqua.blatt1.common.Direction;
import aqua.blatt1.common.FishModel;
import aqua.blatt1.common.msgtypes.Token;

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

	public TankModel(ClientCommunicator.ClientForwarder forwarder) {
		this.fishies = Collections.newSetFromMap(new ConcurrentHashMap<FishModel, Boolean>());
		this.forwarder = forwarder;
	}

	synchronized void onRegistration(String id) {
		this.id = id;
		newFish(WIDTH - FishModel.getXSize(), rand.nextInt(HEIGHT - FishModel.getYSize()));
	}

	synchronized void onNeighborUpdate(InetSocketAddress leftNeighbor, InetSocketAddress rightNeighbor) {
		this.leftNeighbor = leftNeighbor;
		this.rightNeighbor = rightNeighbor;
	}

	synchronized void onReceiveToken() {
		token = true;

		timer.schedule(new TokenTimer(this), 2000);
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

			fishies.add(fish);
		}
	}

	synchronized void receiveFish(FishModel fish) {
		fish.setToStart();
		fishies.add(fish);
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
		for (Iterator<FishModel> it = iterator(); it.hasNext();) {
			FishModel fish = it.next();

			fish.update();

			if (fish.hitsEdge()) {
				if (!token) {
					fish.reverse();
				} else if (fish.getDirection() == Direction.LEFT && leftNeighbor != null) {
					forwarder.handOff(fish, leftNeighbor);
				} else if (fish.getDirection() == Direction.RIGHT && rightNeighbor != null) {
					forwarder.handOff(fish, rightNeighbor);
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
		if(token) {
			timer.cancel();
			token = false;
			forwarder.handOffToken(leftNeighbor);
		}
		forwarder.deregister(id);
	}

}
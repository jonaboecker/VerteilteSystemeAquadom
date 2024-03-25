package aqua.blatt1.broker;

import aqua.blatt1.common.Direction;
import aqua.blatt1.common.FishModel;
import aqua.blatt1.common.msgtypes.DeregisterRequest;
import aqua.blatt1.common.msgtypes.HandoffRequest;
import aqua.blatt1.common.msgtypes.RegisterRequest;
import aqua.blatt1.common.msgtypes.RegisterResponse;
import messaging.Endpoint;
import messaging.Message;

import java.awt.*;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Broker {

    private final Endpoint endpoint = new Endpoint(4711);
    private static ClientCollection<InetSocketAddress> clientcol = new ClientCollection<>();
    ReadWriteLock lock = new ReentrantReadWriteLock();
    private static int n;
    ReadWriteLock lock_n = new ReentrantReadWriteLock();

    private static final int NUMTHREADS = 16;
    ExecutorService executor = Executors. newFixedThreadPool(NUMTHREADS);

    public Broker(){
        n = 0;
    }
    public void broker() {
        while(true){
            //System.out.println("Broker is running");
            Message message = endpoint.blockingReceive();
            executor.execute(new BrokerTask(message)::run);
        }
        //executor.shutdown();
    }

    public static void main(String[] args) {
        Broker broker = new Broker();
        broker.broker();
    }

    // Erstellen Sie eine (innere) Klasse BrokerTask, die die Verarbeitung und Beantwortung von Nachrichten übernimmt.
    class BrokerTask implements Runnable {
        private final Serializable payload;
        private final InetSocketAddress sender;

        public BrokerTask(Message message) {
            this.payload = message.getPayload();
            this.sender = message.getSender();
        }

        @Override
        public void run() {
            //System.out.println("BrokerTask is running");
            switch (payload) {
                case RegisterRequest ignored:
                    String clientID;
                    lock_n.writeLock().lock();
                    try {
                        clientID = "tank" + ++n;
                    } finally {
                        lock_n.writeLock().unlock();
                    }
                    lock.writeLock().lock();
                    clientcol.add(clientID, sender);
                    lock.writeLock().unlock();
                    endpoint.send(sender, new RegisterResponse(clientID));
                    break;
                case DeregisterRequest ignored:
                    lock.writeLock().lock();
                    clientcol.remove(clientcol.indexOf(sender));
                    lock.writeLock().unlock();
                    break;
                case HandoffRequest ignored:
                    HandoffRequest hor = (HandoffRequest) payload;
                    FishModel fish = hor.getFish();
                    InetSocketAddress neighbor;
                    lock.readLock().lock();
                    if (fish.getDirection() == Direction.LEFT){
                        neighbor = clientcol.getLeftNeighorOf(clientcol.indexOf(sender));
                    } else {
                        neighbor = clientcol.getRightNeighorOf(clientcol.indexOf(sender));
                    }
                    lock.readLock().unlock();
                    endpoint.send(neighbor, hor);
                    break;
                default:
                    throw new IllegalStateException("Unexpected value: " + payload);
            }
        }
    }
}

package aqua.blatt1.broker;

import aqua.blatt1.common.Direction;
import aqua.blatt1.common.FishModel;
import aqua.blatt1.common.msgtypes.*;
import aqua.blatt2.broker.PoisonPill;
import messaging.Endpoint;
import messaging.Message;

import java.awt.*;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static javax.swing.JOptionPane.showMessageDialog;

public class Broker {

    private final Endpoint endpoint = new Endpoint(4711);
    private static ClientCollection<InetSocketAddress> clientcol = new ClientCollection<>();
    ReadWriteLock lock = new ReentrantReadWriteLock();
    private static int n;
    ReadWriteLock lock_n = new ReentrantReadWriteLock();

    private static final int NUMTHREADS = 16;
    ExecutorService executor = Executors.newFixedThreadPool(NUMTHREADS);

    private static boolean stopFlag = false;

    public Broker() {
        n = 0;
    }

    public void broker() {
        while (!stopFlag) {
            //System.out.println("Broker is running");
            Message message = endpoint.blockingReceive();
            if (message.getPayload() instanceof PoisonPill) {
                System.out.println("poison");

                break;
            }
            executor.execute(new BrokerTask(message)::run);
        }
        executor.shutdown();
    }

    public static void main(String[] args) {
        Thread stopThread = new Thread(() -> {
            showMessageDialog(null, "press ok to end");
            stopFlag = true;
        });
        // stopThread.start();
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
                case PoisonPill ignored:
                    stopFlag = true;
                    break;
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
                    lock.readLock().lock();
                    InetSocketAddress leftNeighbor = clientcol.getLeftNeighorOf(clientcol.indexOf(sender));
                    InetSocketAddress rightNeighbor = clientcol.getRightNeighorOf(clientcol.indexOf(sender));
                    endpoint.send(sender, new NeighborUpdate(leftNeighbor, rightNeighbor));
                    if (clientcol.size() != 1) {
                        endpoint.send(leftNeighbor, new NeighborUpdate(clientcol.getLeftNeighorOf(clientcol.indexOf(leftNeighbor)), sender));
                        endpoint.send(rightNeighbor, new NeighborUpdate(sender, clientcol.getRightNeighorOf(clientcol.indexOf(rightNeighbor))));
                    }
                    if (clientcol.size() == 1) {
                        endpoint.send(sender, new Token());
                    }
                    lock.readLock().unlock();
                    endpoint.send(sender, new RegisterResponse(clientID));

                    break;
                case DeregisterRequest ignored:
                    lock.readLock().lock();
                    InetSocketAddress leftNeighbor2 = clientcol.getLeftNeighorOf(clientcol.indexOf(sender));
                    InetSocketAddress rightNeighbor2 = clientcol.getRightNeighorOf(clientcol.indexOf(sender));
                    endpoint.send(leftNeighbor2, new NeighborUpdate(clientcol.getLeftNeighorOf(clientcol.indexOf(leftNeighbor2)), rightNeighbor2));
                    endpoint.send(rightNeighbor2, new NeighborUpdate(leftNeighbor2, clientcol.getRightNeighorOf(clientcol.indexOf(rightNeighbor2))));
                    lock.readLock().unlock();

                    lock.writeLock().lock();
                    clientcol.remove(clientcol.indexOf(sender));
                    lock.writeLock().unlock();
                    break;
                case NameResolutionRequest ignored:
                    NameResolutionRequest localePayload = (NameResolutionRequest) this.payload;
                    lock.readLock().lock();
                    int index = clientcol.indexOf(localePayload.getTankId());
                    InetSocketAddress client = clientcol.getClient(index);
                    lock.readLock().unlock();
                    endpoint.send(sender, new NameResolutionResponse(client, localePayload.getRequestId()));
                    break;
                default:
                    throw new IllegalStateException("Unexpected value: " + payload);
            }
        }
    }
}

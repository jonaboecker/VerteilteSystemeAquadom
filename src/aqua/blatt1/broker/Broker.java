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

public class Broker {

    private final Endpoint endpoint = new Endpoint(4711);
    private ClientCollection<InetSocketAddress> clientcol = new ClientCollection<>();
    private int n;

    public Broker(){
        n = 0;
    }
    public void broker() {
        while(true){
            Message message = endpoint.blockingReceive();
            Serializable payload = message.getPayload();
            InetSocketAddress sender = message.getSender();
            switch (payload) {
                case RegisterRequest ignored:
                    String clientID = "tank" + ++n;
                    clientcol.add(clientID, sender);
                    endpoint.send(sender, new RegisterResponse(clientID));
                    break;
                case DeregisterRequest ignored:
                    clientcol.remove(clientcol.indexOf(sender));
                    break;
                case HandoffRequest ignored:
                    HandoffRequest hor = (HandoffRequest) payload;
                    FishModel fish = hor.getFish();
                    InetSocketAddress neighbor;
                    if (fish.getDirection() == Direction.LEFT){
                        neighbor = clientcol.getLeftNeighorOf(clientcol.indexOf(sender));
                    } else {
                        neighbor = clientcol.getRightNeighorOf(clientcol.indexOf(sender));
                    }
                    endpoint.send(neighbor, hor);
                    break;
                default:
                    throw new IllegalStateException("Unexpected value: " + payload);
            }
        }
    }

    public static void main(String[] args) {
        Broker broker = new Broker();
        broker.broker();
    }

}

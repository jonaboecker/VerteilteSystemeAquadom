package aqua.blatt1.client;

import java.io.Serializable;
import java.net.InetSocketAddress;

import aqua.blatt1.common.Direction;
import aqua.blatt1.common.msgtypes.*;
import messaging.Endpoint;
import messaging.Message;
import aqua.blatt1.common.FishModel;
import aqua.blatt1.common.Properties;

public class ClientCommunicator {
	private final Endpoint endpoint;

	public ClientCommunicator() {
		endpoint = new Endpoint();
	}

	public class ClientForwarder {
		private final InetSocketAddress broker;

		private ClientForwarder() {
			this.broker = new InetSocketAddress(Properties.HOST, Properties.PORT);
		}

		public void register() {
			endpoint.send(broker, new RegisterRequest());
		}

		public void deregister(String id) {
			endpoint.send(broker, new DeregisterRequest(id));
		}

		public void handOff(FishModel fish, InetSocketAddress receiver) {
			endpoint.send(receiver, new HandoffRequest(fish));
		}

		public void handOffToken(InetSocketAddress receiver) {
			endpoint.send(receiver, new Token());
		}

		public void sendSnapshotMarker(InetSocketAddress receiver) {
			endpoint.send(receiver, new SnapshotMarker());
		}
		public void handOffCollectSnapshotToken(InetSocketAddress reciever, CollectSnapshot collectSnapshot) {endpoint.send(reciever, collectSnapshot);}
		public void sendLocationRequest(InetSocketAddress receiver, String fishId) {
			endpoint.send(receiver, new LocationRequest(fishId));
		}
		public void sendNameResolutionRequest(String tankId, String requestId) {
			endpoint.send(broker, new NameResolutionRequest(tankId, requestId));
		}

		public void sendLocationUpdate(String fishId, InetSocketAddress tankAdress) {
			endpoint.send(tankAdress, new LocationUpdate(fishId));
		}
	}

	public class ClientReceiver extends Thread {
		private final TankModel tankModel;

		private ClientReceiver(TankModel tankModel) {
			this.tankModel = tankModel;
		}

		@Override
		public void run() {
			while (!isInterrupted()) {
				Message msg = endpoint.blockingReceive();

				if (msg.getPayload() instanceof RegisterResponse)
					tankModel.onRegistration(((RegisterResponse) msg.getPayload()).getId());

				if (msg.getPayload() instanceof HandoffRequest)
					tankModel.receiveFish(((HandoffRequest) msg.getPayload()).getFish(), msg.getSender());

				if (msg.getPayload() instanceof NeighborUpdate neighborUpdate)
                    tankModel.onNeighborUpdate(neighborUpdate.getLeftNeighbor(), neighborUpdate.getRightNeighbor());

				if (msg.getPayload() instanceof Token)
					tankModel.onReceiveToken();

				if (msg.getPayload() instanceof SnapshotMarker)
					tankModel.createSnapshot(msg.getSender());

				if (msg.getPayload() instanceof CollectSnapshot)
					tankModel.addSnapshotToToken((CollectSnapshot) msg.getPayload());

				if (msg.getPayload() instanceof LocationRequest)
					tankModel.locateFishLocally(((LocationRequest) msg.getPayload()).getFishId());

				if (msg.getPayload() instanceof NameResolutionResponse nameResolutionResponse) {
                    tankModel.sendLocationUpdate(nameResolutionResponse.getRequestId(), nameResolutionResponse.getTankId());
				}

				if (msg.getPayload() instanceof LocationUpdate) {
					tankModel.onLocationUpdate(((LocationUpdate) msg.getPayload()).getFishId(), msg.getSender());
				}

			}
			System.out.println("Receiver stopped.");
		}
	}

	public ClientForwarder newClientForwarder() {
		return new ClientForwarder();
	}

	public ClientReceiver newClientReceiver(TankModel tankModel) {
		return new ClientReceiver(tankModel);
	}

}

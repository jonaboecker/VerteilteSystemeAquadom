package aqua.blatt1.common.msgtypes;

import java.io.Serializable;
import java.net.InetSocketAddress;

@SuppressWarnings("serial")
public final class NeighborUpdate implements Serializable {
	private final InetSocketAddress leftNeighbor;
	private final InetSocketAddress rightNeighbor;

	public NeighborUpdate(InetSocketAddress id_left, InetSocketAddress id_right) {
		this.leftNeighbor = id_left;
		this.rightNeighbor = id_right;
	}

	public InetSocketAddress getLeftNeighbor() {
		return leftNeighbor;
	}

	public InetSocketAddress getRightNeighbor() {
		return rightNeighbor;
	}
}

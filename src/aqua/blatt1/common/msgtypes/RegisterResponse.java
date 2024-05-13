package aqua.blatt1.common.msgtypes;

import java.io.Serializable;

@SuppressWarnings("serial")
public final class RegisterResponse implements Serializable {
	private final String id;
	private final int leaseTime;

	public RegisterResponse(String id, int leaseTime) {
		this.id = id;
		this.leaseTime = leaseTime;
	}

	public String getId() {
		return id;
	}

	public int getLeaseTime() {
		return leaseTime;
	}

}

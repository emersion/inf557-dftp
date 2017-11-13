import java.net.InetAddress;

/**
 * An envelope holds a message and the sender's address.
 */
class Envelope {
	public final InetAddress address;
	public final Message msg;

	public Envelope(InetAddress address, Message msg) {
		this.address = address;
		this.msg = msg;
	}

	public String toString() {
		return this.address+" "+this.msg;
	}
}

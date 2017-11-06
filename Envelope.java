import java.net.InetAddress;

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

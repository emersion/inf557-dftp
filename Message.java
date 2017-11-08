import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

abstract class Message {
	private static final Pattern idPattern = Pattern.compile("^[a-zA-Z0-9]{0,16}$");

	private static final String HELLO = "HELLO";
	private static final String SYN = "SYN";

	abstract public String format();

	public static Message parse(String raw) {
		String[] parts = raw.split(";");

		String type = parts[0];
		switch (type) {
		case HELLO:
			return new Hello(parts);
		case SYN:
			return new Syn(parts);
		default:
			throw new IllegalArgumentException("unknown message type: "+type);
		}
	}

	public static class Hello extends Message {
		public final String sender;
		public final int seqNum;
		public final int helloInterval;
		private final List<String> peers = new ArrayList<>();

		public Hello(String sender, int seqNum, int helloInterval) {
			if (!idPattern.matcher(sender).matches()) {
				throw new IllegalArgumentException("invalid sender ID");
			}
			if (helloInterval < 0 || helloInterval > 255) {
				throw new IllegalArgumentException("invalid HELLO interval: not in range");
			}

			this.sender = sender;
			this.seqNum = seqNum;
			this.helloInterval = helloInterval;
		}

		public Hello(String[] parts) {
			// Let's be conservative in what we accept, because we want to spot
			// implementation mistakes.

			if (parts.length < 5) {
				throw new IllegalArgumentException("wrong number of fields");
			}
			if (!HELLO.equals(parts[0])) {
				throw new IllegalArgumentException("not a HELLO message");
			}

			this.sender = parts[1];
			if (!idPattern.matcher(this.sender).matches()) {
				throw new IllegalArgumentException("invalid sender ID");
			}

			try {
				this.seqNum = Integer.parseInt(parts[2]);
			} catch (NumberFormatException e) {
				throw new IllegalArgumentException("invalid sequence number: "+e.getMessage());
			}

			try {
				this.helloInterval = Integer.parseInt(parts[3]);
			} catch (NumberFormatException e) {
				throw new IllegalArgumentException("invalid HELLO interval: "+e.getMessage());
			}
			if (this.helloInterval < 0 || this.helloInterval > 255) {
				throw new IllegalArgumentException("invalid HELLO interval: not in range");
			}

			int peersLen;
			try {
				peersLen = Integer.parseInt(parts[4]);
			} catch (NumberFormatException e) {
				throw new IllegalArgumentException("invalid number of peers : "+e.getMessage());
			}
			if (peersLen < 0 || peersLen > 255) {
				throw new IllegalArgumentException("invalid number of peers: not in range");
			}
			if (peersLen != parts.length - 5) {
				throw new IllegalArgumentException("invalid number of peers: mismatched");
			}

			for (int i = 5; i < parts.length; ++i) {
				this.addPeer(parts[i]);
			}
		}

		public String toString() {
			return "HELLO{sender="+this.sender+" seqNum="+this.seqNum+
				" helloInterval="+this.helloInterval+
				" peers="+this.peers.toString()+"}";
		}

		public String format() {
			StringBuilder sb = new StringBuilder(HELLO+";"+this.sender+";"+
				this.seqNum+";"+this.helloInterval+";"+this.peers.size());
			for (String peer : this.peers) {
				sb.append(";"+peer);
			}
			return sb.toString();
		}

		public void addPeer(String peer) {
			if (!idPattern.matcher(peer).matches()) {
				throw new IllegalArgumentException("invalid peer ID");
			}
			this.peers.add(peer);
		}
	}

	public static class Syn extends Message {
		public final String sender;
		public final String peer;
		public final int seqNum;

		public Syn(String sender, String peer, int seqNum) {
			if (!idPattern.matcher(sender).matches()) {
				throw new IllegalArgumentException("invalid sender ID");
			}

			this.sender = sender;

			if (!idPattern.matcher(peer).matches()) {
				throw new IllegalArgumentException("invalid peer ID");
			}

			this.peer = peer;
			this.seqNum = seqNum;
		}

		public Syn(String[] parts){
			if (parts.length != 4) {
				throw new IllegalArgumentException("wrong number of fields");
			}

			if (!SYN.equals(parts[0])) {
				throw new IllegalArgumentException("not a SYN message");
			}

			this.sender = parts[1];
			if (!idPattern.matcher(this.sender).matches()) {
				throw new IllegalArgumentException("invalid sender ID");
			}

			this.peer = parts[2];
			if(!idPattern.matcher(this.peer).matches()){
				throw new IllegalArgumentException("invalid peer ID");
			}

			try {
				this.seqNum = Integer.parseInt(parts[3]);
			} catch (NumberFormatException e) {
				throw new IllegalArgumentException("invalid sequence number: "+e.getMessage());
			}

		}

		@Override
		public String format() {
			return SYN+";"+this.sender+";"+ this.peer+";"+String.valueOf(this.seqNum)+";";
		}

		public String toString() {
			return "SYN{sender="+this.sender+
					" peer="+this.peer+
					" seqNum="+this.seqNum+"}";
		}
	}


}



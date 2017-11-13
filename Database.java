class Database {
	private String[] data;
	private int seqNum;

	public Database() {
		this.data = new String[0];
		this.seqNum = Integer.MIN_VALUE;
	}

	public Database(String[] data, int seqNum) {
		this.data = data;
		this.seqNum = seqNum;
	}

	public synchronized void update(String[] data, int seqNum) {
		if (this.seqNum > seqNum) {
			throw new RuntimeException("attempt to update a Database with an older sequence number");
		}

		this.data = data;
		this.seqNum = seqNum;
	}

	public synchronized String[] data() {
		// We don't need to copy the data array because it's immutable
		// TODO: return a read-only data structure to prevent writes
		return data;
	}

	public synchronized int seqNum() {
		return seqNum;
	}
}

class Database {
	private String[] data = null;
	private int seqNum = 0;

	public Database() {
		this.data = new String[0];
	}

	public Database(String[] data, int seqNum) {
		this.data = data;
		this.seqNum = seqNum;
	}

	public synchronized void update(String[] data) {
		this.data = data;
		++seqNum;
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

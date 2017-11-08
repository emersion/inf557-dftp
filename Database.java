class Database {
	private String[] data = null;
	private int seqNum = 0;

	public synchronized void update(String[] data) {
		this.data = data;
		++seqNum;
	}

	public synchronized int seqNum() {
		return seqNum;
	}
}

package Core;

public class WriteMsg {
	private long transactionId;
	private long timeStamp;
	private ReplicaLoc loc;
	
	public WriteMsg(long transactionId,long timeStamp,ReplicaLoc loc)
	{
		this.transactionId = transactionId;
		this.timeStamp = timeStamp;
		this.loc = loc;
	}
	public void setTransactionId(long transactionId) {
		this.transactionId = transactionId;
	}
	public long getTransactionId() {
		return transactionId;
	}
	public void setTimeStamp(long timeStamp) {
		this.timeStamp = timeStamp;
	}
	public long getTimeStamp() {
		return timeStamp;
	}
	public void setLoc(ReplicaLoc loc) {
		this.loc = loc;
	}
	public ReplicaLoc getLoc() {
		return loc;
	}

	public long getTransactionId() {
		return transactionId;
	}

	public void setTransactionId(long transactionId) {
		this.transactionId = transactionId;
	}

	public long getTimeStamp() {
		return timeStamp;
	}

	public void setTimeStamp(long timeStamp) {
		this.timeStamp = timeStamp;
	}

	public ReplicaLoc getLoc() {
		return loc;
	}

	public void setLoc(ReplicaLoc loc) {
		this.loc = loc;
	}

}

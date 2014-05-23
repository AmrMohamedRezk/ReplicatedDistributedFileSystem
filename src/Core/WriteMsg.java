package Core;

import java.io.Serializable;

public class WriteMsg implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private long transactionId;
	private long timeStamp;
	private ReplicaLoc loc;
	
	public WriteMsg(long transactionId,long timeStamp,ReplicaLoc loc)
	{
		this.transactionId = transactionId;
		this.timeStamp = timeStamp;
		this.loc = loc;
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

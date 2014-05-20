package Server;

import java.io.IOException;
import java.rmi.RemoteException;

import Core.FileContent;
import Core.MessageNotFoundException;
import Core.WriteMsg;
import Interface.ReplicaServerClientInterface;

public class Replicas extends Server implements ReplicaServerClientInterface {
	
	/*
	 * STATUS:
	 * 1)A lock manager is maintained at each replicaServer.
	 * 
	 * 
	 */

	/**
	 * Writes to files stored on the file system.
	 *The following procedure is followed:
	 *1) The client requests a new transaction ID from the server. 
	 * The request includes the name of the file to be muted during the transaction.
	 *
	 *2) The server generates a unique transaction ID and returns it, a timestamp,
	 * and the location of the primary replica of that file to the client in an 
	 * acknowledgment to the client's file update request.
	 * 
	 *3) If the file specified by the client does not exist, the server creates the 
	 *file on the replicaServers and initializes its metadata.
	 * 
	 *4) All subsequent client messages corresponding to a transaction will be 
	 *directed to the replicaServer with the primary replica contain the ID of 
	 *that transaction.
     * 
     *5) The client sends to the replicaServer a series of write requests to the 
     *file specified in the transaction. Each request has a unique serial number. 
     *The server appends all writes sent by the client to the file. Updates are also 
     *propagated in the same order to other replicaServers.
     *
     *6) The server must keep track of all messages received from the client 
     *as part of each transaction. The server must also apply file mutations 
     *based on the correct order of the transactions.
     *
     *
     *7) At the end of the transaction, the client issues a commit request. 
     *This request guarantees that the file is written on all the replicaServer 
     *disks. Therefore, each replicaServer flushes the file data to disk and sends 
     *an acknowledgement of the committed transaction to the primary replicaServer 
     *for that file. Once the primary replicaServer receives acknowledgements from 
     *all replicas, it sends an acknowledgement to the client.
     *
     *8) The new file must not be seen on the file system until the transaction 
     *commits. That is a read request to a file that is being updated by an 
     *uncommitted transaction must generate an error. 
	 */
	@Override
	public WriteMsg write(long txnID, long msgSeqNum, FileContent data)
			throws RemoteException, IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean commit(long txnID, long numOfMsgs)
			throws MessageNotFoundException, RemoteException {
		// TODO Auto-generated method stub
		return false;
	}
	/**
	 * A client can decide to abort the transaction after it has started. 
	 * Note, that the client might have already sent write requests to the server. 
	 * In this case, the client requests transaction abort from the server. 
	 * The client's abort request is handled as follows: 
	 * STATUS:
	 * 1) The primary replicaServer ensures that no data that the client had sent
	 *  as part of transaction is written to the file on the disk of any of the 
	 *  replicaServers. 
	 * STATUS: 
	 * 2) If a new file was created as part of that transaction, the master server
	 *   deletes the file from its metadata and the file is deleted from all 
	 *   replicaServers. 
	 * STATUS:  
	 * 3) The primary replicaServer acknowledges the client's abort.
	 */
	@Override
	public boolean abort(long txnID) throws RemoteException {
		// TODO Auto-generated method stub
		return false;
	}

}

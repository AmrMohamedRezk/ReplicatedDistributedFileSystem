package Server;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.rmi.RemoteException;

import Core.FileContent;
import Core.WriteMsg;
import Interface.MasterServerClientInterface;

public class Master extends Server implements MasterServerClientInterface {

/*
 * ========================================================================
 * STATUS:
 * 1)The master server maintains metadata about the replicas and their locations
 * STATUS:DONE
 * 2)The server should communicate with clients through the given RMI interface.
 * STATUS:
 * 3)The server need to be invoked using the following command.
 *  	a. server -ip [ip_address_string] -port [port_number] -dir <directory_path> 
 *  	b. where:  ip_address_string is the ip address of the server. 
 *  						The default value is 127.0.0.1.
 *				   port_number is the port number at which the server will be listening to messages. 
 *							The default is 8080. 
 * 				   directory_path is the directory where files are created and written by client transactions. 
 * 				   This directory is the same for all replicaServers. At the server 
 * 				   starts up, that directory may already contain some files 
 * 				  (we could put some files there for testing). 
 * 				  You must ensure that the files in that directory are not deleted after
 * 				  the server exits. We will use the contents in those files to check 
 * 				  the correctness of your system
 * STATUS:
 * 4) Each file is replicated on there replicaServers. The IP addresses of 
 * all replicaServers are specified in a file called "repServers.txt". The master 
 * keeps heartbeats with these servers. 
 * STATUS:
 * 5) Acknowledgements are sent to the client when data is written to all the 
 * replicaServers that contain replicas of a certain file. 
 * STATUS:
 * 6) You will need to implement one of the primary-based consistency protocols 
 * studied in class.-->Remote Write Protocols
 * =============================================================================
 *	 
 * 
 * 
 * 
 * 
 */
	/**
	 * Reads from the file system.
	 * Files are read entirely. When a client sends a file name to the server, 
	 * the entire file is returned to the client.
	 */
	@Override
	public FileContent read(String fileName) throws FileNotFoundException,
			IOException, RemoteException {
		// TODO Auto-generated method stub
		return null;
	}

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
	public WriteMsg write(FileContent data) throws RemoteException, IOException {
		// TODO Auto-generated method stub
		return null;
	}

}

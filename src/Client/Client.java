package Client;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Scanner;
import java.util.StringTokenizer;
import java.util.Vector;

import Core.FileContent;
import Core.MessageNotFoundException;
import Core.ReplicaLoc;
import Core.WriteMsg;
import Interface.MasterServerClientInterface;
import Interface.ReplicaServerClientInterface;

public class Client {

	int MSport_number; // master server port
	String MSaddress; // master server IP
	String server_name; // master sever name in rmi
	public static int index = 0;
	WriteMsg current_write;
	int seq_number = 0;
	String current_filename;
	Vector<FileContent> messages = new Vector<FileContent>();
	boolean error_Nmessages = false;

	public Client() throws FileNotFoundException {
		Scanner scan = new Scanner(new File("MasterServer.txt"));
		MSaddress = scan.nextLine();
		server_name = scan.nextLine();
		MSport_number = scan.nextInt();

		scan.close();
	}

	/*
	 * STATUS: 1) Clients read and write data to the distributed file system.
	 * STATUS:DONE 2) Each client has a file in its local directory that
	 * specifies the main server IP address. STATUS: 3)The server should
	 * communicate with clients through the given RMI interface.
	 */

	/**
	 * Writes to files stored on the file system. The following procedure is
	 * followed: 1) The client requests a new transaction ID from the server.
	 * The request includes the name of the file to be muted during the
	 * transaction.
	 * 
	 * 2) The server generates a unique transaction ID and returns it, a
	 * timestamp, and the location of the primary replica of that file to the
	 * client in an acknowledgment to the client's file update request.
	 * 
	 * 3) If the file specified by the client does not exist, the server creates
	 * the file on the replicaServers and initializes its metadata.
	 * 
	 * 4) All subsequent client messages corresponding to a transaction will be
	 * directed to the replicaServer with the primary replica contain the ID of
	 * that transaction.
	 * 
	 * 5) The client sends to the replicaServer a series of write requests to
	 * the file specified in the transaction. Each request has a unique serial
	 * number. The server appends all writes sent by the client to the file.
	 * Updates are also propagated in the same order to other replicaServers.
	 * 
	 * 6) The server must keep track of all messages received from the client as
	 * part of each transaction. The server must also apply file mutations based
	 * on the correct order of the transactions.
	 * 
	 * 
	 * 7) At the end of the transaction, the client issues a commit request.
	 * This request guarantees that the file is written on all the replicaServer
	 * disks. Therefore, each replicaServer flushes the file data to disk and
	 * sends an acknowledgement of the committed transaction to the primary
	 * replicaServer for that file. Once the primary replicaServer receives
	 * acknowledgements from all replicas, it sends an acknowledgement to the
	 * client.
	 * 
	 * 8) The new file must not be seen on the file system until the transaction
	 * commits. That is a read request to a file that is being updated by an
	 * uncommitted transaction must generate an error.
	 */
	public void write(FileContent data, boolean dontSend)
			throws RemoteException, IOException {
		System.setProperty("java.rmi.server.hostname", "localhost");
		MasterServerClientInterface MrmiServer;
		ReplicaServerClientInterface RrmiServer;
		Registry Mregistry, Rregistry;
		WriteMsg response;
		if (dontSend)
			return;
		try {
			// calling the Master server
			if (current_write == null) {
				System.out.println("Getting info from main server ");
				error_Nmessages = false;
				Mregistry = LocateRegistry.getRegistry("localhost",
						(new Integer(5555)).intValue());
				MrmiServer = (MasterServerClientInterface) (Mregistry
						.lookup("rmiServer"));
				// call the remote method
				response = MrmiServer.write(data);
				current_filename = data.getFileName();
			} else {
				response = current_write;
				if (!data.getFileName().equalsIgnoreCase(current_filename))
					throw new Exception();
			}
			messages.add(data);
			long transactionId = response.getTransactionId();
			// long timeStamp = response.getTimeStamp();
			System.out
					.println("MASTER SERVER REPLY TO WRITE with Xaction id : "
							+ transactionId);
			ReplicaLoc loc = response.getLoc();

			// calling the Primary replica
			Rregistry = LocateRegistry.getRegistry(loc.getAddress(),
					(new Integer(loc.getPort())).intValue());
			RrmiServer = (ReplicaServerClientInterface) (Rregistry.lookup(loc
					.getRmiReg_name()));

			current_write = RrmiServer.write(transactionId, seq_number, data);
			System.out.println("Message number : " + seq_number + " is sent");
			seq_number = (seq_number + 1) % 1000000;
		} catch (Exception e) {
			// TODO: handle exception
			System.err.println(e.getMessage());
		}
		return;

	}

	public boolean commit(long numOfMsgs) throws MessageNotFoundException,
			RemoteException {
		System.setProperty("java.rmi.server.hostname", "127.0.0.1");
		ReplicaServerClientInterface RrmiServer;
		Registry Rregistry;
		try {
			// calling the Master server
			long transactionId = current_write.getTransactionId();
			ReplicaLoc loc = current_write.getLoc();
			// calling the Primary replica
			Rregistry = LocateRegistry.getRegistry(loc.getAddress(),
					(new Integer(loc.getPort())).intValue());
			RrmiServer = (ReplicaServerClientInterface) (Rregistry.lookup(loc
					.getRmiReg_name()));

			if (RrmiServer.commit(transactionId, numOfMsgs)) {
				System.out.println("Message commited successfully :) ");
				current_write = null;
				seq_number = 0;
				messages.clear();
				return true;

			} else if (!error_Nmessages) {
				FileContent c = RrmiServer.wrongNumberOfMsgs(transactionId,
						numOfMsgs);

				if (c.getFileName().equalsIgnoreCase("Missing messages")) {
					error_Nmessages = true;
					StringTokenizer token = new StringTokenizer(c.getContent(),
							",");
					System.out.println("Wrong number of messages in commit :(");
					while (token.hasMoreTokens()) {
						int number = Integer.parseInt(token.nextToken());
						write(messages.get(number), false);
					}

					commit(numOfMsgs);
					System.out.println("Commited again :/");
				}
				return false;
			}

		} catch (Exception e) {
			// TODO: handle exception
			System.err.println(e.getMessage());
		}
		return false;
	}

	/**
	 * A client can decide to abort the transaction after it has started. Note,
	 * that the client might have already sent write requests to the server. In
	 * this case, the client requests transaction abort from the server. The
	 * client's abort request is handled as follows: STATUS: 1) The primary
	 * replicaServer ensures that no data that the client had sent as part of
	 * transaction is written to the file on the disk of any of the
	 * replicaServers. STATUS: 2) If a new file was created as part of that
	 * transaction, the master server deletes the file from its metadata and the
	 * file is deleted from all replicaServers. STATUS: 3) The primary
	 * replicaServer acknowledges the client's abort.
	 */
	public boolean abort() throws RemoteException {
		// TODO Auto-generated method stub
		System.setProperty("java.rmi.server.hostname", "127.0.0.1");
		ReplicaServerClientInterface RrmiServer;
		Registry Rregistry;
		try {
			// calling the Master server
			long transactionId = current_write.getTransactionId();
			ReplicaLoc loc = current_write.getLoc();
			// calling the Primary replica
			Rregistry = LocateRegistry.getRegistry(loc.getAddress(),
					(new Integer(loc.getPort())).intValue());
			RrmiServer = (ReplicaServerClientInterface) (Rregistry.lookup(loc
					.getRmiReg_name()));

			if (RrmiServer.abort(transactionId)) {
				System.out.println("Abort is done (Y)");
				current_write = null;
				seq_number = 0;
				messages.clear();
				return true;

			} else {
				System.out.println("Abort failed :O ");
				return false;
			}
		} catch (Exception e) {
			// TODO: handle exception
			System.err.println(e.getMessage());
		}
		return false;
	}

	/**
	 * Reads from the file system. Files are read entirely. When a client sends
	 * a file name to the server, the entire file is returned to the client.
	 */
	public void read(String fileName) throws FileNotFoundException,
			IOException, RemoteException {
		System.setProperty("java.rmi.server.hostname", "127.0.0.1");
		MasterServerClientInterface MrmiServer;
		Registry registry;

		try {
			registry = LocateRegistry.getRegistry(MSaddress, (new Integer(
					MSport_number)).intValue());

			MrmiServer = (MasterServerClientInterface) (registry
					.lookup(server_name));
			// call the remote method
			FileContent response = MrmiServer.read(fileName);
			long transactionId = response.getXaction_number();
			System.out.println("Reading file with xaction id : "
					+ transactionId);
			ReplicaLoc rl = response.getRl();
			// int seq_no = generator.nextInt();
			// calling the Primary replica
			Registry Rregistry = LocateRegistry.getRegistry(rl.getAddress(),
					(new Integer(rl.getPort())).intValue());
			ReplicaServerClientInterface RrmiServer = (ReplicaServerClientInterface) (Rregistry
					.lookup(rl.getRmiReg_name()));
			FileContent c = new FileContent(fileName, transactionId);
			c = RrmiServer.read(c);
			System.out.println("File name : " + c.getFileName());
			System.out.println("Xaction id : " + c.getXaction_number());
			System.out.println("Content: \n" + c.getContent());

		} catch (Exception e) {
			System.err.println(e.getMessage());
		}

	}

}

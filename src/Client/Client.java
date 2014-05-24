package Client;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Random;
import java.util.Scanner;

import Core.FileContent;
import Core.MessageNotFoundException;
import Core.ReplicaLoc;
import Core.WriteMsg;
import Interface.MasterServerClientInterface;
import Interface.ReplicaServerClientInterface;

public class Client {
	int MSport_number;
	String MSaddress;
	String server_name;
	static int index = 0;
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
	public WriteMsg write(WriteMsg wm, FileContent data)
			throws RemoteException, IOException {
		System.setProperty("java.rmi.server.hostname", "localhost");
		MasterServerClientInterface MrmiServer;
		ReplicaServerClientInterface RrmiServer;
		Registry Mregistry, Rregistry;
		Random generator = new Random();
		WriteMsg response;
		try {
			// calling the Master server
			if (wm == null) {
				Mregistry = LocateRegistry.getRegistry("localhost",
						(new Integer(5555)).intValue());
				MrmiServer = (MasterServerClientInterface) (Mregistry
						.lookup("rmiServer"));
				// call the remote method
				response = MrmiServer.write(data);
			} else
				response = wm;
			long transactionId = response.getTransactionId();
			long timeStamp = response.getTimeStamp();
			System.out.println("MASTER SERVER REPLY TO WRITE : " + timeStamp);
			ReplicaLoc loc = response.getLoc();
			int seq_no = generator.nextInt();
			// calling the Primary replica
			Rregistry = LocateRegistry.getRegistry(loc.getAddress(),
					(new Integer(loc.getPort())).intValue());
			RrmiServer = (ReplicaServerClientInterface) (Rregistry.lookup(loc
					.getRmiReg_name()));

			return RrmiServer.write(transactionId, seq_no, data);

		} catch (Exception e) {
			// TODO: handle exception
			System.err.println(e.getMessage());
		}
		return null;

	}

	public boolean commit(WriteMsg wmessage, long numOfMsgs)
			throws MessageNotFoundException, RemoteException {
		System.setProperty("java.rmi.server.hostname", "127.0.0.1");
		ReplicaServerClientInterface RrmiServer;
		Registry Rregistry;
		try {
			// calling the Master server
			long transactionId = wmessage.getTransactionId();
			ReplicaLoc loc = wmessage.getLoc();
			// calling the Primary replica
			Rregistry = LocateRegistry.getRegistry(loc.getAddress(),
					(new Integer(loc.getPort())).intValue());
			RrmiServer = (ReplicaServerClientInterface) (Rregistry.lookup(loc
					.getRmiReg_name()));

			return RrmiServer.commit(transactionId, numOfMsgs);

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
	public boolean abort(WriteMsg wmessage) throws RemoteException {
		// TODO Auto-generated method stub
		System.setProperty("java.rmi.server.hostname", "127.0.0.1");
		ReplicaServerClientInterface RrmiServer;
		Registry Rregistry;
		try {
			// calling the Master server
			long transactionId = wmessage.getTransactionId();
			ReplicaLoc loc = wmessage.getLoc();
			// calling the Primary replica
			Rregistry = LocateRegistry.getRegistry(loc.getAddress(),
					(new Integer(loc.getPort())).intValue());
			RrmiServer = (ReplicaServerClientInterface) (Rregistry.lookup(loc
					.getRmiReg_name()));

			return RrmiServer.abort(transactionId);

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

	public static void main(String[] args) throws RemoteException, IOException,
			MessageNotFoundException {
//<<<<<<< HEAD
		Client c1 = new Client();
		Client c2 = new Client();

		System.out.println("Creating data for testing ........ !");
		FileContent fc = new FileContent("c1.txt", 1);
		FileContent fc1 = new FileContent("c2.txt", 1);

		fc.setContent("I am Client one");
		fc1.setContent("I am Client two");

		System.out.println("Each client write and read from his file...!");
		System.out.println("client 1");
		WriteMsg m = c1.write(null, fc);
		c1.commit(m, 1);
		System.out.println("read:  \n");
		c1.read("\\c1.txt");

		System.out.println("client 2");
		m = c2.write(null, fc1);
		c2.commit(m, 1);
		System.out.println("read:  \n");
		c2.read("\\c2.txt");

		System.out
				.println(" client 1  write and commit with wrong number of messages and client 2 write and abort ");
		System.out.println("client 1");
		m = c1.write(null, fc);
		c1.commit(m, 2);

		System.out.println("client 2");
		m = c2.write(null, fc1);
		c2.abort(m);
		System.out.println("read:  \n");
		c2.read("\\c2.txt");

		System.out
				.println("client1 try to access file which client2 is writing in ");

		System.out.println("client 2");
		m = c2.write(null, fc1);
		System.out.println("client 1");
		System.out.println("read:  \n");
		c1.read("\\c2.txt");
		System.out.println("client 2 commit and client 1 read");
		c2.commit(m, 1);
		System.out.println("read:  \n");
		c1.read("\\c2.txt");

		System.out
				.println("client1 try to access file newly created bt Client 2 and not commited and aborted ");

		FileContent fnew = new FileContent("disappear.txt", 1);
		m = c2.write(null, fnew);
		c1.read("\\disappear.txt");
		c2.abort(m);

		System.out
				.println("client1 try to access file newly created bt Client 2 and not commited  then commited and seen :D  ");

		fnew = new FileContent("trick.txt", 1);
		fnew.setContent("Yuuuuuuuuuuupy");
		m = c2.write(null, fnew);
		c1.read("\\trick.txt");
		c2.commit(m, 1);
		c1.read("\\trick.txt");
//=======
		// Client c = new Client();
		// FileContent content = new FileContent("Ahmad.txt", 0);
		// content.setContent("I am testing :P :P ");
		// WriteMsg m = c.write(null, content);
		// content = new FileContent("Ahmad.txt", m.getTransactionId());
		// content.setContent("I am testing :P :P ");
		// c.write(m, content);
		// c.commit(m, 2);
		// //c.read("Ahmad.txt");
//		for (int i = 0; i < 3; i++) {
//			Runnable r = new Runnable() {
//		         public void run() {
//		        	 try{
//		        		Client c = new Client();
//		    			FileContent content = new FileContent("Ahmad.txt", Client.index);
//		    			Client.index++;
//		    			content.setContent("not seen 1 :P :P ");
//		    			WriteMsg m = c.write(null, content);
//		    			content = new FileContent("Ahmad.txt", m.getTransactionId());
//		    			content.setContent("not seen 2 :P :P ");
//		    			c.write(m, content);
//		    			// c.abort(m);
//		    			c.commit(m, 2);
//		    			c.read("amr.txt");
//		        	 }catch(Exception e )
//		        	 {
//		        		 e.printStackTrace();
//		        	 }
// 
//		         }
//		     };
//
//		     new Thread(r).start();
//				}
//>>>>>>> 42b95f0b46d1b6c57897001b2cd40493d7f1393a

	}
}

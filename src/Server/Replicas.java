package Server;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.HashMap;


import Core.FileContent;
import Core.MessageNotFoundException;
import Core.ReplicaLoc;
import Core.WriteMsg;
import Interface.ReplicaServerClientInterface;

public class Replicas extends java.rmi.server.UnicastRemoteObject implements
		ReplicaServerClientInterface {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	/*
	 * STATUS: 1)A lock manager is maintained at each replicaServer.
	 */
	private HashMap<String, Boolean> lockManager;
	private String root;
	private Master master;
	private HashMap<Long, HashMap<Long, FileContent>> pendingTransactions;
	private HashMap<Long, String> transactionToFileNameMap;
	private int port;
	private String address, rmi_name;

	public Replicas(String location, int i, Master m) throws RemoteException {
		master = m;
		Registry registry;
		address = "localhost";
		rmi_name = "replica" + i;
		port = 8080 + i;
		System.setProperty("java.rmi.server.hostname", address);
		registry = LocateRegistry.createRegistry(port);
		try {
			registry.rebind(rmi_name, this);
		} catch (RemoteException e) {
			System.out.println("remote exception" + e);
		}

		root = location;
		lockManager = new HashMap<String, Boolean>();
		pendingTransactions = new HashMap<Long, HashMap<Long, FileContent>>();
		transactionToFileNameMap = new HashMap<Long, String>();
		Runnable r = new Runnable() {
			public void run() {
				try {
					while (true) {
						Thread.sleep(3000);
						master.replicasHeartBeats.put(root, true);
					}
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		};

		new Thread(r).start();
	}

	public void addLock(String fileName) {
		lockManager.put(fileName, false);
	}

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
	@Override
	public WriteMsg write(long txnID, long msgSeqNum, FileContent data)
			throws RemoteException, IOException {
		if (!pendingTransactions.containsKey(txnID)) {
			pendingTransactions.put(txnID, new HashMap<Long, FileContent>());
			transactionToFileNameMap.put(txnID, data.getFileName());
		}
		HashMap<Long, FileContent> transactionLog = pendingTransactions
				.get(txnID);
		if (transactionLog.containsKey(msgSeqNum))
			throw new RemoteException(
					"Client Already transmitted a transaction with this transatcion ID and message sequence number...");
		else
			transactionLog.put(msgSeqNum, data);

		return new WriteMsg(txnID, System.currentTimeMillis(),
				master.getLocations(data.getFileName()));
	}

	@Override
	public boolean commit(long txnID, long numOfMsgs)
			throws MessageNotFoundException, RemoteException {
		
		if (!pendingTransactions.containsKey(txnID))
			throw new MessageNotFoundException();
		HashMap<Long, FileContent> transactionLog = pendingTransactions
				.get(txnID);
		if (transactionLog.size() != numOfMsgs)
			throw new RemoteException("INVALID NUMBER OF MESSAGES...");
		ReplicaLoc loc = master.getLocations(transactionToFileNameMap
				.get(txnID));
		for (String path : loc.getAddresses()) {
			for (Long key : transactionLog.keySet()) {
				FileContent current = transactionLog.get(key);
				String fileName = current.getFileName();
				try {
					// Acquiring lock...
					int counter = 0;
					boolean lock = lockManager.get(fileName);
					if (lock)
						System.out
								.println("File is currently being used by another user...");
					while (counter < 10 && lock) {
						try {
							Thread.sleep(1000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						counter++;
					}
					if (lock) {
						System.out.println("File Timeout...Try again later");
						return false;
					} else {
						lockManager.put(fileName, true);
						try {
							Thread.sleep(20000);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						FileWriter fw = new FileWriter(new File(path + "\\"
								+ fileName), true);
						fw.append(current.getContent() + "\n");
						fw.flush();
						fw.close();
						// master.newFileTransactions.remove(fileName);
						lockManager.put(fileName, false);
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return true;
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
	@Override
	public boolean abort(long txnID) throws RemoteException {
		// TODO Auto-generated method stub
		if (!pendingTransactions.containsKey(txnID))
			throw new RemoteException("INVALID TRANSACTION ID...");
		pendingTransactions.remove(txnID);
		return true;
	}

	@Override
	public FileContent read(FileContent fc) throws FileNotFoundException,
			IOException, RemoteException {
		File f = new File(root + "\\" + fc.getFileName());
		if (!f.exists())
			throw new FileNotFoundException(
					"File not found ... you must commit first before u can read...");
		int counter = 0;
		boolean lock = lockManager.get(fc.getFileName());
		if (lock)
			System.out
					.println("File is currently being used by another user...");
		while (counter < 10 && lock) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			counter++;
		}
		if (lock) {
			System.out.println("File Timeout...Try again later");
			return null;
		} else {
			FileReader fr = new FileReader(new File(root + "\\"
					+ fc.getFileName()));
			BufferedReader br = new BufferedReader(fr);
			String line;
			StringBuilder sb = new StringBuilder();
			while ((line = br.readLine()) != null)
				sb.append(line + "\n");
			fc.setContent(sb.toString());
			return fc;
		}
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public String getRmi_name() {
		return rmi_name;
	}

	public void setRmi_name(String rmi_name) {
		this.rmi_name = rmi_name;
	}

}

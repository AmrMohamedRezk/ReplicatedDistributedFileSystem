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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

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
	private HashMap<String, Integer> readersLock;
	private String root;
	private Master master;
	private HashMap<Long, HashMap<Long, FileContent>> pendingTransactions;
	private HashMap<Long, Pair> isCommited;
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
		readersLock = new HashMap<String, Integer>();
		pendingTransactions = new HashMap<Long, HashMap<Long, FileContent>>();
		transactionToFileNameMap = new HashMap<Long, String>();
		isCommited = new HashMap<Long, Replicas.Pair>();
		Runnable r = new Runnable() {
			public void run() {
				try {
					while (true) {
						Thread.sleep(2000);
						if (master.getReplicasHeartBeats().containsKey(root))
							master.getReplicasHeartBeats().put(root, true);
						else
							break;
					}
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		};

		new Thread(r).start();

		Runnable r2 = new Runnable() {
			public void run() {
				while (true) {
					try {
						Thread.sleep(60000);// Sleep one Minute
						HashSet<Long> set = new HashSet<Long>();
						for (Long key : isCommited.keySet()) {
							if ((System.currentTimeMillis() - isCommited
									.get(key).timeStamp) > 30000)// 30 second
																	// passed
							{
								pendingTransactions.remove(key);
								set.add(key);
							}
						}
						for (Long key : set)
							isCommited.remove(key);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}

		};
		new Thread(r2).start();

	}

	public void addLock(String fileName) {
		lockManager.put(fileName, false);
		readersLock.put(fileName, 0);
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
			isCommited.put(txnID, new Pair(System.currentTimeMillis(), false));
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

	public String getRoot() {
		return root;
	}

	@Override
	public boolean commit(long txnID, long numOfMsgs)
			throws MessageNotFoundException, RemoteException {
		if (!pendingTransactions.containsKey(txnID))
			throw new MessageNotFoundException();
		HashMap<Long, FileContent> transactionLog = pendingTransactions
				.get(txnID);
		if (transactionLog.size() != numOfMsgs)
			return false;
		// throw new RemoteException("INVALID NUMBER OF MESSAGES...");
		String fileName = transactionToFileNameMap.get(txnID);
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
			lock = lockManager.get(fileName);
		}
		if (lock) {
			System.out.println("File Timeout...COULD NOT ACQUIRE WRITE LOCK");
			return false;
		} else {
			// ACQUIRED WRITE LOCK
			// LOCKING THE FILE ON PRIMARY REPLICA
			lockManager.put(fileName, true);
			// MAKING SURE THAT THERE IS NO READ ON THE PRIMARY REPLICA...
			int readCounter = readersLock.get(fileName);
			while (readCounter != 0) {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				readCounter = readersLock.get(fileName);
			}

			// ACQUIRED WRITE LOCK AND READ LOCK NOW WE CAN START EDITING ALL
			// REPLICAS...

			// SORT MSGSEQNUM
			ArrayList<Long> sortedTransaction = new ArrayList<Long>(
					transactionLog.size());
			for (Long key : transactionLog.keySet())
				sortedTransaction.add(key);
			Collections.sort(sortedTransaction);
			HashMap<Long, FileContent> data = pendingTransactions.get(txnID);

			try {
				File f = new File(root + "\\" + fileName);
				if (!f.exists())
					f.createNewFile();
				FileWriter fw = new FileWriter(f, true);
				for (Long key : sortedTransaction)
					fw.append(data.get(key).getContent() + "\n");
				fw.flush();
				fw.close();

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			ReplicaLoc loc = master.getLocations(fileName);
			for (String replicaLocation : loc.getAddresses()) {
				if (replicaLocation.equals(root)) {
					continue;
				}
				Replicas copy = master.getReplicasObjects()
						.get(replicaLocation);
				int copyCounter = 0;
				boolean copyLock = copy.lockManager.get(fileName);
				if (copyLock)
					System.out
							.println("File is currently being used by another user...");
				while (copyCounter < 10 && copyLock) {
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					copyCounter++;
					copyLock = copy.lockManager.get(fileName);
				}
				if (copyLock) {
					System.out
							.println("File Timeout...COULD NOT ACQUIRE WRITE LOCK");
					return false;
				} else {
					// ACQUIRED WRITE LOCK
					// LOCKING THE FILE ON PRIMARY REPLICA
					copy.lockManager.put(fileName, true);
					// MAKING SURE THAT THERE IS NO READ ON THE PRIMARY
					// REPLICA...
					int copyReadCounter = readersLock.get(fileName);
					while (copyReadCounter != 0) {
						try {
							Thread.sleep(1000);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						copyReadCounter = readersLock.get(fileName);
					}
					// NOW WE ACQUIRED WRITE LOCK AND READ LOCK WE CAN START
					// EDITING...
					try {
						File f = new File(copy.getRoot() + "\\" + fileName);
						if (!f.exists())
							f.createNewFile();
						FileWriter fw = new FileWriter(f, true);
						for (Long key : sortedTransaction)
							fw.append(data.get(key).getContent() + "\n");
						fw.flush();
						fw.close();

					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					copy.lockManager.put(fileName, false);
				}

			}
			lockManager.put(fileName, false);

		}
		return true;
	}

	// @Override
	// public boolean commit(long txnID, long numOfMsgs)
	// throws MessageNotFoundException, RemoteException {
	//
	// if (!pendingTransactions.containsKey(txnID))
	// throw new MessageNotFoundException();
	// HashMap<Long, FileContent> transactionLog = pendingTransactions
	// .get(txnID);
	// if (transactionLog.size() != numOfMsgs)
	// return false;
	// // throw new RemoteException("INVALID NUMBER OF MESSAGES...");
	// ReplicaLoc loc = master.getLocations(transactionToFileNameMap
	// .get(txnID));
	// ArrayList<Long> sortedTransaction = new ArrayList<Long>(
	// transactionLog.size());
	// for (Long key : transactionLog.keySet())
	// sortedTransaction.add(key);
	// Collections.sort(sortedTransaction);
	// //ACQUIRING PRIMARY LOCK...
	// String fileName = transactionToFileNameMap.get(txnID);
	// int counter = 0;
	// boolean lock = lockManager.get(fileName);
	// int readCounter = readersLock.get(fileName);
	// if (lock)
	// System.out
	// .println("File is currently being used by another user...");
	// if (readCounter > 0)
	// System.out
	// .println("Someone is currently reading the file...");
	// while (counter < 10 && (lock || readCounter != 0)) {
	// try {
	// Thread.sleep(1000);
	// } catch (InterruptedException e) {
	// e.printStackTrace();
	// }
	// counter++;
	// lock = lockManager.get(fileName);
	// readCounter = readersLock.get(fileName);
	// }
	// if (lock || readCounter > 0) {
	// System.out.println("File Timeout...Try again later");
	// return false;
	// } else {
	// //PRIMARY LOCK ACQUIRED
	// lockManager.put(fileName, true);
	// // try {
	// // Thread.sleep(20000);
	// // } catch (InterruptedException e) {
	// // // TODO Auto-generated catch block
	// // e.printStackTrace();
	// // }
	// //WRITING TO PRIMARY REPLICA
	// for (Long key : sortedTransaction) {
	// try {
	// FileContent current = transactionLog.get(key);
	// FileWriter fw = new FileWriter(new File(root + "\\"
	// + current.getFileName()), true);
	// fw.append(current.getContent() + "\n");
	// fw.flush();
	// fw.close();
	// } catch (IOException e) {
	// // TODO Auto-generated catch block
	// e.printStackTrace();
	// }
	// }
	// // master.newFileTransactions.remove(fileName);
	//
	//
	// }
	// for (String path : loc.getAddresses()) {
	// if(path.equals(root))
	// continue;
	// FileContent current = transactionLog.get(sortedTransaction.get(0));
	// fileName = current.getFileName();
	// try {
	// // Acquiring lock...
	// counter = 0;
	// lock = lockManager.get(fileName);
	// readCounter = readersLock.get(fileName);
	// if (lock)
	// System.out
	// .println("File is currently being used by another user...");
	// if (readCounter > 0)
	// System.out
	// .println("Someone is currently reading the file...");
	// while (counter < 10 && (lock || readCounter != 0)) {
	// try {
	// Thread.sleep(1000);
	// } catch (InterruptedException e) {
	// e.printStackTrace();
	// }
	// counter++;
	// lock = lockManager.get(fileName);
	// readCounter = readersLock.get(fileName);
	// }
	// if (lock || readCounter > 0) {
	// System.out.println("File Timeout...Try again later");
	// return false;
	// } else {
	//
	// lockManager.put(fileName, true);
	// // try {
	// // Thread.sleep(20000);
	// // } catch (InterruptedException e) {
	// // // TODO Auto-generated catch block
	// // e.printStackTrace();
	// // }
	// for (Long key : sortedTransaction) {
	// current = transactionLog.get(key);
	// FileWriter fw = new FileWriter(new File(path + "\\"
	// + fileName), true);
	// fw.append(current.getContent() + "\n");
	// fw.flush();
	// fw.close();
	// }
	// // master.newFileTransactions.remove(fileName);
	// lockManager.put(fileName, false);
	// isCommited.get(txnID).isCommited = true;
	//
	// }
	// } catch (IOException e) {
	// e.printStackTrace();
	//
	// }
	// lockManager.put(fileName, false);
	// isCommited.get(txnID).isCommited = true;
	//
	// }
	// return true;
	// }

	public FileContent wrongNumberOfMsgs(long txnID, long numOfMsgs)
			throws RemoteException {
		FileContent c = new FileContent("Missing message", txnID);
		HashMap<Long, FileContent> transactionLog = pendingTransactions
				.get(txnID);
		String missing = "";
		for (long i = 0L; i < numOfMsgs; i++) {
			if (!transactionLog.containsKey(i))
				missing += i + ",";
		}
		missing = missing.substring(0, missing.length() - 1);
		c.setContent(missing);
		return c;
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
		if (!pendingTransactions.containsKey(txnID))
			throw new RemoteException("INVALID TRANSACTION ID...");
		pendingTransactions.remove(txnID);
		isCommited.remove(txnID);
		File f = new File(root + "\\" + transactionToFileNameMap.get(txnID));
		if (!f.exists()) {
			master.removeMetaData(transactionToFileNameMap.get(txnID));
		}
		return true;
	}

	@Override
	public FileContent read(FileContent fc) throws FileNotFoundException,
			IOException, RemoteException {
		File f = new File(root + "\\" + fc.getFileName());
		if (!f.exists())
			throw new FileNotFoundException(
					"File not found ... you must commit first before you can read...");
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
			readersLock.put(fc.getFileName(),
					readersLock.get(fc.getFileName()) + 1);
			FileReader fr = new FileReader(new File(root + "\\"
					+ fc.getFileName()));
			BufferedReader br = new BufferedReader(fr);
			String line;
			StringBuilder sb = new StringBuilder();
			while ((line = br.readLine()) != null)
				sb.append(line + "\n");
			fc.setContent(sb.toString());
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			readersLock.put(fc.getFileName(),
					readersLock.get(fc.getFileName()) - 1);
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

	private class Pair {
		Long timeStamp;
		boolean isCommited;

		public Pair(Long timeStamp, boolean isCommited) {
			this.timeStamp = timeStamp;
			this.isCommited = isCommited;
		}
	}
}

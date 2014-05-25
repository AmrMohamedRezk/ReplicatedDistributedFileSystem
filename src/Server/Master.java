package Server;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

import Core.FileContent;
import Core.ReplicaLoc;
import Core.WriteMsg;
import Interface.MasterServerClientInterface;

public class Master extends java.rmi.server.UnicastRemoteObject implements
		MasterServerClientInterface {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	/*
	 * ========================================================================
	 * STATUS: 1)The master server maintains metadata about the replicas and
	 * their locations STATUS:DONE 2)The server should communicate with clients
	 * through the given RMI interface. STATUS: 3)The server need to be invoked
	 * using the following command. a. server -ip [ip_address_string] -port
	 * [port_number] -dir <directory_path> b. where: ip_address_string is the ip
	 * address of the server. The default value is 127.0.0.1. port_number is the
	 * port number at which the server will be listening to messages. The
	 * default is 8080. directory_path is the directory where files are created
	 * and written by client transactions. This directory is the same for all
	 * replicaServers. At the server starts up, that directory may already
	 * contain some files (we could put some files there for testing). You must
	 * ensure that the files in that directory are not deleted after the server
	 * exits. We will use the contents in those files to check the correctness
	 * of your system STATUS: 4) Each file is replicated on there
	 * replicaServers. The IP addresses of all replicaServers are specified in a
	 * file called "repServers.txt". The master keeps heartbeats with these
	 * servers. STATUS: 5) Acknowledgements are sent to the client when data is
	 * written to all the replicaServers that contain replicas of a certain
	 * file. STATUS: 6) You will need to implement one of the primary-based
	 * consistency protocols studied in class.-->Remote Write Protocols
	 * ==========
	 * ===================================================================
	 */
	private final String repServerFileName = "repServers.txt";
	private HashMap<String, ReplicaLoc> replicaList;// map from fileName to
													// replicas addresses
	private HashMap<String, Replicas> replicasObjects;// map from replica Name
														// to replica location
	private LinkedList<String> replicasAddress;
	private long transactionID;
	private HashMap<String, Boolean> replicasHeartBeats;

	public Master() throws Exception {
		replicaList = new HashMap<String, ReplicaLoc>();
		replicasAddress = new LinkedList<String>();
		replicasObjects = new HashMap<String, Replicas>();
		FileReader fr = new FileReader(new File(repServerFileName));
		BufferedReader br = new BufferedReader(fr);
		String line;
		HashMap<String, LinkedList<String>> tempHashMap = new HashMap<String, LinkedList<String>>();// FROM
																									// FILE
																									// TO
		replicasHeartBeats = new HashMap<String, Boolean>(); // PATH
		int j = 1;
		while ((line = br.readLine()) != null) {
			replicasAddress.add(line);
			replicasObjects.put(line, new Replicas(line, j++, this));
			replicasHeartBeats.put(line, false);
			File folder = new File(line);

			File[] listOfFiles = folder.listFiles();
			for (int i = 0; i < listOfFiles.length; i++) {
				if (listOfFiles[i].isFile()) {
					if (!tempHashMap.containsKey(listOfFiles[i].getName()))
						tempHashMap.put(listOfFiles[i].getName(),
								new LinkedList<String>());
					tempHashMap.get(listOfFiles[i].getName()).add(line);
				}
			}
		}
		if (replicasAddress.size() < 3) {
			System.err.println("SIZE OF REPLICAS MUST BE >= 3");
			System.exit(0);
		}
		for (String s : tempHashMap.keySet()) {
			// s is the fileName
			LinkedList<String> locations = tempHashMap.get(s);
			if (locations.size() == 3) {
				replicaList.put(s, new ReplicaLoc(locations, locations.peek()));
				for (String s2 : locations) {
					Replicas r = replicasObjects.get(s2);
					r.addLock(s);
				}
			} else if (locations.size() < 3) {
				String filePath = locations.peek() + "\\" + s;
				File source = new File(filePath);
				while (locations.size() != 3) {
					String current= "";
					do{
					current = replicasAddress.poll();
					replicasAddress.add(current);
					}while(locations.contains(current));
					replicasAddress.add(current);
					File dest = new File(current + "\\" + s);
					Files.copy(source.toPath(), dest.toPath());
					locations.add(current);
				}
				replicaList.put(s, new ReplicaLoc(locations, locations.peek()));
				for (String s2 : locations) {
					Replicas r = replicasObjects.get(s2);
					r.addLock(s);
				}
			} else if (locations.size() > 3) {
				while (locations.size() != 3) {
					String last = locations.removeLast();
					File f = new File(last + "\\" + s);
					f.delete();
				}
				replicaList.put(s, new ReplicaLoc(locations, locations.peek()));
				for (String s2 : locations) {
					Replicas r = replicasObjects.get(s2);
					r.addLock(s);
				}
			}
		}
		Runnable r = new Runnable() {
			public void run() {
				try {
					while (true) {
						Thread.sleep(5000);
						HashSet<String> indices = new HashSet<String>();
						//USED TO SIMULATE FAILURE...
						replicasHeartBeats.put("F:\\replica5", false);
						synchronized (replicasHeartBeats) {
							for (String s : replicasHeartBeats.keySet()) {
								boolean flag = replicasHeartBeats.get(s);
								if (!flag) {
									System.err.println("REPLICA " + s
											+ " IS DEAD...");
									System.err
											.println("REMOVING IT FROM THE SYSTEM...");
									indices.add(s);
								} else
									replicasHeartBeats.put(s, false);
							}
						}
						int replicasSize = replicasAddress.size()
								- indices.size();
						if (replicasSize < 3) {
							System.err
									.println("***FAILURE NUMBER OF REPLICAS IS LESS THAN 3***");
							System.err.println("SYSTEM WILL EXIST NOW...");
							System.exit(0);
						}
						for (String s : indices) {
							synchronized (replicasObjects) {
								replicasObjects.remove(s);
							}
							synchronized (replicasHeartBeats) {

								replicasHeartBeats.remove(s);
							}
							synchronized (replicasAddress) {

								replicasAddress.remove(s);
							}
							synchronized (replicaList) {
								for (String fileName : replicaList.keySet()) {
									ReplicaLoc rl = replicaList.get(fileName);
									if (rl.getAddresses().contains(s)) {

										rl.getAddresses().remove(s);
										String root = rl.getAddresses().peek();
										File src = new File(root + "\\"
												+ fileName);
										String destination = "";
										synchronized (replicasAddress) {
											do {
												destination = replicasAddress
														.poll();
												replicasAddress
														.add(destination);
											} while (rl.getAddresses()
													.contains(destination));
										}
										File dest = new File(destination + "\\"
												+ fileName);
										try {
											Files.copy(src.toPath(),
													dest.toPath());
										} catch (IOException e) {
											// TODO Auto-generated catch block
											e.printStackTrace();
										}
										rl.getAddresses().add(destination);
										rl.setPrimaryLocation(rl.getFirstLocation());
										rl.setAddress(rl.getFirstLocation());
									}
								}
							}
						}
					}
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		};

		new Thread(r).start();
		transactionID = 0L;
	}

	public ReplicaLoc getLocations(String fileName) {
		synchronized (replicaList) {
			return replicaList.get(fileName);
		}
	}

	public HashMap<String, Replicas> getReplicasObjects() {
		synchronized (replicasObjects) {
			return replicasObjects;
		}
	}

	/**
	 * Reads from the file system. Files are read entirely. When a client sends
	 * a file name to the server, the entire file is returned to the client.
	 */
	@Override
	public FileContent read(String fileName) throws FileNotFoundException,
			IOException, RemoteException {
		FileContent currentFileContent = null;
		ReplicaLoc rl = null;
		synchronized (replicaList) {
			if (!replicaList.containsKey(fileName))
				throw new FileNotFoundException();
			currentFileContent = new FileContent(fileName, transactionID++);
			rl = replicaList.get(fileName);

		}
		Replicas r = null;
		synchronized (replicasObjects) {
			r = replicasObjects.get(rl.getFirstLocation());
		}
		rl.setAddress(r.getAddress());
		rl.setPort(r.getPort());
		rl.setRmiReg_name(r.getRmi_name());
		rl.advanceQueue();
		currentFileContent.setRl(rl);
		return currentFileContent;

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
	 * 
	 * 8) The new file must not be seen on the file system until the transaction
	 * commits. That is a read request to a file that is being updated by an
	 * uncommitted transaction must generate an error.
	 */
	@Override
	public WriteMsg write(FileContent data) throws RemoteException, IOException {
		// TODO Auto-generated method stub
		String fileName = data.getFileName();
		ReplicaLoc rl = null;
		synchronized (replicaList) {
			if (!replicaList.containsKey(fileName))
				replicaList.put(fileName, createFile(fileName));

			rl = replicaList.get(fileName);
		}
		Replicas r = replicasObjects.get(rl.getPrimaryLocation());
		rl.setPrimaryLocation(rl.getPrimaryLocation());
		rl.setAddress(r.getAddress());
		rl.setPort(r.getPort());
		rl.setRmiReg_name(r.getRmi_name());
		return new WriteMsg(transactionID++, System.currentTimeMillis(), rl);
	}

	private ReplicaLoc createFile(String fileName) throws IOException {
		LinkedList<String> replicaLocations = null;
		String primary = "";
		synchronized (replicasAddress) {
			primary = replicasAddress.peek();
			replicasAddress.add(replicasAddress.poll());
			replicaLocations = new LinkedList<String>();
			replicaLocations.add(primary);
			replicaLocations.add(replicasAddress.peek());
			replicasAddress.add(replicasAddress.poll());
			replicaLocations.add(replicasAddress.peek());
			replicasAddress.add(replicasAddress.poll());
		}
		for (String s : replicaLocations) {
			Replicas r = replicasObjects.get(s);
			r.addLock(fileName);
		}
		return new ReplicaLoc(replicaLocations, primary);
	}

	public HashMap<String, Boolean> getReplicasHeartBeats() {
		synchronized (replicasHeartBeats) {
			return replicasHeartBeats;
		}
	}

	public static void main(String[] args) throws Exception {
		System.setProperty("java.rmi.server.hostname", "localhost");
		Registry registry = LocateRegistry.createRegistry(5555);
		Master m = new Master();
		try {

			registry.rebind("rmiServer", m);
		} catch (RemoteException e) {
			System.out.println("remote exception" + e);
		}
	}
}

ReplicatedDistributedFileSystem
===============================
This assignment is loosely based on lab assignments given at Simon Fraser University, Canada.
Objective
It is required to implement a replicated file system. There will be one main server (master) and, data will be partitioned and replicated on multiple replicaServers. This file system allows its concurrent users to perform transactions, while guaranteeing ACID properties. This means that the following need to be ensured:
  - The master server maintains metadata about the replicas and their locations.
  - The user can submit multiple operations that are performed atomically on the shared objects stored in the file system.
  - Files are not partitioned.
  - Assumption: each transaction access only one file.
  - Each file stored on the distributed file system has a primary replica.
  - After performing a set of operations, it is guaranteed that the file system will remain in consistent state.
  - A lock manager is maintained at each replicaServer.
  - Once any transaction is committed, its mutations to objects stored in the file system are required to be durable.

The characteristics of the file

  1. Reads from the file system.
      Files are read entirely. When a client sends a file name to the server, the entire file is returned to the client.
  2. Writes to files stored on the file system.
     The following procedure is followed: 

        1) The client requests a new transaction ID from the server. The request includes the name of the file to be muted          during the transaction. 
        2) The server generates a unique transaction ID and returns it, a timestamp, and the location of the primary              replica of that file to the client in an acknowledgment to the client's file update request. 
        3) If the file specified by the client does not exist, the server creates the file on the replicaServers and                initializes its metadata. 
        4) All subsequent client messages corresponding to a transaction will be directed to the replicaServer with the             primary replica contain the ID of that transaction. 
        5) The client sends to the replicaServer a series of write requests to the file specified in the transaction. Each           request has a unique serial number. The server appends all writes sent by the client to the file. Updates are             also propagated in the same order to other replicaServers.
        6) The server must keep track of all messages received from the client as part of each transaction. The server              must also apply file mutations based on the correct order of the transactions. 
        7) At the end of the transaction, the client issues a commit request. This request guarantees that the file is              written on all the replicaServer disks. Therefore, each replicaServer flushes the file data to disk and sends an           acknowledgement of the committed transaction to the primary replicaServer for that file. Once the primary                 replicaServer receives acknowledgements from all replicas, it sends an acknowledgement to the client. 
        8) The new file must not be seen on the file system until the transaction commits. That is a read request to a                file that is being updated by an uncommitted transaction must generate an error.

  3. Client specification:
  

        1) Clients read and write data to the distributed file system.
        2) Each client has a file in its local directory that specifies the main server IP address.
 

 4. Main Server specification:
 

  
      1) The server should communicate with clients through the given RMI interface.
      2) The server need to be invoked using the following command. 
          a. server -ip [ip_address_string] -port [port_number] -dir <directory_path> 
          b. where: 
                  - ip_address_string is the ip address of the server. The default value is 127.0.0.1.
                  - port_number is the port number at which the server will be listening to messages. The default is 8080.                   - directory_path is the directory where files are created and written by client transactions. This                          directory is the same for all replicaServers. At the server starts up, that directory may already                         contain some files (we could put some files there for testing). You must ensure that the files in that                     directory are not deleted after the server exits. We will use the contents in those files to check the                     correctness of your system. 
      3) When the client asks the primary replicaServer to commit a transaction, the server must ensure that the                  transaction data is flushed to disk on the all the replicaServers local file systems. Using the write is not              enough to accomplish this because it puts the data into file system buffer cache, but does not force it to disk.          Therefore, you must explicitly flush the data to disk when committing client transactions or when writing                 important housekeeping messages that must survive crashes. To flush the data to disk, use flush() and close()             methods of the FileWriter in Java.
  5. ReplicaServer specification :

 
  
        1) Each file is replicated on there replicaServers. The IP addresses of all replicaServers are specified in a file            called "repServers.txt". The master keeps heartbeats with these servers. 
        2) Acknowledgements are sent to the client when data is written to all the replicaServers that contain replicas of            a certain file. 
        3) You will need to implement one of the primary-based consistency protocols studied in class.
  6. Concurrency. Multiple clients may be executing transactions against the same file. Isolation and concurrency control     properties should be guaranteed by the server.
  7. Handling aborting a transaction.
    A client can decide to abort the transaction after it has started. Note, that the client might have already sent write     requests to the server. In this case, the client requests transaction abort from the server. The client's abort           request is handled as follows: 

        1) The primary replicaServer ensures that no data that the client had sent as part of transaction is written to             the file on the disk of any of the replicaServers. 
        2) If a new file was created as part of that transaction, the master server deletes the file from its metadata and             the file is deleted from all replicaServers. 3) The primary replicaServer acknowledges the client's abort.

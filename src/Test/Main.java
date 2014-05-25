package Test;

import java.util.Scanner;

import Client.Client;
import Core.FileContent;

public class Main {
	public static void main(String[] args) throws Exception {

		// PHASE ONE TESTING SERVER ALONE...
		/**
		 * Test case One: Put the files in all replica and make sure the server
		 * handles it by putting it in only three locations and delete the
		 * rest...
		 * 
		 * Status : SUCCESS
		 */

		/**
		 * Test case Two: Put the file in one replica and make sure the server
		 * handles it by putting it in only three locations...
		 * 
		 * Status : Failure we didn't check if the current location was already
		 * there... Status : SUCCESS
		 * 
		 * 
		 * Test case Three Simulated failure in replica Files in the failed
		 * replica copied to other replicas Status : SUCCESS
		 */

		// PHASE TWO TESTING TESTING USING CLIENTS...
		Client c1 = new Client();
		Client c2 = new Client();
		/**
		 * Basic client read and write operations to the file system. This
		 * includes: (1) the file updated by the client contains the correct
		 * written data after committing the transaction, (2) partial updates
		 * (before transaction commits) are not seen by other clients or other
		 * transactions by the same client, and (3) a new file created by a
		 * transaction is not visible in the file system until the transaction
		 * commits.
		 **/
		System.out.println("**Writing File1.txt and reading it again ");
		FileContent data = new FileContent("File1.txt", 0);
		data.setContent("Cause I am Happyyyyyyyyyyyy :)");
		System.out.println("C1 writing ......");
		c1.write(data, false);
		System.out.println("Check the File Visiablity");
		Scanner scan = new Scanner(System.in);
		scan.next();
		scan.close();
		System.out.println("C1 commiting ......1 happy ;)");
		c1.commit(1);
		System.out.println("C1 reading ......");
		c1.read("File1.txt");

		System.out
				.println("** C1 Writing to File1.txt and reading it again by C1 and C2 without commiting");
		System.out.println("C1 writing ...... ");
		c1.write(data, false);
		System.out.println("C1 reading ......");
		c1.read("File1.txt");
		System.out.println("C2 reading ......");
		c2.read("File1.txt");
		System.out.println("C1 commiting ......2 happy ;)");
		c1.commit(1);
		System.out.println("C2 reading ......");
		c2.read("File1.txt");
		/**
		 * In case of aborted transactions, we will double check that any
		 * mutations to a file as part of this transaction is not rolled back
		 * after the transaction is aborted. We will also check that any file
		 * that was created as part of the aborted transaction does not exist
		 * after the transaction aborts.
		 */

		System.out
				.println("** C1 Writing to File1.txt and reading it again by C1 and C2 without commiting");
		System.out.println("C1 writing ...... ");
		c1.write(data, false);
		System.out.println("C1 abort ..... no happy :(");
		c1.abort();
		/**
		 * Bonus: We will intentionally have the client omit some messages to
		 * test how the server handles undelivered messages.
		 * 
		 * STATUS: will be tested in Bonus part
		 */


		/**
		 * We will test multiple concurrent clients accessing the same file and
		 * check if the mutations to the file are done as if the clients'
		 * transactions were performed sequentially.
		 */
		for (int i = 0; i < 3; i++) {
			Runnable r = new Runnable() {
				public void run() {
					try {
						Client c = new Client();
						FileContent content = new FileContent("Ahmad.txt",
								Client.index);
						Client.index++;
						content.setContent("not seen 1 :P :P ");
						c.write(content, false);
						content = new FileContent("Ahmad.txt", 0);
						content.setContent("not seen 2 :P :P ");
						c.write(content, false);
						// c.abort(m);
						c.commit(2);
						c.read("amr.txt");
					} catch (Exception e) {
						e.printStackTrace();
					}

				}
			};

			new Thread(r).start();
		}

		/**
		 * Bonus: We will also test failing a client before committing its
		 * transaction. STATUS : like closing and didn't committed will be test
		 * in Bonus
		 */

		/**
		 * Bonus: The server failure while sending a transaction will also be
		 * tested. If there is only one server, we expect it to continue its
		 * transactions when it is restarted. If there are multiple servers,
		 * killing one of them means that the other servers should resume
		 * processing the transactions.
		 * 
		 * STATUS: ??? 27na olna eih ?!
		 * 
		 */

		/**
		 * Handling loss of messages. Each message sent within a transaction has
		 * a serial number. If some of the messages that are part of a
		 * transaction are lost, the server should not commit until it receives
		 * them. This can be handled as follows: 1) When the transaction commit
		 * request is sent by the client, it should include the total number of
		 * messages that the server should have received. 2) If the server
		 * realizes that it has not received all of the messages that the client
		 * claims to have sent, it must ask the client to retransmit the missing
		 * messages. The retransmission request must include the transaction ID
		 * as well as the missing message number. 3) A separate retransmission
		 * request is sent for each missing message.
		 */

		System.out
				.println("C1 writing  new File and commiting with 5 and sent is 3 ");
		data = new FileContent("WMessages.txt", 0);
		data.setContent("1");
		c1.write(data, true);
		data.setContent("2");
		c1.write(data, false);
		data.setContent("3");
		c1.write(data, true);
		data.setContent("4");
		c1.write(data, false);
		data.setContent("5");
		c1.write(data, false);
		c1.commit(5);
		System.out.println("Reading after Commiting ");
		c1.read("WMessages.txt");

		/**
		 * Moreover, the client may lose the server's acknowledgment of the
		 * committed transaction. In that case, it will retransmit the message
		 * asking the server to commit the transaction. The server must remember
		 * that the transaction has been committed and re-send the
		 * acknowledgment to the client.
		 * 
		 * could be showed in code :/
		 */

		/**
		 * Client failure. If a client crashes before committing the
		 * transaction, the transaction must never be committed. It is the
		 * responsibility of the server to decide how long to keep track of
		 * unfinished transactions by setting a timeout
		 * */
		data = new FileContent("Crash.txt", 0);
		data.setContent("I am crashing I am crashing .........!!");
		c1.write(data, false);
		c1.write(data, false);
		System.exit(0);


	}
}

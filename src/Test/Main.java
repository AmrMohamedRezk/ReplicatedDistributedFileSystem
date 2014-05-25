package Test;

import Client.Client;

public class Main {
	public static void main(String[] args) throws Exception {
		
		//PHASE ONE TESTING SERVER ALONE...
		/**
		 * Test case One:
		 * Put the files in all replica and make sure the server handles it
		 * by putting it in only three locations and delete the rest...
		 * 
		 * Status : SUCCESS
		 */
		
		/**
		 * Test case Two:
		 * Put the file in one replica and make sure the server handles it
		 * by putting it in only three locations...
		 * 
		 * Status : Failure we didn't check if the current location was already there...
		 * Status : SUCCESS
		 * 
		 * 
		 * Test case Three Simulated failure in replica
		 * Files in the failed replica copied to other replicas
		 * Status : SUCCESS
		 */
		
		//PHASE TWO TESTING TESTING USING CLIENTS...
		/**
		 * 
		 */
		
		
		//test wrong number of committed messages 
		Client c1 = new Client();
		Client c2 = new Client();
		

	}
}

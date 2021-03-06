package Interface;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.rmi.Remote;
import java.rmi.RemoteException;

import Core.FileContent;
import Core.WriteMsg;


public interface MasterServerClientInterface extends Remote {
	/**
	 * Read file from server
	 * 
	 * @param fileName
	 * @return File data
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws RemoteException
	 */
	public FileContent read(String fileName) throws FileNotFoundException,
			IOException, RemoteException;

	/**
	 * Start a new write transaction
	 * 
	 * @param fileName
	 * @return the requiref info
	 * @throws RemoteException
	 * @throws IOException
	 */
	public WriteMsg write(FileContent data) throws RemoteException, IOException;

}

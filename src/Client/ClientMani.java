package Client;

import java.io.IOException;
import java.rmi.RemoteException;

import Core.FileContent;
import Core.MessageNotFoundException;
import Core.WriteMsg;

public class ClientMani {

	public ClientMani() throws RemoteException, IOException, MessageNotFoundException {
		Client c = new Client();
		FileContent content = new FileContent("Ahmad.txt", 0);

		WriteMsg m = c.write(null, content);
		content = new FileContent("Ahmad.txt", m.getTransactionId());
		content.setContent("I am testing :P :P ");
		c.commit(m, 2);
		

	}

	public static void main(String[] args) throws RemoteException, IOException, MessageNotFoundException {
		ClientMani c = new ClientMani();
	}

}

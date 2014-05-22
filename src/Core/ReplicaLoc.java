package Core;

import java.util.LinkedList;

public class ReplicaLoc {
	private LinkedList<String> locationList;
	private String primaryLocation;
	public ReplicaLoc(LinkedList<String> locationList,String primaryLocation)
	{
		this.locationList=locationList;
		this.setPrimaryLocation(primaryLocation);
	}
	
	public void advanceQueue()
	{
		locationList.add(locationList.poll());
	}

	public void setPrimaryLocation(String primaryLocation) {
		this.primaryLocation = primaryLocation;
	}

	public String getPrimaryLocation() {
		return primaryLocation;
	}
	
	String address;
	int port;
	String rmiReg_name;
	public String getAddress() {
		return address;
	}
	public void setAddress(String address) {
		this.address = address;
	}
	public int getPort() {
		return port;
	}
	public void setPort(int port) {
		this.port = port;
	}
	public String getRmiReg_name() {
		return rmiReg_name;
	}
	public void setRmiReg_name(String rmiReg_name) {
		this.rmiReg_name = rmiReg_name;
	}
	
	
	
	
}

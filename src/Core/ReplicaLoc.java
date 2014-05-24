package Core;

import java.io.Serializable;
import java.util.LinkedList;

public class ReplicaLoc implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private LinkedList<String> locationList;
	private String primaryLocation;
	private String address;
	private int port;
	private String rmiReg_name;
	
		
	public ReplicaLoc(LinkedList<String> locationList, String primaryLocation) {
		this.locationList = locationList;
		this.setPrimaryLocation(primaryLocation);
	}

	public void advanceQueue() {
		locationList.add(locationList.poll());
	}

	public LinkedList<String> getAddresses() {
		return locationList;
	}

	public String getFirstLocation() {
		return locationList.peek();
	}

	public void setPrimaryLocation(String primaryLocation) {
		this.primaryLocation = primaryLocation;
	}

	public String getPrimaryLocation() {
		return primaryLocation;
	}

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

	public LinkedList<String> getLocationList() {
		return locationList;
	}

	public void setLocationList(LinkedList<String> locationList) {
		this.locationList = locationList;
	}

}

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
	
}

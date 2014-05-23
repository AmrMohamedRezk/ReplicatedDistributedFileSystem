package Core;

import java.io.Serializable;

public class FileContent implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String fileName;
	private String Content;
	long xaction_number;

	// private ReplicaLoc rl;

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public String getFileName() {
		return fileName;
	}

	public FileContent(String name, long xaction) {
		fileName = name;
		xaction_number = xaction;
	}

	public FileContent(String name, String content, int xaction) {
		// TODO Auto-generated constructor stub
		fileName = name;
		Content = content;
		xaction_number = xaction;
	}

	public String getContent() {
		return Content;
	}

	public void setContent(String content) {
		Content = content;
	}

	public long getXaction_number() {
		return xaction_number;
	}

	public void setXaction_number(int xaction_number) {
		this.xaction_number = xaction_number;
	}

	// public void setRl(ReplicaLoc rl) {
	// this.rl = rl;
	// }
	//
	// public ReplicaLoc getRl() {
	// return rl;
	// }

}

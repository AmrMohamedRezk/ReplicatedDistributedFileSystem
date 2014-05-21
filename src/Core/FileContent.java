package Core;

public class FileContent {

	String file_name;
	String Content;
	int xaction_number;

	public FileContent(String name, String content, int xaction) {
		// TODO Auto-generated constructor stub
		file_name = name;
		Content = content;
		xaction_number = xaction;
	}

	public String getFile_name() {
		return file_name;
	}

	public void setFile_name(String file_name) {
		this.file_name = file_name;
	}

	public String getContent() {
		return Content;
	}

	public void setContent(String content) {
		Content = content;
	}

	public int getXaction_number() {
		return xaction_number;
	}

	public void setXaction_number(int xaction_number) {
		this.xaction_number = xaction_number;
	}

}

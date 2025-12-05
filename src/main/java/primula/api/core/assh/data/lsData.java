package primula.api.core.assh.data;

import java.util.ArrayList;
import java.util.List;

public class lsData {
	private List<Object> data;

	public lsData() {
		data = new ArrayList();
	}

	public void setData(String line) {
		data.add(line);
	}

	public ArrayList<Object> getData() {
		return (ArrayList<Object>) data;
	}
}

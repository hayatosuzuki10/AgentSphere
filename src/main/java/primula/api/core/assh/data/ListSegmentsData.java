package primula.api.core.assh.data;

import java.util.ArrayList;
import java.util.List;

public class ListSegmentsData {
	private List<Object> data;

	public ListSegmentsData() {
		data = new ArrayList();
	}

	public void setData(String line) {
		data.add(line);
	}

	public ArrayList<Object> getData() {
		return (ArrayList<Object>) data;
	}
}

package primula.api.core.assh.command;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import primula.api.core.assh.data.catData;

public class cat extends AbstractCommand {

	private catData catd = new catData();

	@Override
	public List<Object> runCommand(List<String> fileNames, Object instance, List<String> opt) {
		while(!fileNames.isEmpty()) {
			File file = new File(shellEnv.getDirectory() + "\\" + fileNames.remove(0));
			System.out.println(file.getName());
			if(file.isFile()) {
				InputStream is = null;
				Reader r = null;
				BufferedReader br = null;
				List<String> textList = new ArrayList();
				try {
					is = new FileInputStream(file);
					r  = new InputStreamReader(is, "UTF-8");
					br = new BufferedReader(r);
					for (;;) {
						String text = br.readLine();
						if (text == null) break;
						System.out.println(text);
						catd.setData(text);
					}
				} catch (Exception e) {
					throw new RuntimeException(e);
				} finally {
					if (br != null) try { br.close(); } catch (IOException e) {}
					if (r  != null) try { r .close(); } catch (IOException e) {}
					if (is != null) try { is.close(); } catch (IOException e) {}
				}
			}
		}
		data = catd.getData();
		return data;
	}
}

package primula.api.core.assh.command.interim;

import java.io.Serializable;
import java.util.List;

import primula.api.core.assh.command.AbstractCommand;
import primula.api.core.network.dhtmodule.data.hubimpl.IntegerRange;
import primula.api.core.network.dhtmodule.nodefunction.CommunicationException;
import primula.api.core.network.dhtmodule.routing.impl.MercuryImpl;
import primula.api.core.network.dhtmodule.utill.MercuryUtil;

public class range extends AbstractCommand {

	@Override
	public List<Object> runCommand(List<String> fileNames, Object instance, List<String> opt) {
		System.out.println(fileNames);
		int lower = Integer.parseInt(fileNames.get(0));
		int upper = Integer.parseInt(fileNames.get(1));
		int max = Integer.parseInt(fileNames.get(2));

		MercuryImpl impl = MercuryUtil.getImpl();
		IntegerRange range = new IntegerRange("INTEGER", lower, upper, max);
		List<Serializable> result = null;
		try {
			result = impl.getObject("INTEGER", range);
		} catch (CommunicationException e) {
			e.printStackTrace();
		}

		if(result != null) {
			for(int i=0; i<result.size(); i++) {
				System.out.println(result.get(i) + " *****");
			}
		}
		return null;
	}

}

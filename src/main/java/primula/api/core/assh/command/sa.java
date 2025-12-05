package primula.api.core.assh.command;

import java.util.List;

import primula.util.IPAddress;

// set addressコマンド

public class sa extends AbstractCommand {

	@Override
	public List<Object> runCommand(List<String> fileNames, Object instance, List<String> opt) {
		if(!fileNames.isEmpty()) {
			String initial = fileNames.get(0);
	    	if(initial.equals("o")) {
	    		IPAddress.IPAddress = "133.220.114.240";
	    		System.out.println("connect to \"" + IPAddress.IPAddress + "\" (Okubo)");
	    	}
	    	else if(initial.equals("k")) {
	    		IPAddress.IPAddress = "133.220.114.244";
	    		System.out.println("connect to \"" + IPAddress.IPAddress + "\" (Kurosaki)");
	    	}
	    	else if(initial.equals("h")) {
	    		IPAddress.IPAddress = "133.220.114.245";
	    		System.out.println("connect to \"" + IPAddress.IPAddress + "\" (Hikida)");
	    	}
	    	else if(initial.equals("s")) {
	    		IPAddress.IPAddress = "133.220.114.108";
	    		System.out.println("connect to \"" + IPAddress.IPAddress + "\" (Sato)");
	    	}
	    	else if(initial.equals("n")) {
	    		IPAddress.IPAddress = "133.220.114.106";
	    		System.out.println("connect to \"" + IPAddress.IPAddress + "\" (Nagashio)");
	    	}
		}
		return null;
	}

}

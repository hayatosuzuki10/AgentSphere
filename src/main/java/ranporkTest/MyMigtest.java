package ranporkTest;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.apache.commons.javaflow.api.continuable;

import primula.agent.AbstractAgent;
import primula.util.IPAddress;

public class MyMigtest extends AbstractAgent {
	private int max = 100000000;
	//try-catch節内でmigrateすると挙動が不安定になるので注意
	public @continuable void run() {
		System.out.println("Starting Migration\n");
		InetAddress addr;
		try {
			addr = InetAddress.getByName(IPAddress.IPAddress);
		} catch (UnknownHostException e) {
			throw new RuntimeException("なんかダメだった", e);
		}
		System.out.println(this.getClass().getName() + " go to " + IPAddress.IPAddress);
		migrate(addr);
		System.out.println("Migrated");
		try {
			addr = InetAddress.getByName(IPAddress.IPAddress);
		} catch (UnknownHostException e) {
			throw new RuntimeException("なんかダメだった", e);
		}
		System.out.println(this.getClass().getName() + " go to " + IPAddress.IPAddress);
		migrate(addr);
		System.out.println("Migrated2");
		try {
			addr = InetAddress.getByName(IPAddress.IPAddress);
		} catch (UnknownHostException e) {
			throw new RuntimeException("なんかダメだった", e);
		}
		System.out.println(this.getClass().getName() + " go to " + IPAddress.IPAddress);
		migrate(addr);
		System.out.println("Migrated3");
		for (int i = 0; i < 100; i++) {
			//if(i==50)this.backup();
			System.out.println(i);
		}
		System.out.println("finish");
	}

	@Override
	public void requestStop() {
		// TODO Auto-generated method stub

	}

}

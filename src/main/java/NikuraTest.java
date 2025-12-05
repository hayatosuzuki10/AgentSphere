
/**
 * @author RanPork
 * みちゃいやん
 */

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.io.BufferedOutputStream;
import java.io.CharArrayReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.stream.Stream;

import javax.swing.JFrame;
import javax.swing.JLabel;

import org.seasar.framework.container.factory.SingletonS2ContainerFactory;

import primula.agent.AbstractAgent;
import primula.util.IPAddress;
import ranporkTest.StaticTest;
import sphereConnection.EasyDistributeHashTable;
import sphereConnection.EasySphereNetworkManeger;
import sphereConnection.stub.SphereSpec;
import sphereIO.SphereFile;
import sphereIO.SphereFileChannel;
import sphereIO.SphereFileInputStream;
import sphereIO.system.FileInfo;
import sphereIO.system.FileTask;
import sphereIO.system.SphereFileContentContainer;

public class NikuraTest extends AbstractAgent {

	public void run() {
		System.err.println("うっす");
		ranporkTest.Util.func();
		if (true)
			return;
		System.out.println("デッドコード");
		try {
			Files.deleteIfExists(Paths.get("share/ChannelTest.dat"));
			SphereFileChannel fc = SphereFileChannel.open(new SphereFile("share/ChannelTest.dat", "192.168.1.91"),
					StandardOpenOption.WRITE, StandardOpenOption.CREATE);
			System.out.println("おーぷんなう");
			byte[] data = { 1, 2, 3, 4, 5, 6, 7, 8 };
			ByteBuffer databuf = ByteBuffer.wrap(data);
			System.out.println("らいとなう");
			System.out.println(fc.write(databuf));
			databuf.rewind();
			System.out.println(fc.write(databuf, 8192));

			fc.close();
			System.out.println("closeなう");
		} catch (IOException e) {
			// TODO: handle exception
			e.printStackTrace();
		}

		try {
			SphereFileChannel fc = null;
			fc = SphereFileChannel.open(new SphereFile("share/charTest.dat"), StandardOpenOption.READ);

			System.out.println("NiikuraTest:オープンかんりょう");
			ByteBuffer buffer = ByteBuffer.allocate(2);

			System.out.println("byte:" + fc.read(buffer));
			buffer.flip();
			while (buffer.hasRemaining())
				System.out.println(buffer.get());
			buffer.clear();

			System.out.println("byte:" + fc.read(buffer, 2));

			buffer.flip();
			while (buffer.hasRemaining())
				System.out.println(buffer.get());
			buffer.clear();
		} catch (IOException e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		/*
		 * SphereFileInputStream
		 */
		try {
			SphereFileInputStream fi = new SphereFileInputStream(
					new SphereFile("share/ChannelTest.dat", "192.168.1.91"));
			System.out.println(fi.read());
			fi.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		// テスト用
		/*
		 * serializableでないインスタンスの実験 当然むり susspendが例外投げた（絶望）
		 */
		char[] charBuf = new char[10];
		for (int i = 0; i < 10; i++) {
			charBuf[i] = (char) ('a' + i);
		}
		CharArrayReader car = new CharArrayReader(charBuf);
		for (int i = 0; i < 10; i++) {
			if (i == 5) {
				try {
					this.migrate(InetAddress.getByName(IPAddress.myIPAddress));
				} catch (UnknownHostException e) {
				}
			}
			try {
				System.out.println((char) car.read());
			} catch (IOException e) {
				// TODO 自動生成された catch ブロック
				e.printStackTrace();
			}
		}
		/*
		 * TEST FileInfo
		 */
		System.out.println("TEST:FileInfo");
		SphereFile file = new SphereFile("share/test.txt");
		System.out.println(file.getPath() + file.getHostIP());
		FileInfo info = null;
		try {
			info = FileInfo.getFileInfo(file);
		} catch (IOException e2) {
			// TODO 自動生成された catch ブロック
			e2.printStackTrace();
		}
		System.out.println(info.toString());
		try {
			info = FileInfo.getFileInfo(file);
		} catch (IOException e2) {
			// TODO 自動生成された catch ブロック
			e2.printStackTrace();
		}
		System.out.println(info.toString());

		/*
		 * 1024KBファイル作成
		 */
		try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream("share/largeData.dat"));) {
			for (int i = 0; i < 1024 * 1024; i++)
				bos.write(0);
		} catch (FileNotFoundException e3) {
			// TODO 自動生成された catch ブロック
			e3.printStackTrace();
		} catch (IOException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}
		Stream<Path> stream = null;
		try {
			stream = Files.walk(Paths.get("share"));
		} catch (IOException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}
		List<Path> list = new ArrayList<Path>();
		stream.forEach(new Consumer<Path>() {
			public void accept(Path t) {
				list.add(t);
			}
		});
		for (Path path : list) {
			System.out.println(path);
		}
		// System.out.println(Paths.get("share/test/util.txt").normalize().toString());//share\\test\\util.txt

		EasyDistributeHashTable table = (EasyDistributeHashTable) SingletonS2ContainerFactory.getContainer()
				.getComponent("EasyDistributeHashTable");
		table.setIP("test.txt", "192.168.2.3");
		table.setSpec("AbstractAgent", new SphereSpec(114, 514, 810));
		System.out.println("あえ");
		System.out.println("Starting Migration\n");

		int num = 10;
		for (int i = 0; i < num; i++) {
			if (i == num / 2) {
				System.out.println("Migrated");
				this.migrate();
			}
			try {
				Thread.sleep(1 * 1000);
			} catch (InterruptedException e) {
				// TODO 自動生成された catch ブロック
				e.printStackTrace();
			}
			System.out.println(i);
		}
		System.out.println("Migrated");
		this.migrate();
		System.out.println("finish");

		System.out.println("TEST:EasySphereNetworkManeger");
		EasySphereNetworkManeger ESNM = (EasySphereNetworkManeger) SingletonS2ContainerFactory.getContainer()
				.getComponent("EasySphereNetworkManeger");
		for (String iterable_element : ESNM.getIPTable()) {
			System.out.println(iterable_element);
		}

		/*
		 * TEST SphereContentContainer FileTask
		 */
		System.out.println("TEST:SphereContentContainer,FileTask");
		SphereFileContentContainer sfcc = null;
		FileTask ft = new FileTask();
		supplier sp = new supplier(ft);
		sp.start();
		try {
			sfcc = ft.get();
		} catch (InterruptedException e1) {
			// TODO 自動生成された catch ブロック
			e1.printStackTrace();
		} catch (ExecutionException e1) {
			// TODO 自動生成された catch ブロック
			e1.printStackTrace();
		}
		System.out.println(sfcc.getTag().toString());
		System.out.println("TEST:(´･ω･｀)");
		Font[] fonts = GraphicsEnvironment.getLocalGraphicsEnvironment().getAllFonts();
		for (Font f : fonts) {
			// System.out.println(f.getName());
		}
		JLabel label = new JLabel("<html><pre>　　　　　(⌒`)\r\n" + "　　　　　（　）　\r\n" + "　　　＿＿( )　\r\n"
				+ "　　　|;;lヽ::/　ｺﾎﾟｺﾎﾟ 　,､＿,､＿,_　　\r\n" + "　　　|;;| □o　　　　　 (´･ω･｀)　)　　　\r\n"
				+ "　　　i===i=i.　　　　　　`u--u'-u'　\r\n" + "|￣￣￣￣￣￣￣￣￣￣￣￣￣</pre></html>");
		label.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
		JFrame frame = new JFrame() {
			{
				setBounds(100, 100, 600, 400);
			}
		};
		frame.add(label);
		frame.setVisible(true);
		if (true) {
			throw new RuntimeException("らんらん♪");
		}
		Enumeration<NetworkInterface> enuIfs = null;
		try {
			enuIfs = NetworkInterface.getNetworkInterfaces();
		} catch (SocketException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}
		if (null != enuIfs) {
			while (enuIfs.hasMoreElements()) {
				System.out.println("INTERFECE FOUND");
				NetworkInterface ni = (NetworkInterface) enuIfs.nextElement();
				System.out.println("getDisplayName:\t" + ni.getDisplayName());
				System.out.println("getName:\t" + ni.getName());
				Enumeration<InetAddress> enuAddrs = ni.getInetAddresses();
				while (enuAddrs.hasMoreElements()) {
					InetAddress in4 = (InetAddress) enuAddrs.nextElement();
					System.out.println("getHostAddress:\t" + in4.getHostAddress());
				}
			}
		}
		FileInputStream h;

		System.out.println("-----------------------");
		// System.out.println(this.getClass().getResource(this.getClass().getName()).getPath());
		System.out.println("My IP is" + IPAddress.myIPAddress);
		System.out.println(StaticTest.uniqueID);
		// System.out.println(new File(".").getAbsoluteFile().getParent()+"なのだ");
		System.out.println("-----------------------");
		try {
			if (InetAddress.getByName("192.168.1.28").isReachable(2000)) {
				System.out.println("migrate now.");
				// this.migrate(Inet4Address.getByName("192.168.1.28"));
				// AgentAPI.migration(new KeyValuePair<InetAddress,
				// Integer>(Inet4Address.getByName("192.168.1.28"),55878), this);
				Thread.sleep(1000);
				System.out.println("hello world");
				System.out.println("ignoree");
			}
		} catch (UnknownHostException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		} catch (IOException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}

	}

	private static String getFullPath(Object inst) {
		return new File(inst.getClass().getResource(inst.getClass().getCanonicalName()).getPath()).getAbsolutePath();
	}

	@Override
	public void requestStop() {
		// TODO 自動生成されたメソッド・スタブ

	}

	private class supplier extends Thread {
		FileTask ft;

		public supplier(FileTask ft) {
			this.ft = ft;
		}

		@Override
		public void run() {
			System.out.println("10秒たいきなう");
			// ft.set(new SphereFileContentContainer(UUID.randomUUID()));
			System.out.println("かいたなう");
		}
	}
}

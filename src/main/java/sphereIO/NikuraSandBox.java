package sphereIO;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import net.arnx.jsonic.JSON;
import primula.api.core.resource.SystemConfigResource;

public class NikuraSandBox {
	final static String target = "share/charTest.dat";

	static int test() throws IOException {
		throw new IOException();
	}

	public static void main(String[] args) throws IOException {
		if (Files.notExists(Paths.get("/setting/SytemConfig.json"))) {
			JSON myJOSN = new JSON();
			myJOSN.setPrettyPrint(true);
			Map<String, Object> myMap = new HashMap<String, Object>();
			myMap.put(SystemConfigResource.DEFAULT_DEST, "");//AbstractAgent.migrete()のデフォルト転送先
			System.out.println(myJOSN.format(myMap));
		}

//		int count = 2;
//		try {
//		count=test();
//		}catch (IOException e) {
//
//		}
//		System.out.println(count);
		/*
		 * ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
		 * lock.writeLock().lock(); lock.writeLock().lock();
		 * System.out.println("台ジョーブ");
		 * System.out.println(lock.writeLock().getHoldCount());
		 *
		 * lock.writeLock().unlock(); lock.writeLock().unlock();
		 */

		/*
		 * SphereFileChannel fc=SphereFileChannel.open(new SphereFile(target),
		 * StandardOpenOption.READ); ByteBuffer buffer=ByteBuffer.allocate(1);
		 * fc.read(buffer); buffer.flip(); System.out.println(buffer.get());
		 */

		/*
		 * FileInputStream fin=new FileInputStream(target); FileChannel
		 * channel=fin.getChannel(); ByteBuffer buffer=ByteBuffer.allocate(1);
		 * channel.read(buffer); buffer.flip(); System.out.println(buffer.get());
		 * buffer.clear(); System.out.println("position:"+channel.position());
		 * channel.read(buffer, 2); buffer.flip(); System.out.println(buffer.get());
		 * buffer.clear(); System.out.println("position:"+channel.position());
		 * channel.read(buffer); buffer.flip(); System.out.println(buffer.get());
		 * buffer.clear(); System.out.println("position:"+channel.position());
		 */

		/*
		 * //記述子はポジション持ってる FileInputStream fin=new FileInputStream(target); FileChannel
		 * channel=fin.getChannel(); ByteBuffer buffer=ByteBuffer.allocate(1);
		 * channel.read(buffer); buffer.flip(); System.out.println(buffer.get());
		 * channel.position(0);
		 *
		 * FileInputStream fin2=new FileInputStream(fin.getFD()); FileChannel
		 * channel2=fin2.getChannel(); buffer.flip(); channel2.read(buffer);
		 * buffer.flip(); System.out.println(buffer.get()); buffer.flip();
		 * channel.read(buffer); buffer.flip(); System.out.println(buffer.get());
		 * System.out.println(channel.position()+" "+channel2.position());
		 */

		/*
		 * // 1byte FileOutputStream fos = new FileOutputStream("share/charTest.dat");
		 * byte[] arr = { 1, 2, 3 }; fos.write(arr); fos.close();
		 */

		/*
		 * //キャッシュされぬ FileInputStream fin=new FileInputStream(target); FileInputStream
		 * fin2=new FileInputStream(fin.getFD()); System.out.println(fin.read());
		 * System.out.println(fin2.read()); System.out.println(fin.read());
		 */

		/*
		 * //????キャッシュされる FileInputStream fin=new FileInputStream(target); FileReader
		 * fr=new FileReader(fin.getFD()); char[] buf = new char[1]; fr.read(buf);
		 * System.out.println(buf); FileReader fr2=new FileReader(fin.getFD());
		 * System.out.println(fr2.read()); fr.read(buf); System.out.println(buf);
		 * fr.close(); fr2.close(); fin.close();
		 */
		/*
		 * //3byte FileWriter fw=new FileWriter(target); fw.append('田'); fw.flush();
		 * fw.close();
		 */
	}
}

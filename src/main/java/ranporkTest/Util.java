package ranporkTest;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import sphereIO.SphereFile;
import sphereIO.SphereFileChannel;

public class Util {
	static int NUM = 1;

	public static void func() {
		if (NUM == 2) {

		}
		if (NUM == 1) {
			try {
				Files.deleteIfExists(Paths.get("share/ChannelTest2.dat"));
				SphereFileChannel fc = SphereFileChannel.open(new SphereFile("share/ChannelTest2.dat"),
						StandardOpenOption.WRITE, StandardOpenOption.CREATE);
				System.out.println("open");
				byte[] data = { 1, 2, 3, 4, 5, 6, 7, 8 };
				ByteBuffer databuf = ByteBuffer.wrap(data);
				System.out.println("write");
				System.out.println(fc.write(databuf));
				databuf.rewind();
				System.out.println(fc.write(databuf, 8192));

				fc.close();
				System.out.println("close");
			} catch (IOException e) {
				// TODO: handle exception
				e.printStackTrace();
			}
		} else if (NUM == 0) {
			try {
				System.out.println(new SphereFile("share/charTest.dat"));
				SphereFileChannel fc = null;
				fc = SphereFileChannel.open(new SphereFile("share/charTest.dat"), StandardOpenOption.READ);

				System.out.println("NiikuraTest:open");
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
		}
	}
}

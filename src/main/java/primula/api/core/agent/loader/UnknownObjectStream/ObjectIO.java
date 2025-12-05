package primula.api.core.agent.loader.UnknownObjectStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import primula.api.core.agent.loader.multiloader.ClassLoaderSelector;


public class ObjectIO extends Thread{

	private final static boolean debugflag = true;
	private final static String debugname   = "ObjectIO:";

	public ObjectIO(){};

	public void writeFile(String name,Object object) throws IOException{
		writeFile(new File(name),object);
	}

	public void writeFile(File file, Object object) throws IOException{
		FileOutputStream fos = new FileOutputStream(file);
		fos.write(getBinary(object));
	}

	public byte[] getBinary(String name) throws IOException{
		return getBinary(new File(name));
	}

	public byte[] getBinary(File file) throws IOException{
		byte[] binary = null;
		FileInputStream inStream = new FileInputStream(file);
		FileChannel channel = inStream.getChannel();
		ByteBuffer buffer = ByteBuffer.allocate((int)channel.size());
		channel.read(buffer);
		buffer.clear();
		binary = new byte[buffer.capacity()];
		buffer.get(binary);
		channel.close();
		return binary;
	}

	public byte[] getBinary(Object object, ClassLoaderSelector... selectors) throws IOException{
		byte[] binary = null;

			UnknownObjectOutputStream oos;
			ZipOutputStream zos;
			ByteArrayOutputStream baos;
			baos = new ByteArrayOutputStream();
			zos = new ZipOutputStream(baos);
			zos.setMethod(ZipOutputStream.DEFLATED);
			zos.setLevel(9);
			ZipEntry entry = new ZipEntry("BINARY");
			zos.putNextEntry(entry);
			oos = new UnknownObjectOutputStream(zos);
			for(ClassLoaderSelector selector:selectors){
				oos.pushPack(selector);
			}
			oos.uncheckWriteObject(object);
			oos.flush();
			zos.flush();
			zos.closeEntry();
			zos.close();

			binary = baos.toByteArray();
			baos.flush();
			baos.close();

//			check("getBinary-BYTESIZE:"+binary.length);
			//test();

			//for(int i=0; i<binary.length; i++) System.err.println(binary[i] + " 11111 ObjectIO#getBinary()$binary[" + i + "]"); // 送信するバイト配列の中身を確認する。debag用。
		return binary;
	}

	static void check(String message){
		if(debugflag)
			System.err.println(debugname+message);
	}

	static void test(){
		if(debugflag)
			System.err.println(debugname+currentThread().getStackTrace()[3].getClassName());//呼び出し元表示
	}

	public Object getObject(String filename) throws IOException, ClassNotFoundException{
		return getObject(new File(filename));
	}

	public Object getObject(File file) throws IOException, ClassNotFoundException{
		return getObject(getBinary(file));
	}

	public Object getObject(byte[] binary) throws IOException, ClassNotFoundException{
//		check("getBinary-BYTESIZE:"+binary.length);
		Object object = null;
		ByteArrayInputStream bis = new ByteArrayInputStream(binary);//①通信入手バイト配列をストリームにセット
		ZipInputStream zis = new ZipInputStream(bis);				//②ZIPストリームへセット
		zis.getNextEntry();											//③ZIPストリームから先頭ヘッダを入手
		UnknownObjectInputStream ois = new UnknownObjectInputStream(zis);//④OISへZIPストリームを連結
		try{
			object = ois.uncheckReadObject();
			return object;
		}finally{
			zis.closeEntry();
			zis.close();
			ois.close();
		}
	}
}
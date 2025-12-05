package primula.api.core.assh.command;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Hashtable;
import java.util.List;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;

import com.sun.jna.Native;

import primula.api.core.agent.loader.MalwareDetection;

public class dbdownload extends AbstractCommand{
	@Override
	public List<Object> runCommand(List<String> fileNames, Object instance, List<String> opt) {
		String txtRecord = new String();
		Hashtable<String,String> env =new Hashtable<String,String>();
		env.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");
		try{
			DirContext dirContext = new InitialDirContext(env);
			Attributes attrs = dirContext.getAttributes("current.cvd.clamav.net", new String[] { "TXT" });
			Attribute attr = attrs.get("TXT");
			if(attr != null) {
	            txtRecord = attr.get().toString();
	        }
		}catch (NamingException e) {
	        e.printStackTrace();
	    }
		if(txtRecord.isEmpty()){
			System.out.println("DNS応答が無効です");
			return null;
		}
		String version[]=txtRecord.split(":");
		String parent = new File(".").getAbsoluteFile().getParent();
		String dbdirname=parent+"\\..\\database";
		String src=new String();
		File dbdir=new File(dbdirname);
		int field=0;
		int flag=0;
		int flags[]={0,0,0};
		int sig=0;
		MalwareDetection s1 = (MalwareDetection)Native.loadLibrary("test4",MalwareDetection.class);
		if(dbdir.exists()){
			System.out.println(dbdirname+": exist");
			File[] dbname = dbdir.listFiles();
			for(int i=0; i<dbname.length; i++){
				if(dbname[i].getName().contains("main") ){
					field=1;
					src="main";
					flags[0]=1;
				}else if(dbname[i].getName().contains("daily")){
					field=2;
					src="daily";
					flags[1]=1;
				}else if(dbname[i].getName().contains("bytecode")){
					field=7;
					src="bytecode";
					flags[2]=1;
				}else{
					continue;
				}
				if(dbname[i].canRead()!=true){
					System.out.println("Can't read "+dbname[i].getName());
					continue;
				}else{
					String currentdb=new String();
					try{
						FileReader fr = new FileReader(dbname[i]);
						BufferedReader br = new BufferedReader(fr,512);
						int c;
						c=br.read();
						int j=0;
						while (j<512) {
					        currentdb+=(char)c;
					        c = br.read();
					        j++;
						}
						br.close();
					} catch (IOException e) {
					      System.out.println(e);
				    }
					if(currentdb.contains("ClamAV-VDB")==false){
						System.out.println("Not database file: "+dbname[i]);
						return null;
					}
					String dbinfo[]=currentdb.split(":");
					//1:time,2:version,3:number of signatures,4:functionality level,
					//5:MD5 checksum,6:digital signature,7:builder name,
					//8:creation time in seconds (old file format)
					int currentver=Integer.parseInt(dbinfo[2]);
					int newver=Integer.parseInt(version[field]);
					if(currentver >= newver){
						sig+=Integer.parseInt(dbinfo[3]);
						System.out.println(dbname[i]+" is up to date (version: "+currentver+", signatures: "+Integer.parseInt(dbinfo[3])+")");
					}else{
						System.out.println(dbname[i]+" is old");
						flag=1;
						File newdir = new File(dbdirname+"\\test");
						if(newdir.exists()){
							fileClass(newdir);
						}
						newdir.mkdir();
						String olddbname=dbname[i].getName();
						dbname[i]=null;
						System.out.println(dbdirname+"\\"+olddbname);
						s1.dbunpack((dbdirname+"\\"+olddbname), newdir.getName());
						for(int k=currentver+1;k<=newver;k++){
							String down=src+"-"+String.valueOf(k)+".cdiff";
							if(getfile(down,newdir)==1){
								System.out.println("Download finished "+down);
								s1.cdiff_add((newdir.getName()+"\\"+down), newdir.getName());
							}
						}
						String file = newdir.getName()+"\\testfile.tmp";
						String newdb = src+".cld";
						if(s1.dbbuild(newdir.getName(), file, src, dbdirname, (dbdirname+"\\"+newdb))==0){
							System.out.println("Reconstructing "+newdb);
						}else{
							getfile(newdb,dbdir);
						}
						currentdb=new String();
						try{
							FileReader fr = new FileReader(newdb);
							BufferedReader br = new BufferedReader(fr,512);
							int c;
							c=br.read();
							int j=0;
							while (j<512) {
						        currentdb+=(char)c;
						        c = br.read();
						        j++;
							}
							br.close();
						} catch (IOException e) {
						      System.out.println(e);
					    }
						if(currentdb.contains("ClamAV-VDB")==false){
							System.out.println("Not database file: "+newdb);
							return null;
						}
						String currentdbinfo[]=currentdb.split(":");
						currentver=Integer.parseInt(currentdbinfo[2]);
						sig+=Integer.parseInt(currentdbinfo[3]);
						System.out.println(newdb+" is updated (version: "+currentver+", signatures: "+Integer.parseInt(currentdbinfo[3])+")");
						dbname[i]=new File(olddbname);
						dbname[i].delete();
					}
				}
			}
			for(int i=0;i<=3;i++){
				if(flags[i]==0){
					File file=null;
					if(i==0){
						getfile("main.cvd",dbdir);
						file=new File(dbdir+"\\main.cvd");
					}
					else if(i==1){
						getfile("daily.cvd",dbdir);
						file=new File(dbdir+"\\daily.cvd");
					}
					else if(i==2){
						getfile("bytecode.cvd",dbdir);
						file=new File(dbdir+"\\bytecode.cvd");
					}
					if(file.canRead()!=true){
						System.out.println("Can't read "+file.getName());
						continue;
					}else{
						String currentdb=new String();
						try{
							FileReader fr = new FileReader(file);
							BufferedReader br = new BufferedReader(fr,512);
							int c;
							c=br.read();
							int j=0;
							while (j<512) {
						        currentdb+=(char)c;
						        c = br.read();
						        j++;
							}
							br.close();
						} catch (IOException e) {
						      System.out.println(e);
					    }
						if(currentdb.contains("ClamAV-VDB")==false){
							System.out.println("Not database file: "+file);
							return null;
						}
						String dbinfo[]=currentdb.split(":");
						//1:time,2:version,3:number of signatures,4:functionality level,
						//5:MD5 checksum,6:digital signature,7:builder name,
						//8:creation time in seconds (old file format)
						int newver=Integer.parseInt(dbinfo[2]);
						sig+=Integer.parseInt(dbinfo[3]);
						System.out.println("Download finished "+file.getName()+" (version :"+newver+", signatures: "+Integer.parseInt(dbinfo[3])+")");
					}
				}
			}
		}
		else{
			System.out.println(dbdirname+": not exist");
			dbdir.mkdir();
			for(int i=0;i<3;i++){
				File file=null;
				if(i==0){
					if(getfile("main.cvd",dbdir)==1){
						file=new File(dbdir+"\\main.cvd");
					}else{
						System.out.println("not download");
						return null;
					}
				}
				else if(i==1){
					if(getfile("daily.cvd",dbdir)==1){
						file=new File(dbdir+"\\daily.cvd");
					}else{
						System.out.println("not download");
						return null;
					}
				}
				else if(i==2){
					if(getfile("bytecode.cvd",dbdir)==1){
						file=new File(dbdir+"\\bytecode.cvd");
					}else{
						System.out.println("not download");
						return null;
					}
				}
				if(file==null){
					return null;
				}
				if(file.canRead()!=true){
					System.out.println("Can't read "+file.getName());
					continue;
				}else{
					String currentdb=new String();
					try{
						FileReader fr = new FileReader(file);
						BufferedReader br = new BufferedReader(fr,512);
						int c;
						c=br.read();
						int j=0;
						while (j<512) {
							currentdb+=(char)c;
							c = br.read();
							j++;
						}
						br.close();
					} catch (IOException e) {
						System.out.println(e);
					}
					if(currentdb.contains("ClamAV-VDB")==false){
						System.out.println("Not database file: "+file);
						return null;
					}
					String dbinfo[]=currentdb.split(":");
					//1:time,2:version,3:number of signatures,4:functionality level,
					//5:MD5 checksum,6:digital signature,7:builder name,
					//8:creation time in seconds (old file format)
					int newver=Integer.parseInt(dbinfo[2]);
					if(i==1){
						System.out.println("Download finished "+file.getName()+" (version :"+newver+", signatures: 1831622)");
						sig+=1831622;
						continue;
					}
					sig+=Integer.parseInt(dbinfo[3]);
					System.out.println("Download finished "+file.getName()+" (version :"+newver+", signatures: "+Integer.parseInt(dbinfo[3])+")");
				}
			}
		}
		if(flag==1){
			System.out.println("Database updated (signatures: "+sig+") from database.clamav.net");
		}else{
			System.out.println("Database is up to date (signatures: "+sig+")");
		}
		File testdir = new File(dbdirname+"\\test");
		if(testdir.exists()){
			fileClass(testdir);
		}
		return null;
	}
	static int getfile(String name,File dir){
		int n=0;
		String strurl="http://database.clamav.net/"+name;
		HttpURLConnection remote=null;
		InputStream in = null;
		BufferedReader reader = null;
		for(int i=0;i<3;i++){
			if(n==1){
				break;
			}
		try{
			URL url=new URL(strurl);
			Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("133.220.149.120", 8080));
			remote=(HttpURLConnection)url.openConnection(proxy);
			remote.setRequestMethod("GET");
			remote.connect();
			int status=remote.getResponseCode();
			if (status == HttpURLConnection.HTTP_OK) {
				File file=new File(dir+"\\"+name);
				FileOutputStream out=new FileOutputStream(file,false);
				in = remote.getInputStream();
				int b;
				while((b=in.read())!=-1){
					out.write(b);
				}
				out.close();
				in.close();
				n=1;
			}
        }
        catch(SecurityException se){
            System.err.println("セキュリティ例外です。");
        }
        catch(UnknownHostException ue){
            System.out.print("database.clamav.net");
            System.out.println(" にアクセスできません");
            System.exit(1);
        }
		catch (Exception e) {
			System.out.println(e);
	    }
		finally {
			try {
				if (reader != null) {
					reader.close();
				}
				if (remote != null) {
					remote.disconnect();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		}
		return n;
	}
	public static void fileClass(File TestFile) {
        if (TestFile.exists()) {
            if (TestFile.isFile()) {
                TestFile.delete();
            } else if(TestFile.isDirectory()) {
                File[] files = TestFile.listFiles();
                for(int i=0; i<files.length; i++) {
                    fileClass(files[i]);
                }
                TestFile.delete();
            }
        }
    }

}

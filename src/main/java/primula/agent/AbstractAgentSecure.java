package primula.agent;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Properties;

import jcifs.Config;
import jcifs.smb.ACE;
import jcifs.smb.SID;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;
import jcifs.smb.SmbFileInputStream;
import jcifs.smb.SmbFileOutputStream;
import primula.util.IPAddress;

/**
 * Security機能を拡張したAbstractAgent
 * @author takamura
 */

public abstract class AbstractAgentSecure extends AbstractAgent {

	/* JCIFS関連 */
	private JCIFSAPI jcifs = new JCIFSAPI();
	private boolean useLocalFile = true;

	/* 受け入れ制限に関するデータ */
	protected final int CONTINUE = 0;
	protected final int INTERRUPTION = 1;
	protected final int RONDOM_MIGRATE = 2;

	private final int sendPort = 55858;
	private final int receivePort = 55868;
	private int op = 2;
	private InetAddress address;
	//	static String username;
	//	static String password;
	private int write = 2;
	private int read = 1;

	private final String Attrifile = "Attributes.txt";
	private final String Info = "FileInfo.txt";

	InetAddress myIP;

	//	public static String myIPAddress = "133.220.114.240";

	public AbstractAgentSecure() {
		super();
		setMyIP();
		this.getCurrentPath();
		//	this.getCurrentPath();
	}

	protected final boolean migrate(final int op, final InetAddress address) {
		this.address = address;
		return this.migrate(op);
	}

	protected final boolean migrate(final int op) {
		//this.setOp(op);// TODO 何故かmigrationより後に実行されOPが正しく処理されない
		//		System.out.println("[DEBUG:AbstractAgentSecure] op = "+this.op);
		this.migrate();
		//	System.out.println("[DEBUG:AbstractAgentSecure] op = "+this.op);
		boolean result = (this.op == -1);
		this.setOp(op);
		//	System.out.println("[DEBUG:AbstractAgentSecure] op = "+this.op+", result = "+result);
		return result;
	}

	private void setOp(int op2) {
		// TODO 自動生成されたメソッド・スタブ
		op = op2;
	}

	/* JCIFS用メソッド */
	protected final void setUser(String username, String password) {
		jcifs.setUser(username, password);
	}

	protected final void setUseLocalFile(final boolean flag) {
		this.useLocalFile = flag;
	}

	protected final SmbFile getFile(final String pathname) {
		try {
			Config.setProperties(jcifs.prop);
			if (pathname.equals(Attrifile) || pathname.equals(Info)) {
				return new SmbFile(this.getCurrentPath() + pathname);
			}
			return new SmbFile(this.getCurrentPath() + pathname);
			//return new SmbFile("smb://133.220.114.244/Users/hasumi/Documents/AgentSphere/workspace/Primula_Eclipse/");
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		return null;
	}

	//ローカルアクセスかリモートアクセスか決定する
	protected final InputStream getInputStream(final String name) throws Exception {
		if (!(name.equals(Attrifile) || name.equals(Info)) && !isMyAgent())
			if (!CheckAttribute(name, read)){
				System.out.println("読み取り不可");
				try {
					this.migrate(1 , InetAddress.getByName(getmyIP().toString().split("/")[1]));
				} catch (UnknownHostException e1) {
					// TODO 自動生成された catch ブロック
					e1.printStackTrace();
				}
			}
		if (this.useLocalFile && this.isMyAgent()) {
			try {
				//				System.out.println("\n\nlocal access INPUT ");
				return new FileInputStream(name);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}
		else {
			try {
				//				System.out.println("\n\nremote access INPUT\n\n");
				//System.out.println("here:  " + this.getCurrentPath()+name);
				Config.setProperties(this.jcifs.prop);
				return new SmbFileInputStream(new SmbFile(this.getCurrentPath() + name));
			} catch (MalformedURLException e) {
				e.printStackTrace();
			} catch (UnknownHostException e) {
				e.printStackTrace();
			} catch (SmbException e) {
				e.printStackTrace();
			}
		}

		return null;
	}

	//ローカルアクセスかリモートアクセスか決定する
	protected final OutputStream getOutputStream(final String name) throws Exception {
		if (!(name.equals(Attrifile) || name.equals(Info))){
			setAttribute(name);
			if(isMyAgent())	setFileInfo(name);
			else
				if (CheckAttribute(name, write))
					;
		//		System.out.println("書き込み可能");
				else {
					System.out.println("書き込み不可");
					try {

						this.migrate(1 , InetAddress.getByName(getmyIP().toString().split("/")[1]));
					} catch (UnknownHostException e1) {
						// TODO 自動生成された catch ブロック
						e1.printStackTrace();
					}
				}
		}
		if (this.useLocalFile && this.isMyAgent()) { //ここいじった
			try {
				//				System.out.println("\n\nlocal access OUTPUT \n" + name + "\n");
				return new FileOutputStream(name);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}
		else {
			try {
				//				System.out.println("\n\nremote access OUTPUT \n\n");
				//				System.out.println("カレントパス　" + getCurrentPath());
				Config.setProperties(this.jcifs.prop);
				return new SmbFileOutputStream(this.getCurrentPath() + name);
			} catch (MalformedURLException e) {
				e.printStackTrace();
			} catch (UnknownHostException e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	/*
		protected final AgentFileStream createFileStream(final String pathname){
			return new AgentFileStream(this.jcifs, this.getCurrentPath(), pathname);
		}
	 */

	private final boolean isMyAgent() {
		try {
			return this.myIP.equals((InetAddress.getLocalHost()));
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		return false;

	}

	protected final String getCurrentPath() {

		Config.setProperties(this.jcifs.prop);

		if (this.useLocalFile && this.isMyAgent()) { //ここいじった
			File file = AccessController.doPrivileged(new PrivilegedAction<File>() { //特権ブロック
				@Override
				public File run() {
					return new File(".");
				}
			});
			this.jcifs.setAddress(IPAddress.myIPAddress);
			this.jcifs.setFilePath(file.getAbsoluteFile().getParent().substring(3));
		}

		/*
		this.jcifs.setAddress(myIPAddress);
		String path = new File(".").getAbsoluteFile().getParent().substring(3);
		jcifs.setFilePath(path);
		path = path.replace("\\", "/");
		 */
		//System.out.println("access to: smb://"+this.jcifs.address+"/"+this.jcifs.filePath +"/");
		//		System.out.println("smb://" + this.jcifs.address+"/"+this.jcifs.filePath +"/");
		//		if(name.equals(Attrifile) || name.equals(Info) || name.equals("dir") || name.equals("constructor")){return "smb://133.220.114.240/Users/selab/Desktop/w/Primula_Eclipse/";}
		return ("smb://" + this.jcifs.address + "/" + this.jcifs.filePath + "/");
	}

	/**
	 * JCIFSをエージェント上で透過的に使うためのクラス
	 * @author hasumi
	 */
	private final class JCIFSAPI implements Serializable {

		private Properties prop = new Properties();
		private String address = null;
		private String filePath = null;

		/* コンストラクタ */
		private JCIFSAPI() {
		}

		/* 設定用メソッド */
		/**
		 * CIFSサーバアクセスのためのユーザ名とパスワードをセットします
		 * @param username
		 * @param password
		 * @return
		 */
		private void setUser(final String username, final String password) {
			final class Action implements PrivilegedAction<Properties> {
				final String username;
				final String password;

				public Action(final String username, final String password) {
					this.username = username;
					this.password = password;
				}

				@Override
				public Properties run() {
					Properties prop = new Properties();
					prop.setProperty("jcifs.smb.client.username", username);
					prop.setProperty("jcifs.smb.client.password", password);
					return prop;
				}
			}
			prop = AccessController.doPrivileged(new Action(username, password));
		}

		/**
		 * CIFSサーバの所在アドレスをセットします
		 * @param address
		 */
		private void setAddress(String address) {
			if (this.address == null)
				this.address = address;
		}

		/**
		 * CIFSサーバ上のファイルパスをセットします
		 * @param filePath
		 */
		private void setFilePath(String filePath) {
			if (this.filePath == null)
				this.filePath = filePath.replace("\\", "/");
		}

		protected String getAddress() {
			return address;
		}
	}

	private void setMyIP() {
		if (myIP == null) {
			try {
				this.myIP = InetAddress.getLocalHost();
			} catch (UnknownHostException e) {
				e.printStackTrace();
			}
		}
	}

	protected void setFileInfo(String filename) throws Exception {
		try {

			String Info = "FileInfo.txt";
			String buf = "";
			SmbFile f = getFile(Info);
			boolean flag = false;
			//		BufferedWriter wr = new BufferedWriter(new OutputStreamWriter(new SmbFileOutputStream((getCurrentPath(Info) + Info))));
			BufferedWriter wr = new BufferedWriter(new OutputStreamWriter(this.getOutputStream((Info))));
			BufferedReader fin = new BufferedReader(new InputStreamReader(this.getInputStream(Info)));
			String info[];

			if (f.length() == 0 || !f.exists())
				buf += ("Attributes.txt," + getAgentID() + "," + getAgentSphereId() + "\r\n");
			//wr.write(Info + "," + getAgentID() + "," + getAgentSphereId());
			String str = fin.readLine();
			while (str != null) {
				String buftmp = str;
				info = buftmp.split(",");
				//System.out.println(info[0] + "   " +  filename);
				//				System.out.println(info[0]);
				//				System.out.println(filename);
				if (info[0].equals(filename)) {
					buf += (filename + "," + getAgentID() + "," + getAgentSphereId() + "\r\n");
					flag = true;
				}
				else {
					buf += (buftmp + "\n");
				}

				str = fin.readLine();
			}
			if (!flag)
				buf += (filename + "," + getAgentID() + "," + getAgentSphereId() + "\r\n");
			int o = 0;
			wr.write(buf);
			wr.flush();
			wr.close();
			fin.close();
		} catch (Exception e) {

		}
	}

	//@author takamura

	private boolean CheckAttribute(String name, int option) throws Exception {
		boolean flag = false;
		boolean checkFlag = false;
		String filename = name;
		String Attrifile = "Attributes.txt";
		//		System.out.println("Check Start ");
		String Attri[] = new String[3];
		String Strtmp[][] = new String[6][3];
		boolean fileexist = false;
		int i = 0, j = 0, k = 0;
		BufferedReader fin;
		SmbFile dir = new SmbFile(getCurrentPath());
		SmbFile[] files = dir.listFiles();
		boolean attri = false;
		for (int o = 0; o < files.length; o++) {
			SmbFile file = files[o];
			String tmp[] = file.toString().split("/", 0);
			if (tmp[8].equals(filename)) {
				fileexist = true;
				break;
			}
		}

		if (fileexist) {
			try {
				fin = new BufferedReader(new InputStreamReader(this.getInputStream(Attrifile)));
				String str = fin.readLine();
				while (str != null) {
					String tmp = str.toString();
					str = fin.readLine();
					if (checkFlag == false && (tmp.equals(filename))) {
						checkFlag = true;
					}
					else {
						if ((i++) == 7) {
							break;
						} //アクセス権を走査し終わる
						Attri = tmp.split(",", 0);//分類、read、writeで分割
						if (i == 1)
							continue; //ファイル名部分は飛ばす
						for (String a : Attri) {
							Strtmp[j][k++] = a;

						}
						j++;
						k = 0;
						//boolean bool = Boolean.valueOf( Attri[option] );
						//return bool;
						//System.out.println(bool);
						//ここで判断する　あとで実装 flag更新
						//					for(String str: Attri)
						//						System.out.print(str + " ");

					}
				}
				attri = check(name, Strtmp, option);
				fin.close();
			} catch (IOException e) {
				// TODO 自動生成された catch ブロック
				e.printStackTrace();
			}

		}
		//System.out.println("Check  end\n\n");

		return attri;
	}

	private void setAttribute(String filename) throws IOException { //ファイルがない時にAttribute値を設定する
		BufferedReader fin;
		boolean flag  = false;
		try {
			fin = new BufferedReader(new InputStreamReader(this.getInputStream(Attrifile)));
			String str = fin.readLine();
			while (str != null) {
				String tmp = str.toString();
				if(tmp.indexOf(filename) != -1){ flag = true; return ;}
				str = fin.readLine();
			}
			if(flag == true) return;
			SmbFileOutputStream fos = new SmbFileOutputStream(getCurrentPath() + Attrifile,true);
			BufferedWriter fout = new BufferedWriter(new OutputStreamWriter(fos));
			fout.write(filename);
			fout.newLine();
			String attri = ",true,false";
			String[] name = {"FileOwner,true,true","AgentOwner","AgentSphere","同じ作業をしてるgroup","信頼しているAgent","信頼しているグループ"};
			if(!(fout == null)){
				for(int i = 0; i < 6; i++){
				fout.write(name[i]);
				if(i == 0) {fout.newLine();   continue;}
				fout.write(attri);
				fout.newLine();
				}
			}
			fout.flush();
			fout.close();
			fos.close();
			fin.close();
		}
		catch (Exception e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();

		}
		return ;
	}

	private  Boolean check(String name, String strAttri[][], int option) throws Exception {

		boolean tmp;
		//		tmp = checkFileAuthor(name, option); System.out.println("  author  :  " + tmp);
		//		tmp = checkAgent(option);            System.out.println("  Agent   :  " + tmp);
		//		tmp = checkAgentID(name, option);    System.out.println("  AgentID :  " + tmp);
		if(!checkAgentID(name, option))
			return Boolean.valueOf(strAttri[2][option]);
		return true;

	}

	private boolean checkFileAuthor(String filename, int option) throws IOException { //ファイルの所有者
		SmbFile file = getFile(filename);
		return retrieveOwnerIdOfFile(file).equals(System.getProperty("user.name"));
	}

	private boolean checkAgentID(String name, int option) throws Exception { //AgentSphereIDによる判断

		BufferedReader fin = new BufferedReader(new InputStreamReader(this.getInputStream(Info)));
		String AgentSphereID[];
		int i = 0;
		String str = fin.readLine();
		while (str != null) {
			//	System.out.println(str);
			AgentSphereID = str.split(",");
			str = fin.readLine();
			if (!name.equals(AgentSphereID[0]))
				continue;
			else {
				//				System.out.println("getAgentID : " + getAgentSphereId());
				//				System.out.println("メモ帳     : " + AgentSphereID[2]);
				fin.close();
				return getAgentSphereId().equals(AgentSphereID[2]);
			}
		}
		fin.close();
		return false;

	}

	private boolean checkAgent(int option) throws UnknownHostException { //IPアドレスで移動したか判定する
		String ip[] = new String[2];

		InetAddress addr = InetAddress.getLocalHost();
		//		System.out.println("IP Address :  " + addr.getHostAddress());
		ip[0] = getmyIP().toString();
		ip = ip[0].split("/", 0);//分類、read、writeで分割
		//		System.out.println("IP Address :  " + ip[1]);
		return ip[1].equals(addr.getHostAddress());
		//isMyAgentを使う

	}

	private boolean checkGroup() {
		//SystemResource.javaにある
		// agentSphereId-> getAgentSphereId AgentSphere
		//
		//		System.out.println(getAgentSphereId());
		//		System.out.println(getAgentID());
		//		System.out.println(getAgentName());

		return true;
	}

	public String retrieveOwnerIdOfFile(SmbFile smbFile) throws IOException { //ファイルを作成したユーザ名を調べる。checkFileAuthorで使う

		String userName = "";
		SID sid = null;
		ACE[] acl = smbFile.getSecurity();

		for (int i = 0; i < acl.length; i++) {
			sid = acl[i].getSID();
			if (sid.getType() == 1 && sid.getTypeText().equalsIgnoreCase("user")
					&& sid.getAccountName() != null) {
				userName = sid.getAccountName();
				break;
			}
		}

		return userName;
	}

	@Override
	public abstract void requestStop();

}

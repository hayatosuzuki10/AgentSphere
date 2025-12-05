/**
 *
 */
package primula.api.core.agent.loader.multiloader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.Set;

import primula.api.core.agent.loader.multiloader.ChainContainer.MultiClassLoader;

/**
 * LocalFileClassLoaderは特定のディレクトリ以下のパスをパッケージとして、検索します。<br>
 * 通常のクラスローダ同様に検索委譲先として親クラスローダを設定する事が可能です。<br>
 * 指定するディレクトリは複数可能で、予めロードするクラスが必要とするパッケージが存在するトップディレクトリを指定する必要があります<br>
 * ディレクトリを指定しない場合にはカレントディレクトリが自動的に設定され、実行環境依存の為保証できかねます。
 *
 * @see ClassLoader
 * @see LocalFileClassLoader#LocalFileClassLoader(File[], ClassLoader)
 * @author Mr.RED
 */
public class LocalFileClassLoader extends ClassLoader implements SelectableClassLoader {

	private File[]						directorys;
	private HashMap<String, byte[]>	binarymap;
	//private String					keyClassName;
	private ClassLoaderSelector 		selector;
	private final int 				level;
	public static enum 				Type{LOCAL,REMOTE,UNKNOWN};
	public final Type 					type;

	private static boolean	debugFlag = false;

	/**
	 * デフォルトコンストラクタ。たぶん使わないよ。
	 * @see ClassLoader
	 * @see LocalFileClassLoader#LocalFileClassLoader(File[], ClassLoader)
	 */
	public LocalFileClassLoader() {
		this.directorys = null;
		this.level = 0;
		initNonFilePath();
		this.type = Type.UNKNOWN;
	}

	/**
	 * 親持ちコンストラクタ。たぶん使わないよ。
	 *
	 * @see ClassLoader
	 * @see LocalFileClassLoader#LocalFileClassLoader(File[], ClassLoader)
	 * @param parent
	 */
	public LocalFileClassLoader(ClassLoader parent) {
		super(parent);
		this.directorys = null;
		if(parent instanceof MultiClassLoader){
			this.level = ((MultiClassLoader)parent).getLevel()+1;
		}else if(parent instanceof LocalFileClassLoader){
			this.level = ((LocalFileClassLoader)parent).getLevel()+1;
		}else{
			this.level = 0;
		}
		initNonFilePath();
		this.type = Type.UNKNOWN;
	}

	/**
	 * @see ClassLoader
	 * @see LocalFileClassLoader#LocalFileClassLoader(File[], ClassLoader, ClassLoaderSelector)
	 * @param directory
	 * @throws IOException
	 */
	public LocalFileClassLoader(File directory) throws IOException {
		this(new File[]{directory}, null, null);
	}

	/**
	 * @see ClassLoader
	 * @see LocalFileClassLoader#LocalFileClassLoader(File[], ClassLoader, ClassLoaderSelector)
	 * @param directory
	 * @param parent
	 * @throws IOException
	 */
	public LocalFileClassLoader(File directory, ClassLoader parent) throws IOException {
		this(new File[]{directory}, parent, null);
	}

	/**
	 * @see ClassLoader
	 * @see LocalFileClassLoader#LocalFileClassLoader(File[], ClassLoader, ClassLoaderSelector)
	 * @param directorys
	 * @throws IOException
	 */
	public LocalFileClassLoader(File[] directorys) throws IOException {
		this(directorys, null, null);
	}


	/**
	 * @see LocalFileClassLoader#LocalFileClassLoader(File[], ClassLoader, ClassLoaderSelector)
	 * @param directorys
	 * @param parent
	 * @throws IOException
	 */
	public LocalFileClassLoader(File[] directorys, ClassLoader parent) throws IOException {
		this(directorys, parent, null);
	}

	/**
	 * ディレクトリ、委譲先ローダ、このローダに対応するセレクタを指定して初期化します。
	 *
	 * @see ClassLoader
	 * @param directorys
	 *            - 検索先としてのFile配列
	 * @param parent
	 *            - 検索委譲先としてのClassLoader
	 * @param selector - これをラップするクラスに対するセレクタ
	 * @throws IOException
	 *             指定したディレクトリが存在しない場合
	 */
	public LocalFileClassLoader(File[] directorys, ClassLoader parent, ClassLoaderSelector selector) throws IOException {
		super(parent);
		this.directorys = directorys;
		this.selector = selector;
		initFilePathCheck();
		if(parent instanceof MultiClassLoader){
			this.level = ((MultiClassLoader)parent).getLevel()+1;
		}else if(parent instanceof LocalFileClassLoader){
			this.level = ((LocalFileClassLoader)parent).getLevel()+1;
		}else{
			this.level = 0;
		}
		this.type = Type.LOCAL;
	}

	public LocalFileClassLoader(HashMap<String, byte[]> binarymap){
		this(binarymap, null, null);
	}

	public LocalFileClassLoader(HashMap<String, byte[]> binarymap, ClassLoader parent){
		this(binarymap, parent, null);
	}

	public LocalFileClassLoader(HashMap<String, byte[]> binarymap, ClassLoader parent, ClassLoaderSelector selector){
		super(parent);
		this.binarymap = binarymap;
		this.selector = selector;
		if(parent instanceof MultiClassLoader){
			this.level = ((MultiClassLoader)parent).getLevel()+1;
		}else if(parent instanceof LocalFileClassLoader){
			this.level = ((LocalFileClassLoader)parent).getLevel()+1;
		}else{
			this.level = 0;
		}
		this.type = Type.REMOTE;
	}

	/**
	 * パス無しコンストラクタで例外スローしたくないから用
	 */
	private void initNonFilePath() {
		try{
			initFilePathCheck();
		}catch(Exception e){
			e.printStackTrace();
		}
	}

	/**
	 * ファイルパスを再形成しつつ、そんなパス存在しなければ例外投げる用 ちゃんと動く。
	 *
	 * @throws FileNotFoundException
	 */
	private void initFilePathCheck() throws FileNotFoundException, FileNotFoundException, IOException {
		if(this.directorys == null){
			directorys = new File[]{getCurrentDirectory()};// カレントディレクトリを設定する
		}
		for(File directory : directorys){
			if(!directory.exists()) throw new FileNotFoundException("NO EXISTS");
			else if(!directory.isDirectory()) throw new FileNotFoundException("NOT PATH");
			try{
				directory = directory.getCanonicalFile();// パス終端が￥でない正規ファイルパスへ
			}catch(IOException e){
				e.printStackTrace();
			}
		}
		binarymap = new HashMap<String, byte[]>();
		searchFile(this.directorys);
	}

	/**
	 * カレントディレクトリを返すだけです。
	 *
	 * @return 実行時カレントディレクトリ
	 */
	private File getCurrentDirectory() {
		try{
			return new File(".").getCanonicalFile();
		}catch(IOException e){
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * クラス正規名とバイナリ表現のHashMapを返します。
	 *
	 * @return クラス正規名とバイナリ表現のHashMap
	 */
	@Override
	public HashMap<String, byte[]> getBinaryMap() {
		synchronized(binarymap){
			return new HashMap<String, byte[]>(binarymap);
		}
	}

	/**
	 * コードベースとしてFile配列を指定し、そのディレクトリ以下を正規クラス名として<b>全部読み込み</b>ます。
	 *
	 * @param codebases
	 */
	protected void searchFile(File[] codebases) throws FileNotFoundException, IOException {
		for(File directory : codebases){
			searchFile(directory, null);
		}
	}

	/**
	 * コードベース以下のディレクトリを再帰的に追っていき、.classを終端文字列に持つファイルを読み込みます。
	 *
	 * @param directory
	 * @param canonicalpath
	 */
	private void searchFile(File directory, String canonicalpath) throws FileNotFoundException, IOException {
		File[] directorys = directory.listFiles();
		for(File f : directorys){
			if(f.isDirectory()) searchFile(f, (canonicalpath == null || canonicalpath.equals("") ? f.getName() : canonicalpath + File.separator + f.getName()));
			else if(f.getName().endsWith(".class")){
				// 正規ファイル名の生成
				String canonicalname = ((canonicalpath == null ? "" : canonicalpath + File.separator) + f.getName()).replace(File.separatorChar, '.').replace(".class", "");
				// バイナリマップへ追加
				binarymap.put(canonicalname, readFile(f));
			}
		}
	}

	/**
	 * ファイル実体からバイナリ表現を取得します。
	 *
	 * @param canonicalfilepath
	 * @return
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	private byte[] readFile(File canonicalfilepath) throws FileNotFoundException, IOException {
		byte[] bytes = null;
		FileInputStream inStream = new FileInputStream(canonicalfilepath);
		FileChannel channel = inStream.getChannel();
		ByteBuffer buffer = ByteBuffer.allocate((int)channel.size());
		channel.read(buffer);
		buffer.clear();
		bytes = new byte[buffer.capacity()];
		buffer.get(bytes);
		channel.close();
		return bytes;
	}


//	public void testByteSize() {// TODO test_code
//
//		Set<String> set = binarymap.keySet();
//		String[] sta = (String[])set.toArray(new String[set.size()]);
//		int binarysize = 0;
//		for(String stmp : sta){
//			System.out.println(stmp);
//			binarysize += binarymap.get(stmp).length;
//		}
//		Fool.out.println("BinarySize:" + binarysize);
//
//	}

	@Override
	protected Class<?> findClass(String name) throws ClassNotFoundException {
		check("LF-findClass."+level+":" + name);//TODO
		try{
			return findSystemClass(name);
		}catch(Exception e){
			// 絶対にここに来る。なぜかfindSystemClassしないとダメっぽい。
			// e.printStackTrace();
			//Fool.out.println(name);
		}
		check("notsystems-LF-findClass."+level+":" + name);//TODO
		if(!binarymap.containsKey(name)) throw new ClassNotFoundException("NOT CONTAINS!");
		check("found-LF-findClass."+level+":" + name);//TODO
		return defineClass(name, binarymap.get(name), 0, binarymap.get(name).length);
	}

	@Override
	public int getLevel(){
		return level;
	}

	@Override
	public boolean isKnown(String classname) {
		Set<String> set = binarymap.keySet();
		String[] sta = (String[])set.toArray(new String[set.size()]);
		for(String binaryname : sta){
			if(classname.equals(binaryname)) return true;
		}
		return false;
	}


	@Override
	public ClassLoaderSelector getClassLoaderSelector() {
		return this.selector;
	}

	@Override
	public void setClassLoaderSelector(ClassLoaderSelector selector) {
		this.selector = selector;
	}

	private static void check(String message){
		if(debugFlag){
			String name = "LFC:";//this.getClass().getName();
			System.err.println(name+message);
		}else return;
	}
}

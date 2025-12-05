package primula.api.core.agent.loader.multiloader;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Set;


/**
 * MultiClassLoaderを扱う為のクラス。内部でLinkedListで保持し、その順序がMultiClassLoaderのレベルと対応付いている事が求められます。
 *
 * @author Mr.RED
 *
 *
*/
public class ChainContainer {

	private MultiClassLoader				current;
	private LinkedList<MultiClassLoader>	queue;

	/**
	 * デフォルトコンストラクタ。要素としてMCLを新規に空生成し、カレントローダとします。
	 */
	public ChainContainer() {
		queue = new LinkedList<MultiClassLoader>();
		current = new MultiClassLoader();
		queue.add(current);
	}

	/**
	 * 最上位のMCLを設定します。MCLのリストをブランチするように使う事ができます。
	 * @param currentParent	このクラスの持つトップ要素としてのMCL
	 */
	public ChainContainer(MultiClassLoader currentParent) {
		this.current = currentParent;
		queue = new LinkedList<MultiClassLoader>();
		queue.add(currentParent);
	}

	/**
	 * 現在のカレントMCLを上位にし、一段階押し上げます。MCLを階層的に生成するにあたって、
	 * 上位のMCLから順番に生成して、このメソッドを呼ぶたびに階層が上がっていく形になります。
	 */
	public void push() {
		synchronized(queue){
			MultiClassLoader tmp = new MultiClassLoader(current);
			queue.add(tmp);
			this.current = tmp;
		}
	}

	public MultiClassLoader getLast() {
		// return this.current;
		return queue.getLast();
	}

	public MultiClassLoader get(int index) {
		return queue.get(index);
	}

	public SelectableClassLoader get(ClassLoaderSelector selector){
//		SelectableClassLoader scl = null;
//		for(int i=0;i<queue.size();i++){
//			scl = queue.get(i).getSelectableClassLoader(selector);
//			if(scl!=null) return scl;
//		}
//		return null;
		for(MultiClassLoader mcl:queue){
			if(mcl.contains(selector)) return mcl.getSelectableClassLoader(selector);
		}
		return null;
	}

	public int size() {
		return queue.size();
	}

	public void resistNewClassLoader(ClassLoaderSelector selector, HashMap<String, byte[]> binarymap) {
		resistNewClassLoader(selector, binarymap, queue.size() - 1);
	}

	/**
	 * バイナリマップをリソースとして、指定したセレクタとレベルでクラスローダを生成して登録します。
	 *
	 * @param selector
	 * @param binarymap
	 * @param index
	 */
	public void resistNewClassLoader(ClassLoaderSelector selector, HashMap<String, byte[]> binarymap, int index) {
		//System.err.println("CreateRemote0:"+selector);//TODO
		MultiClassLoader mclparent = null;
		if(index > 0&& queue.size()>index ) mclparent = queue.get(index - 1);
		else if(index>0){
			int loop = index-queue.size()+1;
			for(int i=0;i<loop;i++){
				//System.err.println("Illegal push!");
				push();
			}
			if(index > 0&& queue.size()>index ) mclparent = queue.get(index - 1);
		}
		//queue.get(index).put(selector, new RemoteBinaryClassLoader(binarymap, mclparent, selector));// TODO 確認
		//System.err.println("CreateRemote:"+selector);//TODO
		queue.get(index).put(selector, new LocalFileClassLoader(binarymap, mclparent, selector));
	}

	public void resistNewClassLoader(ClassLoaderSelector selector, File codebase) throws IOException {
		resistNewClassLoader(selector, new File[]{codebase}, queue.size()-1);
	}

	public void resistNewClassLoader(ClassLoaderSelector selector, File codebase, int index) throws IOException {
		resistNewClassLoader(selector, new File[]{codebase}, index);
	}

	public void resistNewClassLoader(ClassLoaderSelector selector, File[] codebase) throws IOException {
		resistNewClassLoader(selector, codebase, queue.size() - 1);
	}

	/**
	 * ファイルディレクトリをコードベースとして、指定したセレクタとレベルで新しいクラスローダを生成して登録します。
	 *
	 * @param selector
	 * @param codebase
	 * @param index
	 * @throws IOException
	 */
	public void resistNewClassLoader(ClassLoaderSelector selector, File[] codebase, int index) throws IOException {
		//System.err.println("CC#create:"+selector);
		MultiClassLoader mclparent = null;
		if(index > 0) mclparent = queue.get(index - 1);
		queue.get(index).put(selector, new LocalFileClassLoader(codebase, mclparent, selector));
	}

	public SelectableClassLoader createNewClassLoader(ClassLoaderSelector selector, File[] codebase, int index) throws IOException {
		MultiClassLoader mclparent = null;
		if(index > 0) mclparent = queue.get(index - 1);
		return (SelectableClassLoader) new LocalFileClassLoader(codebase, mclparent, selector);
	}

	public SelectableClassLoader createNewClassLoader(ClassLoaderSelector selector, HashMap<String, byte[]> binarymap, int index) throws IOException {
		MultiClassLoader mclparent = null;
		if(index > 0) mclparent = queue.get(index - 1);
		return (SelectableClassLoader) new LocalFileClassLoader(binarymap, mclparent, selector);
	}

	public void putClassLoader(ClassLoaderSelector selector, SelectableClassLoader classloader, int index) {
		queue.get(index).put(selector, classloader);
	}

	/**
	 *
	 * <p>
	 * 内部でHashMapでクラスローダを要素として持つクラスです。キーとしてセレクタを用い、クラスローダの追加と取得を可能とします。
	 * このクラスローダでのloadClassではシステムローダへの検索委譲は行わず、要素ローダに対してクラス名を問い合わせてロードします。
	 * 要素ローダはその検索委譲先として、上位のMultiClassLoaderを個々に設定する必要があります。
	 * </p>
	 *
	 * @author Mr.RED
	 *
	 */
	public class MultiClassLoader extends ClassLoader {

		/** 委譲先の親ローダ。MultiClassLoaderで親を必要とするのは、子側からのSelectableClassLoaderの取得時。 */
		private MultiClassLoader parent;
		/** クラスローダの階層レベル。0が最上位。 */
		private final int level;
		/** ClassLoaderSelectorとSelectableClassLoaderのマップ */
		private HashMap<ClassLoaderSelector, SelectableClassLoader> classloaderMap;


		/**
		 * デフォルトコンストラクタ。最上位のクラスローダが用いる。
		 */
		public MultiClassLoader() {
			this.parent = null;
			this.classloaderMap = new HashMap<ClassLoaderSelector, SelectableClassLoader>();
			this.level = 0;
		}

		/**
		 * 親ローダを指定するコンストラクタ。
		 *
		 * @param parent 検索委譲先としての親ローダを指定
		 */
		public MultiClassLoader(MultiClassLoader parent) {
			this.parent = parent;
			this.classloaderMap = new HashMap<ClassLoaderSelector, SelectableClassLoader>();
			this.level = parent.level+1;
		}

		/**
		 * 要素ローダを追加する。
		 *
		 * @param selector	要素ローダと関連するセレクタ
		 * @param classloader	追加したい要素ローダ
		 * @return	置き換え前の要素ローダ
		 */
		public SelectableClassLoader put(ClassLoaderSelector selector, SelectableClassLoader classloader){
			return this.classloaderMap.put(selector, classloader);
		}

		/**
		 * @param selector	要素ローダと関連するセレクタ
		 * @return	削除した要素ローダ
		 */
		public SelectableClassLoader remove(ClassLoaderSelector selector){
			return this.classloaderMap.remove(selector);
		}

		/* (非 Javadoc)
		 * MCLのloadClassではシステムに対する委譲は行わない
		 *
		 * @see java.lang.ClassLoader#loadClass(java.lang.String, boolean)
		 */
		@Override
		protected synchronized Class<?> loadClass(String name, boolean resolve)
		throws ClassNotFoundException {
			//System.err.println("MCL-loadClass:"+name);
			Class<?> c=null;
			try {
				/* 検索委譲先がある場合、まずはそちらへ投げる。なければ自身の要素から検索。 */
				if (parent != null) {
					c = parent.loadClass(name);
				} else {
					c = findClassLoaderMap(name);
				}
			/* 検索委譲先で見つからなかった場合に、自身の要素から検索。 */
			} catch (ClassNotFoundException e) {
				c = findClassLoaderMap(name);
			}
			if (c == null) {
				/* 自身の要素から見つからなかった場合例外を投げる */
				throw new ClassNotFoundException("UNKNOWN CLASS");
			}
			if (resolve) {
				resolveClass(c);
			}
			return c;
		}

		/**
		 * 引数で渡されたセレクタに対応する要素ローダを返す。
		 *
		 * @param selector	要素ローダに対応するセレクタ
		 * @return	クラスローダ
		 */
		public ClassLoader getClassLoader(ClassLoaderSelector selector){
			return (ClassLoader)getSelectableClassLoader(selector);
		}

		/**
		 * 引数で渡されたセレクタに対応する要素ローダを返す。
		 *
		 * @param selector	要素ローダに対応するセレクタ
		 * @return	クラスローダ
		 */
		public SelectableClassLoader getSelectableClassLoader(ClassLoaderSelector selector){
			SelectableClassLoader scl=null;

			if(parent != null){
				scl = parent.getSelectableClassLoader(selector);
			}
			if(scl == null){
				scl = classloaderMap.get(selector);
			}
			return scl;
		}

		/**
		 * クラス名を指定して、その要素を持つ要素クラスローダを返します。
		 *
		 * @param classname	正規クラス名
		 * @return クラスローダ
		 */
		@Deprecated
		public ClassLoader getClassLoader(String classname){
			return (ClassLoader)getSelectableClassLoader(classname);
		}

		/**
		 * クラス名を指定して、その要素を持つ要素クラスローダを返します。
		 *
		 * @param classname	正規クラス名
		 * @return クラスローダ
		 */
		@Deprecated
		public SelectableClassLoader getSelectableClassLoader(String classname){
			if(parent != null){
				return parent.getSelectableClassLoader(classname);
			}else{
				Set<ClassLoaderSelector> set = classloaderMap.keySet();
				Object[] keys = set.toArray();
				for(Object selector :keys){
					if(classloaderMap.get((ClassLoaderSelector)selector).isKnown(classname)){
						return classloaderMap.get((ClassLoaderSelector)selector);
					}
				}
			}
			return null;
		}

		public boolean contains(ClassLoaderSelector selector){
			Set<ClassLoaderSelector> selectset = getClassLoaderSelectors();
			return selectset.contains(selector);
		}

		/**
		 * このローダに追加されている要素に対する、キーセットを返します。
		 *
		 * @return	このローダの持つ要素のキーセット
		 */
		public Set<ClassLoaderSelector> getClassLoaderSelectors(){
			return classloaderMap.keySet();
		}

		/**
		 * 階層レベルを返す。
		 *
		 * @return	このローダの階層レベル
		 */
		public int getLevel(){
			return this.level;
		}

		/**
		 * 正規クラス名を指定してクラスを返します。
		 *
		 * @param classname	正規クラス名
		 * @return	このローダが保持している指定されたクラス。該当するクラス名が存在しない場合はnull
		 * @throws ClassNotFoundException	指定クラスの要素を保持していなかった場合
		 */
		protected Class<?> findClassLoaderMap(String classname) throws ClassNotFoundException{
			//System.err.println("MCL-findClassLoaderMap:"+classname);
			Set<ClassLoaderSelector> set = classloaderMap.keySet();
			Object[] keys = set.toArray();
			for(Object selector :keys){
				if(classloaderMap.get((ClassLoaderSelector)selector).isKnown(classname)){
					return classloaderMap.get((ClassLoaderSelector)selector).loadClass(classname);
				}
			}
			//throw new ClassNotFoundException("UNKNOWN CLASS");
			return null;
		}
	}
}

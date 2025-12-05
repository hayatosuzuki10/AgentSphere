package primula.api.core.agent.loader.multiloader;

/**
 *  実用性の為のフロントエンドローダ。
 *　
 * @author Mr.RED
 *
 */
public class GhostClassLoader extends ClassLoader {

	public static GhostClassLoader	unique;
	private ChainContainer			cc;

	static{
		synchronized(GhostClassLoader.class){
			unique = new GhostClassLoader();
		}
	}

	private GhostClassLoader() {
		cc = new ChainContainer();
	}

	public ChainContainer getChainContainer() {
		return cc;
	}

	public SelectableClassLoader getSelectableClassLoader(ClassLoaderSelector selector){
		return cc.get(selector);
	}

	@Override
	protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
		System.err.println("GCL:" + name);
		// First, check if the class has already been loaded
		Class<?> c = findLoadedClass(name);
		//if(c == null) c = findSystemClass(name);
		//Class<?> c = super.loadClass(name, resolve);

		if(c == null){
			try{
				if(cc.size() != 0){
					c = cc.getLast().loadClass(name, false);
				}else{
					c = super.loadClass(name);
				}
			}catch(ClassNotFoundException e){
				// ClassNotFoundException thrown if class not found
				// from the non-null parent class loader
				//throw e;
				c = findSystemClass(name);
			}
			if(c == null){
				// If still not found, then invoke findClass in order
				// to find the class.

				throw new ClassNotFoundException("No Files");
				// c = findClass(name);
			}
		}
		if(resolve){
			resolveClass(c);
		}
		return c;
	}
}

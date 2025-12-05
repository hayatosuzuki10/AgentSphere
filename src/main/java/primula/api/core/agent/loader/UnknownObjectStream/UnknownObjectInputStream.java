/**
 *
 */
package primula.api.core.agent.loader.UnknownObjectStream;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.util.ArrayList;

import primula.api.core.agent.loader.multiloader.ChainContainer;
import primula.api.core.agent.loader.multiloader.GhostClassLoader;

/**
 * @author moyomoto
 *
 */
public class UnknownObjectInputStream extends ObjectInputStream {

	private enum Mode{LIST,ATOMIC};
	private Mode mode = Mode.ATOMIC;
	private static boolean	debugflag = false;
	private ArrayList<ClassLoaderPack> packlist = null;
//	static int count = 0;//////////////////////////////////////// debag用。

	//private GhostClassLoader classLoader;

	public UnknownObjectInputStream() throws IOException, SecurityException {
		super();
		// TODO 自動生成されたコンストラクター・スタブ
	}

	public UnknownObjectInputStream(InputStream in) throws IOException {
		super(in);
		// TODO 自動生成されたコンストラクター・スタブ
	}

//	public UnknownObjectInputStream(InputStream in, GhostClassLoader classloader) throws IOException {
//		super(in);
//		this.classLoader = classloader;
//	}

	public Object uncheckReadObject() throws IOException, ClassNotFoundException{
		Object obj = readObject();
//		System.err.println(obj + " 11111 UnknownObjectInputStream#uncheckReadObject()$obj"); // objの中身を表示。debag用。
		if(obj instanceof ClassLoaderPack){
			ClassLoaderPack pack = (ClassLoaderPack)obj;
			packLoad(pack);
		}else if(obj instanceof ArrayList){
			@SuppressWarnings("unchecked")
			ArrayList<ClassLoaderPack> packlist = (ArrayList<ClassLoaderPack>)obj;
			this.packlist = packlist;
			for(ClassLoaderPack pack : packlist){
				packLoad(pack);
			}
			mode = Mode.LIST;
		}else{
			mode = Mode.ATOMIC;
			this.packlist = null;
			return obj;
		}
		return uncheckReadObject();
	}

	private void packLoad(ClassLoaderPack pack){
		ChainContainer cc = GhostClassLoader.unique.getChainContainer();
		check("pre-unpack:"+pack.getSelector());//TODO
		if(cc.get(pack.getSelector()) != null) return;//未知データならロード
		cc.resistNewClassLoader(pack.getSelector(), pack.getBinaryMap(), pack.getLevel());
	}

	@Override
	protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
		check("resolveClass:"+desc.getName());//TODO
		try {
//			System.err.println(desc + " 22222 UnknownObjectInputStream#resolveClass()$desc ***** resolveOK?" + count++); // 解決するクラスを表示。debag用。
			return super.resolveClass(desc);
		}catch (ClassNotFoundException e) {
//			System.err.println(desc + " 33333 UnknownObjectInputStream#resolveClass()$desc ***** catch unknown."); // 解決しようとしたクラスが未知クラスだった場合。debag用。
			String name = desc.getName();
			try{
				if(mode==Mode.ATOMIC){
					return GhostClassLoader.unique.loadClass(name);
				}
				else if(mode == Mode.LIST){
//					System.err.println(packlist + " 44444 UnknownObjectInputStream#resolveClass()$packlist ***** mode is LIST"); // packlistの中身を表示。debag用。
					check("LIST");
					for(ClassLoaderPack pack : packlist){
						byte[] binary = pack.getBinaryMap().get(name);
						if(binary != null){
							ChainContainer cc = GhostClassLoader.unique.getChainContainer();
							return cc.get(pack.getLevel()).loadClass(name);
						}
					}
				}
			}catch (Exception e2) {
//				check("resolveClass-deep:"+desc.getName());//TODO
//				Object obj = readObject();
//				if(obj instanceof ClassLoaderPack){
//					this.mode = Mode.ATOMIC;
//					ClassLoaderPack pack = (ClassLoaderPack)obj;
//					ChainContainer cc = GhostClassLoader.unique.getChainContainer();
//					check("unpack:"+pack.getSelector());//TODO
//					cc.createNewClassLoader(pack.getSelector(), pack.getBinaryMap(), pack.getLevel());
//					return cc.get(pack.getLevel()).loadClass(name);
//				}else if(obj instanceof ArrayList){
//					@SuppressWarnings("unchecked")
//					ArrayList<ClassLoaderPack> packlist = (ArrayList<ClassLoaderPack>)obj;
//					this.mode = Mode.LIST;
//					this.packlist = packlist;
//					ChainContainer cc = GhostClassLoader.unique.getChainContainer();
//					for(ClassLoaderPack pack : packlist){
//						check("unpack:"+pack.getSelector());//TODO
//						cc.createNewClassLoader(pack.getSelector(), pack.getBinaryMap(), pack.getLevel());
//					}
//					for(ClassLoaderPack pack : packlist){
//						byte[] binary = pack.getBinaryMap().get(name);
//						if(binary != null){
//							return cc.get(pack.getLevel()).loadClass(name);
//						}
//					}
//				}
				e2.printStackTrace();
			}
		}
		throw new ClassNotFoundException("UNKNOWN CLASS");
	}

	private static void check(String message){
		if(!debugflag) return;
		String name = "UOIS:";
		System.err.println(name+message);
	}
}
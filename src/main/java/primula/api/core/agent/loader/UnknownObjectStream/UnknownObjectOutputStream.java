/**
 *
 */
package primula.api.core.agent.loader.UnknownObjectStream;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;

import primula.api.core.agent.loader.multiloader.ClassLoaderSelector;
import primula.api.core.agent.loader.multiloader.GhostClassLoader;
import primula.api.core.agent.loader.multiloader.SelectableClassLoader;
/**
 * @author moyomoto
 *
 */
public class UnknownObjectOutputStream extends ObjectOutputStream {

	ArrayList<ClassLoaderSelector> selectorlog;
	ArrayList<ClassLoaderPack> packlist = null;

	/**
	 * @throws IOException
	 * @throws SecurityException
	 */
	public UnknownObjectOutputStream() throws IOException, SecurityException {
		init();
	}

	/**
	 * @param out
	 * @throws IOException
	 */
	public UnknownObjectOutputStream(OutputStream out) throws IOException {
		super(out);
		init();
	}

	private void init(){
		selectorlog = new ArrayList<ClassLoaderSelector>();
	}

	public void pushPack(ClassLoaderSelector selector) throws IOException{
		SelectableClassLoader scl = GhostClassLoader.unique.getChainContainer().get(selector);
//		ClassLoaderPack pack = new ClassLoaderPack(selector, scl.getBinaryMap(), scl.getLevel());
		ClassLoaderPack pack = new ClassLoaderPack(scl);
		writeObject(pack);
		if(!selectorlog.contains(selector)){
			selectorlog.add(selector);
		}
		System.err.println(selectorlog + " 11111 UnknownObjectOutputStream#pushPack");///////////////////////////
	}

	public void uncheckWriteObject(Object object) throws IOException{
		packlist = null;
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		UnChecker un = new UnChecker(baos,selectorlog);
		packlist = un.getPackList();
		un.writeObject(object);
		writeObject(packlist);
		writeObject(object);
		packlist = null;
	}

//	@Override
//	protected void annotateClass(Class<?> cl) throws IOException {
//		//super.annotateClass(cl);
//		SelectableClassLoader scl=null;
//		System.err.println("Annotate:"+cl.getCanonicalName());
//		if(cl.getClassLoader() instanceof SelectableClassLoader){
//			scl = (SelectableClassLoader)cl.getClassLoader();
//		}else{
//			return;
//		}
//
//		/*
//		ClassLoaderSelector selector = scl.getClassLoaderSelector();
//		String classname = cl.getCanonicalName();
//		byte[] binary = scl.getBinaryMap().get(classname);
//		writeObject(new DataPack(selector, classname, binary));
//		System.err.println("UOOS:"+cl.getCanonicalName());
//		*/
//
//		for(Field cls : cl.getDeclaredFields()){
//			System.err.println("contains:"+cls.getType());
//		}
//
//
//		ClassLoaderSelector selector = scl.getClassLoaderSelector();
//
//		if(!selectorlog.contains(selector)){
//			HashMap<String, byte[]> binarymap = scl.getBinaryMap();
//			ClassLoaderPack pack = new ClassLoaderPack(selector, binarymap, scl.getLevel());
//			writeObject(pack);
//			if(packlist != null) packlist.add(pack);
//			selectorlog.add(selector);
//			System.err.println("UOOS:"+selector+" is index of "+selectorlog.indexOf(selector));
//			System.err.println("UOOS:Level is "+scl.getLevel());
//		}
//	}

	private class UnChecker extends ObjectOutputStream{

		private ArrayList<ClassLoaderPack> packlist;
		private ArrayList<ClassLoaderSelector> selectorlog;

//		protected UnChecker(OutputStream out) throws IOException{
//			super(out);
//			this.packlist = new ArrayList<ClassLoaderPack>();
//			this.selectorlog = new ArrayList<ClassLoaderSelector>();
//		}

		protected UnChecker(OutputStream out,ArrayList<ClassLoaderSelector> selectorlog) throws IOException{
			super(out);
			this.packlist = new ArrayList<ClassLoaderPack>();
			this.selectorlog = selectorlog;
		}

		public ArrayList<ClassLoaderPack> getPackList(){
			return this.packlist;
		}

		@Override
		protected void annotateClass(Class<?> cl) throws IOException {
			SelectableClassLoader scl=null;
//			System.err.println("Annotate:"+cl.getCanonicalName());
//			System.err.println(cl.getClassLoader() + " 11111 UnknownObjectOutputStream#annotateClass()$cl.getClassLoader() ***** " + cl); // 送信するクラスのクラスローダーを表示。debag用。
			if(cl.getClassLoader() instanceof SelectableClassLoader){ // 送信するクラスのクラスローダーがSelectableClassLoaderだった場合。
				scl = (SelectableClassLoader)cl.getClassLoader();
				//�X�V���Ă݂�H
				scl = GhostClassLoader.unique.getSelectableClassLoader(scl.getClassLoaderSelector());
			}else{ // その他のクラスローダー（SystemClassLoaderなど)の場合。
				return;
			}
			ClassLoaderSelector selector = scl.getClassLoaderSelector();

			if(!selectorlog.contains(selector)){
				HashMap<String, byte[]> binarymap = scl.getBinaryMap();
				ClassLoaderPack pack = new ClassLoaderPack(selector, binarymap, scl.getLevel());
				if(packlist != null) packlist.add(pack);
				selectorlog.add(selector);
				//System.err.println("UOOS:"+selector+" is index of "+selectorlog.indexOf(selector));
				//System.err.println("UOOS:Level is "+scl.getLevel());
			}
		}

	}
}

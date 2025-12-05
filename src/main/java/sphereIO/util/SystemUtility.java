package sphereIO.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;

import org.seasar.framework.container.factory.SingletonS2ContainerFactory;

import sphereIO.SphereFile;
import sphereIO.system.SphereFileSystem;

public class SystemUtility {
	private SystemUtility() {

	}
	public static BasicFileAttributes getFileAttribute(String path) throws IOException {
		return Files.getFileAttributeView(Paths.get(path), BasicFileAttributeView.class).readAttributes();
	}

	/**
	 * 与えられた文字列をパスとしてそのパスが指すファイルを表すFileKeyを取得します
	 * @see BasicFileAttributes#fileKey()
	 * @param path
	 * @return Objact
	 * @throws IOException
	 */
	public static Object getFileKey(String path) throws IOException {
		return getFileAttribute(path);
	}

	/**
	 * 与えられたパスからIPを取得してSphereFileとして返します
	 * @param Path
	 * @return
	 */
	public SphereFile resolvePath(String Path) {
		//TODO ここの実装はガバになる可能性がなくもない
		//もしもの場合はFileSystemのinitを確認するlistener必要かもしれない
		if (((SphereFileSystem) SingletonS2ContainerFactory.getContainer()
				.getComponent("SphereFileSystem")).getDht().contains(Path)) {
			return new SphereFile(Path);
		}
		return null;
	}
}

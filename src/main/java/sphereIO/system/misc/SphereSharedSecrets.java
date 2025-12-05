package sphereIO.system.misc;

import sphereIO.SphereFileDescriptor;

/**
 * パッケージプライベートとなっているクラスのアクセッサを提供するクラスです
 * <p>
 * リフレクション使って無理やりやるのと違って
 * 対象クラスの存在をコンパイル時にチェックできて幸せらしい
 * <p>
 * プライベートになってるところを無理やり変えられるパワーなので、ファイルシステム開発者以外は使わないこと
 * @author Norito
 *
 */
public class SphereSharedSecrets {
	//private static final Unsafe unsafe = Unsafe.getUnsafe();
	public static SphereFileDescriptorAccess sphereFileDescriptorAccess;
	public static void setSphereFileDescriptorAccess(SphereFileDescriptorAccess sphereFileDescriptorAccess) {
		SphereSharedSecrets.sphereFileDescriptorAccess = sphereFileDescriptorAccess;
	}
	public static SphereFileDescriptorAccess getSphereFileDescriptorAccess() {
		if (sphereFileDescriptorAccess == null) {
			new SphereFileDescriptor();
		}
		return sphereFileDescriptorAccess;
	}
}

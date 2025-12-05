package sphereIO;

import java.io.IOException;
import java.io.Serializable;
import java.io.SyncFailedException;
import java.util.UUID;

import org.seasar.framework.container.factory.SingletonS2ContainerFactory;

import sphereIO.system.SphereFileSystem;
import sphereIO.system.misc.SphereFileDescriptorAccess;
import sphereIO.system.misc.SphereSharedSecrets;

public class SphereFileDescriptor implements Serializable {
	private static SphereFileSystem sfs = (SphereFileSystem) SingletonS2ContainerFactory.getContainer()
			.getComponent("SphereFileSystem");

	private SphereFile path;
	private UUID id;
	private boolean closed; //このディスクリプタが閉じたかどうか
	private boolean append;
	private boolean deleteOnClose;
	private boolean read;
	private boolean write;

	/*
	 * sharedsecretsにアクセッサ登録
	 */
	static {
		SphereSharedSecrets.setSphereFileDescriptorAccess(new SphereFileDescriptorAccess() {

			@Override
			public void setPath(SphereFileDescriptor sfd, SphereFile path) {
				sfd.path = path;
			}

			@Override
			public void setAppend(SphereFileDescriptor sfd, boolean append) {
				sfd.append = append;
			}

			@Override
			public SphereFile getPath(SphereFileDescriptor sfd) {
				return sfd.path;
			}

			@Override
			public boolean getAppend(SphereFileDescriptor sfd) {
				return sfd.append;
			}

			@Override
			public void close(SphereFileDescriptor sfd) throws IOException {
				sfs.fileClose(sfd);
			}

			@Override
			public void setId(SphereFileDescriptor sfd, UUID id) {
				sfd.id = id;
			}

			@Override
			public UUID getId(SphereFileDescriptor sfd) {
				return sfd.id;
			}

			@Override
			public void setDeleteOnClose(SphereFileDescriptor sfd, boolean flag) {
				sfd.deleteOnClose = flag;
			}

			@Override
			public boolean isDeleteOnClose(SphereFileDescriptor sfd) {
				return sfd.deleteOnClose;
			}

			@Override
			public void setWrite(SphereFileDescriptor sfd, boolean flag) {
				sfd.write = flag;
			}

			@Override
			public boolean isWrite(SphereFileDescriptor sfd) {
				return sfd.write;
			}

			@Override
			public void setRead(SphereFileDescriptor sfd, boolean flag) {
				sfd.read = flag;
			}

			@Override
			public boolean isRead(SphereFileDescriptor sfd) {
				return sfd.read;
			}
		});
	}

	/**
	 * 無効な記述子を作成します
	 */
	public SphereFileDescriptor() {
	}

	void sync() throws SyncFailedException {
		try {
			sfs.fileClose(this);
		} catch (IOException e) {
			// TODO 自動生成された catch ブロック
			throw new SyncFailedException(e.getMessage());
		}
	}

	boolean valid() {
		return !closed;
	}

	/**
	 * 格納しているUUIDを比較してどちらも同一ならtrueです
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof SphereFileDescriptor) {
			SphereFileDescriptor tmp = (SphereFileDescriptor) obj;
			return this.id.equals(tmp.id);
		}
		return false;
	}
	/**
	 * てきとう
	 * <p>
	 * UUIDがユニークになるだろうしhashcodeとしての最低限は何とかなってるはず
	 * <p>
	 * 中のパスも見ないといけないきがする
	 */
	@Override
	public int hashCode() {
		// TODO 自動生成されたメソッド・スタブ
		return this.id.hashCode();
	}

	public boolean isClosed() {
		return closed;
	}
}

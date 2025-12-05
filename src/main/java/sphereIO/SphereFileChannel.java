package sphereIO;

import java.io.IOException;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.ScatteringByteChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.channels.spi.AbstractInterruptibleChannel;
import java.nio.file.OpenOption;

import org.seasar.framework.container.factory.SingletonS2ContainerFactory;

import sphereIO.system.FileContent;
import sphereIO.system.IOUtil;
import sphereIO.system.SphereFileSystem;

/**
 * FileChannelもどきです。
 * <p>
 * ふるまいとしては現地でオープンしているFileChannelのコントローラーです<br>
 * Lockなんて恐ろしいしくみはAgentに使わせられないので、FileChannelはextendsしてない
 * <p>
 * よくわからない人はFileChannelについて履修するのだ
 * <p>
 * かなしいことにbytebufferの配列にまとめて出し入れする奴はまだ最適化されてないのだ
 * ゆるして
 * @author RanPork
 *
 */
public class SphereFileChannel extends AbstractInterruptibleChannel
		implements SeekableByteChannel, GatheringByteChannel, ScatteringByteChannel, Serializable, FileContent {

	SphereFileDescriptor fd;
	// SphereFileCache cache;
	boolean readable;
	boolean writable;

	public static SphereFileChannel open(SphereFile f, OpenOption... opt) throws IOException {
		SphereFileSystem sfs = (SphereFileSystem) SingletonS2ContainerFactory.getContainer()
				.getComponent("SphereFileSystem");
		return sfs.fileOpen(f, opt);
	}

	public SphereFileChannel(SphereFileDescriptor fd, boolean readable, boolean writable) {
		if (fd == null) {
			throw new NullPointerException();
		}
		this.fd = fd;
		this.readable = readable;
		this.writable = writable;
	}

	@Override
	protected void implCloseChannel() throws IOException {
		SphereFileSystem sfs = (SphereFileSystem) SingletonS2ContainerFactory.getContainer()
				.getComponent("SphereFileSystem");
		sfs.fileClose(fd);
	}

	@Override
	public int read(ByteBuffer dst) throws IOException {
		if (!readable) {
			throw new UnsupportedOperationException("読み込み専用");
		}
		begin();
		int count = -2;
		try {
			count = IOUtil.fetch(fd, dst);
			return count;
		} finally {
			end(count > -2);
		}
	}

	@Override
	public int write(ByteBuffer src) throws IOException {
		begin();
		int count=-1;
		try {
			count=IOUtil.push(fd, src);
			return count;
		}finally {
			end(count>-1);
		}
	}

	@Override
	public long read(ByteBuffer[] dsts, int offset, int length) throws IOException {
		// TODO 自動生成されたメソッド・スタブ
		if (offset + length > dsts.length) {
			throw new ArrayIndexOutOfBoundsException();
		}
		long count=0;
		for(int i=offset;i<offset+length;i++) {
			int num=this.read(dsts[i]);
			if(num<0) {
				return num;
			}
			count+=num;
		}
		return count;
	}

	@Override
	public long read(ByteBuffer[] dsts) throws IOException {
		return read(dsts, 0, dsts.length);
	}

	@Override
	public long write(ByteBuffer[] srcs, int offset, int length) throws IOException {
		// TODO 自動生成されたメソッド・スタブ
		if (offset + length > srcs.length) {
			throw new ArrayIndexOutOfBoundsException();
		}
		long count=0;
		for(int i=offset;i<offset+length;i++) {
			int num=this.write(srcs[i]);
			if(num<0) {
				return num;
			}
			count+=num;
		}
		return count;
	}

	@Override
	public long write(ByteBuffer[] srcs) throws IOException {
		return write(srcs, 0, srcs.length);
	}

	public int read(ByteBuffer dst, long pos) throws IOException {
		if (!readable) {
			throw new UnsupportedOperationException("これは読み込みできねえ");
		}
		begin();
		int count = -2;
		try {
			count = IOUtil.fetch(fd, dst, pos);
			return count;
		} finally {
			end(count > -2);
		}
	}

	public int write(ByteBuffer src, long pos) throws IOException {
		// TODO 自動生成されたメソッド・スタブ
		if (!writable) {
			throw new UnsupportedOperationException("これは書き込みできねえ");
		}
		begin();
		int count=-1;
		try {
			count=IOUtil.push(fd, src, pos);
			return count;
		}finally {
			end(count>-1);
		}
	}

	@Override
	public long position() throws IOException {
		SphereFileSystem sfs = (SphereFileSystem) SingletonS2ContainerFactory.getContainer()
				.getComponent("SphereFileSystem");
		return sfs.getDescPos(fd);
	}

	@Override
	public SphereFileChannel position(long newPosition) throws IOException {
		SphereFileSystem sfs = (SphereFileSystem) SingletonS2ContainerFactory.getContainer()
				.getComponent("SphereFileSystem");
		sfs.setDescPos(fd, newPosition);
		return this;
	}

	@Override
	public long size() throws IOException {
		SphereFileSystem sfs = (SphereFileSystem) SingletonS2ContainerFactory.getContainer()
				.getComponent("SphereFileSystem");
		return sfs.getFileInfo(fd).size();
	}

	@Override
	public SphereFileChannel truncate(long size) throws IOException {
		// TODO 自動生成されたメソッド・スタブ
		return this;
	}

}

package sphereIO;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.file.StandardOpenOption;

/**
 * FileInputStreamもどきです。
 * <p>
 * 使い方は普通のFileInputStreamと同じでござる。BufferedReaderに食わせるなりなんなり自由になさい
 * <p>
 * ただしBufferedReaderとかはSerializableついてないからマイグレーションできんがな！！！
 * @author RanPork
 *
 */
public class SphereFileInputStream extends InputStream implements Serializable {

	long pos;

	SphereFileChannel channel;

	public SphereFileInputStream(SphereFile file) throws IOException {
		pos = 0;
		channel = SphereFileChannel.open(file, StandardOpenOption.READ);
	}

	@Override
	/**
	 * 入力ストリームから1バイト読み込みます
	 *
	 * @return int 読み込まれたバイトデータ EOFは-1
	 */
	public int read() throws IOException {
		ByteBuffer tmp = ByteBuffer.allocate(1);
		channel.read(tmp, pos);
		pos++;
		return tmp.get(0);
	}

	@Override
	/**
	 * 入力ストリームから、最大buffer.lengthバイト読み込みます
	 *
	 * @param buffer データ読込先バッファ
	 * @return int 実際に読み込まれたバイト数。ファイルの終わりに達していた場合は-1
	 */
	public int read(byte[] buffer) throws IOException {
		ByteBuffer tmp = ByteBuffer.wrap(buffer);
		int count = channel.read(tmp, pos);

		if (count > 0) {
			pos += count;
		}

		return count;
	}

	@Override
	/**
	 * 入力ストリームから、最大lengthバイトをバッファに読み込みます。
	 *
	 * @param buffer データ読込先バッファ
	 * @param offset 読み込み開始位置
	 * @param length 読み込む最大バイト数
	 * @return int 実際に読み込まれたバイト数 EOFは-1
	 */
	public int read(byte[] buffer, int offset, int length) throws IOException {
		ByteBuffer tmp = ByteBuffer.wrap(buffer, offset, length);
		int count = channel.read(tmp, pos);

		if (count > 0) {
			pos += count;
		}

		return count;
	}

}

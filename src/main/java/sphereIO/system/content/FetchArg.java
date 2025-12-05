package sphereIO.system.content;

import java.io.Serializable;

import sphereIO.SphereFileDescriptor;
import sphereIO.system.FileContent;
/**
 * IOUtil#Fetchでの通信で使いたい
 * @author RanPork
 *
 */
public class FetchArg implements Serializable,FileContent{
	public SphereFileDescriptor fd;
	public long pos;
	public int length;
	/**
	 * 外部ファイルをfetchする際の引数を表すインスタンスを構築します
	 * @param fd fetchするファイルに関連する記述子
	 * @param pos 読み込みを開始する位置 -1の場合はオープンしているストリームの位置を使用します
	 * @param length 読み込むバイトサイズ ファイルの終端位置にによって実際に読み込まれるバイト列がこの数より小さくなる場合もある
	 */
	public FetchArg(SphereFileDescriptor fd,long pos, int length) {
		this.fd=fd;
		this.pos=pos;
		this.length=length;
	}
}

package sphereIO.system.content;

import java.io.Serializable;

import sphereIO.system.FileContent;

public class FetchResult implements FileContent,Serializable{
	public byte[] data;
	public long pos;//読んだ後の記述子の位置
	public boolean eof;
	public FetchResult(byte[] data,long pos,boolean eof) {
		this.data=data;
		this.pos=pos;
		this.eof=eof;
	}
	public byte[] getData() {
		return data;
	}
	public boolean isEof() {
		return eof;
	}
	public long getPos() {
		return pos;
	}
}

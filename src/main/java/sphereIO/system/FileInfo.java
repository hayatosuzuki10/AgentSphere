package sphereIO.system;

import java.io.IOException;
import java.io.Serializable;
import java.time.Instant;
import java.util.Map;

import org.seasar.framework.container.factory.SingletonS2ContainerFactory;

import sphereIO.FileType;
import sphereIO.SphereFile;

public class FileInfo implements Serializable,FileContent{
	SphereFile f;

	long filesize;
	//ファイルの種類
	FileType ft;
	boolean hidden;

	//ブロックごとの最終更新
	Map<Integer,Instant> lastModified;
	Instant attributeLastModified;
	//直列化の要件にpublicのコンストラクタが必要だった希ガス
	public FileInfo() {
	}

	public FileInfo(SphereFile f,long fileSize,FileType fileType,boolean hidden,Map<Integer,Instant> lastModified,Instant attributeLastModified) {
		this.f=f;
		this.filesize=fileSize;
		this.ft=fileType;
		this.hidden=hidden;
		this.lastModified=lastModified;
		this.attributeLastModified=attributeLastModified;
	}

	public long size() {
		return filesize;
	}

	public static FileInfo getFileInfo(SphereFile f) throws IOException {
		SphereFileSystem SFS = (SphereFileSystem) SingletonS2ContainerFactory.getContainer()
				.getComponent("SphereFileSystem");
		return SFS.getFileInfo(f);
	}
	@Override
	public String toString() {
		StringBuilder builder=new StringBuilder();
		builder.append(f);
		builder.append("\n");
		builder.append(ft.name());
		builder.append("\n");
		builder.append(hidden);
		builder.append("\n");
		lastModified.keySet().stream().sorted().forEach(a->builder.append(a+":"+lastModified.get(a)+"\n"));
		builder.append(attributeLastModified.toString());
		builder.append("\n");
		builder.append(filesize);
		return builder.toString();
	}

}

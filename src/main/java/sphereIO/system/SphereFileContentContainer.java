package sphereIO.system;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

import primula.api.core.network.message.AbstractContentContainer;

/**
 * SphereFileSystemが通信する際のメッセージとなるクラスです
 * <pre>
 * タグとして使用するEnumSetはSetクラスの皮をかぶったビットフラグだそうです
 * 詳しいとこはEnumSetクラスのドキュメントを見て察してください
 * </pre>
 * @author Ranton
 * @see EnumSet
 */
public class SphereFileContentContainer extends AbstractContentContainer {
	//メッセージの中身を表すタグです
	private EnumSet<FileContentType> tag;
	//一回の処理において通しで使用するID
	private UUID session;
	//メッセージの本体を格納する
	private FileContent data;
	//メッセージの起因となるエージェントのID
	private String agentID;
	//このメッセージを送信したAgentSphereのIP
	private String from;

	private Throwable exception;

	public SphereFileContentContainer(UUID session, FileContent data, String agentID, String from) {
		this.session = session;
		this.data = data;
		this.agentID = agentID;
		this.from = from;
		this.tag = EnumSet.noneOf(FileContentType.class);
	}

	/**
	 * タグを1つ設定したファイルデータ通信データのコンストラクタ
	 * @param session そのセッションで使われる通し番号
	 * @param t
	 */
	public SphereFileContentContainer(UUID session, FileContent data, String agentID, String from,
			FileContentType tag) {
		this.session = session;
		this.data = data;
		this.agentID = agentID;
		this.from = from;
		this.tag = EnumSet.of(tag);
	}

	/**
	 * タグを2つ設定したファイルデータ通信データのコンストラクタ
	 * <p>
	 * これ以上に必要であれば増やしてもよい
	 * @param session そのセッションで使われる通し番号
	 * @param tag1
	 * @param tag2
	 */
	public SphereFileContentContainer(UUID session, FileContent data, String agentID, String from, FileContentType tag1,
			FileContentType tag2) {
		this.session = session;
		this.data = data;
		this.agentID = agentID;
		this.from = from;
		this.tag = EnumSet.of(tag1, tag2);
	}

	/**
	 * タグを可変長で設定したファイルデータ通信データのコンストラクタ
	 * <p>
	 * オーバーヘッドありますねぇ！
	 *
	 * @param session そのセッションで使われる通し番号
	 * @param tags
	 */
	public SphereFileContentContainer(UUID session, FileContent data, String agentID, String from,
			FileContentType... tags) {
		this.session = session;
		this.data = data;
		this.agentID = agentID;
		this.from = from;
		this.tag = EnumSet.copyOf(Arrays.asList(tags));
	}

	/**
	 * EnumSetを使用するコンストラクタ
	 * @param s
	 */
	public SphereFileContentContainer(UUID session, FileContent data, String agentID, String from,
			EnumSet<FileContentType> s) {
		this.session = session;
		this.data = data;
		this.agentID = agentID;
		this.from = from;
		this.tag = EnumSet.copyOf(s);
	}

	void setTag(EnumSet<FileContentType> s) {
		this.tag = s;
	}

	/**
	 * このコンテナが所持しているタグの変更不可能なビューを取得します
	 * <pre>
	 * </pre>
	 * @return
	 */
	public Set<FileContentType> getTag() {
		return Collections.unmodifiableSet(tag);
	}

	public String getAgentID() {
		return agentID;
	}

	public void setAgentID(String agentID) {
		this.agentID = agentID;
	}

	public UUID getSession() {
		return session;
	}

	public void setData(FileContent data) {
		this.data = data;
	}

	public FileContent getData() {
		return data;
	}

	public void setFrom(String from) {
		this.from = from;
	}

	public String getFrom() {
		return from;
	}

	public Throwable getException() {
		return exception;
	}

	public void setException(Throwable exception) {
		this.exception = exception;
	}
}

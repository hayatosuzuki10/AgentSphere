package sphereIO.system;
/**
 * コンテンツコンテナの中身を示すタグのためのenum
 * @author Ranton
 * @see SphereFileContentContainer
 */
enum FileContentType {
	/**
	 * ファイルオープン
	 */
	OPEN,
	/**
	 * ファイルクローズ
	 */
	CLOSE,
	/**
	 * 対象を読み込むことを宣言
	 */
	READ,
	/**
	 * 対象に書き込むことを宣言
	 */
	WRITE,
	/**
	 * 対象データに指定
	 */
	DATA,
	/**
	 * 対象をファイル情報に指定
	 */
	INFO,
	/**
	 * 実行した結果例外が出たことを示す
	 */
	ERROR,
	/**
	 * 実行が正常に行われたことを示す
	 */
	COMPLETE;
}

package primula.api.core.agent.loader;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Agent用クラスローダに使うデータコンテナです。<br>
 * Agent用クラスローダはデータの取得元としてこのコンテナを利用します。
 * 
 * @author AK
 * 
 */
public class ClassDataCollectionContainer implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = 200800L;
    private List<ClassBinaryData> container = null;
    private final String canonicalName;

    /**
     * 初期起動用のコンテナなど、どれがAgentか分からない状況で用います。
     */
    public ClassDataCollectionContainer() {
        container = Collections.synchronizedList(new ArrayList<ClassBinaryData>());
        this.canonicalName = null;
    }

    /**
     * スケジューラ向けコンストラクタ。どのクラスがAgentクラスなのかを指定してください。
     *
     * @param canonicalname
     *            AgentであるClassの正規名
     */
    public ClassDataCollectionContainer(String canonicalname) {
        container = Collections.synchronizedList(new ArrayList<ClassBinaryData>());
        this.canonicalName = canonicalname;
    }

    /**
     * シェル向けメソッド。コンテナにクラスバイナリを追加します。
     *
     * @param name
     *            正規なクラス名
     * @param binarydata
     *            クラスのバイナリデータ
     */
    public void add(String name, byte[] binarydata) {
        ClassBinaryData ds = new ClassBinaryData(name, binarydata);
        if (!container.contains(ds)) {
            container.add(ds);
        }
    }

    /**
     * スケジューラ向けメソッド。コンテナにクラスバイナリを追加します。
     *
     * @param classbinary
     *            クラスバイナリデータ構造体
     */
    public void add(ClassBinaryData classbinary) {
        if (!container.contains(classbinary)) {
            container.add(classbinary);
        }
    }

    /**
     * スケジューラ向けメソッド。コンテナに含まれるクラスバイナリを取得します。
     *
     * @param name
     *            正規クラス名
     * @return 指定した名前に該当するクラスバイナリデータ構造体、無ければnull
     */
    public ClassBinaryData pickup(String name) {
        for (int i = 0; i < container.size(); i++) {
            if (container.get(i).name.equals(name)) {
                return container.get(i);
            }
        }
        return null;
    }

    /**
     * スケジューラ向けメソッド。コンテナに含まれるクラスバイナリを取得します。<br>
     * #getNameList()と併用するのがスマートだと思われます。NameListの配列数と要素数は同じと保証されます。
     *
     * @param index
     *            要素番号
     * @return 要素番号のバイナリデータ構造体
     */
    public ClassBinaryData pickup(int index) {
        if (index >= 0 && index < container.size()) {
            return container.get(index);
        }
        return null;
    }

    /**
     * スケジューラ向けメソッド。コンテナに含まれるクラスバイナリを削除します。
     *
     * @param name
     *            正規クラス名
     */
    public void remove(String name) {
        for (int i = 0; i < container.size(); i++) {
            if (container.get(i).name.equals(name)) {
                container.remove(i);
                i--;
            }
        }
    }

    /**
     * スケジューラ向けメソッド。コンテナに含まれるクラスバイナリを削除します。
     *
     * @param namehead
     *            正規クラス名の先頭合致文字
     */
    public void removePackage(String namehead) {
        for (int i = 0; i < container.size(); i++) {
            if (container.get(i).name.startsWith(namehead)) {
                container.remove(i);
                i--;
            }
        }
    }

    /**
     * クラスローダ向けメソッド。コンテナをマージします。
     *
     * @param another_container
     *            追加したいコンテナ
     */
    protected void merge(ClassDataCollectionContainer another_container) {
        for (int i = 0; i < another_container.container.size(); i++) {
            if (!container.contains(another_container.container.get(i))) {
                container.add(another_container.container.get(i));
            }
        }
    }

    /**
     * クラスローダ向けメソッド。クラスのバイナリを返します。
     *
     * @param name
     *            正規のクラス名
     * @return クラスのバイナリ
     */
    protected byte[] getBinaryData(String name) {
        for (int i = 0; i < container.size(); i++) {
            if (container.get(i).name.equals(name)) {
                return container.get(i).binarydata;
            }
        }
        return null;
    }

    /**
     * 汎用メソッド。コンテナの要素数を返します。
     *
     * @return コンテナの要素数
     */
    public int size() {
        return container.size();
    }

    /**
     * 汎用メソッド。コンテナの持つクラス構造体から名前のString[]を返します。
     *
     * @return コンテナの持つクラス名
     */
    public String[] getNameList() {
        String[] str = new String[container.size()];
        for (int i = 0; i < container.size(); i++) {
            str[i] = container.get(i).name;
        }
        return str;
    }

    /**
     * コンテナのフィールドにある「エージェントの本体」のクラス名を返します。<br>
     * センダー用コンテナには必ずセットされている必要があります。
     *
     * @return 正規なクラス名
     */
    public String getCanonicalClassName() {
        return canonicalName;
    }

    /*
     * (非 Javadoc)
     *
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object obj) {
        return equals(obj, false);
    }

    /**
     * 厳密な同値判定を行うequalsメソッドのオーバーロードになります。<br>
     * コンテナ内の全てのデータが同じものを含んでいた場合trueを返します。
     *
     * @param obj
     *            比較するClassDataCollectionContainer
     * @param flag
     *            厳密に比較するならtrueをセットする
     * @return 厳密に比較された結果、同等ならtrue、そうでなければfalse
     */
    public boolean equals(Object obj, boolean flag) {
        if ((obj != null) && (obj instanceof ClassDataCollectionContainer)) {
            if (!flag) {
                return this.canonicalName.equals(((ClassDataCollectionContainer) obj).canonicalName);
            } else {
                if (container.size() == ((ClassDataCollectionContainer) obj).size()) {
                    return container.containsAll(((ClassDataCollectionContainer) obj).container);
                }
            }
        }
        return false;
    }

    /*
     * (非 Javadoc)
     *
     * @see java.lang.Object#hashCode()
     */
    public int hashCode() {
        return canonicalName.hashCode();
    }

    /**
     * クラスのバイナリデータを保持する内部クラス。
     *
     * @author AK
     *
     */
    public class ClassBinaryData {

        private final String name;// Canonicalな名前　例：hoge.piyo.MyAgent
        private final byte[] binarydata;// クラスファイルのバイナリデータ

        ClassBinaryData(String name, byte[] binarydata) {
            this.name = name;
            this.binarydata = binarydata;
        }

        /*
         * (non-Javadoc)
         *
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals(Object obj) {
            if ((obj != null) && (obj instanceof ClassBinaryData)) {
                return this.name.equals(((ClassBinaryData) obj).name);
            }
            return false;
        }

        /*
         * (non-Javadoc)
         *
         * @see java.lang.Object#hashCode()
         */
        @Override
        public int hashCode() {
            return name.hashCode();
        }
    }
}

package primula.api.core;

/**
 * @author yamamoto
 */
public interface ICoreModule {

    /**
     * モジュールの初期化時に呼び出されます
     */
    void initializeCoreModele();

    /**
     *モジュールの終了時に呼び出されます
     */
    void finalizeCoreModule();
}

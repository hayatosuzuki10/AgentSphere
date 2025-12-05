package scheduler2022.util;

import scheduler2022.DynamicPCInfo;

/**
 * Schedulerのアップデートをトリガーとしてなんかさせたいインターフェース
 * @author selab
 *
 */
public interface IInfoUpdateListener {
	void pcInfoUpdate(DynamicPCInfo info);
}

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package primula.api.core.network.dthmodule2.data;

import java.io.Serializable;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.logging.Logger;

import primula.api.core.network.dthmodule2.address.Address;
import primula.util.KeyValuePair;

/**
 *　Hubを管理するためのクラス 　1ノードに同一種類かつ複数のHubを管理するためにMAPで種類別に管理、更にその内部をListで保持している。
 *
 * @author kousuke
 */
public final class HubContainer {

	private HashMap<String, LinkedList<Hub>> hubContainer;

	private ArrayList<String> keyContainer;

	private InetAddress directSuccessor;

	private InetAddress directPredecessor;

	private Address addressOfLocalNode;

	private boolean isMainNode = false;

	private List<InetAddress> accessList;

	private ArrayList<InetAddress> crossHubLink;

	private Logger logger;

	private int divideScore;

	public HubContainer(int divideScore) {
		this.hubContainer = new HashMap<String, LinkedList<Hub>>();
		this.keyContainer = new ArrayList<String>();
		this.crossHubLink = new ArrayList<InetAddress>();
		this.accessList = new ArrayList<InetAddress>();
		// logger=Logger.getLogger(hubContainer.getClass());
		this.divideScore = divideScore;
	}

	public void setAddress(Address addressOfLocalNode) {
		this.addressOfLocalNode = addressOfLocalNode;
	}

	public final void addHub(Hub hubToAdd) {
		if (!this.isMainNode) {
			this.accesToMercury();
		}
		if (this.hubContainer.containsKey(hubToAdd.getType())) {
			this.hubContainer.get(hubToAdd.getType()).add(hubToAdd);
		} else {
			LinkedList<Hub> newHub = new LinkedList<Hub>();
			newHub.add(hubToAdd);
			this.keyContainer.add(hubToAdd.getType());
			this.hubContainer.put(hubToAdd.getType(), newHub);
		}
	}

	public void addCrossHubLink(InetAddress proxy) {
		this.crossHubLink.add(proxy);
	}

	public InetAddress getCrossHubLinkRandomly() {
		int linkSize = this.crossHubLink.size();
		Random random = new Random();
		int randomInt = random.nextInt(linkSize);
		return this.crossHubLink.get(randomInt);
	}

	public final String getKeyRandomly() {
		Random random = new Random();
		int r = random.nextInt(keyContainer.size());
		return this.keyContainer.get(r);
	}

	public final boolean isMainNetwork() {
		return this.isMainNode;
	}

	public final boolean hasHub(String type) {
		return this.hubContainer.containsKey(type);
	}

	public final Hub getHub(String typeOfHub) {
		return this.hubContainer.get(typeOfHub).get(0);
	}

	public final InetAddress[] createHubNetwork(Address[] addressOfNodes,
			String typeOfHub) {
		InetAddress[] Nodes = null;
		for (Address address : addressOfNodes) {

		}

		return Nodes;
	}

	/**
	 * 指定された{@link DataRange}がこのノードの担当範囲かどうかを調べる
	 *
	 * @param keyTocheck
	 *            　探しているデータの範囲
	 * @return 完全に範囲内の場合＝
	 *         ０、完全に範囲外の場合=-1。範囲内＆範囲以上＝１。範囲内＆範囲以下≒２,範囲内だが最小値、最大値ともにハミ出た場合＝３、詳しくはドキュメント参
	 *         照
	 */
	public final int isRange(DataRange keyTocheck) {
		if (this.hubContainer.containsKey(keyTocheck.getType())) {
			LinkedList<Hub> checkingList = this.hubContainer.get(keyTocheck
					.getType());
			Hub checkingHub = null;
			for (int i = 0; i < checkingList.size(); i++) {
				checkingHub = checkingList.get(i);
				return checkingHub.getRange().isInRange(keyTocheck);

			}
		}
		return -1;
	}


	/**
	 * 担当範囲を分割して返す このメソッドを使う前にisRangeメソッドを利用してIndexを調べる必要がある。
	 *
	 * @param key
	 *            　Hubの種類を表すKey
	 * @param index
	 *            複数Hubを保持している場合のIndex
	 * @return {@link DataRange}によって分割されてできた新しいRange
	 */
	public final DataRange divideRange(String key, int index) {
		DataRange newRange = hubContainer.get(key).get(index).divideRange(
				divideScore);
		return newRange;
	}

	/**
	 * エントリをセットする このメソッドを使う前にisRangeメソッドを利用してIndexを調べる必要がある。
	 *
	 * @param type
	 *            　 Hubの種類を表すKey
	 * @param index
	 *            　複数Hubを保持している場合のIndex
	 * @param pair
	 *            　　
	 */
	public final void setEntry(String type, int index, KeyValuePair pair) {

		hubContainer.get(type).get(index).setEntry(pair);
	}

	/**
	 * エントリをセットする このメソッドを使う前にisRangeメソッドを利用してIndexを調べる必要がある。
	 *
	 * @param type
	 *            　 Hubの種類を表すKey
	 * @param index
	 *            　複数Hubを保持している場合のIndex
	 * @param pair
	 *            　　
	 */
	public final void setEntry(String type, int index,
			ArrayList<KeyValuePair> pairs) {

		for (int i = 0; i < pairs.size(); i++) {
			hubContainer.get(type).get(index).setEntry(pairs.get(i));
		}

	}

	/**
	 * 保存してあったエントリを取得するためのメソッド このメソッドを使う前にisRangeメソッドを利用してIndexを調べる必要がある。
	 *
	 * @param range
	 *            　取得するデータの範囲
	 * @param index
	 *            　取得するデータを保持しているINDEX
	 * @return 　rangeが示す範囲内に存在しているデータ
	 */
	public final List<Serializable> getEntries(DataRange range) {
		int index = this.isRange(range);
		return hubContainer.get(range.getType()).get(index).getEntry(range);
	}

	/**
	 * 保持しているHubをランダムに渡す
	 *
	 * @param fromAddress
	 * @return
	 */
	public final Hub getHubRandomly(InetAddress fromNode) {
		Set keySet = hubContainer.keySet();

		String[] keySetToString = (String[]) keySet.toArray();
		Random random = new Random();
		int randomNumber = random.nextInt(keySetToString.length);
		String randomKey = keySetToString[randomNumber];

		LinkedList<Hub> randomHubList = this.hubContainer.get(randomKey);
		Hub randomHub = randomHubList.get(0);

		// DataRange range = randomHub.divideRange(divideScore);

		// randomHub.addPredecessor(fromNode);
		//
		// randomHub.addSuccessor(randomHub.getDirectSuccessor());

		return randomHub;
	}

	public final DataRange getDataRange(String typeOfHub) {
		return this.hubContainer.get(typeOfHub).get(0).getRange();
	}

	public final int getDataRangeWithInteger(String typeOfHub) {
		if (this.hasHub(typeOfHub)) {
			return this.hubContainer.get(typeOfHub).get(0).getRange()
					.caluculateDifference();
		} else {
			RuntimeException e = new RuntimeException(
					"this node hasnt successor hub! somewhere seriously wrong!");
			// logger.fatal(e);
			throw e;
		}
	}

	public final void setAccessLink(InetAddress accessLink) {
		this.accessList.add(accessLink);
	}

	/**
	 * ランダムにアクセスリンクを抽出するクラス
	 *
	 * @return 　ランダムに選ばれたアクセスリンク
	 */
	public final InetAddress getAccessLinkRandomly() {
		Random random = new Random();
		int randomNum = random.nextInt(this.accessList.size());
		return this.accessList.get(randomNum);
	}

	/**
	 * mercuryへの参加フラグを立てる関数
	 */
	public final void accesToMercury() {
		this.isMainNode = true;
	}

	/**
	 * 引数のアドレスを持つアクセスリンクが存在しているかどうかをチェックする関数
	 *
	 * @param address
	 * @return 引数のアドレスを持つアドレスリンクが存在している場合にtureを返す
	 */
	public final boolean isContains(Address address) {
		return this.accessList.contains(address);
	}

	@Override
	public final String toString() {
		StringBuilder builder = new StringBuilder();
		Set<String> keys = hubContainer.keySet();
		LinkedList<Hub> containedHub;
		builder.append("THIS HUB CONTAINS " + keys.size() + " ENTRIES\n");
		for (String key : keys) {
			containedHub = hubContainer.get(key);
			for (int i = 0; i < containedHub.size(); i++) {
				builder.append("KEY =" + (i + 1) + " :"
						+ containedHub.get(i).toString());
			}
		}

		return builder.toString();
	}

	public void updateHub(Hub hub){
		this.hubContainer.get(hub.getType()).remove(0);
		this.hubContainer.get(hub.getType()).add(hub);
	}

}

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package primula.api.core.network.message;

import java.io.Serializable;

import primula.api.SystemAPI;
import primula.api.core.network.AgentAddress;
import primula.api.core.resource.SystemConfigResource;

/**
 *
 * @author yamamoto
 */
public abstract class AbstractEnvelope implements Serializable {

	private AbstractContentContainer content;
	private AgentAddress targetAgentAddress;
	private int ttl;
	{
		ttl = Integer.parseInt((String) SystemAPI.getSystemConfigData(SystemConfigResource.DEFAULT_MESSAGE_TTL));
		//System.err.println(this.getClass().toString()+":ttl->"+ttl);//テスト用
		//except();//テスト用
	}

	public void except() {
		throw new UnsupportedOperationException();
	}
	/**
	 * @return the Content
	 */
	public AbstractContentContainer getContent() {
		return content;
	}

	protected void setContent(AbstractContentContainer content) {

		this.content = content;
	}

	/**
	 * @return the agentAddress
	 */
	public AgentAddress getTargetAgentAddress() {
		return targetAgentAddress;
	}

	/**
	 * @param agentAddress the agentAddress to set
	 */
	protected void setTargetAgentAddress(AgentAddress agentAddress) {
		this.targetAgentAddress = agentAddress;
	}

	public void setTTL(int ttl) {
		if (ttl > 0)
			this.ttl = ttl;
		throw new IllegalArgumentException("メッセージ転送数が0未満になっています");
	}

	public int getTTL() {
		return ttl;
	}
	/**
	 * このメッセージの生存期間を-1します
	 * メッセージAPIが再送する際1度呼ばれます
	 */
	public void decrementTTL() {
		ttl--;
	}
}

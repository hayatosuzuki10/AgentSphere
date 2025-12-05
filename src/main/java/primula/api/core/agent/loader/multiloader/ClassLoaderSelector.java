/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package primula.api.core.agent.loader.multiloader;
/**
 * クラスローダの識別子としてのインターフェース。<br/>
 *
 *
 * @author Mr.RED
 *
 */
public interface ClassLoaderSelector {
	@Override
	public boolean equals(Object obj);

	@Override
	public int hashCode();
}

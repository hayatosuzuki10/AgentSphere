/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package primula.api.core.network.dhtmodule.data;

import java.util.ArrayList;
import java.util.List;

/**
 * マシン情報の管理機構、AgentSphereから取得したデータをこちらで保管して参照する。
 * さらに、{@link tequila.routing.impl.MaintenanceLayer}で関数呼び出しによりMercuryネットワークを作成、
 * 参加、離脱の判定を行う。
 * @author kousuke
 */
public final class MachineDataManeger {
    
   MachineData ownThreshold;
   ArrayList<MachineData> machineDataOfAccesslink;
   ArrayList<MachineData> pastMachineData;
   
   
   
    public MachineDataManeger(MachineData threshold) {
        this.ownThreshold = threshold;
        this.machineDataOfAccesslink = new ArrayList<MachineData>();
        this.pastMachineData = new ArrayList<MachineData>();
    
    }
    
    private MachineData getOwnData(){
        throw new RuntimeException("not supported yet");
    }
    
    public List<MachineData> chooseMachine(MachineData threshold,int number){
        throw new RuntimeException("not supported yet");
    }
    
    public boolean checkThreshold(){
        return false;
    }
   
    
}

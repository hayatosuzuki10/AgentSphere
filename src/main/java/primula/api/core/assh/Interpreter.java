package primula.api.core.assh;

import primula.api.core.ICoreModule;

//必ずMainPanelもしくはParserを起動させること

public class Interpreter implements ICoreModule {

	//private Parser parser = new Parser();//MainPanel(Terminal)の起動を行わない場合には、コメントを外すこと

	@Override
	public void finalizeCoreModule() {
		//parser.requestStop();
	    System.out.println("AgentSphereShell is finished！");
	}

	@Override
	public void initializeCoreModele() {
		MainPanel panel=new MainPanel(); //Terminalの設定
		panel.setVisible(true); // Terminalの表示
		//parser.start();//MainPanel(Terminal)の起動を行わない場合には、コメントを外すこと
	}
}

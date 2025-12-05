package primula.api.core.assh;

//@author nakadaira(2019)
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.PrintStream;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import primula.api.SystemAPI;


/*
 * コンソールパネルを表示するプログラム
 * 基本的にターミナル(MainPanel)と併用して使用する。
 * System.err.print()の内容がこの画面で表示される
 */
public class ConsolePanel extends JFrame implements ActionListener, KeyListener{
	static OutputTextArea textArea;
	static JScrollPane scrollPane;
	static ConsolePanel myPanel;
	ShellEnvironment shellEnv = ShellEnvironment.getInstance();
	static JTextField textField;
	History history;
	private static String str = "";
	static boolean flg = false;
	int WIDTH = 700;
	int HEIGHT = 500;
	//Parser parser = new Parser();
	PrintStream sysout=System.out;
	PrintStream syserr=System.err;
	public static boolean scroll_flg=true;

	public ConsolePanel(){
		myPanel=this;//2022年度新倉追加ウィンドウ名に諸情報を載せたい
		textArea = new OutputTextArea();
		//textArea.setToSystemOut();//outの中身をコンソールからテキストボックスに変更
		textArea.setToSystemErr();
		this.add(textArea);

		//Parser parser = new Parser();//ここを加えるとParserが重複して起動してしまう。Interpreter.javaでTerminalをくわえるならば、コメントをとる
		//parser.start();//Terminalのコマンド入力が可能になる部分
		//Parser.terminal_flg=true;//Terminalが動いていると判断

		setTitle("Console");
		setBounds(100, 100, WIDTH, HEIGHT);
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowClosing());

		JPanel panel = new JPanel();

		//メニューバー
		JMenuBar menuBar = new JMenuBar();
		getContentPane().add(menuBar, BorderLayout.NORTH);

		JMenu menu1 = new JMenu("メニュー");
		menuBar.add(menu1);
		JMenuItem item1 = new JMenuItem("フォルダの選択");
		menu1.add(item1);
		JMenuItem item3 = new JMenuItem("オートスクロール切り替え");
		menu1.add(item3);
		JMenuItem item4 = new JMenuItem("clean");
		menu1.add(item4);
		item1.addActionListener(this);
		item3.addActionListener(this);
		item4.addActionListener(this);

		//画面中央のテキストエリアの作成
		textArea.setEditable(true);
		textArea.setDisabledTextColor(Color.black);

		//折り返しの設定
		textArea.setWrapStyleWord(true);
		textArea.setLineWrap(true);
		panel.setLayout(new BorderLayout());

		//テキストエリアにスクロールバーをつける
		scrollPane=new JScrollPane(textArea);
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		panel.add(scrollPane, BorderLayout.CENTER);


		//画面下部のテキストフィールドの作成
		textField = new JTextField("");
		textField.addKeyListener(this);
		//history = new History(textField);
		//history.read();
		//panel.add(textField,BorderLayout.SOUTH);

		Container contentPane = getContentPane();
		contentPane.add(panel);
	}

	class WindowClosing extends WindowAdapter{
		public void windowClosing(WindowEvent e){

			int ans = JOptionPane.showConfirmDialog(ConsolePanel.this, "本当に終了しますか？");
			if(ans == JOptionPane.YES_OPTION){
				//history.save();
				Parser.terminal_flg=false;
				System.setOut(sysout);//outの中身がテキストの表示になっているので、コンソール表示に戻す
				System.setErr(syserr);
				try{
				SystemAPI.shutdown();//AgentSphereの終了
				}catch (Exception exception) {
					// TODO: handle exception
				}
				System.exit(0);//Java仮想マシンの終了を意味する
				dispose();//呼び出し元のウィンドウおよびそこから呼び出されているすべてのコンポーネントの開放
				//SphereTerminalをコマンドとして呼び出すのであれば、dispose()
				//最初からTerminalとして表示させるのであれば、System.exit()が正しい
			}

		}
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if(e.getActionCommand().equals("フォルダの選択")){
			JFileChooser filechooser = new JFileChooser();
			filechooser.setApproveButtonText("選択");
			filechooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

			int selected = filechooser.showOpenDialog(this);
			if(selected == JFileChooser.APPROVE_OPTION){
				File file = filechooser.getSelectedFile();
				String s = file.toString();
				textField.setText(" ");
				shellEnv.setDirectory(s);
				flg = true;
			}
		}else if(e.getActionCommand().equals("オートスクロール切り替え")){
			if(scroll_flg) {
				scroll_flg=false;
			}else {
				scroll_flg=true;
			}
		}else if(e.getActionCommand().equals("clean")){
			textArea.setText("");
		}
	}

	public void settext(String s){
		textField.setText(s);
	}
	/**
	 * コンソール画面のスクロールバーを最後尾にするメソッドです
	 * ユーザーがオートスクロールを有効にすると機能します
	 */
	public static void autoscroll() {
		if(scroll_flg) {
			scrollPane.getVerticalScrollBar().setValue(scrollPane.getVerticalScrollBar().getMaximum());
		}
	}

	public static void setPanelTitle(String title) {
		myPanel.setTitle("Console"+" "+title);
	}


	@Override
	public void keyPressed(KeyEvent e) {
		if(e.getKeyChar() == '\n'){//Enterが押されたら
			//scrollPane.getVerticalScrollBar().setValue(scrollPane.getVerticalScrollBar().getMaximum());
			//加えたことで、下まで動くようになったが結果が隠れてしまう→表示以後にこの一行が必要になる。
			flg = true;
		}
		if(e.getKeyCode() == 38){//↑キーが押されたら
			history.up();
		}
		if(e.getKeyCode() == 40){//↓キーが押されたら
			history.down();
		}
	}

	@Override
	public void keyReleased(KeyEvent e) {
		// TODO 自動生成されたメソッド・スタブ
	}

	@Override
	public void keyTyped(KeyEvent e) {
	}

	public static String getText(){
		return textField.getText();
	}

}

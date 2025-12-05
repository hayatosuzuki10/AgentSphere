package primula.api.core.assh;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import javax.swing.JTextField;

public class History {
	JTextField textField;
	private static int index=0;
	private static int MAX=50;//記憶の最大数
	private static ArrayList<String> strList=new ArrayList<String>();
	public History(JTextField tf) {
		textField=tf;
	}

	public static void add(String line) {
		strList.add(line);
		if(strList.size()>MAX)strList.remove(0);
		index=strList.size();
	}


	public void up() {//上に行くほど(indexの値が小さいほど)古い
		if(index>0 && index<=strList.size())
			{
				index--;
				textField.setText(strList.get(index));
			}
	}

	public void down() {
		if(index>=-1 && index<strList.size()-1){
			textField.setText(strList.get(index+1));
			index++;
		}

	}

	public static String log() {	//historyのログを出力
		String tmp = "";
		for(int i=0;i<strList.size();i++)
			tmp=tmp+strList.get(i)+"\r\n";//Windowsじゃないと改行されないかも
		return tmp;
	}

	public void read() { //起動時history読み込み
		try{
			FileReader file=new FileReader("history.txt");
			BufferedReader br = new BufferedReader(file);
			String s;
			while((s=br.readLine())!=null && !s.equals("")){
				add(s);
			}
		}catch(Exception e){
			System.out.println("historyファイルの読み込みに失敗しました");
		}
	}

	public void save() {
		File file = new File("history.txt");
		PrintWriter pw;
		try {
			pw = new PrintWriter(new BufferedWriter(new FileWriter(file)));
			pw.println(History.log());
			pw.close();
		} catch (IOException e1) {
			// TODO 自動生成された catch ブロック
			e1.printStackTrace();
		}

	}



}

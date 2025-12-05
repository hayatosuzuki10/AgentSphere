/*
 * To change this template, choose Tools | textlates
 * and open the template in the editor.
 */
package primula.api.core.interim.monitor;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.FileWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;

import primula.api.AgentAPI;
import primula.api.NetworkAPI;
import primula.api.SystemAPI;
import primula.api.core.agent.AgentInfo;
import primula.api.core.network.message.AbstractEnvelope;
import primula.api.core.network.message.StandardContentContainer;
import primula.util.KeyValuePair;

/**
 *
 * @author onda
 *
 */
public class AgentMonitor {

    public void runMonitor(AgentMonitorAgent agent) {
        MonitorFrame frm = new MonitorFrame("AgentMonitor");
        frm.setxy(0, 0);
        frm.setAgent(agent);
        frm.setLocation(300, 200);
        frm.setSize(550, 620);
        frm.cp.setBackground(Color.LIGHT_GRAY);
        frm.setVisible(true);
        while (frm.a) {
        }//閉じたらAgentが終わるよう
    }
}

class MonitorFrame extends JFrame implements ActionListener {

    int x, y, machineSize;
    String myIP, myHostName;
    String[] ip, asId;
    Container cp;
    AbstractEnvelope envelope;
    AgentMonitorAgent agent;
//    //全部乗っける一番大きいパネル
//    JPanel pn_All = new JPanel();
    //pn_Allに乗っけるタブ
    private JTabbedPane tab = new JTabbedPane();
    //Text用
    private JPanel tabPanelTextInfo = new JPanel();
    private JScrollPane scrollpane, scrollpaneImage;
    private JTextArea txtar, txtarImage;
    private JPanel pnTextOperation = new JPanel();
    private JLabel lbTextOperation, lbTextDisp;
    private JButton btnClear, btnSave, btnMyDisp, btnOtherDisp, btnAgentGroup, btnMachineList;
    private JFileChooser fileChooser = new JFileChooser();
    private JPanel pnTextDisp = new JPanel();
    //Image用
    private AgentCanvas canvas0 = new AgentCanvas(100, 90);
    private AgentCanvas canvas1 = new AgentCanvas(100, 90);
    private AgentCanvas canvas2 = new AgentCanvas(100, 90);
    private AgentCanvas canvas3 = new AgentCanvas(100, 90);
    private AgentCanvas canvas4 = new AgentCanvas(100, 90);
    private AgentCanvas canvas5 = new AgentCanvas(100, 90);
    private AgentCanvas canvas6 = new AgentCanvas(100, 90);
    private AgentCanvas canvas7 = new AgentCanvas(100, 90);
    private AgentCanvas canvas8 = new AgentCanvas(100, 90);
    private AgentCanvas canvas9 = new AgentCanvas(100, 90);
    private AgentCanvas canvas10 = new AgentCanvas(100, 90);
    private AgentCanvas canvas11 = new AgentCanvas(100, 90);
    private AgentCanvas canvas12 = new AgentCanvas(100, 90);
    private AgentCanvas canvas13 = new AgentCanvas(100, 90);
    private AgentCanvas canvas14 = new AgentCanvas(100, 90);
    private AgentCanvas canvas15 = new AgentCanvas(100, 90);
    private AgentCanvas canvas16 = new AgentCanvas(100, 90);
    private AgentCanvas canvas17 = new AgentCanvas(100, 90);
    private AgentCanvas canvas18 = new AgentCanvas(100, 90);
    private AgentCanvas canvas19 = new AgentCanvas(100, 90);
    private AgentCanvas canvasList[] = {canvas0, canvas1, canvas2, canvas3, canvas4, canvas5, canvas6, canvas7, canvas8, canvas9, canvas10,
        canvas11, canvas12, canvas13, canvas14, canvas15, canvas16, canvas17, canvas18, canvas19};
    private String canvasNameList[] = {"canvas0.jpg", "canvas1.jpg", "canvas2.jpg", "canvas3.jpg", "canvas4.jpg", "canvas5.jpg",
        "canvas6.jpg", "canvas7.jpg", "canvas8.jpg", "canvas9.jpg", "canvas10.jpg", "canvas11.jpg", "canvas12.jpg", "canvas13.jpg",
        "canvas14.jpg", "canvas15.jpg", "canvas16.jpg", "canvas17.jpg", "canvas18.jpg", "canvas19.jpg"};
    private AgentCanvas savecanvas = new AgentCanvas(400, 500);
    private JPanel tabPanelImageInfo = new JPanel();
    private JPanel pnImage = new JPanel();
    private JPanel pnImageOperation = new JPanel();
    private JLabel lbImageOperation, lbImageInfo;
    private JButton btnImageClear, btnImageSave, btnImageMachineInfo, btnAgentMovement;
    private JPanel pnImageDisp = new JPanel();
    private JPanel pnImageInfo = new JPanel();
    boolean a = true;

    public boolean setStop() {
        a = false;
        return a;
    }

    //コンストラクタ
    public MonitorFrame(String title) {
        setTitle(title);
        cp = getContentPane();
        /*
         * 「すべてのフレームに、setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE)を設定して、
         * Java VM 内で最後の表示可能なウィンドウが破棄されると、VM が終了するようになっています.」
         * らしい。いろいろやってて何か問題起きたら変えます。
         */
        addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosing(WindowEvent e) {
                setStop();
                setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            }
        });

        //テキストエリアの作成
        txtar = new JTextArea(22, 45);
        txtar.setFont(new Font("Dialog", Font.PLAIN, 12));//文字書式サイズ変更
        txtar.setForeground(new Color(64, 64, 64));//文字色変更

        //pnTextOperationラベル
        lbTextOperation = new JLabel("TextAreaの操作", Label.LEFT);
        Font f = new Font("Serif", Font.BOLD, 12);
        lbTextOperation.setFont(f);
        lbTextOperation.setForeground(Color.blue);

        //テキストエリア操作ラベルに乗せるボタン設定
        btnClear = new JButton("Clear");
        btnSave = new JButton("Save");
        btnClear.addActionListener(this);
        btnSave.addActionListener(this);

        //pnTextDispラベル
        lbTextDisp = new JLabel("情報表示", Label.LEFT);
        lbTextDisp.setFont(f);
        lbTextDisp.setForeground(Color.blue);
        //pnTextDispボタン設定
        btnMyDisp = new JButton("My Machine");
        btnOtherDisp = new JButton("Remote Machine");
        btnAgentGroup = new JButton("Agent Group");
        btnMachineList = new JButton("Machine一覧");
        btnMyDisp.addActionListener(this);
        btnOtherDisp.addActionListener(this);
        btnAgentGroup.addActionListener(this);
        btnMachineList.addActionListener(this);

        //イメージエリア
        txtarImage = new JTextArea();
        txtarImage.setPreferredSize(new Dimension(100, 200));
        txtarImage.setFont(new Font("Dialog", Font.PLAIN, 10));//文字書式サイズ変更
        txtarImage.setForeground(new Color(64, 64, 64));//文字色変更
        txtarImage.setBackground(Color.LIGHT_GRAY);

        //pnImageOperationラベル
        lbImageOperation = new JLabel("ImageAreaの操作", Label.LEFT);
        lbImageOperation.setFont(f);
        lbImageOperation.setForeground(Color.blue);
        //イメージエリア操作ラベルに乗せるボタン設定
        btnImageClear = new JButton("Clear");
        btnImageSave = new JButton("Save");
        btnImageClear.addActionListener(this);
        btnImageSave.addActionListener(this);
        //pnImageDispラベル
        lbImageInfo = new JLabel("情報表示", Label.LEFT);
        lbImageInfo.setFont(f);
        lbImageInfo.setForeground(Color.blue);
        //pnTextDispボタン設定
        btnImageMachineInfo = new JButton("MachineInfo");
        btnAgentMovement = new JButton("AgentMovement");
        btnImageMachineInfo.addActionListener(this);
        btnAgentMovement.addActionListener(this);

        /*
         * TextInfoタブにいろいろ乗っけます～
         */
        //テキストエリア
        scrollpane = new JScrollPane(txtar);
        //テキストエリア操作のパネルに乗せる
        pnTextOperation.add(lbTextOperation);
        pnTextOperation.add(btnClear);
        pnTextOperation.add(btnSave);
        //情報表示のパネルに乗せる
        pnTextDisp.add(lbTextDisp);
        pnTextDisp.add(btnMyDisp);
        pnTextDisp.add(btnOtherDisp);
        pnTextDisp.add(btnAgentGroup);
        pnTextDisp.add(btnMachineList);
        //↑×３をパネルに乗っけて、それをタブTextInfoに乗せる
        //BoxLayout.Y_AXISで縦一列に並べる
        tabPanelTextInfo.setLayout(new BoxLayout(tabPanelTextInfo, BoxLayout.Y_AXIS));
        tabPanelTextInfo.add(scrollpane);
        tabPanelTextInfo.add(pnTextOperation);
        tabPanelTextInfo.add(pnTextDisp);
        tab.addTab("TextInfo", tabPanelTextInfo);

        /*
         * ImageInfoタブにいろいろ乗っけます～
         */
//        pnImageは5*4の格子
        pnImage.setLayout(new GridLayout(5, 4));
        //キャンバスのセット
        for (int i = 0; i < 20; i++) {
            pnImage.add(canvasList[i]);
        }
        scrollpaneImage = new JScrollPane(txtarImage);
        //イメージエリアの操作パネルに乗せる
        pnImageOperation.add(lbImageOperation);
        pnImageOperation.add(btnImageClear);
        pnImageOperation.add(btnImageSave);
        //イメージ表示のパネルに乗せる
        pnImageDisp.add(lbImageInfo);
        pnImageDisp.add(btnImageMachineInfo);
        pnImageDisp.add(btnAgentMovement);
        //
        pnImageInfo.setLayout(new BoxLayout(pnImageInfo, BoxLayout.Y_AXIS));
        pnImageInfo.add(pnImageOperation);
        pnImageInfo.add(pnImageDisp);

        tabPanelImageInfo.setLayout(new BorderLayout());
        tabPanelImageInfo.add(scrollpaneImage, BorderLayout.WEST);
        tabPanelImageInfo.add(pnImage, BorderLayout.CENTER);
        tabPanelImageInfo.add(pnImageInfo, BorderLayout.SOUTH);
        tab.addTab("ImageInfo", tabPanelImageInfo);

        cp.add(tab, BorderLayout.PAGE_END);
    }

    private void test(){
        for (int i = 0; i < 2; i++) {
                canvasList[i].draw("ShellAgentImage.jpg");
                canvasList[i].draw("StationaryAgentImage.jpg");
            }
        canvasList[0].draw("ClearAgentImage.jpg");
        canvasList[0].draw("AgentMonitorAgentImage.jpg");
        txtarImage.append("192.168.114.19\r\n"
                + "<in>\r\n"
                + "AgentMonitorAgent\r\n"
                + "192.168.114.19\r\n"
                + "<in>\r\n"
                + "AgentMonitorAgent\r\n"
                + "192.168.114.19\r\n"
                + "<out>\r\n"
                + "AgentMonitorAgent\r\n"
                );
    }


    /*
     *AgentMonitor上のボタン操作
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        try {
            InetAddress inet = InetAddress.getLocalHost();
            myHostName = inet.getHostName();
            myIP = inet.getHostAddress();
        } catch (UnknownHostException uhe) {
            System.err.println(uhe.getMessage());
            System.exit(-1);
        }
        Object obj = e.getSource();
        if (obj == btnClear) {
            txtar.setText("");
        } else if (obj == btnSave) {
            TextSave();
        } else if (obj == btnImageClear) {
            txtarImage.setText("");
            imageClear();
        } else if (obj == btnImageSave) {
            for (int i = 0; i < 20; i++) {
                canvasList[i].save(canvasNameList[i]);
            }
            // savecanvas.savedraw(canvasNameList);
        } else {
            if (obj == btnMyDisp) {
                separate();
                myMachineInfo();
            } else if (obj == btnOtherDisp) {
                separate();
                otherMachineInfo();
            } else if (obj == btnAgentGroup) {
                separate();
                agentGroup();
            } else if (obj == btnMachineList) {
                separate();
                machineList();
            } else if (obj == btnImageMachineInfo) {
                txtarImage.setText("");
                imageClear();
                imageMachineInfo();
            } else if (obj == btnAgentMovement) {
                txtarImage.setText("");
                imageClear();
                test();
//                agentMovement();
            }
        }
    }

    public void setxy(int x, int y) {
        this.x = x;
        this.y = y;
    }
    /*
     * テキスト：MyMachineボタンの処理
     */

    private void myMachineInfo() {
        StringBuffer text = new StringBuffer();
        StringBuffer AInfo = new StringBuffer();
        
        int count = 0;
        text.append("<MyMachine>\r\n");
        text.append("Host Name  ：").append(myHostName).append("\r\n");
        text.append("IP Address ：").append(myIP).append("\r\n");
        HashMap<String, List<AgentInfo>> agentInfos = AgentAPI.getAgentInfos();
        AInfo.append("Agent Name / Agent ID：\r\n");
        for (String string : agentInfos.keySet()) {
            AInfo.append(string).append(":\r\n"); //AgentGroup
            for (AgentInfo info : agentInfos.get(string)) {
                count++;
                AInfo.append("\t").append(info.getAgentName()).append(" / ").append(info.getAgentId()).append("\r\n");
            }
        }
        text.append("エージェント数：").append(count).append("\r\n");
        

        
        txtar.append(text.toString());
        txtar.append(AInfo.toString());
    }

    /*
     * テキスト：OtherMachineボタンの処理
     */
    private void otherMachineInfo() {
        int i = 0;
        KeyValuePair<InetAddress, Integer> address;
        //ip[] asID[] machineSize をセットします
        setMachine();

        //返り値はint、配列の番号が返ってくると考えてよし。data[select]がIP
        int select = JOptionPane.showOptionDialog(this, "マシンを選択してください", "Choose　Machine",
                JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, ip, ip[0]);

        ReplyContentContainer content = new ReplyContentContainer
                (agent.getAgentID(), SystemAPI.getAgentSphereId(), "MachineInfo");

        StandardContentContainer recieveContent = agent.recieveMessage(asId[select], content);
        String string = recieveContent.getContent().toString();

        txtar.append("<OtherMachine>\r\n");
        txtar.append(string);
    }

    /*
     * テキスト：AgentGroupボタンの処理
     */
    private void agentGroup() {
        String string = null;
        String groupname = JOptionPane.showInputDialog(this, "調べたいAgentGroup名を入力してください");
        setMachine();
        txtar.append("<AgentGroup一覧>\r\n");
        txtar.append("AgentGroup名：" + groupname + "\r\n");
        if (groupname != null) {
            for (int i = 0; i < machineSize; i++) { //全てのマシンに確認　結果をtxtarに出力
                System.out.println("ip[i]/ myIP" + ip[i] + " =? " + myIP);
                if (ip[i].equals(myIP)) {//自分だったら
                    System.out.println("自分や！");
                    HashMap<String, List<AgentInfo>> agentInfos = AgentAPI.getAgentInfos();
                    for (String name : agentInfos.keySet()) {
                        if (name.equals(groupname)) {
                            for (AgentInfo info : agentInfos.get(name)) {
                                txtar.append("\t" + info.getAgentName() + " / " + info.getAgentId() + "\r\n");
                            }
                        }
                    }
                } else {
                    ReplyContentContainer content = new ReplyContentContainer(agent.getAgentID(), SystemAPI.getAgentSphereId(), "AgentGroup", groupname);
                    StandardContentContainer recieveContent = agent.recieveMessage(asId[i], content);
                    string = recieveContent.getContent().toString();
                }
                txtar.append(string+"\r\n");
            }
        }
    }

    /*
     * テキスト：MchineListボタンの処理
     */

    private void machineList() {
        StringBuffer text = new StringBuffer();
        text.append("<Machine一覧>\r\n");
        for (KeyValuePair<String, KeyValuePair<InetAddress, Integer>> info : NetworkAPI.getAddresses()) {
            text.append("AgentSphereID : ").append(info.getKey()).append("\r\n");
            text.append("IPAdress : ").append(info.getValue().getKey()).append("\r\n");
            text.append("Port : ").append(info.getValue().getValue()).append("\r\n");
            text.append("*************************\r\n");
        }
        txtar.append(text.toString());
    }

    /*
     * テキスト：Saveボタンの処理：txtareaの内容を名前を付けて保存
     */
    void TextSave() {
        int returnVal = fileChooser.showSaveDialog(this);
        try {
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                FileWriter fw = new FileWriter(fileChooser.getSelectedFile());
                fw.write(txtar.getText());
                fw.close();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    /*
     * イメージ：MachineInfoボタンの処理
     */

    private void imageMachineInfo() {
        int machineSize = NetworkAPI.getAddresses().size();
        String[] asId = new String[machineSize];
        String[] ip = new String[machineSize];
        int count = 0, i = 0, countAgent = 0;

        //ネットワークで繋がっているマシン情報を取ります
        for (KeyValuePair<String, KeyValuePair<InetAddress, Integer>> addresses : NetworkAPI.getAddresses()) {
            asId[count] = addresses.getKey().toString();
            ip[count] = addresses.getValue().getKey().toString();
            count++;
        }

        ReplyContentContainer content = new ReplyContentContainer
                (agent.getAgentID(), SystemAPI.getAgentSphereId(), "ImageMachineInfo");
        if (machineSize > 0) {
            for (i = 0; i < machineSize; i++) {//ここで描く
                txtarImage.append(ip[i] + "\r\n");
                //contentの受け取り
                StandardContentContainer recieveContent = agent.recieveMessage(asId[i], content);
                //Agentのtext情報表示？
                StringBuffer text = ((ImageMachineInfo) recieveContent.getContent()).getInfo();
                txtarImage.append(text.toString());
                text.setLength(0);
                countAgent = ((ImageMachineInfo) recieveContent.getContent()).getCountAgent();

                drawAgent(countAgent, i);
            }//for終わり。→次のマシンのエージェントの描画
            countAgent = 0;

        } else {//ネットワークに他のマシンがいなかったら自分のだけ表示します
            HashMap<String, List<AgentInfo>> agentInfos = AgentAPI.getAgentInfos();
            for (String string : agentInfos.keySet()) {
                txtarImage.append(string + ":\r\n");
                for (AgentInfo info : agentInfos.get(string)) {
                    countAgent++;
                    txtarImage.append("  " + info.getAgentName() + "\r\n");
                }
            }
            txtarImage.append("エージェント数：" + countAgent + "\r\n");
            drawAgent(countAgent, 0);
            countAgent = 0;
        }
    }

    public synchronized void agentMovement() {//動的情報を取りに行きます

        ArrayList[][] allLog = new ArrayList[machineSize][5];//ここ手動で数字変えなきゃいかんのか
        int countup = 0, i = 0, j = 0, countAgent = 0;

        setMachine();

        ReplyContentContainer content = new ReplyContentContainer(null, SystemAPI.getAgentSphereId(), "wantLog");

        if (machineSize > 0) {
            for (i = 0; i < machineSize; i++) {//ここでログ格納
                txtarImage.append(ip[i] + "\r\n");
                //contentの受け取り
                StandardContentContainer recieveContent = agent.recieveMessage(asId[i], content);
                System.out.println("Logありがとう！");
                ArrayList[] logList = (ArrayList[]) recieveContent.getContent();
                allLog[i] = logList;
            }//for終わり。→次のマシンのログ格納or描画
            //描画
            for (i = 0; i < machineSize; i++) {
                imageClear();
                //allLog[]はArrayList
                //System.out.println("allLog[i]を出力するよ！\n" + allLog[i]);
                for (j = 0; j < allLog[j].length; j++) {
                    countAgent = allLog[j][i].size();
                    System.out.println("Agentの数は" + countAgent + "個です！");

                    //allLog[j][i]はAgentLogが入ってるリスト
                    //リストを配列に変えて回すか？
                    //                   allLog[j][i].toArray();　.toArray()とはなんぞや？
                    //in outの判別をして、描画
                    drawAgent(countAgent, i);
                    countAgent = 0;

                    //描く毎に1秒wait
                    try {
                        wait(1000);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(MonitorFrame.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        }
    }

    private void separate() {
        txtar.append("* * * * * * * * *  情報更新 * * * * * * * * *");
        get_time();
    }
//時間取得&表示

    private void get_time() {
        Calendar cal1 = Calendar.getInstance();  //(1)オブジェクトの生成
        int year = cal1.get(Calendar.YEAR);        //(2)現在の年を取得
        int month = cal1.get(Calendar.MONTH) + 1;  //(3)現在の月を取得
        int day = cal1.get(Calendar.DATE);         //(4)現在の日を取得
        int hour = cal1.get(Calendar.HOUR_OF_DAY); //(5)現在の時を取得
        int minute = cal1.get(Calendar.MINUTE);    //(6)現在の分を取得
        int second = cal1.get(Calendar.SECOND);    //(7)現在の秒を取得
        txtar.append("(" + year + "/ " + month + "/ " + day + " " + hour + ":" + minute + ":" + second + ")\r\n");
    }

    private void imageClear() {
        for (int i = 0; i < 20; i++) {
            canvasList[i].clear();
        }
    }

    private void drawAgent(int countAgent, int i) {
        for (int l = 0; l < countAgent / 10; l++) {
            canvasList[i].draw("Agent10.jpg");
        }
        countAgent = countAgent % 10;
        for (int l = 0; l < countAgent / 5; l++) {
            canvasList[i].draw("Agent05.jpg");
        }
        countAgent = countAgent % 5;
        for (int l = 0; l < countAgent; l++) {
            canvasList[i].draw("Agent01.jpg");
        }
    }

    private void setMachine() {
        machineSize = NetworkAPI.getAddresses().size();
        int i = 0;
        ip = new String[machineSize];
        asId = new String[machineSize];

//        asId[]にはAgentSphereIDを、ipにはマシンのIPアドレスを入れる
        for (KeyValuePair<String, KeyValuePair<InetAddress, Integer>> x : NetworkAPI.getAddresses()) {
            asId[i] = x.getKey().toString();
            ip[i] = x.getValue().getKey().toString();
            System.out.println("ip/ AsId" + ip[i] + "  " + asId[i]);
            i++;
        }
    }

    public void setAgent(AgentMonitorAgent agent) {
        this.agent = agent;
    }
}

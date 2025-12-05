/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package primula.api.core.interim.application;

import java.awt.*;
import java.awt.event.*;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;
import primula.api.AgentAPI;
import primula.api.SystemAPI;
import primula.util.IPAddress;
import primula.util.KeyValuePair;

/**
 *
 * @author kurosaki
 */
class ApplicationFrame extends JFrame{
    public static boolean close = true;
    private JFrame jframe;
    private JPanel jpanel = new JPanel();
    private JLabel label = new JLabel();
    private JButton button = new JButton("send");
    private JTextArea send_area = new JTextArea(15, 25);//送信したいメッセージを書き込むテキストボックス
    public static JTextArea receive_area = new JTextArea(15, 25);//受信したメッセージを表示するテキストボックス
    private JMenuBar menubar = new JMenuBar();
    private JMenu menu1 = new JMenu("menu");
    private JMenuItem menu1_item = new JMenuItem("remote");
    
    public ApplicationFrame(String title){
        jframe = new JFrame(title);   
        JScrollPane send_scroll = new JScrollPane(send_area);
        JScrollPane receive_scroll = new JScrollPane(receive_area);
        /**
         * TODO:"send"ボタンを押すと送信先にテキストを送れるようにしたい
         */
        button.addActionListener(new java.awt.event.ActionListener() {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                StringBuilder buffer = new StringBuilder();
                String lineSeparator = System.getProperty("line.separator");
              
                buffer.append(SystemAPI.getAgentSphereId()).append(":").append(lineSeparator);
                buffer.append(send_area.getText()).append(lineSeparator);
                String text = new String(buffer);
                //label.setText(send_area.getText());//デバッグ用
                receive_area.append(text);
                MessageSendAgent agent = new MessageSendAgent(text);
                AgentAPI.runAgent(agent);
                send_area.setText(null);
            }
        });
        
        //"remote"を選択すると送信先でDesktopAgentを起動させる
        menu1_item.addActionListener(new java.awt.event.ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                /**
                 * TODO:マシンリストから送信先を選択できるようにしたい。
                 *      今は指定先固定。
                 */
                DesktopAgent agent = new DesktopAgent();
                try{
                    KeyValuePair<InetAddress, Integer> address = new KeyValuePair<InetAddress, Integer>(Inet4Address.getByName(IPAddress.IPAddress), 55878);
                    AgentAPI.migration(address, agent);
                } catch(UnknownHostException ex){
                    Logger.getLogger(ApplicationFrame.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });
        
        menu1.add(menu1_item);
        menubar.add(menu1);
        jframe.setJMenuBar(menubar);
        jpanel.add(send_scroll);
        jpanel.add(button);
        jpanel.add(receive_scroll);
        jpanel.add(label);
        jframe.add(jpanel, BorderLayout.WEST);
        

        jframe.addWindowListener(new WindowAdapter(){
            @Override
            public void windowClosing(WindowEvent we){
                close = false;
                setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            }
        });
        jframe.setVisible(true);
        Insets insets = jframe.getInsets();
        jframe.setSize(650 + insets.left + insets.right, 400 + insets.top + insets.bottom);
        jframe.setLocationRelativeTo(null);
    }
}

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package primula.api.core.interim.monitor;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
/**
 * AgentMonitorのイメージエリアとその操作
 * @author onda
 */
public class AgentCanvas extends Canvas {

    int x1 = 0, x5 = 0, x10 = 0, width = 0, height = 0;
    int count=0;
    static int y1 = 0, y5 = 20, y10 = 50;
    BufferedImage image;
    Image img;
    boolean first = true;

    AgentCanvas(int width, int height) {
        this.width = width;
        this.height = height;
    }

    @Override
    public void paint(Graphics g) {
        if (first) {
            image = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
            image.getGraphics().setColor(Color.white);
            image.getGraphics().fillRect(0, 0, image.getWidth(), image.getHeight());
            first = false;
        }
        g.drawImage(image, 0, 0, this);
    }

    public void clear() {
        first = true;
        x1 = 0;
        x5 = 0;
        x10 = 0;
        paint(getGraphics());
    }

    public void draw(String fileName) {
        try {
            FileInputStream input = new FileInputStream(fileName);
            ImageInputStream iis = ImageIO.createImageInputStream(input);
            image = ImageIO.read(iis);
        } catch (IOException e) {
        }
        if (fileName.equals("Agent01.jpg")) {
            getGraphics().drawImage(image, x1, y1, this);
            x1 = x1 + 20;
        } else if (fileName.equals("Agent05.jpg")) {
            getGraphics().drawImage(image, x5, y5, this);
            x5 = x5 + 30;
        } else if (fileName.equals("Agent10.jpg")) {
            getGraphics().drawImage(image, x10, y10, this);
            x10 = x10 + 40;
        } else if (fileName.equals("ShellAgentImage.jpg")){
            getGraphics().drawImage(image, x1, y1, this);
            x1 = x1 + 20;
            count++;
        } else if (fileName.equals("StationaryAgentImage.jpg")){
            getGraphics().drawImage(image, x1, y1, this);
            x1 = x1 + 20;
            count++;
        } else if (fileName.equals("AgentMonitorAgentImage.jpg")){
            getGraphics().drawImage(image, x1, y1, this);
            x1 = x1 + 20;
            count++;
        } else if (fileName.equals("MessengerAgentImage.jpg")){
            getGraphics().drawImage(image, x1, y1, this);
            x1 = x1 + 20;
            count++;
        }else if (fileName.equals("GetNodeAgentImage.jpg")){
            getGraphics().drawImage(image, x1, y1, this);
            x1 = x1 + 20;
            count++;
        }else if (fileName.equals("ClearAgentImage.jpg")){
            getGraphics().drawImage(image, x1, y1, this);
            x1 = x1 + 20;
        }
        if(count==5){
            x1=0;
            y1 = y1 + 20;
            count=0;
        }
    }

    public void save(String fileName) {
        try {
            FileOutputStream output = new FileOutputStream(fileName);
            ImageOutputStream ios = ImageIO.createImageOutputStream(output);
            ImageIO.write(image,"JPEG",ios);
            output.flush();
            output.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void savedraw(String canvasNameList[]) {
        int x = 0, y = 0, count = 0;
        for (String fileName : canvasNameList) {
            try {
                FileInputStream input = new FileInputStream(fileName);
                ImageInputStream iis = ImageIO.createImageInputStream(input);
                image = ImageIO.read(iis);
            } catch (IOException e) {
            }
            getGraphics().drawImage(image, x, y, this);
            if (count >= 4) {
                y = y + 100;
                x = 0;
            } else {
                x = x + 100;
            }
            count++;
        }
        save("AgentMonitorImage.jpg");
    }

    public Image getSeparateImage() {
        try {
            FileInputStream input = new FileInputStream("separate.jpg");
            ImageInputStream iis = ImageIO.createImageInputStream(input);
            image = ImageIO.read(iis);
        } catch (IOException e) {
        }
        return image;
    }
}

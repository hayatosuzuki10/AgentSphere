/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package primula.api.core.agent.loader;


import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JFileChooser;
import javax.swing.JFrame;

import primula.agent.AbstractAgent;
import primula.api.core.agent.loader.UnknownObjectStream.ObjectIO;
import primula.api.core.agent.loader.multiloader.LocalFileClassLoader;
import primula.api.core.agent.loader.multiloader.StringSelector;
import primula.api.core.interim.shell.ShellEnvironment;

 
/**
 *
 * @author kousuke
 */
public class LocalClassLoaderCaller extends JFrame {
    
    JFileChooser jfc;
    File file;
    File[] fileList;
    JFrame parent = null;
    LocalFileClassLoader lfcl;
    ObjectIO io;
    StringSelector ss;
    Class<?> cl;
    HashMap<String ,Object> obj = new HashMap<String, Object>();
    
    public LocalClassLoaderCaller(JFileChooser jfc, File[] pass) {
        io = new ObjectIO();
        this.jfc = jfc;
        parent.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    }



    
    public LocalClassLoaderCaller(ShellEnvironment se){
        jfc= new JFileChooser(se.getDirectory());
        jfc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        }
    
    public void selectDirectory(){
       jfc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        if(jfc.showOpenDialog(parent)==JFileChooser.APPROVE_OPTION)
         {
          file = jfc.getSelectedFile();
          fileList=file.listFiles();
            try {
                lfcl = new LocalFileClassLoader(file);
            } catch (IOException ex) {
                Logger.getLogger(LocalClassLoaderCaller.class.getName()).log(Level.SEVERE, null, ex);
            }
          
          for(int i=0;i<=fileList.length;i++){

             recursiveClassSearch(fileList[i]);
          }
         }
        }

    
    private void recursiveClassSearch(File Directory){
       File[] list = Directory.listFiles();
       
       System.out.println(list.length);
        
       for(int i=0;i<=list.length;i++){
           if(list[i].isDirectory()){
              System.out.println(list[i].getName());
               recursiveClassSearch(list[i]);
           }
           else{

                try {
                           System.out.println(list[i].getName());
                    cl=lfcl.loadClass(list[i].getAbsolutePath());
                    
                } catch (ClassNotFoundException ex) {
                    Logger.getLogger(LocalClassLoaderCaller.class.getName()).log(Level.SEVERE, null, ex);
                }
                try {
                    Object obj = cl.newInstance();
                    Method mtd = cl.getMethod("runAgent");
                    System.out.println("1");
                    mtd.invoke(obj);
                    System.out.println("2");
                } catch (IllegalArgumentException ex) {
                    Logger.getLogger(LocalClassLoaderCaller.class.getName()).log(Level.SEVERE, null, ex);
                } catch (InvocationTargetException ex) {
                    Logger.getLogger(LocalClassLoaderCaller.class.getName()).log(Level.SEVERE, null, ex);
                } catch (NoSuchMethodException ex) {
                    Logger.getLogger(LocalClassLoaderCaller.class.getName()).log(Level.SEVERE, null, ex);
                } catch (SecurityException ex) {
                    Logger.getLogger(LocalClassLoaderCaller.class.getName()).log(Level.SEVERE, null, ex);
                } catch (InstantiationException ex) {
                    Logger.getLogger(LocalClassLoaderCaller.class.getName()).log(Level.SEVERE, null, ex);
                } catch (IllegalAccessException ex) {
                    Logger.getLogger(LocalClassLoaderCaller.class.getName()).log(Level.SEVERE, null, ex);
                }
           }
       }
        
    }
    public void LoadAgent(){
     File file=null;
     byte[] binary;
     AbstractAgent aa;
//
//      jfc.setFileSelectionMode(JFileChooser.FILES_ONLY);
//      
//      if(jfc.showOpenDialog(parent)==JFileChooser.APPROVE_OPTION)
//          file=jfc.getSelectedFile();
//      else 
//          System.out.println("No such File");
//      io = new ObjectIO();
//        try {
//            aa = (AbstractAgent) io.getObject(file);
//        } catch (IOException ex) {
//            Logger.getLogger(LocalClassLoaderCaller.class.getName()).log(Level.SEVERE, null, ex);
//        } catch (ClassNotFoundException ex) {
//            Logger.getLogger(LocalClassLoaderCaller.class.getName()).log(Level.SEVERE, null, ex);
//        }

     
    }    
    
}

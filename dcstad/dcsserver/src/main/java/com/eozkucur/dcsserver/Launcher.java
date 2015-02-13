
package com.eozkucur.dcsserver;

import java.awt.AWTException;
import java.awt.CheckboxMenuItem;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.net.URL;
import java.util.ArrayList;
import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

public class Launcher {

   TrayIcon trayIcon = null;

   ArrayList<Image> images = new ArrayList<Image>();

   boolean serverRunning = false;

   boolean tadRunning = false;

   public void Launcher() {
   }

   public void setupGui() {
      try {
         //UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
         UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");
      } catch (UnsupportedLookAndFeelException ex) {
         ex.printStackTrace();
      } catch (IllegalAccessException ex) {
         ex.printStackTrace();
      } catch (InstantiationException ex) {
         ex.printStackTrace();
      } catch (ClassNotFoundException ex) {
         ex.printStackTrace();
      }
      UIManager.put("swing.boldMetal", Boolean.FALSE);
      if (!SystemTray.isSupported()) {
         System.out.println("SystemTray is not supported");
         return;
      }
      final Launcher launcher = new Launcher();
      final PopupMenu popup = new PopupMenu();
      images.add(createImage("/images/state00.png", "tray icon 00"));
      images.add(createImage("/images/state10.png", "tray icon 10"));
      images.add(createImage("/images/state01.png", "tray icon 01"));
      images.add(createImage("/images/state11.png", "tray icon 11"));
      trayIcon =
          new TrayIcon(images.get(0));
      final SystemTray tray = SystemTray.getSystemTray();

      MenuItem aboutItem = new MenuItem("About");
      CheckboxMenuItem cb1 = new CheckboxMenuItem("Start Server");
      CheckboxMenuItem cb2 = new CheckboxMenuItem("Start TAD");
      MenuItem exitItem = new MenuItem("Exit");

      popup.add(cb1);
      popup.add(cb2);

      popup.addSeparator();
      popup.add(aboutItem);
      popup.addSeparator();
      popup.add(exitItem);

      trayIcon.setPopupMenu(popup);

      try {
         tray.add(trayIcon);
      } catch (AWTException e) {
         System.out.println("TrayIcon could not be added.");
         return;
      }

      trayIcon.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            //TODO disp  message about usage
         }
      });

      aboutItem.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            aboutClicked();
         }
      });

      cb1.addItemListener(new ItemListener() {
         public void itemStateChanged(ItemEvent e) {
            int cb1Id = e.getStateChange();
            if (cb1Id == ItemEvent.SELECTED) {
               startServer();
            } else {
               stopServer();
            }
         }
      });

      cb2.addItemListener(new ItemListener() {
         public void itemStateChanged(ItemEvent e) {
            int cb2Id = e.getStateChange();
            if (cb2Id == ItemEvent.SELECTED) {
               startTad();
            } else {
               stopTad();
            }
         }
      });

      exitItem.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            tray.remove(trayIcon);
            stopServer();
            stopTad();
            System.exit(0);
         }
      });
   }

   private void stopTad() {
      System.out.println("Stop Tad");
   }

   private void startTad() {
      System.out.println("Start Tad");
   }

   private void stopServer() {
      System.out.println("Stop Serve");
   }

   private void startServer() {
      System.out.println("Start Server");
   }

   private void aboutClicked() {
      System.out.println("About Clicked");
   }

   private Image createImage(String path, String description) {
      URL imageURL = Launcher.class.getResource(path);

      if (imageURL == null) {
         System.err.println("Resource not found: " + path);
         return null;
      } else {
         return (new ImageIcon(imageURL, description)).getImage();
      }
   }

   public static void main(String[] args) {

      final Launcher demo = new Launcher();
      SwingUtilities.invokeLater(new Runnable() {
         public void run() {
            demo.setupGui();
         }
      });
      int imageIndx = 0;
      while (true) {
         if (demo.trayIcon != null && demo.images.size() > 0) {
            demo.trayIcon.setImage(demo.images.get(imageIndx));
            imageIndx++;
            if (imageIndx == demo.images.size()) {
               imageIndx = 0;
            }
         }
         try {
            Thread.sleep(1000);
         } catch (InterruptedException e) {
            e.printStackTrace();
         }
      }
   }
}

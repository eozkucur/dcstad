
package com.eozkucur.dcsserver;

import com.eozkucur.dcsnet.AirObject;
import com.eozkucur.dcsnet.AircraftState;
import com.eozkucur.dcsnet.DcsNet;
import com.eozkucur.dcsnet.DcsNetListener;
import com.eozkucur.dcsnet.Point;
import com.eozkucur.dcsnet.Waypoint;
import java.awt.AWTException;
import java.awt.BasicStroke;
import java.awt.CheckboxMenuItem;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.Rectangle;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.net.URL;
import java.util.ArrayList;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

public class Launcher extends JPanel implements DcsNetListener {

   TrayIcon trayIcon = null;

   ArrayList<Image> images = new ArrayList<Image>();

   JFrame tadFrame;

   AircraftState stateRaw = new AircraftState();

   AircraftState state = new AircraftState();

   DcsNet net = new DcsNet(5555, 6666, stateRaw);

   boolean connected = false;

   boolean serverRunning = false;

   boolean tadRunning = false;

   GeneralPath aircraftDrawPath;
   GeneralPath northDrawPath;

   double mileScale = 10;

   int focusedObject = -1;

   private int startx;
   private int starty;

   private Rectangle startRect;

   public Launcher() {
      System.out.println("constructing launcher");
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
      final PopupMenu popup = new PopupMenu();
      images.add(createImage("/images/state00.png", "tray icon 00"));
      images.add(createImage("/images/state10.png", "tray icon 10"));
      images.add(createImage("/images/state01.png", "tray icon 01"));
      images.add(createImage("/images/state11.png", "tray icon 11"));
      trayIcon =
          new TrayIcon(images.get(0));
      trayIcon.setImageAutoSize(true);
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
            System.out.println("tray action");
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
            net.shutdown();
            System.exit(0);
         }
      });

      tadFrame = new JFrame("TAD Window");
      tadFrame.setUndecorated(true);
      tadFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
      //setBorder(BorderFactory.createLineBorder(Color.black));
      GridLayout layout = new GridLayout(1, 1);
      layout.setHgap(0);
      layout.setVgap(0);
      tadFrame.getContentPane().setLayout(layout);
      tadFrame.add(this);
      tadFrame.pack();
      //frame.setVisible(true);
      tadFrame.setAlwaysOnTop(true);
      //BoxLayout layout=new BoxLayout(tadFrame);

      net.addListener(this);

      aircraftDrawPath = new GeneralPath(GeneralPath.WIND_NON_ZERO, 3);
      aircraftDrawPath.moveTo(-1f, -0.6f);
      aircraftDrawPath.lineTo(-1f, 0.6f);
      aircraftDrawPath.lineTo(1f, 0f);
      aircraftDrawPath.closePath();

      northDrawPath = new GeneralPath(GeneralPath.WIND_NON_ZERO, 3);
      //0.866 sqrt(3)/2
      northDrawPath.moveTo(0f, 0.2f);
      northDrawPath.lineTo(0f, -0.2f);
      northDrawPath.lineTo(-0.866 * 0.4f,0f);
      northDrawPath.closePath();

      this.addMouseListener(new MouseListener() {
         @Override
         public void mouseClicked(MouseEvent e) {
            if (e.getButton() == MouseEvent.BUTTON1) {
               System.out.println("Changing scale");
               mileScale *= 2;
               if (mileScale > 160) {
                  mileScale = 10;
               }
               Launcher.this.repaint();
            } else if (e.getButton() == MouseEvent.BUTTON3) {
               System.out.println("Changing focus");
               focusedObject++;
               if (focusedObject == state.waypoints.size()) {
                  focusedObject = -1;
               }
               Launcher.this.repaint();
            }
         }

         @Override
         public void mousePressed(MouseEvent e) {
            startx=e.getX();
            starty=e.getY();
            startRect=tadFrame.getBounds();
         }

         @Override
         public void mouseReleased(MouseEvent e) {

         }

         @Override
         public void mouseEntered(MouseEvent e) {

         }

         @Override
         public void mouseExited(MouseEvent e) {

         }
      });

      this.addMouseMotionListener(new MouseMotionListener() {
         @Override
         public void mouseDragged(MouseEvent e) {
            if(e.getButton()==MouseEvent.BUTTON1) {
               tadFrame.setLocation(e.getX() - startx + tadFrame.getLocation().x,
                                    e.getY() - starty + tadFrame.getLocation().y);
            }else if(e.getButton()==MouseEvent.BUTTON3){
               Rectangle r=tadFrame.getBounds();
               if(r.getHeight()>40&&r.getWidth()>40) {
                  tadFrame.setBounds(startRect.x,startRect.y,startRect.width+e.getX()-startx,startRect.height+e.getX()-startx);
               }
            }
         }

         @Override
         public void mouseMoved(MouseEvent e) {

         }
      });


      //state.pos.x=10;
      //state.pos.y=5;
      //state.bearing=15;
      //Waypoint wp=new Waypoint();
      //wp.id=1;
      //wp.pos.x=-5;
      //wp.pos.y=17;
      //state.waypoints.add(wp);
      //wp=new Waypoint();
      //wp.id=2;
      //wp.pos.x=-2;
      //wp.pos.y=-7;
      //state.waypoints.add(wp);
   }

   public Dimension getPreferredSize() {
      return new Dimension(500, 500);
   }

   public void paintComponent(Graphics g) {
      //System.out.println("Repaint " + this.getWidth() + " " + this.getHeight());
      super.paintComponent(g);
      Graphics2D g2 = (Graphics2D) g;
      g2.translate(this.getWidth() / 2, this.getHeight() / 2);
      g2.setColor(Color.BLACK);
      float margin = 20;
      g2.fill(new Rectangle2D.Float(-2*getWidth(), -2*getHeight(), 4*getWidth(), 4*getHeight()));
      if (this.getHeight() > this.getWidth()) {
         g2.scale((this.getWidth() - margin) / 2, (this.getWidth() - margin) / 2);
         g2.setStroke(new BasicStroke(1.0f / this.getWidth()));
      } else {
         g2.scale((this.getHeight() - margin) / 2, (this.getHeight() - margin) / 2);
         g2.setStroke(new BasicStroke(1.0f / this.getHeight()));
      }

      if (focusedObject >= state.waypoints.size()) {
         focusedObject = -1;
      }

      g2.setColor(Color.GRAY);
      g2.draw(new Ellipse2D.Double(-1,-1,2,2));
      g2.draw(new Ellipse2D.Double(-0.5,-0.5,1,1));
      g2.setColor(Color.BLUE);
      AffineTransform trans2=g2.getTransform();

      //double aircraftBearing=  Math.toRadians(-state.bearing+90);
      float halfpi= (float) (Math.PI/2);
      double aircraftBearing=  -state.bearing+halfpi;
      double aircraftScale=0.05f;
      if (focusedObject == -1) {
         g2.rotate(aircraftBearing-Math.PI/2);
         g2.translate(-state.pos.x / mileScale, state.pos.y / mileScale);
      } else {
         g2.translate(-state.waypoints.get(focusedObject).pos.x / mileScale, state.waypoints.get(focusedObject).pos.y / mileScale);
      }
      AffineTransform trans=g2.getTransform();
      g2.translate(state.pos.x / mileScale, -state.pos.y / mileScale);
      g2.rotate(-aircraftBearing);
      g2.scale(aircraftScale, aircraftScale);
      g2.fill(aircraftDrawPath);
      g2.setTransform(trans);

      for(AirObject ao:state.airObjects){
         AffineTransform trans3=g2.getTransform();
         if(ao.groupId==0){
            g2.setColor(Color.CYAN);
         }else{
            g2.setColor(Color.GREEN);
         }
         g2.translate(ao.pos.x / mileScale, -ao.pos.y / mileScale);
         g2.rotate(ao.bearing-halfpi);
         g2.scale(aircraftScale/2, aircraftScale/2);
         g2.fill(aircraftDrawPath);
         g2.setTransform(trans3);
      }
      g2.setColor(Color.BLUE);


      FontMetrics fm = g2.getFontMetrics();
      double strheight = fm.getHeight();
      double wpscale = 0.05f;

      for(Waypoint wp : state.waypoints) {
         trans = g2.getTransform();
         g2.translate((wp.pos.x/mileScale), -(wp.pos.y/mileScale));
         g2.scale(wpscale, wpscale);
         g2.draw(new Ellipse2D.Double(-1, -1, 2, 2));
         g2.scale(1 / strheight, 1 / strheight);
         if (focusedObject == -1) {
            g2.rotate(-aircraftBearing+halfpi);
         }
         double strwidth = fm.stringWidth(""+wp.id);
         g2.translate(-strwidth, -(strheight - 2 * fm.getAscent()));
         g2.scale(2, 2);
         if(state.selectedwp==wp.id){
            g2.setColor(Color.WHITE);
         }else{
            g2.setColor(Color.GREEN);
         }
         g2.drawString(""+wp.id, 0, 0);
         g2.setTransform(trans);
         g2.setColor(Color.BLUE);
      }
      ArrayList<Waypoint> sortedWps=new ArrayList<Waypoint>();
      int prevMinIndx=-1;
      for(int i=0;i<state.waypoints.size();i++){
         int minId=999;
         Waypoint minWp=null;
         for(int j=0;j<state.waypoints.size();j++) {
            if (state.waypoints.get(j).id>prevMinIndx&&state.waypoints.get(j).id<minId){
               minWp=state.waypoints.get(j);
               minId=minWp.id;
            }
         }
         sortedWps.add(minWp);
         prevMinIndx=minWp.id;
      }
      for(int i=1;i<sortedWps.size();i++){
         g2.draw(new Line2D.Double(sortedWps.get(i-1).pos.x/mileScale,-sortedWps.get(i-1).pos.y/mileScale,sortedWps.get(i).pos.x/mileScale,-sortedWps.get(i).pos.y/mileScale));
      }
      if(sortedWps.size()>=2){
         g2.draw(new Line2D.Double(sortedWps.get(sortedWps.size()-1).pos.x/mileScale,-sortedWps.get(sortedWps.size()-1).pos.y/mileScale,sortedWps.get(0).pos.x/mileScale,-sortedWps.get(0).pos.y/mileScale));
      }

      g2.setColor(Color.WHITE);
      Point northPoint=new Point(0, (float) (mileScale/2));
      if(focusedObject==-1){
         northPoint.x+=state.pos.x;
         northPoint.y+=state.pos.y;
      }else{
         northPoint.x+=state.waypoints.get(focusedObject).pos.x;
         northPoint.y+=state.waypoints.get(focusedObject).pos.y;
      }
      trans = g2.getTransform();
      g2.translate((northPoint.x/mileScale), -(northPoint.y/mileScale));
      g2.rotate(halfpi);
      g2.scale(wpscale*2, wpscale*2);
      g2.fill(northDrawPath);
      g2.setTransform(trans);

      g2.setTransform(trans2);
      if(connected){
         g2.setColor(Color.GREEN);
      }else{
         g2.setColor(Color.RED);
      }
      g2.translate(0.95, -0.95);
      g2.scale(wpscale, wpscale);
      g2.scale(1 / strheight, 1 / strheight);
      double strwidth = fm.stringWidth(""+(int)mileScale);
      g2.translate(-strwidth, -(strheight - 2 * fm.getAscent()));
      g2.scale(2, 2);
      g2.drawString(""+(int)mileScale, 0, 0);

   }

   @Override
   public void dataChanged() {
      //System.out.println("Data changed");
      //System.out.print("State: ");
      //System.out.print(state.pos.x + ",");
      //System.out.print(state.pos.y + ",");
      //System.out.print(state.bearing + " wp count: ");
      //System.out.print(state.waypoints.size() + ": ");
      //for (int i = 0; i < state.waypoints.size(); i++) {
      //   Waypoint wp = state.waypoints.get(i);
      //   System.out.print(wp.pos.x + ",");
      //   System.out.print(wp.pos.y + ",");
      //   System.out.print(wp.id + " ");
      //}
      //System.out.println();
      AircraftState.convertMiles(stateRaw, state);
      //state=stateRaw;
      this.repaint();
   }

   @Override
   public void connectionStateChanged(boolean isConnected) {
      System.out.println("connection " + isConnected);
      connected = isConnected;
	  this.repaint();
   }

   private void stopTad() {

      System.out.println("Stop Tad");
      tadFrame.setVisible(false);
      tadRunning = false;
      if (!serverRunning) {
         net.stopClient();
      }
      System.out.println("Tad stopped");
      if (serverRunning) {
         trayIcon.setImage(images.get(2));
      } else {
         trayIcon.setImage(images.get(0));
      }
   }

   private void startTad() {
      System.out.println("Start Tad");
      tadFrame.setVisible(true);
      tadRunning = true;
      net.startClient(false);
      System.out.println("Stopped Tad");
      if (serverRunning) {
         trayIcon.setImage(images.get(3));
      } else {
         trayIcon.setImage(images.get(1));
      }
   }

   private void stopServer() {
      System.out.println("Stop Serve");
      serverRunning = false;
      net.stopServer();
      System.out.println("Stopped Serve");
      if (tadRunning) {
         trayIcon.setImage(images.get(1));
      } else {
         net.stopClient();
         trayIcon.setImage(images.get(0));
      }
   }

   private void startServer() {
      System.out.println("Start Server");
      serverRunning = true;
      net.startServer();
      System.out.println("Started Server");
      if (tadRunning) {
         trayIcon.setImage(images.get(3));
      } else {
         net.startClient(false);
         trayIcon.setImage(images.get(2));
      }
   }

   private void aboutClicked() {
      System.out.println("About Clicked");
      JOptionPane.showMessageDialog(null,
                                    "TAD for DCS.",
                                    "About",
                                    JOptionPane.PLAIN_MESSAGE);
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

   public static void setupGui() {
      Launcher l = new Launcher();
   }

   public static void main(String[] args) {
      SwingUtilities.invokeLater(new Runnable() {
         public void run() {
            setupGui();
         }
      });
   }
}

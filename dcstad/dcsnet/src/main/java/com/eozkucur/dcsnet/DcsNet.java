package com.eozkucur.dcsnet;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;
import org.msgpack.MessagePack;
import org.msgpack.unpacker.Unpacker;

public class DcsNet implements Runnable, ServiceListener {

   int clientListenPort;

   int servingPort;

   Thread thread;

   boolean clientEnabled = false;

   boolean discover = false;

   boolean serverEnabled = false;

   DatagramSocket clientSocket;

   Socket clientCommandSocket;

   byte[] recBuff = new byte[1024];

   int recBuffSize;

   DatagramPacket receivedPacket = new DatagramPacket(recBuff, recBuff.length);

   ArrayList<DcsNetListener> listeners = new ArrayList<DcsNetListener>();

   int timeoutDur = 100;

   public String serverAddress = "0";

   String discoverString = "_dcsnet._tcp.local.";

   String clientAddress = null;

   ServerSocket serverCommandSocket = null;

   DatagramSocket serverSendSocket = null;

   JmDNS jmdnServer = null;

   JmDNS jmdnClient = null;

   Thread serverThread;

   Thread clientThread;

   boolean shutdownAction;

   boolean clientSocketReady=false;

   MessagePack msgpack = new MessagePack();
   ByteArrayInputStream inBuffStream = new ByteArrayInputStream(recBuff);
   Unpacker unpacker=msgpack.createUnpacker(inBuffStream);

   AircraftState state;

   InetAddress jmdnsaddr;

   public DcsNet(int localListenPort, int servingPort,AircraftState state) {
      this.state=state;
      this.clientListenPort = localListenPort;
      this.servingPort = servingPort;
      thread = new Thread(this);
      thread.start();
   }

   public void setJmdnsaddr(InetAddress jmdnsaddr) {
      this.jmdnsaddr = jmdnsaddr;
   }

   public void addListener(DcsNetListener listener) {
      listeners.add(listener);
   }

   public void removeListener(DcsNetListener listener) {
      listeners.remove(listener);
   }

   public void startClient(boolean discover) {
      if(clientThread != null){
         return;
      }
      this.discover = discover;
      if (!discover) {
         this.serverAddress = "127.0.0.1";
         //this.serverAddress = "192.168.1.50";
      }
      synchronized (this) {
         clientEnabled = true;
         notifyAll();
      }
      clientThread = new Thread(new Runnable() {
         @Override
         public void run() {
            if (DcsNet.this.discover && jmdnClient == null) {
               try {
                  if(jmdnsaddr!=null) {
                     jmdnClient = JmDNS.create(jmdnsaddr);
                  }else{
                     jmdnClient = JmDNS.create();
                  }
               } catch (IOException e) {
                  e.printStackTrace();
               }
               jmdnClient.addServiceListener(discoverString, DcsNet.this);
            }
            clientCommandSocket = null;
            boolean clEnabled = clientEnabled;
            System.out.println("Waiting for a server");
            while (clEnabled) {
               try {
                  clientCommandSocket = new Socket(serverAddress, clientListenPort + 1);
                  System.out.println("Connected to server");
                  if (clientSocket == null) {
                     clientSocket = new DatagramSocket(clientListenPort);
                     synchronized (DcsNet.this){
                        clientSocketReady=true;
                        DcsNet.this.notifyAll();
                     }
                     for(int i=0;i<listeners.size();i++){
                        listeners.get(i).connectionStateChanged(true);
                     }
                     //clientSocket.setSoTimeout(timeoutDur);
                  }
                  clientCommandSocket.getInputStream().read();
                  clientCommandSocket.close();
                  clientCommandSocket = null;
               } catch (IOException e) {
                  //e.printStackTrace();
                  clientCommandSocket = null;
                  if (clientSocket != null) {
                     synchronized (DcsNet.this){
                        clientSocketReady=false;
                     }
                     clientSocket.close();
                     clientSocket = null;
                     for(int i=0;i<listeners.size();i++){
                        listeners.get(i).connectionStateChanged(false);
                     }
                  }
                  if (clientEnabled) {
                     try {
                        Thread.sleep(5 * timeoutDur);
                     } catch (InterruptedException e1) {
                        e1.printStackTrace();
                     }
                  }
               }
               synchronized (DcsNet.this) {
                  clEnabled = clientEnabled;
               }
            }
            if (clientSocket != null) {
               synchronized (DcsNet.this){
                  clientSocketReady=false;
               }
               clientSocket.close();
               clientSocket = null;
            }
            if (jmdnClient != null) {
               jmdnClient.removeServiceListener(discoverString, DcsNet.this);
               try {
                  jmdnClient.close();
               } catch (IOException e) {
                  e.printStackTrace();
               }
               jmdnClient = null;
            }
         }
      });
      clientThread.start();

   }

   public void stopClient() {

      synchronized (this) {
         clientEnabled = false;
      }

      if (clientCommandSocket != null) {
         try {
            clientCommandSocket.close();
         } catch (IOException e) {
            e.printStackTrace();
         }
         clientCommandSocket = null;
      }
      if (clientSocket != null) {
         clientSocket.close();
         clientSocket = null;
      }

      try {
         if (clientThread != null) {
            clientThread.join();
            clientThread = null;
         }
      } catch (InterruptedException e) {
         e.printStackTrace();
      }
   }

   public void startServer() {
      if (serverThread != null) {
         return;
      }
      synchronized (this) {
         serverEnabled = true;
         this.notifyAll();
      }
      try {
         serverCommandSocket = new ServerSocket(servingPort + 1);
         serverThread = new Thread(new Runnable() {
            @Override
            public void run() {
               Socket clientSocket = null;
               boolean srEnabled = serverEnabled;
               while (srEnabled) {
                  try {
                     System.out.println("Waiting for a client");
                     clientAddress = null;
                     clientSocket = serverCommandSocket.accept();
                     clientAddress = ((InetSocketAddress) (clientSocket.getRemoteSocketAddress())).getHostName();
                     System.out.println("Serving for a client");
                     serverSendSocket = new DatagramSocket(0);
                     clientSocket.getInputStream().read();
                     clientSocket.close();
                     clientSocket = null;
                  } catch (IOException e) {
                     //e.printStackTrace();
                     if (serverSendSocket != null) {
                        serverSendSocket.close();
                        serverSendSocket = null;
                     }
                  }
                  if (clientSocket != null) {
                     try {
                        clientSocket.close();
                     } catch (IOException e) {
                        e.printStackTrace();
                     }
                     clientSocket = null;
                  }
                  clientAddress = null;
                  synchronized (DcsNet.this) {
                     srEnabled = serverEnabled;
                  }
               }
               if (serverSendSocket != null) {

                  serverSendSocket.close();
                  serverSendSocket = null;
               }
            }
         });
         serverThread.start();
         if (jmdnServer == null) {
            jmdnServer = JmDNS.create();
            jmdnServer.registerService(ServiceInfo.create(discoverString, "dcstad", servingPort, "description"));
         }
      } catch (IOException e) {
         e.printStackTrace();
      }
   }

   public void stopServer() {

      synchronized (this) {
         serverEnabled = false;
      }
      if (serverSendSocket != null) {
         serverSendSocket.close();
         serverSendSocket = null;
      }
      if (serverCommandSocket != null) {
         try {
            serverCommandSocket.close();
         } catch (IOException e) {
            e.printStackTrace();
         }
         serverCommandSocket = null;
         clientAddress = null;
      }
      if (jmdnServer != null) {
         jmdnServer.unregisterAllServices();
         try {
            jmdnServer.close();
         } catch (IOException e) {
            e.printStackTrace();
         }
         jmdnServer = null;
      }
      try {
         if (serverThread != null) {
            serverThread.join();
            serverThread = null;
         }
      } catch (InterruptedException e) {
         e.printStackTrace();
      }
   }

   public void shutdown() {
      System.out.println("Shutting down");
      System.out.println("Stopping server");
      this.stopServer();
      System.out.println("Stopping client");
      this.stopClient();
      listeners.clear();
      System.out.println("Notify and join");
      synchronized (this) {
         shutdownAction = true;
         this.notifyAll();
      }
      try {
         thread.join();
      } catch (InterruptedException e) {
         e.printStackTrace();
      }
   }

   @Override
   public void run() {
      while (!shutdownAction) {
         if (clientSocketReady) {
            recBuffSize = 0;
            if (clientSocket != null) {
               try {
                  receivedPacket.setData(recBuff, 0, 1024);
                  clientSocket.receive(receivedPacket);
                  recBuffSize = receivedPacket.getLength();
                  if (recBuffSize == 0) {
                     System.out.println("Invalid packet");
                  }
               } catch (IOException e) {
                  //e.printStackTrace();
                  try {
                     Thread.sleep(timeoutDur);
                  } catch (InterruptedException e1) {
                     e1.printStackTrace();
                  }
               }
            } else {
               try {
                  Thread.sleep(timeoutDur);
               } catch (InterruptedException e) {
                  e.printStackTrace();
               }
            }
            if (recBuffSize > 0) {
               inBuffStream.reset();
               try {
                  state.pos.x=unpacker.readFloat();
                  state.pos.y=unpacker.readFloat();
                  state.bearing=unpacker.readFloat();
                  int wpSize=unpacker.readInt();
                  while(state.waypoints.size()>wpSize){
                     state.waypoints.remove(state.waypoints.size()-1);
                  }
                  while(state.waypoints.size()<wpSize){
                     state.waypoints.add(new Waypoint());
                  }
                  for(int i=0;i<wpSize;i++){
                     state.waypoints.get(i).pos.x=unpacker.readFloat();
                     state.waypoints.get(i).pos.y=unpacker.readFloat();
                     state.waypoints.get(i).id=unpacker.readInt();
                  }
                  state.selectedwp=unpacker.readInt();

                  int airObjectSize=unpacker.readInt();
                  while(state.airObjects.size()>airObjectSize){
                     state.airObjects.remove(state.airObjects.size()-1);
                  }
                  while(state.airObjects.size()<airObjectSize){
                     state.airObjects.add(new AirObject());
                  }
                  for(int i=0;i<airObjectSize;i++){
                     state.airObjects.get(i).pos.x=unpacker.readFloat();
                     state.airObjects.get(i).pos.y=unpacker.readFloat();
                     state.airObjects.get(i).bearing=unpacker.readFloat();
                     state.airObjects.get(i).groupId=unpacker.readInt();
                  }
               } catch (IOException e) {
                  e.printStackTrace();
               }
               for (int i = 0; i < listeners.size(); i++) {
                  listeners.get(i).dataChanged();
               }
            }

            if (serverEnabled) {
               if (serverSendSocket != null) {
                  if (recBuffSize >0) {
                     DatagramPacket packet = null;
                     try {
                        packet = new DatagramPacket(recBuff, 0, recBuffSize, InetAddress.getByName(clientAddress), servingPort);
                        serverSendSocket.send(packet);
                     } catch (UnknownHostException e) {
                        e.printStackTrace();
                     } catch (IOException e) {
                        e.printStackTrace();
                     }
                  }
               }
            }
         } else {
            try {
               synchronized (DcsNet.this) {
                  if ((!clientSocketReady) && (!shutdownAction)) {
                     System.out.println("idling");
                     DcsNet.this.wait();
                  }
               }
            } catch (InterruptedException e) {
               e.printStackTrace();
            }
         }
      }
      System.out.println("finished net thread");
   }

   @Override
   public void serviceAdded(ServiceEvent event) {

   }

   @Override
   public void serviceRemoved(ServiceEvent event) {

   }

   @Override
   public void serviceResolved(ServiceEvent event) {
      serverAddress = event.getInfo().getInet4Addresses()[0].getHostAddress();
      clientListenPort = event.getInfo().getPort();
      System.out.println("Found service port: " + clientListenPort);
   }
}


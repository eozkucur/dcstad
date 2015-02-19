package com.eozkucur.dcsserver;

import com.eozkucur.dcsnet.AirObject;
import com.eozkucur.dcsnet.AircraftState;
import com.eozkucur.dcsnet.Waypoint;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import org.msgpack.MessagePack;
import org.msgpack.packer.Packer;

public class DcsServerLua {
   public static void main(String[] args) {
      System.out.println("Server Start");

      try {
         ByteArrayOutputStream bout=new ByteArrayOutputStream(1024);
         MessagePack messagePack=new MessagePack();
         Packer packer=messagePack.createPacker(bout);
         AircraftState state=new AircraftState();
         state.pos.y=41.955505f;
         state.pos.x=41.844450f;
         state.bearing= (float) Math.toRadians(-45);
         Waypoint wp=new Waypoint();
         wp.id=1;
         wp.pos.y=41.986136f;
         wp.pos.x=41.777159f;
         state.waypoints.add(wp);
         wp=new Waypoint();
         wp.id=2;
         wp.pos.y=41.899566f;
         wp.pos.x=41.895949f;
         state.waypoints.add(wp);
         wp=new Waypoint();
         wp.id=3;
         wp.pos.y=41.976693f;
         wp.pos.x=41.991049f;
         state.waypoints.add(wp);
         wp=new Waypoint();
         wp.id=4;
         wp.pos.y=41.933544f;
         wp.pos.x=42.098166f;
         state.waypoints.add(wp);
         state.selectedwp=2;

         AirObject ao=new AirObject();
         ao.groupId=0;
         ao.pos.y=41.9779566f;
         ao.pos.x=41.8775949f;
         ao.bearing=(float) Math.toRadians(30);
         state.airObjects.add(ao);
         ao=new AirObject();
         ao.groupId=1;
         ao.pos.y=41.9669566f;
         ao.pos.x=41.8665949f;
         ao.bearing=(float) Math.toRadians(0);
         state.airObjects.add(ao);
         ao=new AirObject();
         ao.groupId=1;
         ao.pos.y=41.9559566f;
         ao.pos.x=41.8555949f;
         ao.bearing=(float) Math.toRadians(-30);
         state.airObjects.add(ao);


         packer.write((float)state.pos.x);
         packer.write((float)state.pos.y);
         packer.write((float)state.bearing);
         packer.write(state.waypoints.size());
         for(int i=0;i<state.waypoints.size();i++){
            wp=state.waypoints.get(i);
            packer.write((float)wp.pos.x);
            packer.write((float)wp.pos.y);
            packer.write(wp.id);
         }
         packer.write(state.selectedwp);
         packer.write(state.airObjects.size());
         for(int i=0;i<state.airObjects.size();i++){
            ao=state.airObjects.get(i);
            packer.write((float)ao.pos.x);
            packer.write((float)ao.pos.y);
            packer.write((float)ao.bearing);
            packer.write(ao.groupId);
         }
         byte[] buf=bout.toByteArray();

         System.out.println("Starting socket");
         ServerSocket s=new ServerSocket(5556);
         System.out.println("Waiting start command");
         Socket clientSocket=s.accept();
         System.out.println("Waiting 1 sec");
         Thread.sleep(1000);
         DatagramSocket socket=new DatagramSocket(0);
         for(int i=0;i<10000;i++) {
            System.out.println("Sending array of size "+buf.length);
            DatagramPacket packet = new DatagramPacket(buf, 0,buf.length, InetAddress.getLocalHost(), 5555);
            socket.send(packet);
            Thread.sleep(400);
         }
         socket.close();
         clientSocket.close();
         s.close();
         System.out.println("Finish");
      } catch (IOException e) {
         e.printStackTrace();
      } catch (InterruptedException e) {
         e.printStackTrace();
      }
   }

}

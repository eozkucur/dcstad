package com.eozkucur.dcsserver;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class DcsServerLua {
   public static void main(String[] args) {
      System.out.println("Server Start");

      try {
         byte[] buf=new byte[70];
         System.out.println("Starting socket");
         ServerSocket s=new ServerSocket(5556);
         System.out.println("Waiting start command");
         Socket clientSocket=s.accept();
         System.out.println("Waiting 1 sec");
         Thread.sleep(1000);
         DatagramSocket socket=new DatagramSocket(0);
         for(int i=0;i<buf.length;i++){
            buf[i]=(byte)i;
         }
         for(int i=0;i<10000;i++) {
            DatagramPacket packet = new DatagramPacket(buf, 0, 30, InetAddress.getLocalHost(), 5555);
            socket.send(packet);
            Thread.sleep(400);
            System.out.println("Sending");
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

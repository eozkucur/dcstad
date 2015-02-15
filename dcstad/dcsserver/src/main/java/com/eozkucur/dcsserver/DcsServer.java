package com.eozkucur.dcsserver;

import com.eozkucur.dcsnet.AircraftState;
import com.eozkucur.dcsnet.DcsNet;

public class DcsServer {

   public static void main(String[] args) {

      System.out.println("Start Server main");
      AircraftState state=new AircraftState();
      DcsNet net = new DcsNet(5555, 6666,state);
      net.serverAddress="127.0.0.1";
      System.out.println("Starting serverclient");
      net.startClient(false);
      System.out.println("Starting server");
      net.startServer();
      try {
         Thread.sleep(35000);
      } catch (InterruptedException e) {
         e.printStackTrace();
      }
      net.shutdown();
      System.out.println("Finish");
   }
}

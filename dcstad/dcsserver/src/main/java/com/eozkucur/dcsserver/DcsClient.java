package com.eozkucur.dcsserver;

import com.eozkucur.dcsnet.AircraftState;
import com.eozkucur.dcsnet.DcsNet;
import com.eozkucur.dcsnet.DcsNetListener;
import com.eozkucur.dcsnet.Waypoint;

public class DcsClient implements DcsNetListener{
   AircraftState state;

   public static void main(String[] args) {
      try {
         System.out.println("Start client");
         DcsClient dcsClient=new DcsClient();
         dcsClient.state=new AircraftState();
         DcsNet net=new DcsNet(5555,7777,dcsClient.state);
         net.addListener(dcsClient);
         Thread.sleep(2000);
         net.startClient(true);
         System.out.println("Waiting");
         Thread.sleep(30000);
         net.shutdown();
         System.out.println("Finish");
      } catch (InterruptedException e) {
         e.printStackTrace();
      }
   }

   @Override
   public void dataChanged() {
      System.out.println("Data changed");
      System.out.print("State: ");
      System.out.print(state.pos.x+",");
      System.out.print(state.pos.y+",");
      System.out.print(state.bearing+" wp count: ");
      System.out.print(state.waypoints.size()+": ");
      for(int i=0;i< state.waypoints.size();i++){
         Waypoint wp=state.waypoints.get(i);
         System.out.print(wp.pos.x+",");
         System.out.print(wp.pos.y+",");
         System.out.print(wp.id+" ");
      }
      System.out.println();
   }

   @Override
   public void connectionStateChanged(boolean isConnected) {
      System.out.println("connection "+isConnected);
   }
}
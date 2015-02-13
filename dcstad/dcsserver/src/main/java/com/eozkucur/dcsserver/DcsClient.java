package com.eozkucur.dcsserver;

import com.eozkucur.dcsnet.DcsNet;
import com.eozkucur.dcsnet.DcsNetListener;

public class DcsClient implements DcsNetListener{

   public static void main(String[] args) {
      try {
         System.out.println("Start client");
         DcsNet net=new DcsNet(4444,7777);
         net.addListener(new DcsClient());
         Thread.sleep(2000);
         net.startClient(true);
         System.out.println("Waiting");
         Thread.sleep(300000);
         net.shutdown();
         System.out.println("Finish");
      } catch (InterruptedException e) {
         e.printStackTrace();
      }
   }

   @Override
   public void dataChanged() {
      System.out.println("Data changed");
   }

   @Override
   public void connectionStateChanged(boolean isConnected) {
      System.out.println("connection "+isConnected);
   }
}
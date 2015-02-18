package com.eozkucur.tadfordcs.app;

import android.app.Activity;
import android.content.res.Configuration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.view.WindowManager;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class MainActivity extends Activity {

   private WifiManager.MulticastLock lock;

   @Override
   protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                           WindowManager.LayoutParams.FLAG_FULLSCREEN);
      setContentView(R.layout.tad_view);
      System.out.println("On create");

      android.net.wifi.WifiManager wifi =
          (android.net.wifi.WifiManager)
              getSystemService(android.content.Context.WIFI_SERVICE);
      lock = wifi.createMulticastLock("HeeereDnssdLock");
      lock.setReferenceCounted(true);
      lock.acquire();

      int hostAddress = wifi.getConnectionInfo().getIpAddress();
      byte[] addressBytes = { (byte)(0xff & hostAddress),
                              (byte)(0xff & (hostAddress >> 8)),
                              (byte)(0xff & (hostAddress >> 16)),
                              (byte)(0xff & (hostAddress >> 24)) };

      try {
         ((TadView)findViewById(R.id.tadv)).dcsnet.setJmdnsaddr(InetAddress.getByAddress(addressBytes));
      } catch (UnknownHostException e) {
         throw new AssertionError();
      }

      System.out.println("created");
   }

   @Override
   protected void onStart() {
      super.onStart();
      System.out.println("On start");
      ((TadView)findViewById(R.id.tadv)).dcsnet.startClient(true);
      System.out.println("On started");
   }

   @Override
   protected void onStop() {
      super.onStop();
      System.out.println("On stop");
      ((TadView)findViewById(R.id.tadv)).dcsnet.stopClient();
      System.out.println("On stopped");
   }

   @Override
   protected void onDestroy() {
      System.out.println("On destroy");
      if (lock != null){
         lock.release();
      }
      super.onDestroy();
      System.out.println("On destroyed");
   }

   @Override
   public void onConfigurationChanged(Configuration newConfig) {
      super.onConfigurationChanged(newConfig);
   }
}

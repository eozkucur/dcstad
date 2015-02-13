package com.eozkucur.dcsnet;

/**
 * Created by ergin.ozkucur on 09/02/15.
 */
public interface DcsNetListener {
   public void dataChanged();

   public void connectionStateChanged(boolean isConnected);

}

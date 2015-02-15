package com.eozkucur.dcsnet;

/**
 * Created by ergin.ozkucur on 13/02/15.
 */
public class Waypoint {
   public Point pos;
   public int id;

   public Waypoint() {
      this(0,0,0);
   }

   public Waypoint(double x, double y, int id) {
      this.pos = new Point(x,y);
      this.id = id;
   }
}

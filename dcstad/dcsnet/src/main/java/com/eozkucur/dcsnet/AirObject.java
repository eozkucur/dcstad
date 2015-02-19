package com.eozkucur.dcsnet;

/**
 * Created by ergin.ozkucur on 19/02/15.
 */
public class AirObject {
   public Point pos;
   public float bearing;
   public int groupId;

   public AirObject() {
      this(new Point(0, 0), 0,0);
   }
   public AirObject(Point pos, float bearing,int groupId) {
      this.pos = pos;
      this.groupId = groupId;
      this.bearing=bearing;
   }
}

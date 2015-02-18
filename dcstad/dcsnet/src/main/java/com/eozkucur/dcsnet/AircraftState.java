package com.eozkucur.dcsnet;

import java.util.ArrayList;

/**
 * Created by ergin.ozkucur on 13/02/15.
 */
public class AircraftState {
   public Point pos;
   public float bearing;
   public ArrayList<Waypoint> waypoints=new ArrayList<Waypoint>();
   public int selectedwp;

   static final double EARTH_RADIUS_METER = 6378137.0;
   static final double EARTH_RADIUS_MILES = 3963.190592;

   public AircraftState() {
      this(0,0,0,0);
   }

   public AircraftState(float posx, float posy, float bearing,int selectedwp) {
      this.pos=new Point(posx,posy);
      this.bearing = bearing;
      this.selectedwp=selectedwp;
   }

   public static void convertMiles(AircraftState stateRaw,AircraftState stateMiles){

      while(stateMiles.waypoints.size()>stateRaw.waypoints.size()){
         stateMiles.waypoints.remove(stateMiles.waypoints.size()-1);
      }
      while(stateMiles.waypoints.size()<stateRaw.waypoints.size()){
         stateMiles.waypoints.add(new Waypoint());
      }

      stateMiles.pos.x=0;
      stateMiles.pos.y=0;
      stateMiles.bearing=stateRaw.bearing;
      stateMiles.selectedwp=stateRaw.selectedwp;
      for(int i=0;i<stateMiles.waypoints.size();i++){
         stateMiles.waypoints.get(i).pos=convertToMiles(stateRaw.waypoints.get(i).pos, stateRaw.pos);
         stateMiles.waypoints.get(i).id=stateRaw.waypoints.get(i).id;
      }

   }
   public static Point convertToMiles(Point p, Point origin){
      //double rad1=Math.toRadians(p.y);
      //double rad2=Math.toRadians(origin.y);
      //double deltaRad=Math.toRadians(origin.x-p.x);
      //double dist = Math.acos( Math.sin(rad1)*Math.sin(rad2) + Math.cos(rad1)*Math.cos(rad2) * Math.cos(deltaRad) ) * EARTH_RADIUS_MILES;


      double lat1=Math.toRadians(origin.y);
      double lat2=Math.toRadians(p.y);
      double lon1=Math.toRadians(origin.x);
      double lon2=Math.toRadians(p.x);
      double deltalat=lat2-lat1;
      double deltalon=lon2-lon1;

      double a=Math.sin(deltalat/2)*Math.sin(deltalat/2)+Math.cos(lat1)*Math.cos(lat2)*Math.sin(deltalon/2)*Math.sin(deltalon/2);
      double c=2*Math.atan2(Math.sqrt(a),Math.sqrt(1-a));
      double dist=c*EARTH_RADIUS_MILES;

      //var φ1 = lat1.toRadians();
      //var φ2 = lat2.toRadians();
      //var Δφ = (lat2-lat1).toRadians();
      //var Δλ = (lon2-lon1).toRadians();
      //
      //var a = Math.sin(Δφ/2) * Math.sin(Δφ/2) +
      //        Math.cos(φ1) * Math.cos(φ2) *
      //        Math.sin(Δλ/2) * Math.sin(Δλ/2);
      //var c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
      //
      //var d = R * c;

      double y=Math.sin(deltalon)*Math.cos(lat2);
      double x=Math.cos(lat1)*Math.sin(lat2)-Math.sin(lat1)*Math.cos(lat2)*Math.cos(deltalon);
      double bearing=Math.atan2(y,x);

      //ATAN2(COS(lat1)*SIN(lat2)-SIN(lat1)*COS(lat2)*COS(lon2-lon1),
      //      SIN(lon2-lon1)*COS(lat2))
      //
      //var y = Math.sin(λ2-λ1) * Math.cos(φ2);
      //var x = Math.cos(φ1)*Math.sin(φ2) -
      //        Math.sin(φ1)*Math.cos(φ2)*Math.cos(λ2-λ1);
      //var brng = Math.atan2(y, x).toDegrees();


      return new Point((float)(dist*Math.sin(bearing)),(float)(dist*Math.cos(bearing)));
   }


}

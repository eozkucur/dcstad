package com.eozkucur.tadfordcs.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.support.v4.view.GestureDetectorCompat;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import com.eozkucur.dcsnet.AirObject;
import com.eozkucur.dcsnet.AircraftState;
import com.eozkucur.dcsnet.DcsNet;
import com.eozkucur.dcsnet.DcsNetListener;
import com.eozkucur.dcsnet.Point;
import com.eozkucur.dcsnet.Waypoint;
import java.util.ArrayList;

/**
 *
 */
public class TadView extends View implements DcsNetListener,GestureDetector.OnGestureListener,GestureDetector.OnDoubleTapListener{

   private GestureDetectorCompat mDetector;

   AircraftState stateRaw=new AircraftState();
   AircraftState state=new AircraftState();
   DcsNet dcsnet=new DcsNet(5555,8899,stateRaw);

   private boolean connected;

   Paint paint =new Paint();

   private int focusedObject=-1;

   float mileScale = 10;

   private Path aircraftDrawPath;
   private Path northDrawPath;

   private TextPaint textPaint;

   private Point northPoint;

   private ArrayList<Waypoint> sortedWps;

   private Rect txtBound=new Rect();

   private float heightInv=0;

   private float halfheight=0;

   public TadView(Context context) {
      super(context);
      init(context,null, 0);
   }

   public TadView(Context context, AttributeSet attrs) {
      super(context, attrs);
      init(context,attrs, 0);
   }

   public TadView(Context context, AttributeSet attrs, int defStyle) {
      super(context, attrs, defStyle);
      init(context,attrs, defStyle);
   }

   private void init(Context context,AttributeSet attrs, int defStyle) {

      stateRaw.pos.y=41.955505f;
      stateRaw.pos.x=41.844450f;
      stateRaw.bearing= 0;
      stateRaw.selectedwp=2;
      AircraftState.convertMiles(stateRaw,state);
      //stateRaw.bearing= (float) -Math.toRadians(15);
      //Waypoint wp=new Waypoint();
      //wp.id=1;
      //wp.pos.y=41.986136f;
      //wp.pos.x=41.777159f;
      //stateRaw.waypoints.add(wp);
      //wp=new Waypoint();
      //wp.id=2;
      //wp.pos.y=41.899566f;
      //wp.pos.x=41.895949f;
      //stateRaw.waypoints.add(wp);
      //wp=new Waypoint();
      //wp.id=3;
      //wp.pos.y=41.976693f;
      //wp.pos.x=41.991049f;
      //stateRaw.waypoints.add(wp);
      //wp=new Waypoint();
      //wp.id=4;
      //wp.pos.y=41.933544f;
      //wp.pos.x=42.098166f;
      //stateRaw.waypoints.add(wp);


      dcsnet.addListener(this);
      aircraftDrawPath = new Path();
      aircraftDrawPath.moveTo(-1f, -0.6f);
      aircraftDrawPath.lineTo(-1f, 0.6f);
      aircraftDrawPath.lineTo(1f, 0f);
      aircraftDrawPath.close();

      northDrawPath = new Path();
      //0.866 sqrt(3)/2
      northDrawPath.moveTo(0f, 0.2f);
      northDrawPath.lineTo(0f, -0.2f);
      northDrawPath.lineTo(-0.866f * 0.4f,0f);
      northDrawPath.close();
      northPoint=new Point(0,mileScale/2);

      textPaint = new TextPaint();
      textPaint.setTextAlign(Paint.Align.CENTER);

      sortedWps=new ArrayList<Waypoint>();

      mDetector = new GestureDetectorCompat(context,this);

      mDetector.setOnDoubleTapListener(this);

   }

   @Override
   protected void onDraw(Canvas canvas) {
      super.onDraw(canvas);
      canvas.save();

      //System.out.println("w,h: " + this.getWidth() + "," +this.getHeight());

      paint.setAntiAlias(false);
      canvas.translate(this.getWidth() / 2, this.getHeight() / 2);
      paint.setColor(Color.BLACK);
      float margin = 5;
      paint.setStyle(Paint.Style.FILL);
      canvas.drawRect(-2*getWidth(), -2*getHeight(), -2*getWidth()+4*getWidth(),-2*getHeight()+ 4*getHeight(),paint);

      if (this.getHeight() > this.getWidth()) {
         canvas.scale((this.getWidth() - margin) / 2.0f, (this.getWidth() - margin) / 2.0f);
      } else {
         canvas.scale((this.getHeight() - margin) / 2.0f, (this.getHeight() - margin) / 2.0f);
      }
      paint.setStrokeWidth(0);

      if (focusedObject >= state.waypoints.size()) {
         focusedObject = -1;
      }

      paint.setColor(Color.GRAY);
      paint.setStyle(Paint.Style.STROKE);
      canvas.drawCircle(0,0,1,paint);
      canvas.drawCircle(0,0,0.5f,paint);
      paint.setColor(Color.BLUE);
      canvas.save();

      float aircraftBearing=  (float)Math.toDegrees(-state.bearing)+90;
      float aircraftScale=0.05f;
      if (focusedObject == -1) {
         canvas.rotate((aircraftBearing-90));
         canvas.translate(-state.pos.x / mileScale, state.pos.y / mileScale);
      } else {
         canvas.translate(-state.waypoints.get(focusedObject).pos.x / mileScale, state.waypoints.get(focusedObject).pos.y / mileScale);
      }
      canvas.save();
      canvas.translate(state.pos.x / mileScale, -state.pos.y / mileScale);
      canvas.rotate(-aircraftBearing);
      canvas.scale(aircraftScale, aircraftScale);
      paint.setStyle(Paint.Style.FILL);
      canvas.drawPath(aircraftDrawPath, paint);
      canvas.restore();

      for(AirObject ao:state.airObjects) {
         canvas.save();
         if(ao.groupId==0){
            paint.setColor(Color.BLUE);
         }else{
            paint.setColor(Color.GREEN);
         }
         canvas.translate(ao.pos.x / mileScale, -ao.pos.y / mileScale);
         canvas.rotate((float)Math.toDegrees(ao.bearing)-90);
         canvas.scale(aircraftScale/2, aircraftScale/2);
         canvas.drawPath(aircraftDrawPath, paint);
         canvas.restore();
      }
      paint.setColor(Color.BLUE);

      if(heightInv==0){
         String wpStr="8";
         textPaint.getTextBounds(wpStr,0,wpStr.length(),txtBound);
         heightInv=1f/txtBound.height();
         halfheight=txtBound.height()/2f;
      }

      float wpscale = 0.06f;

      paint.setStyle(Paint.Style.STROKE);
      for(Waypoint wp : state.waypoints) {
         canvas.save();
         canvas.translate(wp.pos.x/mileScale, -(wp.pos.y/mileScale));
         canvas.scale(wpscale, wpscale);
         canvas.drawCircle(0,0,1,paint);
         if (focusedObject == -1) {
            canvas.rotate(-aircraftBearing+90);
         }
         canvas.scale(heightInv,heightInv);
         canvas.translate(0, halfheight);
         if(state.selectedwp==wp.id){
            textPaint.setColor(Color.WHITE);
         }else{
            textPaint.setColor(Color.GREEN);
         }
         canvas.drawText(""+wp.id, 0, 0,textPaint);
         canvas.restore();
      }
      sortedWps.clear();
      int prevMinIndx=-1;
      for(int i=0;i<state.waypoints.size();i++){
         int minId=999;
         Waypoint minWp=null;
         for(int j=0;j<state.waypoints.size();j++) {
            if (state.waypoints.get(j).id>prevMinIndx&&state.waypoints.get(j).id<minId){
               minWp=state.waypoints.get(j);
               minId=minWp.id;
            }
         }
         sortedWps.add(minWp);
         prevMinIndx=minWp.id;
      }
      for(int i=1;i<sortedWps.size();i++){
         canvas.drawLine(sortedWps.get(i-1).pos.x/mileScale, -sortedWps.get(i-1).pos.y/mileScale, sortedWps.get(i).pos.x/mileScale, -sortedWps.get(i).pos.y/mileScale,paint);
      }
      if(sortedWps.size()>=2){
         canvas.drawLine(sortedWps.get(sortedWps.size() - 1).pos.x / mileScale, -sortedWps.get(sortedWps.size() - 1).pos.y / mileScale, sortedWps.get(0).pos.x / mileScale, -sortedWps.get(0).pos.y / mileScale,paint);
      }

      paint.setColor(Color.WHITE);

      northPoint.x=0;
      northPoint.y=mileScale/2;
      if(focusedObject==-1){
         northPoint.x+=state.pos.x;
         northPoint.y+=state.pos.y;
      }else{
         northPoint.x+=state.waypoints.get(focusedObject).pos.x;
         northPoint.y+=state.waypoints.get(focusedObject).pos.y;
      }
      paint.setStyle(Paint.Style.FILL);
      canvas.save();
      canvas.translate((northPoint.x/mileScale), -(northPoint.y/mileScale));
      canvas.rotate((float) Math.toDegrees(Math.PI / 2));
      canvas.scale(wpscale*2, wpscale*2);
      canvas.drawPath(northDrawPath, paint);
      canvas.restore();

      canvas.restore();
      if(connected){
         textPaint.setColor(Color.GREEN);
      }else{
         textPaint.setColor(Color.RED);
      }
      canvas.translate(0.90f, -0.90f);
      canvas.scale(wpscale*heightInv, wpscale*heightInv);
      canvas.translate(0, halfheight);
      canvas.scale(1.5f,1.5f);
      canvas.drawText(""+(int)mileScale, 0, 0,textPaint);

      canvas.restore();
   }

   @Override
   public void dataChanged() {
      AircraftState.convertMiles(stateRaw, state);
      this.postInvalidate();
   }

   @Override
   public void connectionStateChanged(boolean isConnected) {
      System.out.println("connection " + isConnected);
      connected = isConnected;
      this.postInvalidate();
   }

   @Override
   public boolean onTouchEvent(MotionEvent event){
      this.mDetector.onTouchEvent(event);
      return super.onTouchEvent(event);
   }



   @Override
   public boolean onSingleTapConfirmed(MotionEvent e) {
      //System.out.println("single tap");
      mileScale *= 2;
      if (mileScale > 160) {
         mileScale = 10;
      }
      postInvalidate();
      return true;
   }

   @Override
   public boolean onDoubleTap(MotionEvent e) {
      //System.out.println("double tap");
      focusedObject++;
      if (focusedObject == state.waypoints.size()) {
         focusedObject = -1;
      }
      postInvalidate();
      return true;
   }

   @Override
   public boolean onDoubleTapEvent(MotionEvent e) {
      return false;
   }

   @Override
   public boolean onDown(MotionEvent e) {
      return false;
   }

   @Override
   public void onShowPress(MotionEvent e) {

   }

   @Override
   public boolean onSingleTapUp(MotionEvent e) {
      return false;
   }

   @Override
   public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
      return false;
   }

   @Override
   public void onLongPress(MotionEvent e) {

   }

   @Override
   public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
      return false;
   }
}

package com.google.xrinput;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.CountDownTimer;
import android.os.Handler;
import android.view.MotionEvent;
import android.widget.ImageView;
import android.graphics.Color;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GestureEvent {

    private ImageView imageView;
    private ImageView drawView;
    private int height;
    private int width;
    private final Random random;

    private final Bitmap imageViewBitmap;
    private final Canvas imageViewCanvas;
    private Canvas drawViewCanvas;
    private int[] locationOnScreen;
    private final Paint gesturePaint;
    private Paint drawPaint;
    public Gesture active;

    private final List<Gesture> gestureStack;

    private int numTaps;
    private int numDoubleTaps;
    private int numPinchIn;
    private int numPinchOut;
    private int numSwipeUp;
    private int numSwipeDown;
    private int numSwipeLeft;
    private int numSwipeRight;
    private boolean terminate = false;
    private final TextView gestureText;
    private final int circleRadius = 50;
    private final int arrowDistance = 350;
    private final int arrowLength = 100;
    private final TextView timerView;
    public boolean negative = false;
    private int distanceThreshold = 200;

    public GestureEvent(ImageView display, ImageView drawView, TextView timerView, TextView gestureText) {
        imageView = display;
        this.drawView = drawView;
        this.gestureText = gestureText;
        this.timerView = timerView;
        height = display.getHeight();
        width = display.getWidth();
        random = new Random();
        imageViewBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Bitmap drawViewBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        imageViewCanvas = new Canvas(imageViewBitmap);
        drawViewCanvas = new Canvas(drawViewBitmap);

        gestureStack = new ArrayList<>();

        gesturePaint = new Paint();
        gesturePaint.setStyle(Paint.Style.FILL);
        gesturePaint.setColor(Color.GREEN);
        gesturePaint.setStrokeWidth(7);
        gesturePaint.setStrokeCap(Paint.Cap.ROUND);

        locationOnScreen = new int[2];
        imageView.getLocationOnScreen(locationOnScreen);
    }

    public void start() {
        gestureStack.clear();
        terminate = false;

        for (int i = 0; i < numSwipeDown; i++){
            gestureStack.add(new SwipeDown());
        }
        for (int i = 0; i < numSwipeUp; i++){
            gestureStack.add(new SwipeUp());
        }
        for (int i = 0; i < numSwipeLeft; i++){
            gestureStack.add(new SwipeLeft());
        }
        for (int i = 0; i < numSwipeRight; i++){
            gestureStack.add(new SwipeRight());
        }
        for (int i = 0; i < numPinchOut; i++){
            gestureStack.add(new PinchOut());
        }
        for (int i = 0; i < numPinchIn; i++){
            gestureStack.add(new PinchIn());
        }
        for (int i = 0; i < numDoubleTaps; i++){
            gestureStack.add(new DoubleTap());
        }
        for (int i = 0; i < numTaps; i++){
            gestureStack.add(new Tap());
        }
        nextGesture();
    }

    public void terminate(){
        terminate = true;
    }

    private void updateLocations(){
        locationOnScreen = new int[2];
        height = imageView.getHeight();
        width = imageView.getWidth();
        imageView.getLocationOnScreen(locationOnScreen);
    }

    public void setCounts(int[] counts){
        numTaps = counts[0];
        numDoubleTaps = counts[1];
        numPinchIn = counts[2];
        numPinchOut = counts[3];
        numSwipeUp = counts[4];
        numSwipeDown = counts[5];
        numSwipeLeft = counts[6];
        numSwipeRight = counts[7];
        distanceThreshold = counts[8];
    }

    private void nextGesture(){
        gestureText.setText("");
        imageViewCanvas.drawColor(Color.BLACK);
        updateLocations();

        if (terminate){
            active = null;
            return;
        }
        if (!gestureStack.isEmpty()){
            Gesture curr =  gestureStack.remove(gestureStack.size()-1);
            active = curr;
            curr.draw();

            if (negative){
                startCountdown(3);
                new Handler().postDelayed(this::clearCanvas, 3000);
                new Handler().postDelayed(this::nextGesture, 5000);
            }
        }
    }
    private void startCountdown(int seconds) {
        // Convert seconds to milliseconds for the CountDownTimer
        CountDownTimer countDownTimer = new CountDownTimer(seconds * 1000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                // Update the TextView with the remaining time
                String secondsRemaining = ""+(int) (millisUntilFinished / 1000);
                timerView.setText(secondsRemaining);
            }

            @Override
            public void onFinish() {
                // Set the TextView to indicate the timer is done
                timerView.setText("");
            }
        };
        // Start the timer
        countDownTimer.start();
    }

    public void sendTouchSignal(MotionEvent event, Class<?> class_){

        if (negative){
            return;
        }

        if (class_.isInstance(active)) {

            if (class_ == Tap.class || class_ == DoubleTap.class){
                double distance = Math.sqrt(Math.pow(active.getX()-event.getX(), 2) + Math.pow(active.getX()-event.getX(), 2));
                if (distance <= distanceThreshold){
                    clearCanvas();
                    new Handler().postDelayed(this::nextGesture, 1500);
                }
            } else {
                clearCanvas();
                new Handler().postDelayed(this::nextGesture, 1500);
            }
        }
    }

    private void clearCanvas(){
        imageViewCanvas.drawColor(Color.TRANSPARENT, android.graphics.PorterDuff.Mode.CLEAR);
        imageView.invalidate();
        active = null;
    }

    public interface Gesture {
        void draw();
        String repr();
        int getX();
        int getY();
    }

    public class Tap implements Gesture {
        private final int x;
        private final int y;
        private Tap(){
            x = random.nextInt(width-2*circleRadius) + circleRadius;
            y = random.nextInt(height-2*circleRadius) + circleRadius;
        }

        public void draw(){
            imageViewCanvas.drawCircle(x, y, circleRadius, gesturePaint);
            imageView.setImageBitmap(imageViewBitmap);
            gestureText.setText("Tap");
        }

        public String repr(){
            return "SINGLE_TAP,"
                    + (this.x + locationOnScreen[0])
                    + ","
                    + (this.y + locationOnScreen[1]);
        }
        public int getX(){
            return this.x;
        }
        public int getY(){
            return this.y;
        }
    }

    public class DoubleTap implements Gesture {
        private final int x;
        private final int y;

        private DoubleTap(){
            x = random.nextInt(width-2*(circleRadius+20)) + circleRadius + 20;
            y = random.nextInt(height-2*(circleRadius+20)) + circleRadius + 20;
        }

        public void draw(){
            imageViewCanvas.drawCircle(x, y, circleRadius, gesturePaint);
            gesturePaint.setStyle(Paint.Style.STROKE);
            imageViewCanvas.drawCircle(x, y, circleRadius+20, gesturePaint);
            gesturePaint.setStyle(Paint.Style.FILL);

            imageView.setImageBitmap(imageViewBitmap);
            gestureText.setText("Double Tap");
        }

        public String repr(){
            return "DOUBLE_TAP,"
                    + (this.x + locationOnScreen[0])
                    + ","
                    + (this.y + locationOnScreen[1]);
        }
        public int getX(){
            return this.x;
        }
        public int getY(){
            return this.y;
        }

    }

    public class PinchIn implements Gesture {
        private final int x;
        private final int y;
        private final double angle;

        private PinchIn(){
            x = random.nextInt(width-2*circleRadius-2*arrowDistance) + circleRadius + arrowDistance;
            y = random.nextInt(height-2*circleRadius-2*arrowDistance) + circleRadius + arrowDistance;
            angle = Math.random() * (Math.PI/2) + Math.PI/2;
        }

        public void draw(){
            imageViewCanvas.drawCircle(x, y, circleRadius, gesturePaint);

            drawArrowhead(imageViewCanvas, (float)(x+arrowLength*Math.cos(angle)), (float)(y+arrowLength*Math.sin(angle)),
                    arrowLength, angle+Math.PI);
            drawArrowhead(imageViewCanvas, (float)(x-arrowLength*Math.cos(angle)), (float)(y-arrowLength*Math.sin(angle)),
                    arrowLength, angle);

            imageView.setImageBitmap(imageViewBitmap);
            gestureText.setText("Pinch In");
        }

        public String repr(){
            return "PINCH_IN,"
                    + (this.x + locationOnScreen[0])
                    + ","
                    + (this.y + locationOnScreen[1]);
        }
        public int getX(){
            return this.x;
        }
        public int getY(){
            return this.y;
        }
    }

    public class PinchOut implements Gesture {
        private final int x;
        private final int y;
        private final double angle;

        private PinchOut(){
            x = random.nextInt(width-2*circleRadius-2*(arrowDistance)) + circleRadius + (arrowDistance);
            y = random.nextInt(height-2*circleRadius-2*(arrowDistance)) + circleRadius + (arrowDistance);
            angle = Math.random() * (Math.PI/2) + Math.PI/2;
        }

        public void draw(){
            imageViewCanvas.drawCircle(x, y, circleRadius, gesturePaint);

            drawArrowhead(imageViewCanvas, (float)(x+(arrowDistance)*Math.cos(angle)), (float)(y+(arrowDistance)*Math.sin(angle)),
                    arrowLength, angle);
            drawArrowhead(imageViewCanvas, (float)(x-(arrowDistance)*Math.cos(angle)), (float)(y-(arrowDistance)*Math.sin(angle)),
                    arrowLength, angle+Math.PI);

            imageView.setImageBitmap(imageViewBitmap);
            gestureText.setText("Pinch Out");
        }

        public String repr(){
            return "PINCH_OUT,"
                    + (this.x + locationOnScreen[0])
                    + ","
                    + (this.y + locationOnScreen[1]);
        }
        public int getX(){
            return this.x;
        }
        public int getY(){
            return this.y;
        }
    }

    public class SwipeUp implements Gesture {
        private final int x;
        private final int y;

        private SwipeUp(){
            x = random.nextInt(width-2*circleRadius) + circleRadius;
            y = random.nextInt(height-2*circleRadius-arrowDistance) + circleRadius + arrowDistance;
        }
        public void draw(){
            imageViewCanvas.drawCircle(x, y, circleRadius, gesturePaint);
            imageViewCanvas.drawLine(x, y, x, y-arrowDistance, gesturePaint);
            drawArrowhead(imageViewCanvas, x, y-arrowDistance, arrowLength, 3*Math.PI/2);
            gestureText.setText("Swipe Up");
        }
        public String repr(){
            return "SWIPE_UP,"
                    + (this.x + locationOnScreen[0])
                    + ","
                    + (this.y + locationOnScreen[1]);
        }
        public int getX(){
            return this.x;
        }
        public int getY(){
            return this.y;
        }
    }

    public class SwipeDown implements Gesture {
        private final int x;
        private final int y;

        private SwipeDown(){
            x = random.nextInt(width-2*circleRadius) + circleRadius;
            y = random.nextInt(height-2*circleRadius - arrowDistance) + circleRadius;
        }
        public void draw(){
            imageViewCanvas.drawCircle(x, y, circleRadius, gesturePaint);
            imageViewCanvas.drawLine(x, y, x, y+arrowDistance, gesturePaint);
            drawArrowhead(imageViewCanvas, x, y+arrowDistance, arrowLength, Math.PI/2);
            gestureText.setText("Swipe Down");
        }
        public String repr(){
            return "SWIPE_DOWN,"
                    + (this.x + locationOnScreen[0])
                    + ","
                    + (this.y + locationOnScreen[1]);
        }
        public int getX(){
            return this.x;
        }
        public int getY(){
            return this.y;
        }
    }

    public class SwipeLeft implements Gesture {
        private final int x;
        private final int y;

        private SwipeLeft(){
            x = random.nextInt(width-2*circleRadius-arrowDistance) + circleRadius + arrowDistance;
            y = random.nextInt(height-2*circleRadius) + circleRadius;
        }
        public void draw(){
            imageViewCanvas.drawCircle(x, y, circleRadius, gesturePaint);
            imageViewCanvas.drawLine(x, y, x-arrowDistance, y, gesturePaint);
            drawArrowhead(imageViewCanvas, x-arrowDistance, y, arrowLength, Math.PI);
            gestureText.setText("Swipe Left");
        }
        public String repr(){
            return "SWIPE_LEFT,"
                    + (this.x + locationOnScreen[0])
                    + ","
                    + (this.y + locationOnScreen[1]);
        }
        public int getX(){
            return this.x;
        }
        public int getY(){
            return this.y;
        }
    }

    public class SwipeRight implements Gesture {
        private final int x;
        private final int y;

        private SwipeRight(){
            x = random.nextInt(width-2*circleRadius - arrowDistance) + circleRadius;
            y = random.nextInt(height-2*circleRadius) + circleRadius;
        }
        public void draw(){
            imageViewCanvas.drawCircle(x, y, circleRadius, gesturePaint);
            imageViewCanvas.drawLine(x, y, x+arrowDistance, y, gesturePaint);
            drawArrowhead(imageViewCanvas, x+arrowDistance, y, arrowLength, 0);
            gestureText.setText("Swipe Right");
        }
        public String repr(){
            return "SWIPE_RIGHT,"
                    + (this.x + locationOnScreen[0])
                    + ","
                    + (this.y + locationOnScreen[1]);
        }
        public int getX(){
            return this.x;
        }
        public int getY(){
            return this.y;
        }
    }

    private void drawArrowhead(Canvas canvas, float x, float y, float arrowLength, double angle) {

        // Calculate the arrowhead points
        float arrowPointX1 = x - arrowLength * (float) Math.cos(angle - Math.PI / 6);
        float arrowPointY1 = y - arrowLength * (float) Math.sin(angle - Math.PI / 6);

        float arrowPointX2 = x - arrowLength * (float) Math.cos(angle + Math.PI / 6);
        float arrowPointY2 = y - arrowLength * (float) Math.sin(angle + Math.PI / 6);

        canvas.drawLine(x, y, arrowPointX1, arrowPointY1, gesturePaint);
        canvas.drawLine(x, y, arrowPointX2, arrowPointY2, gesturePaint);
    }
}

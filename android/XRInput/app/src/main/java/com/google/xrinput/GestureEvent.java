package com.google.xrinput;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Handler;
import android.widget.ImageView;
import android.graphics.Color;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GestureEvent {

    private ImageView imageView;
    private ImageView drawView;
    private int height;
    private int width;
    private Random random;

    private Bitmap imageViewBitmap;
    private Bitmap drawViewBitmap;
    private Canvas imageViewCanvas;
    private Canvas drawViewCanvas;
    private int[] locationOnScreen;
    private Paint gesturePaint;
    private Paint drawPaint;
    public Gesture active;

    private List<Gesture> gestureStack;

    private int numTaps;
    private int numDoubleTaps;
    private int numPinchIn;
    private int numPinchOut;
    private int numSwipeUp;
    private int numSwipeDown;
    private int numSwipeLeft;
    private int numSwipeRight;

    public GestureEvent(ImageView display, ImageView drawView) {
        imageView = display;
        this.drawView = drawView;
        height = display.getHeight();
        width = display.getWidth();
        random = new Random();
        imageViewBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        drawViewBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        imageViewCanvas = new Canvas(imageViewBitmap);
        drawViewCanvas = new Canvas(drawViewBitmap);

        gestureStack = new ArrayList<>();

        gesturePaint = new Paint();
        gesturePaint.setStyle(Paint.Style.FILL);
        gesturePaint.setColor(Color.BLUE);
        gesturePaint.setStrokeWidth(7);

        locationOnScreen = new int[2];
        imageView.getLocationOnScreen(locationOnScreen);
    }

    public void start() {
        gestureStack.clear();

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

    public void setCounts(int[] counts){
        numTaps = counts[0];
        numDoubleTaps = counts[1];
        numPinchIn = counts[2];
        numPinchOut = counts[3];
        numSwipeUp = counts[4];
        numSwipeDown = counts[5];
        numSwipeLeft = counts[6];
        numSwipeRight = counts[7];
    }

    private void nextGesture(){
        imageViewCanvas.drawColor(Color.BLACK);
        if (!gestureStack.isEmpty()){
            Gesture curr =  gestureStack.remove(gestureStack.size()-1);
            active = curr;
            curr.draw();
            new Handler().postDelayed(this::clearCanvas, 5000);
            new Handler().postDelayed(this::nextGesture, 2000);
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
    }

    class Tap implements Gesture {
        private final int x;
        private final int y;
        private Tap(){
            x = random.nextInt(width-40) + 20;
            y = random.nextInt(height-40) + 20;
        }

        public void draw(){
            imageViewCanvas.drawCircle(x, y, 20, gesturePaint);
            imageView.setImageBitmap(imageViewBitmap);
        }

        public String repr(){
            return "TAP,"
                    + (this.x + locationOnScreen[0])
                    + ","
                    + (this.y + locationOnScreen[1]);
        }
    }

    class DoubleTap implements Gesture {
        private int x;
        private int y;

        private DoubleTap(){
            x = random.nextInt(width-60) + 30;
            y = random.nextInt(height-60) + 30;
        }

        public void draw(){
            imageViewCanvas.drawCircle(x, y, 20, gesturePaint);
            gesturePaint.setStyle(Paint.Style.STROKE);
            imageViewCanvas.drawCircle(x, y, 30, gesturePaint);
            gesturePaint.setStyle(Paint.Style.FILL);

            imageView.setImageBitmap(imageViewBitmap);
        }

        public String repr(){
            return "DOUBLE_TAP,"
                    + (this.x + locationOnScreen[0])
                    + ","
                    + (this.y + locationOnScreen[1]);
        }

    }

    class PinchIn implements Gesture {
        private final int x;
        private final int y;
        private final double angle;

        private PinchIn(){
            x = random.nextInt(width-60-150) + 30 + 75;
            y = random.nextInt(height-60-150) + 30 + 75;
            angle = Math.random() * (Math.PI);
        }

        public void draw(){
            imageViewCanvas.drawCircle(x, y, 20, gesturePaint);

            drawArrowhead(imageViewCanvas, (float)(x+50*Math.cos(angle)), (float)(y+50*Math.sin(angle)),
                    30, angle+Math.PI);
            drawArrowhead(imageViewCanvas, (float)(x-50*Math.cos(angle)), (float)(y-50*Math.sin(angle)),
                    30, angle);

            imageView.setImageBitmap(imageViewBitmap);
        }

        public String repr(){
            return "PINCH_IN,"
                    + (this.x + locationOnScreen[0])
                    + ","
                    + (this.y + locationOnScreen[1])
                    + ","
                    + angle;
        }
    }

    class PinchOut implements Gesture {
        private final int x;
        private final int y;
        private final double angle;

        private PinchOut(){
            x = random.nextInt(width-60-150) + 30 + 75;
            y = random.nextInt(height-60-150) + 30 + 75;
            angle = Math.random() * (Math.PI);
        }

        public void draw(){
            imageViewCanvas.drawCircle(x, y, 20, gesturePaint);

            drawArrowhead(imageViewCanvas, (float)(x+75*Math.cos(angle)), (float)(y+75*Math.sin(angle)),
                    30, angle);
            drawArrowhead(imageViewCanvas, (float)(x-75*Math.cos(angle)), (float)(y-75*Math.sin(angle)),
                    30, angle+Math.PI);

            imageView.setImageBitmap(imageViewBitmap);
        }

        public String repr(){
            return "PINCH_OUT,"
                    + (this.x + locationOnScreen[0])
                    + ","
                    + (this.y + locationOnScreen[1])
                    + ","
                    + angle;
        }
    }

    class SwipeUp implements Gesture {
        private final int x;
        private final int y;

        private SwipeUp(){
            x = random.nextInt(width-40) + 20;
            y = random.nextInt(height-40-75) + 20 + 75;
        }
        public void draw(){
            imageViewCanvas.drawCircle(x, y, 20, gesturePaint);
            imageViewCanvas.drawLine(x, y, x, y-75, gesturePaint);
            drawArrowhead(imageViewCanvas, x, y-75, 30, 3*Math.PI/2);
        }
        public String repr(){
            return "SWIPE_UP,"
                    + (this.x + locationOnScreen[0])
                    + ","
                    + (this.y + locationOnScreen[1]);
        }
    }

    class SwipeDown implements Gesture {
        private final int x;
        private final int y;

        private SwipeDown(){
            x = random.nextInt(width-40) + 20;
            y = random.nextInt(height-40 - 75) + 20;
        }
        public void draw(){
            imageViewCanvas.drawCircle(x, y, 20, gesturePaint);
            imageViewCanvas.drawLine(x, y, x, y+75, gesturePaint);
            drawArrowhead(imageViewCanvas, x, y+75, 30, Math.PI/2);
        }
        public String repr(){
            return "SWIPE_DOWN,"
                    + (this.x + locationOnScreen[0])
                    + ","
                    + (this.y + locationOnScreen[1]);
        }
    }

    class SwipeLeft implements Gesture {
        private final int x;
        private final int y;

        private SwipeLeft(){
            x = random.nextInt(width-40-75) + 20 + 75;
            y = random.nextInt(height-40) + 20;
        }
        public void draw(){
            imageViewCanvas.drawCircle(x, y, 20, gesturePaint);
            imageViewCanvas.drawLine(x, y, x-75, y, gesturePaint);
            drawArrowhead(imageViewCanvas, x-75, y, 30, Math.PI);
        }
        public String repr(){
            return "SWIPE_LEFT,"
                    + (this.x + locationOnScreen[0])
                    + ","
                    + (this.y + locationOnScreen[1]);
        }
    }

    class SwipeRight implements Gesture {
        private final int x;
        private final int y;

        private SwipeRight(){
            x = random.nextInt(width-40 - 75) + 20;
            y = random.nextInt(height-40) + 20;
        }
        public void draw(){
            imageViewCanvas.drawCircle(x, y, 20, gesturePaint);
            imageViewCanvas.drawLine(x, y, x+75, y, gesturePaint);
            drawArrowhead(imageViewCanvas, x+75, y, 30, 0);
        }
        public String repr(){
            return "SWIPE_RIGHT,"
                    + (this.x + locationOnScreen[0])
                    + ","
                    + (this.y + locationOnScreen[1]);
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

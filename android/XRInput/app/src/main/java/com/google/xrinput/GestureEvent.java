package com.google.xrinput;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.media.Image;
import android.view.MotionEvent;
import android.widget.ImageView;
import android.graphics.Canvas;
import android.graphics.Bitmap;
import android.graphics.Color;

import java.util.ArrayList;
import java.util.Random;
import android.graphics.Paint;
import android.graphics.Path;
import java.util.List;


public class GestureEvent {
    private Canvas background;
    private Bitmap bitmap;
    private ImageView image_view;
    final private int width;
    final private int height;
    final private Random random;
    private boolean circleActive = false;

    private Path gesturePath;
    private Paint gesturePathPaint;
    private Paint erasePaint;
    private Paint gestureShapePaint;

    private int targetX = -1;
    private int targetY = -1;
    private List<GestureShape> gestureStack;

    private int numHorizontalLines;
    private int numVerticalLines;
    private int numBackSlashes;
    private int numSlashes;
    private int numCircles;
    private int numStatics;
    private final int BORDER = 0;
    private final int MIN_LENGTH = 50;

    public boolean complete = false;
    private ImageView draw_view;
    private Bitmap drawBitmap;
    private Canvas drawArea;

    public GestureEvent(ImageView display, ImageView drawView){
//        set some variables
        image_view = display;
        draw_view = drawView;
        width = image_view.getWidth();
        height = image_view.getHeight();
        bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        drawBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        background = new Canvas(bitmap);
        drawArea = new Canvas(drawBitmap);
        random = new Random();
        gesturePath = new Path();
        gesturePathPaint = new Paint();
        gesturePathPaint.setColor(0x80FFFFFF);
        gesturePathPaint.setAntiAlias(true);
        gesturePathPaint.setStrokeWidth(50);
        gesturePathPaint.setStyle(Paint.Style.STROKE);
        gesturePathPaint.setStrokeJoin(Paint.Join.ROUND);
        gesturePathPaint.setStrokeCap(Paint.Cap.ROUND);
        erasePaint = new Paint();
        erasePaint.setColor(Color.TRANSPARENT);
        erasePaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        erasePaint.setAntiAlias(true);
        erasePaint.setStrokeWidth(52);
        erasePaint.setStyle(Paint.Style.STROKE);
        erasePaint.setStrokeJoin(Paint.Join.ROUND);
        erasePaint.setStrokeCap(Paint.Cap.ROUND);
        gestureShapePaint = new Paint();
        gestureShapePaint.setColor(Color.BLUE); // Set paint color
        gestureShapePaint.setStrokeWidth(5);    // Set paint stroke width
        gestureStack = new ArrayList<>();
    }

    public void start(){
        gestureStack.clear();

        for (int i = 0; i < numHorizontalLines; i++){
            gestureStack.add(new HorizontalLine());
        }
        for (int i = 0; i < numVerticalLines; i++){
            gestureStack.add(new VerticalLine());
        }
        for (int i = 0; i < numSlashes; i++){
            gestureStack.add(new Slash());
        }
        for (int i = 0; i < numBackSlashes; i++){
            gestureStack.add(new BackSlash());
        }
        for (int i = 0; i < numCircles; i++){
            gestureStack.add(new Circle());
        }
        for (int i = 0; i < numStatics; i++){
            gestureStack.add(new Static());
        }
        nextGesture();
    }

    public void setCounts(int[] counts){
        numHorizontalLines = counts[0];
        numVerticalLines = counts[1];
        numSlashes = counts[2];
        numBackSlashes = counts[3];
        numCircles = counts[4];
        numStatics = counts[5];
    }

    private void nextGesture(){
        background.drawColor(Color.BLACK);
        if (!gestureStack.isEmpty()){
            gestureStack.remove(gestureStack.size()-1).draw();
        } else {
            complete = true;
        }
    }

    public void processTouchEvent(MotionEvent event){
        float x = event.getX();
        float y = event.getY();

//// Get the position of image_view on the screen (top-left corner of the image view)
//        int[] location = new int[2];
//        image_view.getLocationOnScreen(location);
//
//// Scale factor handling (if image_view is scaled, adjust coordinates)
//        float scaleX = image_view.getWidth() / (float) image_view.getMeasuredWidth();
//        float scaleY = image_view.getHeight() / (float) image_view.getMeasuredHeight();
//
//// Adjust touch coordinates for scaling
//        float x = (touchX - location[0]) / scaleX;
//        float y = (touchY - location[1]) / scaleY;

        if (!circleActive) {
            if (targetX != -1 && targetY != -1) {
                float distance = (float) Math.sqrt(Math.pow(x - targetX, 2) + Math.pow(y - targetY, 2));
                if (distance <= 50) {
                    nextGesture();
                }
            }
        }

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                gesturePath.moveTo(x, y); // Start a new path at the touch point

                if (circleActive){
                    targetX = (int)x;
                    targetY = (int)y;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                gesturePath.lineTo(x, y); // Draw a line to the new touch point
                break;
            case MotionEvent.ACTION_UP:
                if (circleActive){
                    float distance = (float) Math.sqrt(Math.pow(x - targetX, 2) + Math.pow(y - targetY, 2));
                    if (distance <= 75) {
                        circleActive = false;
                        nextGesture();
                    }
                }
                drawArea.drawPath(gesturePath, erasePaint);
                gesturePath.reset();
                break;
        }

        drawArea.drawPath(gesturePath, gesturePathPaint);
        draw_view.setImageBitmap(drawBitmap);
        image_view.setImageBitmap(bitmap);
    }

    interface GestureShape{
        void draw();
    }

    private class HorizontalLine implements GestureShape{
        int y;
        int startX;
        int endX;

        private HorizontalLine(){
            y = random.nextInt(height-2*BORDER) + BORDER;
            startX = random.nextInt(width-MIN_LENGTH-2*BORDER)+BORDER;
            endX = startX + random.nextInt(Math.max(0, width - startX-MIN_LENGTH-2*BORDER)) + MIN_LENGTH + BORDER;

            if (random.nextInt(2) == 0){
                int temp = startX;
                startX = endX;
                endX = temp;
            }
        }

        public void draw(){
            targetX = endX;
            targetY = y;
            background.drawLine(startX, y, endX, y, gestureShapePaint); // Draw a line
            drawArrowhead(background, endX, y, 25, y, y, startX, endX);
            image_view.setImageBitmap(bitmap);
        }

    }
    private class VerticalLine implements GestureShape{
        int x;
        int startY;
        int endY;

        private VerticalLine(){
            x = random.nextInt(width-2*BORDER) + BORDER;
            startY = random.nextInt(height-MIN_LENGTH-2*BORDER)+BORDER;
            endY = startY + random.nextInt(Math.max(0, height - startY-MIN_LENGTH-2*BORDER)) + MIN_LENGTH + BORDER;

            if (random.nextInt(2) == 0){
                int temp = startY;
                startY = endY;
                endY = temp;
            }
        }

        public void draw(){
            targetX = x;
            targetY = endY;
            background.drawLine(x, startY, x, endY, gestureShapePaint); // Draw a line
            drawArrowhead(background, x, endY, 25, startY, endY, x, x);
            image_view.setImageBitmap(bitmap);
        }

    }
    private class Slash implements GestureShape{
        int startX;
        int startY;
        int endX;
        int endY;

        private Slash(){
            startX = random.nextInt(width - 2 * BORDER - MIN_LENGTH) + BORDER;
            endX = random.nextInt(width - startX - 2 * BORDER - MIN_LENGTH) + startX + MIN_LENGTH;
            endY = random.nextInt(height - 2 * BORDER - MIN_LENGTH) + BORDER;
            startY = random.nextInt(height - endY - 2 * BORDER - MIN_LENGTH) + endY + MIN_LENGTH;
            if (random.nextInt(2) == 0){
                int temp = startX;
                startX = endX;
                endX = temp;
                temp = startY;
                startY = endY;
                endY = temp;
            }
        }

        public void draw(){
            targetX = endX;
            targetY = endY;
            background.drawLine(startX, startY, endX, endY, gestureShapePaint); // Draw a line
            drawArrowhead(background, endX, endY, 25, startY, endY, startX, endX);
            image_view.setImageBitmap(bitmap);
        }

    }
    private class BackSlash implements GestureShape{
        int startX;
        int startY;
        int endX;
        int endY;

        private BackSlash(){
            startX = random.nextInt(width - 2 * BORDER - MIN_LENGTH) + BORDER;
            endX = random.nextInt(width - startX - 2 * BORDER - MIN_LENGTH) + startX + MIN_LENGTH;
            startY = random.nextInt(height - 2 * BORDER - MIN_LENGTH) + BORDER;
            endY = random.nextInt(height - startY - 2 * BORDER - MIN_LENGTH) + startY + MIN_LENGTH;
            if (random.nextInt(2) == 0){
                int temp = startX;
                startX = endX;
                endX = temp;
                temp = startY;
                startY = endY;
                endY = temp;
            }
        }

        public void draw(){
            targetX = endX;
            targetY = endY;
            background.drawLine(startX, startY, endX, endY, gestureShapePaint); // Draw a line
            drawArrowhead(background, endX, endY, 25, startY, endY, startX, endX);
            image_view.setImageBitmap(bitmap);
        }
    }
    private class Circle implements GestureShape{
        int y;
        int x;
        int radius;

        private Circle(){
            radius = random.nextInt(Math.min(height, width)/2-2*BORDER);
            y = radius + random.nextInt(height-radius-2*BORDER)+BORDER;
            x = radius + random.nextInt(width-radius-2*BORDER)+BORDER;
        }

        public void draw(){
            circleActive = true;
            gestureShapePaint.setStyle(Paint.Style.STROKE);
            background.drawCircle(x, y, radius, gestureShapePaint);
            gestureShapePaint.setStyle(Paint.Style.FILL);

            if (random.nextInt(2) == 0){
                drawArrowhead(background, x, y+radius, 25, y+radius, y+radius, x-1, x);
            } else {
                drawArrowhead(background, x, y+radius, 25, y+radius, y+radius, x+1, x);
            }
            image_view.setImageBitmap(bitmap);
        }
    }
    private class Static implements GestureShape {
        int x;
        int y;

        private Static(){
            x = 20 + random.nextInt(width-20-2*BORDER)+BORDER;
            y = 20 + random.nextInt(height-20-2*BORDER)+BORDER;
        }

        public void draw(){
            targetX = x;
            targetY = y;
            background.drawCircle(x, y, 20, gestureShapePaint);
            image_view.setImageBitmap(bitmap);
        }
    }
    private void drawArrowhead(Canvas canvas, float x, float y, float arrowLength, int startY, int endY, int startX, int endX) {
        // Calculate the angle of the line
        float angle = (float) Math.atan2(endY - startY, endX - startX);

        // Calculate the arrowhead points
        float arrowPointX1 = x - arrowLength * (float) Math.cos(angle - Math.PI / 6);
        float arrowPointY1 = y - arrowLength * (float) Math.sin(angle - Math.PI / 6);

        float arrowPointX2 = x - arrowLength * (float) Math.cos(angle + Math.PI / 6);
        float arrowPointY2 = y - arrowLength * (float) Math.sin(angle + Math.PI / 6);

        // Draw the arrowhead (a triangle)
        Path arrowPath = new Path();
        arrowPath.moveTo(x, y);  // Tip of the arrow
        arrowPath.lineTo(arrowPointX1, arrowPointY1);  // Left point of the arrow
        arrowPath.lineTo(arrowPointX2, arrowPointY2);  // Right point of the arrow
        arrowPath.close();  // Close the path to form the triangle

        canvas.drawPath(arrowPath, gestureShapePaint);
    }


}

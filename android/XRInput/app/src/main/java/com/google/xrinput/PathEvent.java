package com.google.xrinput;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.os.CountDownTimer;
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
import android.os.Handler;
import android.widget.TextView;

import org.w3c.dom.Text;


public class PathEvent {
    private final Canvas background;
    private final Bitmap bitmap;
    private final ImageView image_view;
    final private int width;
    final private int height;
    final private Random random;

    private Path gesturePath;
    private Paint gesturePathPaint;
    private Paint erasePaint;
    private Paint gestureShapePaint;

    private List<PathShape> gestureStack;

    private int numHorizontalLines;
    private int numVerticalLines;
    private int numBackSlashes;
    private int numSlashes;
    private int numCircles;
    private int numStatics;
    private final int BORDER = 0;
    private int MIN_LENGTH = 50;
    private final ImageView draw_view;
    private final Bitmap drawBitmap;
    private final Canvas drawArea;
    private final Paint arrowPaint;
    private int[] locationOnScreen;
    public PathShape active;
    private boolean gesturePathBool = true;
    private boolean terminate = false;
    private int freeformDuration;
    private TextView timerView;
    private CountDownTimer countDownTimer;
    private TextView gestureText;

    public PathEvent(ImageView display, ImageView drawView, TextView timerView, TextView gestureText){
//        set some variables
        image_view = display;
        draw_view = drawView;
        this.gestureText = gestureText;
        this.timerView = timerView;
        width = image_view.getWidth();
        height = image_view.getHeight();
        MIN_LENGTH = (int)(Math.min(width, height)*0.75);
        bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        drawBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        background = new Canvas(bitmap);
        drawArea = new Canvas(drawBitmap);
        random = new Random();

        gesturePath = new Path();
        gesturePathPaint = new Paint();
        gesturePathPaint.setColor(Color.argb(16, 255, 255, 255));
        gesturePathPaint.setAntiAlias(true);
        gesturePathPaint.setStrokeWidth(15);
        gesturePathPaint.setStyle(Paint.Style.STROKE);
        gesturePathPaint.setStrokeJoin(Paint.Join.ROUND);
        gesturePathPaint.setStrokeCap(Paint.Cap.ROUND);

        erasePaint = new Paint();
        erasePaint.setColor(Color.TRANSPARENT);
        erasePaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        erasePaint.setAntiAlias(true);
        erasePaint.setStrokeWidth(17);
        erasePaint.setStyle(Paint.Style.STROKE);
        erasePaint.setStrokeJoin(Paint.Join.ROUND);
        erasePaint.setStrokeCap(Paint.Cap.ROUND);

        gestureShapePaint = new Paint();
        gestureShapePaint.setColor(Color.GREEN);
        gestureShapePaint.setStrokeWidth(25);
        gestureShapePaint.setStrokeCap(Paint.Cap.ROUND);

        gestureStack = new ArrayList<>();
        arrowPaint = new Paint();
        arrowPaint.setColor(Color.RED);
        arrowPaint.setStrokeWidth(15);
        arrowPaint.setStyle(Paint.Style.FILL);
        arrowPaint.setStrokeJoin(Paint.Join.ROUND);

        locationOnScreen = new int[2];
        image_view.getLocationOnScreen(locationOnScreen);
    }

    public void start(){
        gestureStack.clear();
        terminate = false;
        gestureStack.add(new FreeForm());
        for (int i = 0; i < numStatics; i++){
            gestureStack.add(new Static());
        }
        for (int i = 0; i < numCircles; i++){
            gestureStack.add(new Circle());
        }
        for (int i = 0; i < numBackSlashes; i++){
            gestureStack.add(new BackSlash());
        }
        for (int i = 0; i < numSlashes; i++){
            gestureStack.add(new Slash());
        }
        for (int i = 0; i < numVerticalLines; i++){
            gestureStack.add(new VerticalLine());
        }
        for (int i = 0; i < numHorizontalLines; i++){
            gestureStack.add(new HorizontalLine());
        }

        nextPath();
    }

    public void terminate(){
        terminate = true;
    }

    private void updateLocations(){
        locationOnScreen = new int[2];
        image_view.getLocationOnScreen(locationOnScreen);
    }


    public void setCounts(int[] counts){
        numHorizontalLines = counts[0];
        numVerticalLines = counts[1];
        numSlashes = counts[2];
        numBackSlashes = counts[3];
        numCircles = counts[4];
        numStatics = counts[5];
        freeformDuration = counts[6] * 1000;
    }

    private void nextPath(){
        background.drawColor(Color.BLACK);
        gestureText.setText("");
        updateLocations();

        if (terminate){
            return;
        }
        if (!gestureStack.isEmpty()){
            PathShape curr = gestureStack.remove(gestureStack.size()-1);
            active = curr;
            curr.draw();

            if (curr instanceof FreeForm){
                gestureText.setText("Freeform");
                startCountdown(freeformDuration/1000);
                new Handler().postDelayed(this::clearCanvas, freeformDuration);
                new Handler().postDelayed(this::nextPath, freeformDuration+1000);
            } else {
                startCountdown(8);
                new Handler().postDelayed(this::clearCanvas, 8000);
                new Handler().postDelayed(this::nextPath, 10000);
            }
        }
    }

    private void clearCanvas(){
        background.drawColor(Color.TRANSPARENT, android.graphics.PorterDuff.Mode.CLEAR);
        image_view.invalidate();
        active = null;
    }

    public void disableGesturePath(){
        gesturePathBool = false;
    }

    public void enableGesturePath(){
        gesturePathBool = true;
    }

    public void processTouchEvent(MotionEvent event){
        if (!gesturePathBool) return;

        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                gesturePath.moveTo(x, y); // Start a new path at the touch point
                break;
            case MotionEvent.ACTION_MOVE:
                gesturePath.lineTo(x, y); // Draw a line to the new touch point
                break;
            case MotionEvent.ACTION_UP:
                drawArea.drawPath(gesturePath, erasePaint);
                gesturePath.reset();
                break;
        }

        drawArea.drawPath(gesturePath, gesturePathPaint);
        draw_view.setImageBitmap(drawBitmap);
        image_view.setImageBitmap(bitmap);
    }

    private void startCountdown(int seconds) {
        // Convert seconds to milliseconds for the CountDownTimer
        countDownTimer = new CountDownTimer(seconds * 1000, 1000) {
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

    public interface PathShape{
        void draw();
        String repr();
    }

    private static class FreeForm implements PathShape{
        private FreeForm(){}
        public void draw(){}
        public String repr(){return "FREEFORM";}

    }
    private class HorizontalLine implements PathShape{
        int y;
        int startX;
        int endX;

        private HorizontalLine(){
            y = random.nextInt(height);
            startX = random.nextInt(width - MIN_LENGTH);
            endX = startX + random.nextInt(Math.max(0, width - startX - MIN_LENGTH)) + MIN_LENGTH;

            if (random.nextInt(2) == 0){
                int temp = startX;
                startX = endX;
                endX = temp;
            }
        }

        public void draw(){
            background.drawLine(startX, y, endX, y, gestureShapePaint); // Draw a line
//            drawArrowhead(background, endX, y, 30, y, y, startX, endX);
            image_view.setImageBitmap(bitmap);
        }

        public String repr(){
            return "HORIZONTAL_LINE,"
                    + (this.startX + locationOnScreen[0])
                    + ","
                    + (this.endX + locationOnScreen[0])
                    + ","
                    + (this.y + locationOnScreen[1]);
        }

    }
    private class VerticalLine implements PathShape{
        int x;
        int startY;
        int endY;

        private VerticalLine(){
            x = random.nextInt(width);
            startY = random.nextInt(height - MIN_LENGTH);
            endY = startY + random.nextInt(Math.max(0, height - startY - MIN_LENGTH)) + MIN_LENGTH;

            if (random.nextInt(2) == 0){
                int temp = startY;
                startY = endY;
                endY = temp;
            }
        }

        public void draw(){
            background.drawLine(x, startY, x, endY, gestureShapePaint); // Draw a line
//            drawArrowhead(background, x, endY, 30, startY, endY, x, x);
            image_view.setImageBitmap(bitmap);
        }

        public String repr(){
            return "VERTICAL_LINE,"
                    + (this.x + locationOnScreen[0])
                    + ","
                    + (this.startY + locationOnScreen[1])
                    + ","
                    + (this.endY + locationOnScreen[1]);
        }

    }
    private class Slash implements PathShape{
        int startX;
        int startY;
        int endX;
        int endY;

        private Slash(){
            startX = random.nextInt(width - MIN_LENGTH);
            endX = random.nextInt(width - startX - MIN_LENGTH) + startX + MIN_LENGTH;
            endY = random.nextInt(height - MIN_LENGTH);
            startY = random.nextInt(height - endY - MIN_LENGTH) + endY + MIN_LENGTH;
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
            background.drawLine(startX, startY, endX, endY, gestureShapePaint); // Draw a line
//            drawArrowhead(background, endX, endY, 30, startY, endY, startX, endX);
            image_view.setImageBitmap(bitmap);
        }

        public String repr(){
            return "SLASH,"
                    + (this.startX + locationOnScreen[0])
                    + ","
                    + (this.endX+ locationOnScreen[0])
                    + ","
                    + (this.startY + locationOnScreen[1])
                    + ","
                    + (this.endY + locationOnScreen[1]);
        }

    }
    private class BackSlash implements PathShape{
        int startX;
        int startY;
        int endX;
        int endY;

        private BackSlash(){
            startX = random.nextInt(width - MIN_LENGTH);
            endX = random.nextInt(width - startX - MIN_LENGTH) + startX + MIN_LENGTH;
            startY = random.nextInt(height - MIN_LENGTH);
            endY = random.nextInt(height - startY - MIN_LENGTH) + startY + MIN_LENGTH;
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
            background.drawLine(startX, startY, endX, endY, gestureShapePaint); // Draw a line
//            drawArrowhead(background, endX, endY, 30, startY, endY, startX, endX);
            image_view.setImageBitmap(bitmap);
        }

        public String repr(){
            return "BACK_SLASH,"
                    + (this.startX + locationOnScreen[0])
                    + ","
                    + (this.endX+ locationOnScreen[0])
                    + ","
                    + (this.startY + locationOnScreen[1])
                    + ","
                    + (this.endY + locationOnScreen[1]);
        }
    }
    private class Circle implements PathShape{
        int y;
        int x;
        int radius;

        private Circle(){
            radius = Math.max(MIN_LENGTH/2, random.nextInt(Math.min(height, width)/2));
            y = radius + random.nextInt(height-2*radius);
            x = radius + random.nextInt(width-2*radius);
        }

        public void draw(){
            gestureShapePaint.setStyle(Paint.Style.STROKE);
            background.drawCircle(x, y, radius, gestureShapePaint);
            gestureShapePaint.setStyle(Paint.Style.FILL);

            if (random.nextBoolean()){
                drawArrowhead(background, x, y-radius, 40, y+radius, y+radius, x-1, x);
            } else {
                drawArrowhead(background, x, y+radius, 40, y+radius, y+radius, x, x);
            }
            image_view.setImageBitmap(bitmap);
        }

        public String repr(){
            return "CIRCLE,"
                    + (this.x + locationOnScreen[0])
                    + ","
                    + (this.y+ locationOnScreen[1])
                    + ","
                    + this.radius;
        }
    }
    private class Static implements PathShape {
        int x;
        int y;
        int radius = 100;

        private Static(){
            x = radius + random.nextInt(width - 2*radius);
            y = radius + random.nextInt(height - 2*radius);
        }

        public void draw(){
            background.drawCircle(x, y, radius, gestureShapePaint);
            image_view.setImageBitmap(bitmap);
        }

        public String repr(){
            return "STATIC,"
                    + (this.x + locationOnScreen[0])
                    + ","
                    + (this.y+ locationOnScreen[1]);
        }
    }
    private void drawArrowhead(Canvas canvas, float x, float y, float arrowLength, int startY, int endY, int startX, int endX) {

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
        arrowPath.close();

//        canvas.drawPath(arrowPath, arrowPaint);
    }


}

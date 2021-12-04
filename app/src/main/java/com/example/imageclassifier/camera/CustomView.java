package com.example.imageclassifier.camera;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.View;

public class CustomView extends View {

    private Paint paint;
    private float x, y, r = 5;

    public CustomView(Context context){
        super(context);

        paint = new Paint();
        paint.setColor(Color.CYAN);
        paint.setStrokeWidth(5f);
    }

    @Override
    protected void onDraw(Canvas canvas){

        canvas.drawCircle(x, y, r, paint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event){

        x = event.getX();
        y = event.getY();

        invalidate();

        return true;
    }

}

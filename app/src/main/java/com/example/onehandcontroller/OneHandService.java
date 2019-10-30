package com.example.onehandcontroller;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.gesture.Gesture;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.constraintlayout.widget.ConstraintLayout;

public class OneHandService extends AccessibilityService {

    private LinearLayout floatingLayout;
    private View cursorView;
    private View swipeView;

    private WindowManager wm;
    private WindowManager.LayoutParams params;
    private WindowManager.LayoutParams cursorParams;
    private boolean isDown = false;
    private int cursorX = 0, cursorY = 0;
    private int lastX, lastY;

    private int LAYOUT_FLAG;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        floatingLayout = (LinearLayout) LayoutInflater.from(this).inflate(R.layout.floating_layout, null);
        setFloatingViewListener();

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            LAYOUT_FLAG = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        }
        else{
            LAYOUT_FLAG = WindowManager.LayoutParams.TYPE_PHONE;
        }

        params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                LAYOUT_FLAG,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT);

        params.height = 500;
        params.width = 500;
        params.gravity = Gravity.BOTTOM | Gravity.RIGHT;        //Initially view will be added to top-left corner

        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        // add overlay
        wm.addView(floatingLayout, params);

        swipeView = LayoutInflater.from(this).inflate(R.layout.view_swipe, null);
        swipeView.setOnTouchListener(new OnSwipeTouchListener(getApplicationContext()) {
            public void onSwipeTop() {
                Log.e("플링", "위");
            }
            public void onSwipeRight() {
                Log.e("플링", "오른");
            }
            public void onSwipeLeft() {
                Log.e("플링", "왼");
                params = new WindowManager.LayoutParams(
                        WindowManager.LayoutParams.MATCH_PARENT,
                        WindowManager.LayoutParams.WRAP_CONTENT,
                        LAYOUT_FLAG,
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                        PixelFormat.TRANSLUCENT);

                params.height = 500;
                params.width = 500;
                params.gravity = Gravity.BOTTOM | Gravity.RIGHT;        //Initially view will be added to top-left corner

                wm = (WindowManager) getSystemService(WINDOW_SERVICE);
                // add overlay
                wm.addView(floatingLayout, params);

                cursorParams = new WindowManager.LayoutParams(
                        WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT,
                        WindowManager.LayoutParams.TYPE_APPLICATION_PANEL,
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                        PixelFormat.TRANSLUCENT);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    cursorParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
                }
                cursorParams.x = cursorX;
                cursorParams.y = cursorY;
                cursorParams.height = 100;
                cursorParams.width = 100;
                wm.addView(cursorView, cursorParams);
            }
            public void onSwipeBottom() {
                Log.e("플링", "아래");
            }
        });

        params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                LAYOUT_FLAG,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT);

        params.height = 1000;
        params.width = 50;
        params.gravity = Gravity.BOTTOM | Gravity.RIGHT;        //Initially view will be added to top-left corner

        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        // add overlay
        wm.addView(swipeView, params);



        cursorView = LayoutInflater.from(this).inflate(R.layout.cursor_view, null);
        cursorParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_PANEL,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            cursorParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        }
        cursorParams.x = cursorX;
        cursorParams.y = cursorY;
        cursorParams.height = 100;
        cursorParams.width = 100;
        wm.addView(cursorView, cursorParams);

        Button btn = floatingLayout.findViewById(R.id.btn);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //stopSelf();
                wm.removeViewImmediate(floatingLayout);
                wm.removeViewImmediate(cursorView);
            }
        });

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {

    }

    @Override
    public void onInterrupt() {

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        floatingLayout.setVisibility(View.GONE);
        cursorView.setVisibility(View.GONE);
        floatingLayout = null;
        cursorView = null;
    }

    private void setFloatingViewListener() {
        final GestureDetector detector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                Log.v("single tap", "occur");
                DisplayMetrics dm = getApplicationContext().getResources().getDisplayMetrics();
                int width = dm.widthPixels;
                int height = dm.heightPixels;
                clickByCor(cursorX + width / 2, cursorY + height / 2);
                Toast.makeText(OneHandService.this, "클릭!", Toast.LENGTH_SHORT).show();
                return super.onSingleTapUp(e);
            }
        });
        floatingLayout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                detector.onTouchEvent(event);
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        isDown = true;
                        lastX = (int)event.getX();
                        lastY = (int)event.getY();
                        break;
                    case MotionEvent.ACTION_MOVE:
                        if(isDown) {
                            cursorX += event.getX() - lastX;
                            cursorY += event.getY() - lastY;
                            lastX = (int) event.getX();
                            lastY = (int) event.getY();
                            cursorParams.x = cursorX;
                            cursorParams.y = cursorY;
                            wm.updateViewLayout(cursorView, cursorParams);
                        }
                        break;
                    case MotionEvent.ACTION_UP:
                        isDown = false;
                        break;
                }
                return false;
            }
        });
    }



    private void clickByCor(int x, int y) {
        clickAtPosition(x, y, getRootInActiveWindow());
    }

    private void clickAtPosition(int x, int y, AccessibilityNodeInfo node) {
        if (node == null) return;

        if (node.getChildCount() == 0) {
            Rect buttonRect = new Rect();
            node.getBoundsInScreen(buttonRect);
            if (buttonRect.contains(x, y)) {
                // Maybe we need to think if a large view covers item?
                node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                System.out.println("1º - Node Information: " + node.toString());
            }
        } else {
            Rect buttonRect = new Rect();
            node.getBoundsInScreen(buttonRect);
            if (buttonRect.contains(x, y)) {
                // Maybe we need to think if a large view covers item?
                node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                System.out.println("2º - Node Information: " + node.toString());
            }
            for (int i = 0; i < node.getChildCount(); i++) {
                clickAtPosition(x, y, node.getChild(i));
            }
        }
    }
}

package com.ohc.onehandcontroller;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.Context;
import android.content.Intent;
import android.graphics.Matrix;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.os.Build;
import android.os.Vibrator;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

public class OneHandService extends AccessibilityService {

    private static final int CLICK_MODE = 0;
    private static final int MOVE_MODE = 1;

    private LinearLayout floatingLayout;
    private View cursorView;
    private View swipeView;

    private boolean leftSwipeFlag = false;
    private boolean alwaysPadFlag = false;

    private int controlPadWidth = 500;
    private int controlPadHeight = 500;

    private int cursorWidth = 80;
    private int cursorHeight = 80;

    private int displayWidth, displayHeight;

    private WindowManager wm;
    private WindowManager.LayoutParams floatingPadLayoutParams;
    private WindowManager.LayoutParams swipeParams;
    private WindowManager.LayoutParams cursorParams;

    private boolean isDown = false;
    private boolean isModeChange = false;
    private boolean isShowPad = false;
    private int cursorX = 0, cursorY = 0;
    private int lastX, lastY;
    private int curMode = CLICK_MODE;

    private int LAYOUT_FLAG;

    private Vibrator vibrator;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        DisplayMetrics dm = getApplicationContext().getResources().getDisplayMetrics();
        displayWidth = dm.widthPixels;
        displayHeight = dm.heightPixels;
        leftSwipeFlag = intent.getBooleanExtra("leftSwipe", false);
        alwaysPadFlag = intent.getBooleanExtra("alwaysPad", false);

        floatingLayout = (LinearLayout) LayoutInflater.from(this).inflate(R.layout.floating_layout, null);
        setFloatingViewListener();

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            LAYOUT_FLAG = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        }
        else{
            LAYOUT_FLAG = WindowManager.LayoutParams.TYPE_PHONE;
        }

        floatingPadLayoutParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                LAYOUT_FLAG,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT);
        floatingPadLayoutParams.height = controlPadHeight;
        floatingPadLayoutParams.width = controlPadWidth;
        floatingPadLayoutParams.x = displayWidth / 2;
        if(leftSwipeFlag) {
            floatingPadLayoutParams.x *= -1;
        }
        floatingPadLayoutParams.y = displayHeight / 2;

        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        // add overlay
        wm.addView(floatingLayout, floatingPadLayoutParams);

        swipeView = LayoutInflater.from(this).inflate(R.layout.view_swipe, null);
        swipeView.setOnTouchListener(new OnSwipeTouchListener(getApplicationContext()) {
            public void onSwipeTop() {
                Log.e("플링", "위");
                Toast.makeText(OneHandService.this, "종료 ", Toast.LENGTH_SHORT).show();
                removeAllViews();
            }
            public void onSwipeRight() {
                Log.e("플링", "오른");
                if(!isShowPad && leftSwipeFlag) {
                    showPad();
                }
            }
            public void onSwipeLeft() {
                Log.e("플링", "왼");
                if(!isShowPad && !leftSwipeFlag) {
                    showPad();
                }
            }
            public void onSwipeBottom() {
                Log.e("플링", "아래");
                if(isShowPad && !alwaysPadFlag) {
                    hidePad();
                }
            }
        });

        swipeParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                LAYOUT_FLAG,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT);

        swipeParams.height = displayHeight / 3;
        swipeParams.width = 50;
        swipeParams.gravity = Gravity.RIGHT;
        if(leftSwipeFlag) {
            swipeParams.gravity = Gravity.LEFT;
        }
        swipeParams.y = displayHeight - swipeParams.height / 2 - displayHeight / 2;

        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        // add overlay
        wm.addView(swipeView, swipeParams);



        cursorView = LayoutInflater.from(this).inflate(R.layout.view_cursor, null);
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
        cursorParams.height = cursorHeight;
        cursorParams.width = cursorWidth;
        wm.addView(cursorView, cursorParams);

        isShowPad = true;

        Button btn = floatingLayout.findViewById(R.id.btn);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hidePad();
            }
        });

        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        return super.onStartCommand(intent, flags, startId);
    }

    private void showPad() {
        floatingLayout.setVisibility(View.VISIBLE);
        cursorView.setVisibility(View.VISIBLE);
        isShowPad = true;
    }

    private void hidePad() {
        floatingLayout.setVisibility(View.GONE);
        cursorView.setVisibility(View.GONE);
        isShowPad = false;
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {

        if(event.getEventType() == AccessibilityEvent.TYPE_VIEW_CLICKED) {
            if(event.getPackageName().equals("com.example.onehandcontroller")) {
                try {
                    String contentDescription = event.getSource().getContentDescription().toString();
                    if(contentDescription.equals("padTurnSwitch") && !event.getSource().isChecked()) {
                        removeAllViews();
                    } else if(contentDescription.equals("alwaysPadSwitch")) {
                        if(event.getSource().isChecked()) {
                            showPad();
                            alwaysPadFlag = true;
                        } else {
                            alwaysPadFlag = false;
                        }
                    } else if(contentDescription.equals("rightSwipeSwitch")) {
                        if(event.getSource().isChecked()) {
                            swipeParams.gravity = Gravity.RIGHT;
                        } else {
                            swipeParams.gravity = Gravity.LEFT;
                        }
                        wm.updateViewLayout(swipeView, swipeParams);
                    } else if(contentDescription.equals("leftSwipeSwitch")) {
                        if(event.getSource().isChecked()) {
                            swipeParams.gravity = Gravity.LEFT;
                        } else {
                            swipeParams.gravity = Gravity.RIGHT;
                        }
                        wm.updateViewLayout(swipeView, swipeParams);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            Log.v("click", "click");
        }
    }

    @Override
    public void onInterrupt() {
        Log.v("interrupt", "interrupt");
    }

    @Override
    public void onDestroy() {
        removeAllViews();
        super.onDestroy();
    }

    private void setFloatingViewListener() {
        final GestureDetector detector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                Log.v("single tap", "occur");
                switch (curMode) {
                    case CLICK_MODE:
                        vibrator.vibrate(3);
                        performGesture(createSingleTap(cursorX + displayWidth / 2 + cursorWidth / 2, cursorY + displayHeight / 2 + cursorHeight / 2));
                        Log.v("event", "cursorX : " + (cursorX + displayWidth / 2) + ", cursorY : " + (cursorY + displayHeight / 2));
                        break;
                    case MOVE_MODE:
                        break;
                }
                return super.onSingleTapConfirmed(e);
            }

            @Override
            public boolean onDoubleTap(MotionEvent e) {
                Log.v("double tap", "occur");
                //curMode = (curMode + 1) % 2;
                isModeChange = true;
                performGlobalAction(GLOBAL_ACTION_BACK);
                return super.onDoubleTap(e);
            }

            @Override
            public void onLongPress(MotionEvent e) {
                vibrator.vibrate(500);
                performGesture(createLongClick(cursorX + displayWidth / 2 + cursorWidth / 2, cursorY + displayHeight / 2 + cursorHeight / 2));
                super.onLongPress(e);
            }
        });
        floatingLayout.findViewById(R.id.floatingPad).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                detector.onTouchEvent(event);

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        curMode = CLICK_MODE;
                        lastX = (int)event.getX();
                        lastY = (int)event.getY();
                        break;
                    case MotionEvent.ACTION_MOVE:
                        if(curMode == CLICK_MODE) {
                            cursorX += (event.getX() - lastX) * 1.5f;
                            cursorY += (event.getY() - lastY) * 1.5f;
                            cursorX = cursorX < -displayWidth / 2 - cursorWidth / 2 ? -displayWidth / 2 - cursorWidth / 2 : cursorX;
                            cursorY = cursorY < -displayHeight / 2 - cursorHeight / 2 ? -displayHeight / 2 - cursorHeight / 2 : cursorY;
                            cursorX = cursorX > displayWidth / 2 - cursorWidth / 2 ? displayWidth / 2 - cursorWidth / 2 : cursorX;
                            cursorY = cursorY > displayHeight / 2 - cursorHeight / 2 ? displayHeight / 2 - cursorHeight / 2 : cursorY;
                            cursorParams.x = cursorX;
                            cursorParams.y = cursorY;
                            wm.updateViewLayout(cursorView, cursorParams);

                            lastX = (int) event.getX();
                            lastY = (int) event.getY();
                        }
                        break;
                    case MotionEvent.ACTION_UP:

                        break;
                }
                return true;
            }
        });
        floatingLayout.findViewById(R.id.topBarLayout).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        curMode = MOVE_MODE;
                        lastX = (int)event.getRawX();
                        lastY = (int)event.getRawY();
                        break;
                    case MotionEvent.ACTION_MOVE:
                        if(curMode == MOVE_MODE) {
                            floatingPadLayoutParams.x += (event.getRawX() - lastX);
                            floatingPadLayoutParams.y += (event.getRawY() - lastY);
                            wm.updateViewLayout(floatingLayout, floatingPadLayoutParams);

                            lastX = (int) event.getRawX();
                            lastY = (int) event.getRawY();
                        }
                        break;
                    case MotionEvent.ACTION_UP:

                        break;
                }
                return true;
            }
        });
    }

    public void performGesture(GestureDescription gesture) {
        dispatchGesture(gesture, new GestureResultCallback() {
            @Override
            public void onCompleted(GestureDescription gestureDescription) {
                super.onCompleted(gestureDescription);
            }

            @Override
            public void onCancelled(GestureDescription gestureDescription) {
                super.onCancelled(gestureDescription);
            }
        }, null);
    }

    public GestureDescription createSingleTap(int x, int y) {
        Path clickPath = new Path();
        clickPath.moveTo(x, y);
        GestureDescription.StrokeDescription clickStroke =
                new GestureDescription.StrokeDescription(clickPath, 0, ViewConfiguration.getTapTimeout());
        GestureDescription.Builder clickBuilder = new GestureDescription.Builder();
        clickBuilder.addStroke(clickStroke);
        return clickBuilder.build();
    }

    public GestureDescription createLongClick(int x, int y) {
        Path clickPath = new Path();
        clickPath.moveTo(x, y);
        int longPressTime = ViewConfiguration.getLongPressTimeout();

        GestureDescription.StrokeDescription longClickStroke =
                new GestureDescription.StrokeDescription(clickPath, 0, longPressTime * 2);
        GestureDescription.Builder longClickBuilder = new GestureDescription.Builder();
        longClickBuilder.addStroke(longClickStroke);
        return longClickBuilder.build();
    }

    private GestureDescription createPinch(int centerX, int centerY, int startSpacing,
                                           int endSpacing, float orientation, long duration) {
        if ((startSpacing < 0) || (endSpacing < 0)) {
            throw new IllegalArgumentException("Pinch spacing cannot be negative");
        }
        float[] startPoint1 = new float[2];
        float[] endPoint1 = new float[2];
        float[] startPoint2 = new float[2];
        float[] endPoint2 = new float[2];

        /* Build points for a horizontal gesture centered at the origin */
        startPoint1[0] = startSpacing / 2;
        startPoint1[1] = 0;
        endPoint1[0] = endSpacing / 2;
        endPoint1[1] = 0;
        startPoint2[0] = -startSpacing / 2;
        startPoint2[1] = 0;
        endPoint2[0] = -endSpacing / 2;
        endPoint2[1] = 0;

        /* Rotate and translate the points */
        Matrix matrix = new Matrix();
        matrix.setRotate(orientation);
        matrix.postTranslate(centerX, centerY);
        matrix.mapPoints(startPoint1);
        matrix.mapPoints(endPoint1);
        matrix.mapPoints(startPoint2);
        matrix.mapPoints(endPoint2);

        Path path1 = new Path();
        path1.moveTo(startPoint1[0], startPoint1[1]);
        path1.lineTo(endPoint1[0], endPoint1[1]);
        Path path2 = new Path();
        path2.moveTo(startPoint2[0], startPoint2[1]);
        path2.lineTo(endPoint2[0], endPoint2[1]);

        GestureDescription.StrokeDescription path1Stroke = new GestureDescription.StrokeDescription(path1, 0, duration);
        GestureDescription.StrokeDescription path2Stroke = new GestureDescription.StrokeDescription(path2, 0, duration);
        GestureDescription.Builder swipeBuilder = new GestureDescription.Builder();
        swipeBuilder.addStroke(path1Stroke);
        swipeBuilder.addStroke(path2Stroke);
        return swipeBuilder.build();
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
            for (int i = 0; i < node.getChildCount(); i++) {
                clickAtPosition(x, y, node.getChild(i));
            }
        }
    }

    private void removeAllViews() {
        wm.removeViewImmediate(floatingLayout);
        wm.removeViewImmediate(cursorView);
        wm.removeViewImmediate(swipeView);
        floatingLayout = null;
        cursorView = null;
        swipeView = null;
    }
}
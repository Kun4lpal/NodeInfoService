package com.example.kupal.nodeinfoservice;


import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.os.Build;
import android.speech.tts.TextToSpeech;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;

public class DemoProxy extends AccessibilityService {

    //<-------------------------------  private Data Members  ---------------------------------------->

    private FrameLayout overlay;
    private FrameLayout overlay_wish;
    private TextToSpeech mTts;
    private int result;
    private WindowManager wm;
    private boolean once;
    private boolean once_wish;


    //<-------------------------------  onCreate method  ---------------------------------------->

    @Override
    public void onCreate() {
        super.onCreate();
    }

    //<-------------------------------  override onServiceConnected  ---------------------------------------->

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    protected void onServiceConnected() {
        AccessibilityServiceInfo info = new AccessibilityServiceInfo();
        info.eventTypes = AccessibilityEvent.TYPE_VIEW_CLICKED |
                AccessibilityEvent.TYPE_VIEW_FOCUSED;


        //<-------------------------------  create Text to Speech object  ----------------------------------->

        mTts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    result = mTts.setLanguage(Locale.US);
                } else {
                    Toast.makeText(getApplicationContext(), "Feature not supported", Toast.LENGTH_SHORT).show();
                }
            }
        });

        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_SPOKEN;
        info.notificationTimeout = 100;
        this.setServiceInfo(info);
    }

    //<-------------------------------  override onAccessibilityEvent  ---------------------------------------->


    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {

        //<-------------------------------  remove overlay after exiting whatsapp  ----------------------------->

        if (!event.getPackageName().equals("com.android.vending")) {
            if (overlay != null) {
                wm.removeView(overlay);
                overlay = null;
                once = false;
            }
            if (overlay_wish != null) {
                wm.removeView(overlay_wish);
                overlay_wish = null;
                once_wish = false;
            }
            return;
        }

        //<-------------------------------  test  ---------------------------------------->

        if (event.getPackageName().equals("com.android.systemui")) {
            Toast.makeText(this, "testing UI", Toast.LENGTH_SHORT).show();
            return;
        }

        //<-------------------------------  add overlay to whatsapp ---------------------------------------->

        if (event.getPackageName().equals("com.android.vending")) {
            AccessibilityNodeInfo source = event.getSource();
            source.getChildCount();
            Toast.makeText(this, "childCount: " + source.getChildCount(), Toast.LENGTH_SHORT).show();

            dfs(getRootInActiveWindow(), 0);


//            List<AccessibilityNodeInfo> findAccessibilityNodeInfosByViewId = null;
//            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR2) {
//                findAccessibilityNodeInfosByViewId = source.findAccessibilityNodeInfosByViewId("YOUR PACKAGE NAME:id/RESOURCE ID FROM WHERE YOU WANT DATA");
//            }

            Log.i("Event", event.toString() + "");
            Log.i("Source", source.toString() + "");

            if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_CLICKED) {
                if (event.getText().toString().contains("wishlist")) {
                    if (!once_wish) {
                        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
                        overlay_wish = new FrameLayout(this);
                        LayoutParams lp_wish = new LayoutParams();
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                            lp_wish.type = LayoutParams.TYPE_ACCESSIBILITY_OVERLAY;
                        }
                        lp_wish.format = PixelFormat.TRANSLUCENT;
                        lp_wish.flags |= LayoutParams.FLAG_NOT_FOCUSABLE;
                        lp_wish.width = LayoutParams.WRAP_CONTENT;
                        lp_wish.height = LayoutParams.WRAP_CONTENT;
                        lp_wish.gravity = Gravity.TOP;
                        lp_wish.alpha = 100;
                        LayoutInflater inflater = LayoutInflater.from(this);
                        inflater.inflate(R.layout.actionbutton2, overlay_wish);
                        configureWishButton();
                        wm.addView(overlay_wish, lp_wish);
                        once_wish = true;
                    } else {
                        wm.removeView(overlay_wish);
                        overlay_wish = null;
                        once_wish = false;
                    }
                }
                speakToUser(event.getText().toString());
            } else if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_SCROLLED) {
                speakToUser("Scrolling");
            } else if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_FOCUSED) {
                if (!once) {
                    wm = (WindowManager) getSystemService(WINDOW_SERVICE);
                    overlay = new FrameLayout(this);
                    LayoutParams lp = new LayoutParams();
                    lp.type = LayoutParams.TYPE_ACCESSIBILITY_OVERLAY;
                    lp.format = PixelFormat.TRANSLUCENT;
                    lp.flags |= LayoutParams.FLAG_NOT_FOCUSABLE;
                    lp.width = LayoutParams.WRAP_CONTENT;
                    lp.height = LayoutParams.WRAP_CONTENT;
                    lp.gravity = Gravity.TOP;
                    lp.alpha = 100;
                    LayoutInflater inflater = LayoutInflater.from(this.getApplicationContext());
                    inflater.inflate(R.layout.actionbutton, overlay);
                    configureSwipeButton();
                    //configureVolumeButton();
                    configureScrollButton();
                    configureRemoveButton();
                    wm.addView(overlay, lp);
                    once = true;
                }


            } else if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_CLICKED) {
                Toast.makeText(this, "test1", Toast.LENGTH_SHORT).show();
            } else if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED) {
                Toast.makeText(this, "test2", Toast.LENGTH_SHORT).show();
            } else if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_CONTEXT_CLICKED) {
                Toast.makeText(this, "test3", Toast.LENGTH_SHORT).show();
            } else if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_LONG_CLICKED) {
                Toast.makeText(this, "test4", Toast.LENGTH_SHORT).show();
            } else if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_SELECTED) {
                Toast.makeText(this, "test5", Toast.LENGTH_SHORT).show();
            } else if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_HOVER_ENTER) {
                Toast.makeText(this, "test6", Toast.LENGTH_SHORT).show();
            }
        }
    }


    //<-------------------------------  helper function  ---------------------------------------->

    private void speakToUser(String eventText) {
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            Toast.makeText(this, "Feature not supported", Toast.LENGTH_SHORT).show();
        } else {
            if (!eventText.contains("null")) {
                Toast.makeText(this, eventText, Toast.LENGTH_SHORT).show();
                mTts.speak(eventText, TextToSpeech.QUEUE_FLUSH, null);
            }
        }
    }

    @Override
    public void onInterrupt() {
    }


    //<-------------------------------  helper  ---------------------------------------->

    private void configureRemoveButton() {
        Button volumeUpButton = (Button) overlay.findViewById(R.id.Remove);
        volumeUpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                speakToUser("Remove Overlay");
                if (overlay != null) {
                    wm.removeView(overlay);
                    overlay = null;
                    once = false;
                }

                if (overlay_wish != null) {
                    wm.removeView(overlay_wish);
                    overlay_wish = null;
                    once_wish = false;
                }
            }
        });
    }


    //<-------------------------------  helper  ---------------------------------------->

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private AccessibilityNodeInfo findScrollableNode(AccessibilityNodeInfo root) {
        Deque<AccessibilityNodeInfo> deque = new ArrayDeque<>();
        deque.add(root);
        while (!deque.isEmpty()) {
            AccessibilityNodeInfo node = deque.removeFirst();
            if (node.getActionList().contains(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_FORWARD)) {
                return node;
            }
            for (int i = 0; i < node.getChildCount(); i++) {
                deque.addLast(node.getChild(i));
            }
        }
        return null;
    }

    //<-------------------------------  helper  ---------------------------------------->

    private void configureScrollButton() {
        Button scrollButton = (Button) overlay.findViewById(R.id.scroll);
        scrollButton.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onClick(View view) {
                AccessibilityNodeInfo scrollable = findScrollableNode(getRootInActiveWindow());
                if (scrollable != null) {
                    speakToUser("Scrolling Right!");
                    scrollable.performAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_FORWARD.getId());
                }
            }
        });
    }

    //<-------------------------------  helper  ---------------------------------------->

    private void configureWishButton() {
        Button scrollButton = (Button) overlay_wish.findViewById(R.id.wish);
        scrollButton.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onClick(View view) {
                speakToUser("Wish Button Clicked");
                TextView textView = (TextView) overlay_wish.findViewById(R.id.textview);
                textView.setText(R.string.add);
            }
        });

    }


    //<-------------------------------  helper  ---------------------------------------->

    private void configureSwipeButton() {
        Button swipeButton = (Button) overlay.findViewById(R.id.swipe);
        swipeButton.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void onClick(View view) {
                speakToUser("Browsing Apps!");
                Path swipePath = new Path();
                swipePath.moveTo(1000, 1000);
                swipePath.lineTo(100, 1000);
                GestureDescription.Builder gestureBuilder = new GestureDescription.Builder();
                gestureBuilder.addStroke(new GestureDescription.StrokeDescription(swipePath, 0, 500));
                dispatchGesture(gestureBuilder.build(), null, null);
            }
        });
    }

    public static void dfs(AccessibilityNodeInfo nodeInfo, final int depth) {

        if (nodeInfo == null) return;

        String spacerString = "";

        for (int i = 0; i < depth; ++i) {
            spacerString += '-';
        }
        //Log the info you care about here... I choce classname and view resource name, because they are simple, but interesting.
        //Log.d("TAG", spacerString + nodeInfo.getClassName() + " " + nodeInfo.getViewIdResourceName());
        if (nodeInfo.getClassName().toString().contains("android.widget.TextView")) {
            if (nodeInfo.getText() != null) {
                Log.d("logs", spacerString + nodeInfo.getText().toString());
                if (nodeInfo.getText().toString().contains("PluralSight")) {
                    AccessibilityNodeInfo nf = nodeInfo.getParent();
                    Log.d("Popup parent", nf.getViewIdResourceName());
                }
            }
//                for(int i=0;i<nodeInfo.getChildCount();i++){
//                    if(nodeInfo.getChild(i)!=null && nodeInfo.getChild(i).getClassName()!=null){
//                        if(nodeInfo.getChild(i).getClassName().toString().contains("android.widget.TextView")){
//                            if(nodeInfo.getChild(i).getText()!=null){
//
//                            }
//                            Log.d("second level",spacerString + nodeInfo);
//                        }
//                    }
//                }
        }


        for (int i = 0; i < nodeInfo.getChildCount(); ++i) {
            dfs(nodeInfo.getChild(i), depth + 1);
        }
    }
}
//<------------------------------------------------  END  ------------------------------------------------->


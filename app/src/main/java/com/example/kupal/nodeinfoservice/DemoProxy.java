package com.example.kupal.nodeinfoservice;


import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Created by kupaliwa on 8/27/17.
 */

public class DemoProxy extends AccessibilityService {

    //<-------------------------------  private Data Members  ---------------------------------------->
    
    private FrameLayout overlay_permission;
    private TextToSpeech mTts;
    private int result;
    private WindowManager wm;
    private String appName = "";
    private boolean once_permission;
    private boolean once_button;
    private ArrayList<BuildOverlay.annotationObject> listOfAnnotationObjects;
    private ArrayList<AccessibilityNodeInfo> listOfNodes;
    private AccessibilityNodeInfo current_overlay_previous_node;
    private AccessibilityNodeInfo current_overlay_next_node;
    private boolean annotation_overlay_exist = false;
    private boolean accessibility_rating_overlay_exist = false;


    //<-------------------------------  onCreate method  ---------------------------------------->

    @Override
    public void onCreate() {
        super.onCreate();
        BuildOverlay.statusBarHeight = BuildOverlay.getStatusBarHeight(this);
        BuildOverlay.window_manager = (WindowManager) getSystemService(WINDOW_SERVICE);
        once_button = false;
    }

    //<-------------------------------  override onServiceConnected  ---------------------------------------->
    // instantiated AccessibilityInfo which is responsible for specificying the accessibility parameters
    // instantiated the textToSpeech object
    // Specified the event types that are allowed. Example: TYPE_WINDOW_CONTENT_CHANGED
    // Specified feedbackType as spoken ( can be made generic instead)
    // Specified notfication timeout as 100
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    protected void onServiceConnected() {
        AccessibilityServiceInfo info = new AccessibilityServiceInfo();

        info.eventTypes = AccessibilityEvent.TYPE_VIEW_CLICKED |
                AccessibilityEvent.TYPE_VIEW_FOCUSED | AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
         | AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED;

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
    // Here we specifiy the package names of the android app we want to access
    // dfs function is for building a log of all the nodes inside the current window
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {

        //dfs(getRootInActiveWindow(),0);
        //<-------------------------------  remove all overlay after exiting playstore  ----------------------------->
        if (!event.getPackageName().equals("com.android.vending")) {
            if (overlay_permission != null) {
                wm.removeView(overlay_permission);
                overlay_permission = null;
                once_permission = false;
            }

            if(annotation_overlay_exist){
                BuildOverlay.removeOverlays(this);
                annotation_overlay_exist = false;
            }
            return;
        }

        //<------------------------------- this checks if we are system ui ---------------------------------------->

        if (event.getPackageName().equals("com.android.systemui")) {
            Toast.makeText(this, "testing UI", Toast.LENGTH_SHORT).show();
            return;
        }

        //<-------------------------------  add overlay to app store ---------------------------------------->
        // childcount is useful to get more information about the current window

        if (event.getPackageName().equals("com.android.vending")) {
            AccessibilityNodeInfo source = event.getSource();
            source.getChildCount();
            //Toast.makeText(this, "childCount: " + source.getChildCount(), Toast.LENGTH_SHORT).show();
            dfs(getRootInActiveWindow(), 0);
            Log.i("Event", event.toString() + "");
            Log.i("Source", source.toString() + "");

            if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_CLICKED) {

            } else if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_SCROLLED) {
                speakToUser("Scrolling");
                // this is the button overlay with swipe/scroll and remove button
            } else if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_FOCUSED) {
                // here we can add an event or overlay
            } else if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {

            } else if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_CLICKED) {
                Toast.makeText(this, "test1", Toast.LENGTH_SHORT).show();
            } else if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
                final Context context = this;

                // Results list is empty if the current window does not contain an install button
                List<AccessibilityNodeInfo> results =
                        getRootInActiveWindow().findAccessibilityNodeInfosByViewId("com.android.vending:id/buy_button");

                // Here we are adding a button on top of the install button if we are on the correct window
                if (results.size() > 0 && !once_button){
                    AccessibilityNodeInfo install_button = results.get(0);
                    listOfNodes = new ArrayList<AccessibilityNodeInfo>();
                    BuildOverlay.refreshListOfNodes(listOfNodes, getRootInActiveWindow());
                    Rect bounds = new Rect();
                    install_button.getBoundsInScreen(bounds);
                    bounds.top -= 150;
                    bounds.bottom -= 150;
                    listOfAnnotationObjects = new ArrayList<>();
                    View.OnClickListener clickListener = new View.OnClickListener() {
                        public void onClick(View v) {
                            if (!once_permission) {
                                wm = (WindowManager) getSystemService(WINDOW_SERVICE);
                                overlay_permission = new FrameLayout(context);
                                LayoutParams lp_wish = new LayoutParams();
                                overlay_permission.setBackgroundColor(Color.WHITE);
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                                    lp_wish.type = LayoutParams.TYPE_ACCESSIBILITY_OVERLAY;
                                }
                                //lp_wish.format = PixelFormat.TRANSLUCENT;
                                lp_wish.flags |= LayoutParams.FLAG_NOT_FOCUSABLE;
                                lp_wish.width = LayoutParams.WRAP_CONTENT;
                                lp_wish.height = LayoutParams.WRAP_CONTENT;
                                lp_wish.gravity = Gravity.TOP;
                                //lp_wish.alpha = 100;
                                LayoutInflater inflater = LayoutInflater.from(context);
                                inflater.inflate(R.layout.ui1, overlay_permission);
                                configureWishButton();
                                wm.addView(overlay_permission, lp_wish);
                                once_permission = true;
                            } else {
                                wm.removeView(overlay_permission);
                                overlay_permission = null;
                                once_permission = false;
                            }

                            new android.os.Handler().postDelayed(
                                    new Runnable() {
                                        public void run() {
                                            overlay_permission.performAccessibilityAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS, null);
                                        }
                                    },
                                    1000);
                        }
                    };
                    listOfAnnotationObjects.add(new BuildOverlay.annotationObject(context, install_button, listOfNodes, bounds, "Permissions", clickListener));
                    annotation_overlay_exist = true;
                    once_button = true;
                }

                // Here we are removing the Permission button if we are not in the app window
                if (results.size() == 0 && annotation_overlay_exist && once_button) {
                    BuildOverlay.removeOverlays(this);
                    annotation_overlay_exist = false;
                    once_button = false;
                }

                // Here we are removing the UI which is displayed when i click the Permission button
                if (overlay_permission != null && results.size() == 0) {
                    wm.removeView(overlay_permission);
                    overlay_permission = null;
                    once_permission = false;
                }

                // a few test functions
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


    //<-------------------------------  text to speech helper function  ---------------------------------------->

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
    
    //<------------------------------- configure wish button ---------------------------------------->

    private void configureWishButton() {
        final Context context = this;
        Button scrollButton = (Button) overlay_permission.findViewById(R.id.removeOv);
        TextView appN = (TextView) overlay_permission.findViewById(R.id.appName);
        appN.setText(appName);
        scrollButton.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onClick(View view) {
                if(once_permission){
                    wm.removeView(overlay_permission);
                    overlay_permission = null;
                    once_permission = false;
                }
            }
        });
    }

    //<--------------------- get all the node information or trigger an event ---------------------------------------->

    public void dfs(AccessibilityNodeInfo nodeInfo, final int depth) {

        if (nodeInfo == null) return;
        String spacerString = "";

        for (int i = 0; i < depth; ++i) {
            spacerString += '-';
        }

        if (nodeInfo.getText() != null) {    // able to get permissions with this function
            Log.d("logs", spacerString + nodeInfo.getText().toString() +
                    "------" + nodeInfo.getClassName().toString() + "------" + nodeInfo.getPackageName().toString());
            if(nodeInfo.getClassName().toString().contains("android.widget.TextView") && nodeInfo.getPackageName().toString().contains("com.android.vending")){
                appName = nodeInfo.getText().toString();
                Log.d("appName",appName);
            }
        }

        for (int i = 0; i < nodeInfo.getChildCount(); ++i) {
            dfs(nodeInfo.getChild(i), depth + 1);
        }
    }
}
//<------------------------------------------------  END  ------------------------------------------------->


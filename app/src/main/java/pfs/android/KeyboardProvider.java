package pfs.android;

// ----------------------------------------------------------------------------
// The MIT License
// UnityMobileInput https://github.com/mopsicus/UnityMobileInput
// Copyright (c) 2018 Mopsicus <mail@mopsicus.ru>
// ----------------------------------------------------------------------------
// package ru.mopsicus.mobileinput;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.PopupWindow;

public class KeyboardProvider extends PopupWindow
{
    private KeyboardObserver _observer;
    private View _popupView;
    private View _parentView;
    private Activity _activity;
    private int _staticStatusBarHeight;
    private int _staticNavBarHeight;

    public class KeyboardGeometry
    {
        public Point displayResolution = new Point();
        public int viewAreaTop = 0;
        public int viewAreaBottom = 0;
        public int keyboardHeight = 0;
        public int keyboardY = 0;
        public int orientation = 0;
        public int navBarHeight = 0;
        public int statusBarHeight = 0;
        public float density = 1f;
    }

    KeyboardGeometry _geom = new KeyboardGeometry();

    public interface KeyboardObserver
    {
        void onKeyboardGeometry (KeyboardGeometry geom);
    }

    public KeyboardProvider (Activity activity, KeyboardObserver listener)
    {
        this(activity, (ViewGroup)activity.findViewById(android.R.id.content), listener);
    }

    public KeyboardProvider (Activity activity, ViewGroup parent, KeyboardObserver listener)
    {
        super(activity);
        this._observer = listener;
        this._activity = activity;
        Resources resources = this._activity.getResources();
        String packageName = this._activity.getPackageName();
        int id = resources.getIdentifier("popup", "layout", packageName);
        LayoutInflater inflater = (LayoutInflater) activity.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        this._popupView = inflater.inflate(id, null, false);
        setContentView(_popupView);
        setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE | WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        setInputMethodMode(PopupWindow.INPUT_METHOD_NEEDED);
        _parentView = parent;
        setWidth(0);
        setHeight(WindowManager.LayoutParams.MATCH_PARENT);
        setBackgroundDrawable(new ColorDrawable(0));

        // Avoid android.view.WindowManager$BadTokenException:
        // Unable to add window -- token null is not valid; is your activity running?
        if (!isShowing()) {
            _parentView.post(new Runnable() {
                @Override
                public void run() {
                    showAtLocation(_parentView, Gravity.NO_GRAVITY, 0, 0);
                }
            });
        }

        _staticNavBarHeight = getStatusBarHeight();
        _staticStatusBarHeight = getStatusBarHeight();

        _geom.navBarHeight = getNavigationBarHeight();
        _geom.statusBarHeight = getStatusBarHeight();

        _popupView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (_popupView != null) {
                    handleOnGlobalLayout();
                }
            }
        });
    }

    private int getScreenOrientation ()
    {
        return _activity.getResources().getConfiguration().orientation;
    }

    private int getActionBarHeight ()
    {
        TypedValue tv = new TypedValue();
        _activity.getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true);
        return _activity.getResources().getDimensionPixelSize(tv.resourceId);
    }

    // Handler to get keyboard height
    private void handleOnGlobalLayout ()
    {
        // FIXME For Landscape orientation

        Rect decorRect = new Rect();
        Rect rect = new Rect();
        _activity.getWindow().getDecorView().getWindowVisibleDisplayFrame(decorRect);
        _popupView.getWindowVisibleDisplayFrame(rect);
        _geom.displayResolution = getDisplayResolution();

        Say.d(String.format("~~~ 1. FRAME RECT: %d - %d, STATIC NAV BAR HEIGHT=%d, action bar height=%d"
               , rect.top, rect.bottom, _staticNavBarHeight, getActionBarHeight()));
        Say.d(String.format("~~~ 2. FRAME RECT: %d - %d", decorRect.top, decorRect.bottom));

        _geom.viewAreaTop = rect.top;
        _geom.viewAreaBottom = rect.bottom;
        _geom.statusBarHeight = decorRect.top;
        _geom.navBarHeight = _geom.displayResolution.y - decorRect.bottom;
        _geom.keyboardY = rect.bottom;
        _geom.keyboardHeight = decorRect.bottom - rect.bottom;
        _geom.orientation = getScreenOrientation();

        Display d = _activity.getWindowManager().getDefaultDisplay();
        DisplayMetrics displayMetrics = new DisplayMetrics();
        d.getRealMetrics(displayMetrics);
        _geom.density = displayMetrics.density;

//        if (_fullscreen) {
            // Keyboard activation can make status bar visible, so hide the status bar.
            // View decorView = _activity.getWindow().getDecorView();
            // decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN);
            // Say.d(String.format("=== FRAME: top=%d, bottom=%d", rect.top, rect.bottom));
//        }

        notifyGeometry(_geom);
    }

    private int getNavigationBarHeight ()
    {
        Resources resources = _activity.getResources();
        int resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android");

        if (resourceId > 0)
            return resources.getDimensionPixelSize(resourceId);

        return 0;
    }

    private int getStatusBarHeight ()
    {
        Resources resources = _activity.getResources();
        int resourceId = resources.getIdentifier("status_bar_height", "dimen", "android");

        if (resourceId > 0)
            return resources.getDimensionPixelSize(resourceId);

        return 0;
    }

    private Point getDisplayResolution ()
    {
        Point displayResolution = new Point();
        _activity.getWindowManager().getDefaultDisplay().getRealSize(displayResolution);
        return displayResolution;
    }

//    public boolean hasSoftKeys ()
//    {
//        Display d = _activity.getWindowManager().getDefaultDisplay();
//
//        DisplayMetrics realDisplayMetrics = new DisplayMetrics();
//        d.getRealMetrics(realDisplayMetrics);
//
//        int realHeight = realDisplayMetrics.heightPixels;
//        int realWidth = realDisplayMetrics.widthPixels;
//
//        DisplayMetrics displayMetrics = new DisplayMetrics();
//        d.getMetrics(displayMetrics);
//
//        int displayHeight = displayMetrics.heightPixels;
//        int displayWidth = displayMetrics.widthPixels;
//
//        boolean hasSoftwareKeys =  (realWidth - displayWidth) > 0 || (realHeight - displayHeight) > 0;
//        return hasSoftwareKeys;
//    }

    // Send data observer
    private void notifyGeometry (KeyboardGeometry geom)
    {
        if (_observer != null)
            _observer.onKeyboardGeometry(geom);
    }
}

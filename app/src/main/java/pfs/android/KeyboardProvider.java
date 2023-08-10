package pfs.android;

// ----------------------------------------------------------------------------
// The MIT License
// UnityMobileInput https://github.com/mopsicus/UnityMobileInput
// Copyright (c) 2018 Mopsicus <mail@mopsicus.ru>
// ----------------------------------------------------------------------------
// package ru.mopsicus.mobileinput;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.PopupWindow;
import android.view.Display;
import android.util.DisplayMetrics;

public class KeyboardProvider extends PopupWindow
{
    private KeyboardObserver _observer;
    private int _keyboardLandscapeHeight;
    private int keyboardPortraitHeight;
    private View _popupView;
    private View _parentView;
    private Activity _activity;
    private int _heightMax;
    private int _navBarHeight;

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
        LayoutInflater inflator = (LayoutInflater) activity.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        this._popupView = inflator.inflate(id, null, false);
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

        _navBarHeight = getNavigationBarHeight();

        _popupView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (_popupView != null) {
                    handleOnGlobalLayout();
                }
            }
        });
    }

    // Close fake popup
    public void disable ()
    {
        dismiss();
    }

    // Return screen orientation
    private int getScreenOrientation ()
    {
        return _activity.getResources().getConfiguration().orientation;
    }

    // Handler to get keyboard height
    private void handleOnGlobalLayout ()
    {
        Rect rect = new Rect();
        _popupView.getWindowVisibleDisplayFrame(rect);

        if (rect.bottom > _heightMax)
            _heightMax = rect.bottom;

        int keyboardY = rect.bottom;
        int keyboardHeight = _heightMax - rect.bottom;

        if (keyboardHeight > 0)
            keyboardHeight += _navBarHeight;

        int orientation = getScreenOrientation();
        notifyKeyboardHeight(keyboardHeight, keyboardHeight, keyboardY, orientation);
    }

    private int getNavigationBarHeight ()
    {
        if (!hasSoftKeys())
            return 0;

        Resources resources = _activity.getResources();
        int resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android");

        if (resourceId > 0)
            return resources.getDimensionPixelSize(resourceId);

        return 0;
    }

    public boolean hasSoftKeys ()
    {
        Display d = _activity.getWindowManager().getDefaultDisplay();

        DisplayMetrics realDisplayMetrics = new DisplayMetrics();
        d.getRealMetrics(realDisplayMetrics);

        int realHeight = realDisplayMetrics.heightPixels;
        int realWidth = realDisplayMetrics.widthPixels;

        DisplayMetrics displayMetrics = new DisplayMetrics();
        d.getMetrics(displayMetrics);

        int displayHeight = displayMetrics.heightPixels;
        int displayWidth = displayMetrics.widthPixels;

        boolean hasSoftwareKeys =  (realWidth - displayWidth) > 0 || (realHeight - displayHeight) > 0;
        return hasSoftwareKeys;
    }

    // Send data observer
    private void notifyKeyboardHeight (float height, int keyboardHeight, int keyboardY, int orientation)
    {
        if (_observer != null)
            _observer.onKeyboardHeight(height, keyboardHeight, keyboardY, orientation);
    }
}
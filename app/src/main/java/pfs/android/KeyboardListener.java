package pfs.android;

import android.app.Activity;
import android.content.Context;
import android.graphics.Rect;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;

import pfs.android.NonNull;

// See
// 1. https://stackoverflow.com/questions/25216749/soft-keyboard-open-and-close-listener-in-an-activity-in-android
// 2. https://proandroiddev.com/how-to-detect-if-the-android-keyboard-is-open-269b255a90f5
// 3. https://www.tutorialspoint.com/how-to-write-a-softkeyboard-open-and-close-listener-in-an-activity-in-android
// 4. https://github.com/ravindu1024/android-keyboardlistener/blob/master/keyboard-listener/src/main/java/com/rw/keyboardlistener/KeyboardUtils.java
// 5. http://angelolloqui.com/blog/41-Keyboard-observer-android
// 6. https://github.com/yshrsmz/KeyboardVisibilityEvent
// 7. https://riptutorial.com/android/example/27571/register-a-callback-for-keyboard-open-and-close
// 8. https://stackoverflow.com/questions/4745988/how-do-i-detect-if-software-keyboard-is-visible-on-android-device-or-not
// 9. https://github.com/qbit-t/qbit
// 10. * https://github.com/mopsicus/UnityMobileInput (KeyboardProvider implementation)
//

// ATTENTION!
// Use KeyboardProvider instead of this class

public class KeyboardListener
{
    Activity _activity = null;
    View _rootView = null;
    private int _orientation = 0;

    private final Rect _rect = new Rect();

    public KeyboardListener (@NonNull Activity activity)
    {
        // https://stackoverflow.com/questions/4486034/get-root-view-from-current-activity
        this(activity, ((ViewGroup)activity.findViewById(android.R.id.content)).getChildAt(0));
    }

    public KeyboardListener (@NonNull Activity activity, int rootViewId)
    {
        this(activity, activity.findViewById(rootViewId));
    }

    private boolean _wasOpened = false;
    private static final int DEFAULT_KEYBOARD_DP = 100;
    private int _estimatedKeyboardHeightPx = 0;

    public KeyboardListener (@NonNull Activity activity, @NonNull View rootView)
    {
        _activity = activity;
        _rootView = rootView;
        _orientation = activity.getResources().getConfiguration().orientation;
        _estimatedKeyboardHeightPx = dpToPx(_activity
                // From Lollipop includes button bar in the root. Add height of button bar (48dp) to maxDiff
                , DEFAULT_KEYBOARD_DP + (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP ? 48 : 0));

        _rootView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                // Conclude whether the keyboard is shown or not.
                _rootView.getWindowVisibleDisplayFrame(_rect);
                int rootViewHeight = _rootView.getRootView().getHeight();
                int heightDiff = rootViewHeight - (_rect.bottom - _rect.top);
                boolean isShown = heightDiff >= _estimatedKeyboardHeightPx;

                // Ignoring global layout change...
                if (isShown == _wasOpened)
                    return;

                _wasOpened = isShown;

                Say.d(String.format("~~~ ON GLOBAL LAYOUT: %s, ROOT HEIGHT=%d, _rect.bottom=%d, _rect.top=%d"
                        , isShown ? "SHOWN" : "INVISIBLE"
                        , rootViewHeight
                        , _rect.bottom, _rect.top));
            }
        });
    }

    private static int dpToPx (Context context, float valueInDp)
    {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        return (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, valueInDp, metrics);
    }

    private int dpToPx (float valueInDp)
    {
        return dpToPx(_activity, valueInDp);
    }
}

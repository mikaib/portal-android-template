package sh.mki.genesis;

import android.app.NativeActivity;
import android.os.Build;
import android.view.Display;
import android.view.DisplayCutout;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.graphics.Point;
import android.util.Log;

public class MainActivity extends NativeActivity {
    private static final String TAG = "MainActivity";

    static {
        System.loadLibrary("aaudio");
        System.loadLibrary("Main");
    }

    private native void nativeOnDisplayInfoReady(int displayWidth, int displayHeight,
                                                 int usableWidth, int usableHeight,
                                                 int usableXOffset, int usableYOffset);

    public void configureFullscreen() {
        Log.d(TAG, "Configuring fullscreen mode");

        Window window = getWindow();
        View decorView = window.getDecorView();

        WindowManager.LayoutParams layoutParams = window.getAttributes();
        layoutParams.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        window.setAttributes(layoutParams);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsetsController controller = window.getInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.systemBars());
                controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
                Log.d(TAG, "Using WindowInsetsController for fullscreen");
            }
        } else {
            @SuppressWarnings("deprecation")
            int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;

            decorView.setSystemUiVisibility(uiOptions);
            Log.d(TAG, "Using deprecated setSystemUiVisibility for compatibility");
        }
    }

    public void getDisplayInfo() {
        Log.d(TAG, "Getting display info");

        WindowManager windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        if (windowManager == null) {
            Log.e(TAG, "Failed to get window manager");
            return;
        }

        Display display = windowManager.getDefaultDisplay();
        Point displaySize = new Point();

        try {
            display.getRealSize(displaySize);
            Log.d(TAG, "Real display size: " + displaySize.x + "x" + displaySize.y);
        } catch (Exception e) {
            display.getSize(displaySize);
            Log.d(TAG, "Display size (fallback): " + displaySize.x + "x" + displaySize.y);
        }

        int displayWidth = displaySize.x;
        int displayHeight = displaySize.y;

        int usableWidth = displayWidth;
        int usableHeight = displayHeight;
        int usableXOffset = 0;
        int usableYOffset = 0;

        getCutoutInfo(displayWidth, displayHeight, usableWidth, usableHeight, usableXOffset, usableYOffset);
    }

    private void getCutoutInfo(int displayWidth, int displayHeight, int usableWidth, int usableHeight, int usableXOffset, int usableYOffset) {
        Window window = getWindow();
        if (window == null) {
            Log.e(TAG, "Window is null");
            notifyNativeDisplayInfo(displayWidth, displayHeight, usableWidth, usableHeight, usableXOffset, usableYOffset);
            return;
        }

        View decorView = window.getDecorView();
        if (decorView == null) {
            Log.e(TAG, "DecorView is null");
            notifyNativeDisplayInfo(displayWidth, displayHeight, usableWidth, usableHeight, usableXOffset, usableYOffset);
            return;
        }

        WindowInsets windowInsets = decorView.getRootWindowInsets();
        if (windowInsets == null) {
            Log.d(TAG, "WindowInsets is null, using full display");
            notifyNativeDisplayInfo(displayWidth, displayHeight, usableWidth, usableHeight, usableXOffset, usableYOffset);
            return;
        }

        DisplayCutout displayCutout = windowInsets.getDisplayCutout();
        if (displayCutout == null) {
            Log.d(TAG, "No display cutout found");
            notifyNativeDisplayInfo(displayWidth, displayHeight, usableWidth, usableHeight, usableXOffset, usableYOffset);
            return;
        }

        int leftInset = displayCutout.getSafeInsetLeft();
        int topInset = displayCutout.getSafeInsetTop();
        int rightInset = displayCutout.getSafeInsetRight();
        int bottomInset = displayCutout.getSafeInsetBottom();

        usableXOffset = leftInset;
        usableYOffset = topInset;
        usableWidth = displayWidth - leftInset - rightInset;
        usableHeight = displayHeight - topInset - bottomInset;

        Log.d(TAG, "Display cutout insets - left: " + leftInset + ", top: " + topInset +
                ", right: " + rightInset + ", bottom: " + bottomInset);
        Log.d(TAG, "Usable area: " + usableWidth + "x" + usableHeight +
                " at offset (" + usableXOffset + ", " + usableYOffset + ")");

        notifyNativeDisplayInfo(displayWidth, displayHeight, usableWidth, usableHeight, usableXOffset, usableYOffset);
    }

    private void notifyNativeDisplayInfo(int displayWidth, int displayHeight,
                                         int usableWidth, int usableHeight,
                                         int usableXOffset, int usableYOffset) {
        nativeOnDisplayInfoReady(displayWidth, displayHeight, usableWidth, usableHeight, usableXOffset, usableYOffset);
    }
}
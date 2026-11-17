package org.chimeramc.client.util;

import android.content.Context;

/**
 * Small view helpers shared across the launcher.
 *
 * <p>The toast methods route through {@link org.chimeramc.client.ui.compose.caramel.CaramelNoticeBus}
 * so a message wears the launcher's own toast when one is mounted, and only falls back to the system
 * bubble when nothing is. The bus marshals to the UI thread itself, so the explicit wrappers this
 * class used to need are gone.
 */
public class UIHelper {

    public static void showToast(Context context, String message) {
        org.chimeramc.client.ui.compose.caramel.CaramelNoticeBus.post(context, message, false);
    }

    public static void showToast(Context context, int resId, Object... formatArgs) {
        showToast(context, context.getString(resId, formatArgs));
    }

    public static void showLongToast(Context context, String message) {
        org.chimeramc.client.ui.compose.caramel.CaramelNoticeBus.post(context, message, true);
    }

    public static void hideKeyboard(android.app.Activity activity) {
        android.view.View view = activity.getCurrentFocus();
        if (view != null) {
            android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
    }
}
package com.verso.cordova.clipboard;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.apache.cordova.CallbackContext;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.PersistableBundle;
import android.view.View;
import android.view.ViewTreeObserver;

public class Clipboard extends CordovaPlugin {

    private static final String actionCopy = "copy";
    private static final String actionPaste = "paste";
    private static final String actionClear = "clear";

    private static final long DEFAULT_EXPIRES_IN_MS = 120000L;
    // Keeps Handler.postDelayed() far away from uptime overflow.
    private static final long MAX_EXPIRES_IN_MS = Integer.MAX_VALUE;

    // All expiry state below is only touched on the main thread.
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private Runnable pendingExpiry;
    private ViewTreeObserver.OnWindowFocusChangeListener pendingFocusListener;
    private String expiringText;
    private long expiringTimestamp;

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (action.equals(actionCopy)) {
            final String text;
            try {
                text = args.getString(0);
            } catch (JSONException e) {
                callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.JSON_EXCEPTION));
                return true;
            }
            JSONObject options = args.optJSONObject(1);
            final long expiresInMs = readExpiresInMs(options);
            final boolean sensitive = options != null && options.optBoolean("sensitive", false);
            // localOnly is iOS only: Android has no device-local clipboard.

            mainHandler.post(() -> copy(text, expiresInMs, sensitive, callbackContext));
            return true;
        } else if (action.equals(actionPaste)) {
            try {
                ClipboardManager clipboard = getClipboard();
                String text = "";

                if (clipboard != null && clipboard.hasPrimaryClip()) {
                    ClipData clip = clipboard.getPrimaryClip();
                    if (clip == null || clip.getItemCount() < 1) {
                        callbackContext.success(text);
                        return true;
                    }

                    ClipData.Item item = clip.getItemAt(0);
                    CharSequence coercedText = item.coerceToText(cordova.getContext());
                    if (coercedText != null) {
                        text = coercedText.toString();
                    }
                }
                callbackContext.success(text);

                return true;
            } catch (Exception e) {
                callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, e.toString()));
            }
        } else if (action.equals(actionClear)) {
            mainHandler.post(() -> clear(callbackContext));
            return true;
        }

        return false;
    }

    @Override
    public void onDestroy() {
        cancelPendingExpiry();
        super.onDestroy();
    }

    private void copy(String text, long expiresInMs, boolean sensitive, CallbackContext callbackContext) {
        try {
            ClipboardManager clipboard = getClipboard();
            if (clipboard == null) {
                callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, "Clipboard service unavailable"));
                return;
            }

            ClipData clip = ClipData.newPlainText("Text", text);

            if (sensitive && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // Only hides the system clipboard preview; other apps can still paste.
                PersistableBundle extras = new PersistableBundle();
                extras.putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true);
                clip.getDescription().setExtras(extras);
            }

            clipboard.setPrimaryClip(clip);

            // Replace any earlier pending clear only once the new clip is in place.
            cancelPendingExpiry();
            if (expiresInMs > 0) {
                expiringText = text;
                expiringTimestamp = readPrimaryClipTimestamp(clipboard);
                pendingExpiry = this::onExpiry;
                mainHandler.postDelayed(pendingExpiry, expiresInMs);
            }

            callbackContext.success(text);
        } catch (Exception e) {
            callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, e.toString()));
        }
    }

    private void clear(CallbackContext callbackContext) {
        try {
            ClipboardManager clipboard = getClipboard();
            if (clipboard == null) {
                callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, "Clipboard service unavailable"));
                return;
            }

            clearClipboard(clipboard);
            cancelPendingExpiry();

            callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK));
        } catch (Exception e) {
            callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, e.toString()));
        }
    }

    private void onExpiry() {
        pendingExpiry = null;
        if (expiringText == null) {
            return;
        }

        try {
            ClipboardManager clipboard = getClipboard();
            if (clipboard == null) {
                resetExpiryState();
                return;
            }

            // Compare timestamps first: it identifies our clip without reading another app's
            // content (which would trigger the Android 12+ "pasted from clipboard" toast).
            if (expiringTimestamp != 0 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ClipDescription description = clipboard.getPrimaryClipDescription();
                if (description == null) {
                    handleUnreadableClip();
                    return;
                }
                if (description.getTimestamp() != expiringTimestamp) {
                    resetExpiryState();
                    return;
                }
            }

            ClipData clip = clipboard.getPrimaryClip();
            if (clip == null) {
                handleUnreadableClip();
                return;
            }

            if (isClipTextOurs(clip)) {
                clearClipboard(clipboard);
            }
        } catch (Exception e) {
            // Never wipe a clip we could not verify.
        }
        resetExpiryState();
    }

    private boolean isClipTextOurs(ClipData clip) {
        if (clip.getItemCount() != 1) {
            return false;
        }

        CharSequence clipText = clip.getItemAt(0).getText();
        return clipText != null && expiringText.equals(clipText.toString());
    }

    private void handleUnreadableClip() {
        if (hasWindowFocus()) {
            // Clipboard is genuinely empty; nothing to clear.
            resetExpiryState();
        } else {
            // Android 10+ denies clipboard reads while the app is not focused, so the
            // clip cannot be verified yet. Re-check as soon as the app regains focus.
            retryExpiryWhenFocused();
        }
    }

    private void retryExpiryWhenFocused() {
        removeFocusListener();

        View view = webView != null ? webView.getView() : null;
        if (view == null) {
            resetExpiryState();
            return;
        }

        pendingFocusListener = hasFocus -> {
            if (hasFocus) {
                removeFocusListener();
                pendingExpiry = this::onExpiry;
                mainHandler.post(pendingExpiry);
            }
        };
        view.getViewTreeObserver().addOnWindowFocusChangeListener(pendingFocusListener);
    }

    private void removeFocusListener() {
        if (pendingFocusListener == null) {
            return;
        }

        View view = webView != null ? webView.getView() : null;
        if (view != null) {
            ViewTreeObserver observer = view.getViewTreeObserver();
            if (observer.isAlive()) {
                observer.removeOnWindowFocusChangeListener(pendingFocusListener);
            }
        }
        pendingFocusListener = null;
    }

    private void cancelPendingExpiry() {
        if (pendingExpiry != null) {
            mainHandler.removeCallbacks(pendingExpiry);
            pendingExpiry = null;
        }
        resetExpiryState();
    }

    private void resetExpiryState() {
        removeFocusListener();
        expiringText = null;
        expiringTimestamp = 0;
    }

    private boolean hasWindowFocus() {
        View view = webView != null ? webView.getView() : null;
        return view != null && view.hasWindowFocus();
    }

    private ClipboardManager getClipboard() {
        return (ClipboardManager) cordova.getContext().getSystemService(Context.CLIPBOARD_SERVICE);
    }

    private static void clearClipboard(ClipboardManager clipboard) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            clipboard.clearPrimaryClip();
        } else {
            clipboard.setPrimaryClip(ClipData.newPlainText("", ""));
        }
    }

    private static long readPrimaryClipTimestamp(ClipboardManager clipboard) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return 0;
        }
        try {
            ClipDescription description = clipboard.getPrimaryClipDescription();
            return description != null ? description.getTimestamp() : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    // Returns 0 (no expiry) unless the caller opted in by passing expiresInMs.
    private static long readExpiresInMs(JSONObject options) {
        if (options == null || options.isNull("expiresInMs")) {
            return 0;
        }
        double value = options.optDouble("expiresInMs", DEFAULT_EXPIRES_IN_MS);
        if (Double.isNaN(value) || value < DEFAULT_EXPIRES_IN_MS) {
            return DEFAULT_EXPIRES_IN_MS;
        }
        return (long) Math.min(value, (double) MAX_EXPIRES_IN_MS);
    }
}

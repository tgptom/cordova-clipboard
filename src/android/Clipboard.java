package com.verso.cordova.clipboard;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.apache.cordova.CallbackContext;

import org.json.JSONArray;
import org.json.JSONException;

import android.content.Context;
import android.content.ClipboardManager;
import android.content.ClipData;
import android.os.Build;

public class Clipboard extends CordovaPlugin {

    private static final String actionCopy = "copy";
    private static final String actionPaste = "paste";
    private static final String actionClear = "clear";

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        ClipboardManager clipboard = (ClipboardManager) cordova.getContext().getSystemService(Context.CLIPBOARD_SERVICE);

        if (action.equals(actionCopy)) {
            try {
                if (clipboard == null) {
                    callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, "Clipboard service unavailable"));
                    return true;
                }

                String text = args.getString(0);
                ClipData clip = ClipData.newPlainText("Text", text);

                clipboard.setPrimaryClip(clip);

                callbackContext.success(text);

                return true;
            } catch (JSONException e) {
                callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.JSON_EXCEPTION));
            } catch (Exception e) {
                callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, e.toString()));
            }
        } else if (action.equals(actionPaste)) {
            try {
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
            try {
                if (clipboard == null) {
                    callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, "Clipboard service unavailable"));
                    return true;
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    clipboard.clearPrimaryClip();
                } else {
                    ClipData clip = ClipData.newPlainText("", "");
                    clipboard.setPrimaryClip(clip);
                }

                callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK));

                return true;
            } catch (Exception e) {
                callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, e.toString()));
            }
        }

        return false;
    }
}


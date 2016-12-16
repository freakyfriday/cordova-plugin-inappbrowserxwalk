package com.example.plugin.InAppBrowserXwalk;

import com.example.plugin.InAppBrowserXwalk.BrowserDialog;

import android.content.res.Resources;
import org.apache.cordova.*;
import org.apache.cordova.PluginManager;
import org.apache.cordova.PluginResult;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

import org.xwalk.core.XWalkView;
import org.xwalk.core.XWalkResourceClient;
import org.xwalk.core.XWalkCookieManager;
import org.xwalk.core.internal.XWalkClient;
import android.view.View;
import android.view.Window;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.graphics.Typeface;
import android.widget.Toast;

import android.webkit.WebResourceResponse;
import org.crosswalk.engine.XWalkCordovaView;
import android.util.Log;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.http.SslError;
import android.webkit.ValueCallback;
import android.webkit.WebResourceResponse;

public class InAppBrowserXwalk extends CordovaPlugin {

    private BrowserDialog dialog;
    private XWalkView xWalkWebView;
    private CallbackContext callbackContext;

    @Override
    public boolean execute(String action, JSONArray data, CallbackContext callbackContext) throws JSONException {

        if(action.equals("open")) {
            this.callbackContext = callbackContext;
            this.openBrowser(data);
        }

        if(action.equals("close")) {
            this.closeBrowser();
        }

        if(action.equals("show")) {
            this.showBrowser();
        }

        if(action.equals("hide")) {
            this.hideBrowser();
        }

        // Adding executeScript
        if(action.equals("executeScript")) {
            this.executeScript(data.getString(0));
        }

        return true;
    }

    class MyResourceClient extends XWalkResourceClient {
           MyResourceClient(XWalkView view) {
               super(view);
           }

           @Override
           public void onLoadStarted (XWalkView view, String url) {
               try {
                   JSONObject obj = new JSONObject();
                   obj.put("type", "loadstart");
                   obj.put("url", url);
                   PluginResult result = new PluginResult(PluginResult.Status.OK, obj);
                   result.setKeepCallback(true);
                   callbackContext.sendPluginResult(result);
               } catch (JSONException ex) {}
           }

           @Override
           public void onLoadFinished (XWalkView view, String url) {
               try {
                   JSONObject obj = new JSONObject();
                   obj.put("type", "loadstop");
                   obj.put("url", url);
                   PluginResult result = new PluginResult(PluginResult.Status.OK, obj);
                   result.setKeepCallback(true);
                   callbackContext.sendPluginResult(result);
               } catch (JSONException ex) {}
           }
            // This is Membery specific. Hence, disabled.
            /*@Override
            public boolean shouldOverrideUrlLoading(XWalkView view, String url) {
                if(url.equalsIgnoreCase("membery://index")) {
                    Log.d("DEBUG", url);
                    closeBrowser();
                    return true;
                } else {
                    return false;
                }
        
            }*/
            /**
    * Notify the host application that an SSL error occurred while loading a
    * resource. The host application must call either callback.onReceiveValue(true)
    * or callback.onReceiveValue(false). Note that the decision may be
    * retained for use in response to future SSL errors. The default behavior
    * is to pop up a dialog.
    */
    @Override
    public void onReceivedSslError(XWalkView view, ValueCallback<Boolean> callback, SslError error) {
        final String packageName = cordova.getActivity().getPackageName();
        final PackageManager pm = cordova.getActivity().getPackageManager();

        ApplicationInfo appInfo;
        try {
            appInfo = pm.getApplicationInfo(packageName, PackageManager.GET_META_DATA);
            if ((appInfo.flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
                // debug = true
                callback.onReceiveValue(true);
            } else {
                // debug = false
                callback.onReceiveValue(true);
            }
        } catch (PackageManager.NameNotFoundException e) {
            // When it doubt, lock it out!
            callback.onReceiveValue(false);
        }
    }
   }

    private void openBrowser(final JSONArray data) throws JSONException {
        final String url = data.getString(0);
        this.cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                dialog = new BrowserDialog(cordova.getActivity(), android.R.style.Theme_NoTitleBar);
                xWalkWebView = new XWalkView(cordova.getActivity(), cordova.getActivity());
                XWalkCookieManager mCookieManager = new XWalkCookieManager();
                mCookieManager.setAcceptCookie(true);
                mCookieManager.setAcceptFileSchemeCookies(true);
                xWalkWebView.setResourceClient(new MyResourceClient(xWalkWebView));
                xWalkWebView.load(url, "");

                String toolbarColor = "#FFCE0D";
                int toolbarHeight = 80;
                String closeButtonText = "(X) Close";
                int closeButtonSize = 25;
                String closeButtonColor = "#000000";
                boolean openHidden = false;

                if(data != null && data.length() > 1) {
                    try {
                            JSONObject options = new JSONObject(data.getString(1));

                            if(!options.isNull("toolbarColor")) {
                                toolbarColor = options.getString("toolbarColor");
                            }
                            if(!options.isNull("toolbarHeight")) {
                                toolbarHeight = options.getInt("toolbarHeight");
                            }
                            if(!options.isNull("closeButtonText")) {
                                closeButtonText = options.getString("closeButtonText");
                            }
                            if(!options.isNull("closeButtonSize")) {
                                closeButtonSize = options.getInt("closeButtonSize");
                            }
                            if(!options.isNull("closeButtonColor")) {
                                closeButtonColor = options.getString("closeButtonColor");
                            }
                            if(!options.isNull("openHidden")) {
                                openHidden = options.getBoolean("openHidden");
                            }
                        }
                    catch (JSONException ex) {

                    }
                }

                LinearLayout main = new LinearLayout(cordova.getActivity());
                main.setOrientation(LinearLayout.VERTICAL);

                RelativeLayout toolbar = new RelativeLayout(cordova.getActivity());
                toolbar.setBackgroundColor(android.graphics.Color.parseColor(toolbarColor));
                toolbar.setLayoutParams(new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, toolbarHeight));
                toolbar.setPadding(5, 5, 5, 5);

                TextView closeButton = new TextView(cordova.getActivity());
                closeButton.setText(closeButtonText);
                closeButton.setTextSize(closeButtonSize);
                closeButton.setTextColor(android.graphics.Color.parseColor(closeButtonColor));
                closeButton.setTypeface(Typeface.create("sans-serif-thin", Typeface.NORMAL));
                toolbar.addView(closeButton);

                closeButton.setOnClickListener(new View.OnClickListener() {
                     public void onClick(View v) {
                         closeBrowser();
                     }
                 });
                //main.addView(toolbar);
                main.addView(xWalkWebView);

                dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                dialog.getWindow().getAttributes().windowAnimations = android.R.style.Animation_Dialog;
                dialog.setCancelable(true);
                LayoutParams layoutParams = new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
                dialog.addContentView(main, layoutParams);
                if(!openHidden) {
                    dialog.show();
                }
            }
        });
    }

    public void hideBrowser() {
        this.cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(dialog != null) {
                    dialog.hide();
                }
            }
        });
    }

    public void showBrowser() {
        this.cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(dialog != null) {
                    dialog.show();
                }
            }
        });
    }

    public void closeBrowser() {
        this.cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                xWalkWebView.onDestroy();
                dialog.dismiss();
                try {
                    JSONObject obj = new JSONObject();
                    obj.put("type", "exit");
                    PluginResult result = new PluginResult(PluginResult.Status.OK, obj);
                    result.setKeepCallback(true);
                    callbackContext.sendPluginResult(result);
                } catch (JSONException ex) {}
            }
        });
    }

InAppBrowserXwalk.prototype = {
    close: function () {
        cordova.exec(null, null, "InAppBrowserXwalk", "close", []);
    },
    addEventListener: function (eventname, func) {
        callbacks[eventname] = func;
    },
    removeEventListener: function (eventname) {
        callbacks[eventname] = undefined;
    },
    show: function () {
        cordova.exec(null, null, "InAppBrowserXwalk", "show", []);
    },
    hide: function () {
        cordova.exec(null, null, "InAppBrowserXwalk", "hide", []);
    },

    executeScript: function(injectDetails) {
        cordova.exec(null, null, "InAppBrowserXwalk", "injectScriptCode", [injectDetails]);
    }
}

var callback = function(event) {
    if (event.type === "loadstart" && callbacks['loadstart'] !== undefined) {
        callbacks['loadstart'](event.url);
    }
    if (event.type === "loadstop" && callbacks['loadstop'] !== undefined) {
        callbacks['loadstop'](event.url);
    }
    if (event.type === "exit" && callbacks['exit'] !== undefined) {
        callbacks['exit']();
    }
    if (event.type === "jsCallback" && callbacks['jsCallback'] !== undefined) {
        callbacks['jsCallback'](event.result);
    }
}
    public void executeScript(injectDetails) {
        final String finalScriptToInject = source;
        this.cordova.getActivity().runOnUiThread(new Runnable() {

            @Override
            public void run() {

                xWalkWebView.evaluateJavascript(finalScriptToInject, new ValueCallback<String>() {
                    @Override
                    public void onReceiveValue(String scriptResult) {
                        try {
                            JSONObject obj = new JSONObject();
                            obj.put("type", "jsCallback");
                            obj.put("result", scriptResult);
                            PluginResult result = new PluginResult(PluginResult.Status.OK, obj);
                            result.setKeepCallback(true);
                            callbackContext.sendPluginResult(result);
                        } catch (JSONException ex) {}
                    }
                });
            }
        });
    }

}

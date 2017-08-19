package com.example.exposed.exposed1;

import android.content.pm.PackageManager;
import android.graphics.Color;
import android.widget.TextView;

import static de.robv.android.xposed.SELinuxHelper.getContext;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;
import de.robv.android.xposed.XposedBridge;

public class NfcModifier implements IXposedHookLoadPackage {
    public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {

        XposedBridge.log("we are hooked to " + lpparam.packageName);


        if (!lpparam.packageName.equals("com.librelink.app") && !lpparam.packageName.equals("com.example.exposed.myapplication"))
            return;

        // android.content.pm.PackageManager is the abstract name
        findAndHookMethod("android.app.ApplicationPackageManager", lpparam.classLoader, "getInstallerPackageName", String.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                XposedBridge.log("We are before getInstallerPackageName " + param.args[0] );
            }

            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                XposedBridge.log("We are after getInstallerPackageName " + param.args[0] + " result = " + param.getResult());
                param.setResult("com.android.vending");

/*
                TextView tv = (TextView) param.thisObject;
                String text = tv.getText().toString();
                tv.setText(text + " :)");
                tv.setTextColor(Color.RED);
*/
            }
        });

/*
        if (!lpparam.packageName.equals("com.android.systemui"))
            return;

        findAndHookMethod("com.android.systemui.statusbar.policy.Clock", lpparam.classLoader, "updateClock", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                TextView tv = (TextView) param.thisObject;
                String text = tv.getText().toString();
                tv.setText(text + " :)");
                tv.setTextColor(Color.RED);
            }
        });
        */
    }

}
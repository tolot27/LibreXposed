package com.example.exposed.exposed1;

import android.app.AndroidAppHelper;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.widget.TextView;

import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import android.os.Bundle;

import static de.robv.android.xposed.SELinuxHelper.getContext;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;
import de.robv.android.xposed.XposedBridge;

class MyReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context arg0, Intent arg1) {
        XposedBridge.log("we are inside the broadcast reciever");

    }

}

public class NfcModifier implements IXposedHookLoadPackage {


    public static String byteArrayToHex(byte[] a) {
        StringBuilder sb = new StringBuilder(a.length * 2);
        for(byte b: a)
            sb.append(String.format("0x%02x ", b));
        return sb.toString();
    }

    public static boolean comparePartialByteArray(byte []b1, byte[] b2) {
        int len = Math.min(b1.length, b2.length);
        for (int i = 0 ; i < len ; i++) {
            if(b1[i] != b2[i]) {
                return false;
            }
        }
        return true;

    }

    public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {

        XposedBridge.log("we are hooked to " + lpparam.packageName);


        if (!lpparam.packageName.equals("com.librelink.app") && !lpparam.packageName.equals("com.example.exposed.myapplication"))
            return;

        findAndHookMethod("com.abbottdiabetescare.flashglucose.sensorabstractionservice.rf.DefaultNfcRfModule", lpparam.classLoader, "tranceiveWithRetries",
                "com.abbottdiabetescare.flashglucose.sensorabstractionservice.rf.NfcOsFunctions.NfcOsHandle", byte[].class, new XC_MethodHook() {

                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        XposedBridge.log("We are after tranceiveWithRetries " + param.args[0] + " result = " + param.getResult());

                        byte[] result = (byte[]) param.getResult();
                        byte[] input = (byte[]) param.args[1];
                        XposedBridge.log("We are after tranceiveWithRetries input = " + byteArrayToHex(input) + " output =" + byteArrayToHex(result));
                        if(result.length != 7) {
                            return;
                        }
                        byte[] cmp = {0x00,(byte)0xdf, 0x00, 0x00, 0x08};
                        if(!comparePartialByteArray(cmp, result)) {
                            XposedBridge.log("We are after tranceiveWithRetries did not find the right string");
                            return;
                        }
                        XposedBridge.log("We are after tranceiveWithRetries changing the string");
                        result[4] = 1;

                    }

        });


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

            }
        });


        // Hook the SplashActivity.oncreate method, to register a broadcast receiver.
        findAndHookMethod("com.librelink.app.ui.SplashActivity", lpparam.classLoader, "onCreate", Bundle.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                XposedBridge.log("We are before SplashActivity.onCreate " + param.args[0] );


                // Register a broadcast reciever
                Context context = (Context) AndroidAppHelper.currentApplication();
               XposedBridge.log("context = " + context);

               IntentFilter filter = new IntentFilter("com.example.Broadcast");

               MyReceiver receiver = new MyReceiver();
               context.registerReceiver(receiver, filter);

               //?? intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
           }
       });

    }

}
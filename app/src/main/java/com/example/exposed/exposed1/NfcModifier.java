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
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;
import de.robv.android.xposed.XposedBridge;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

class MyReceiver extends BroadcastReceiver {

    ClassLoader classLoader_;
    MyReceiver(ClassLoader classLoader) {
        classLoader_ = classLoader;
    }

    Object createApplicationRegion() {
        Class<?> ApplicationRegionDef = XposedHelpers.findClass("com.abbottdiabetescare.flashglucose.sensorabstractionservice.ApplicationRegion", classLoader_);
        XposedBridge.log("ApplicationRegionDef =  " + ApplicationRegionDef );

        Object ApplicationRegionInstance = XposedHelpers.newInstance(ApplicationRegionDef,"applicationRegion2", 2, 3);
        XposedBridge.log("ApplicationRegionInstance =  " + ApplicationRegionInstance );

        //Object intRes = XposedHelpers.callMethod(ApplicationRegionInstance, "toValue");
        //XposedBridge.log("ApplicationRegionInstance toValue returned =  " + intRes );

        Object intRes1 = XposedHelpers.callMethod(ApplicationRegionInstance, "fromValue", 0);
        XposedBridge.log("ApplicationRegionInstance fromValue returned =  " + intRes1 );

        return intRes1;
    }


    @Override
    public void onReceive(Context arg0, Intent arg1) {
        XposedBridge.log("we are inside the broadcast reciever");

        // Create DataProcessingNative
        Class<?> DataProcessingNativeDef = XposedHelpers.findClass("com.abbottdiabetescare.flashglucose.sensorabstractionservice.dataprocessing.DataProcessingNative", classLoader_);
        XposedBridge.log("DataProcessingNativeDef =  " + DataProcessingNativeDef );
        Object DataProcessingNativeInstance = XposedHelpers.newInstance(DataProcessingNativeDef, 1095774808);
        XposedBridge.log("DataProcessingNativeInstance =  " + DataProcessingNativeInstance );
        Object ApplicationRegionInstance = createApplicationRegion();

        byte[] bDat = {0x00,(byte)0xdf, 0x00, 0x00, 0x01, 01, 02};
        boolean ret = (boolean)XposedHelpers.callMethod(DataProcessingNativeInstance, "isPatchSupported", bDat, ApplicationRegionInstance);
        XposedBridge.log("return from  isPatchSupported is " + ret);
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


                Class<?> DataProcessingNativeDef = XposedHelpers.findClass("com.abbottdiabetescare.flashglucose.sensorabstractionservice.dataprocessing.DataProcessingNative", lpparam.classLoader);
                XposedBridge.log("DataProcessingNativeDef =  " +DataProcessingNativeDef );
                Object DataProcessingNativeInstance = XposedHelpers.newInstance(DataProcessingNativeDef, 1095774808);
                XposedBridge.log("DataProcessingNativeInstance =  " +DataProcessingNativeInstance );


                // Create CommonSensorModule
                Class<?> CommonSensorModuleDef = XposedHelpers.findClass("com.librelink.app.core.modules.CommonSensorModule", lpparam.classLoader);
                XposedBridge.log("CommonSensorModuleDef =  " + CommonSensorModuleDef );
                Object CommonSensorModuleInstance = XposedHelpers.newInstance(CommonSensorModuleDef);
                XposedBridge.log("CommonSensorModuleInstance =  " + CommonSensorModuleInstance );


                Method[] allMethods = CommonSensorModuleDef.getDeclaredMethods();
                for (Method m : allMethods) {

                    XposedBridge.log("method name =  " + m.getName() );
                }

                Class<?> ApplicationRegionDef = XposedHelpers.findClass("com.abbottdiabetescare.flashglucose.sensorabstractionservice.ApplicationRegion", lpparam.classLoader);
                XposedBridge.log("ApplicationRegionDef =  " + ApplicationRegionDef );



//                Class<?> CommonSensorModule_ProvideApplicationRegionFactoryDef = XposedHelpers.findClass("com.librelink.app.core.modules.CommonSensorModule_ProvideApplicationRegionFactory", lpparam.classLoader);
//                XposedBridge.log("CommonSensorModule_ProvideApplicationRegionFactoryDef =  " + CommonSensorModule_ProvideApplicationRegionFactoryDef );





                allMethods = ApplicationRegionDef.getDeclaredMethods();
                for (Method m : allMethods) {

                    XposedBridge.log("method name =  " + m.getName() );
                }
                Constructor[] allConstructors = ApplicationRegionDef.getDeclaredConstructors();
                for (Constructor ctor : allConstructors) {
                    XposedBridge.log("ctor name1 =  " + ctor.getName());
                    ctor.setAccessible(true);

                    Class<?>[] pType  = ctor.getParameterTypes();
                    for (int i = 0; i < pType.length; i++) {
                        XposedBridge.log("constructor parameter =  " + pType[i] );
                    }
                    Object ApplicationRegionInstance = ctor.newInstance("applicationRegion2",1,2);
                    XposedBridge.log("constructor ApplicationRegionInstance =  " + ApplicationRegionInstance );
                }

                Object ApplicationRegionInstance = XposedHelpers.newInstance(ApplicationRegionDef,"applicationRegion2", 2, 3);
                XposedBridge.log("ApplicationRegionInstance =  " + ApplicationRegionInstance );

                Object intRes = XposedHelpers.callMethod(ApplicationRegionInstance, "toValue");
                XposedBridge.log("ApplicationRegionInstance toValue returned =  " + intRes );

                Object intRes1 = XposedHelpers.callMethod(ApplicationRegionInstance, "fromValue", 1);
                XposedBridge.log("ApplicationRegionInstance fromValue returned =  " + intRes1 );

                // Does not exist
                // Method m = XposedHelpers.findMethodBestMatch(CommonSensorModuleDef, "provideApplicationRegion");
                // XposedBridge.log("method m =  " + m );


                // create ApplicationRegion factory
                //Class<?> applicationRegionFactoryDef = XposedHelpers.findClass("com.librelink.app.core.modules.CommonSensorModule_ProvideApplicationRegionFactory", lpparam.classLoader);
                //XposedBridge.log("applicationRegionFactoryDef =  " + applicationRegionFactoryDef );
                //Object applicationRegionFactory = XposedHelpers.callStaticMethod(applicationRegionFactoryDef, "create",CommonSensorModuleInstance );
                //XposedBridge.log("applicationRegionFactory =  " + applicationRegionFactory );



                //Object applicationRegionFactoryInstance =

                // Get an ApplicationRegion
                //Object ApplicationRegionInstance = XposedHelpers.callMethod(CommonSensorModuleInstance, "provideApplicationRegion");
                //XposedBridge.log("ApplicationRegionInstance =  " + ApplicationRegionInstance );


                MyReceiver receiver = new MyReceiver(lpparam.classLoader);
                context.registerReceiver(receiver, filter);

               //?? intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
           }
       });

    }

}
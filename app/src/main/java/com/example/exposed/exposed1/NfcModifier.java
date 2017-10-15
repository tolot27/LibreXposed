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
import static de.robv.android.xposed.XposedHelpers.findAndHookConstructor;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;
import de.robv.android.xposed.XposedBridge;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

class Utils {
    // Use reflection print the value of an object
    public static String objectToString (Object obj) {
        if (obj == null) {
            return "{null}";
        }
        StringBuilder result = new StringBuilder();
        String newLine = System.getProperty("line.separator");

        result.append( obj.getClass().getName() );
        result.append( " Object {" );
        result.append(newLine);

        //determine fields declared in this class only (no fields of superclass)
        Field[] fields = obj.getClass().getDeclaredFields();

        //print field names paired with their values
        for ( Field field : fields  ) {
            result.append("  ");
            try {
                result.append( field.getName() );
                result.append(": ");
                //requires access to private field:
                field.setAccessible(true);

                result.append( field.get(obj) );
            } catch ( IllegalAccessException ex ) {
                System.out.println(ex);
            }
            result.append(newLine);
        }
        result.append("}");

        return result.toString();
    }


    public static String byteArrayToHex(byte[] a) {
        if(a == null) {
            return null;
        }
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

    public static byte [] stringToByte(String str) {
        // This function receives a string in the form "0x1, 0x5" and creates a byte array from it.
        String []str_array = str.split(",");
        byte[] numbers = new byte[str_array.length];
        for(int i = 0;i < str_array.length;i++)
        {
            // Note that this is assuming valid input
            // If you want to check then add a try/catch
            // and another index for the numbers if to continue adding the others (see below)
            try {
                numbers[i] = Byte.decode(str_array[i].trim());
            } catch (NumberFormatException nfe){
                XposedBridge.log("Invalid value for a byte in " + str);
                XposedBridge.log("Invalid value index is " + i + " value is " + str_array[i].trim());
                return null;
            }
        }
        return numbers;
    }

    public static byte[] readBinaryFile(String fullPath) {
        if(fullPath == null) {
            return null;
        }
        File file = new File(fullPath);
        byte[] fileData = new byte[(int) file.length()];
        DataInputStream dis = null;
        try {
            dis = new DataInputStream(new FileInputStream(file));
        } catch (FileNotFoundException e) {
            XposedBridge.log("File not found " + fullPath);
            return null;
        }
        try {
            dis.readFully(fileData);
            dis.close();
        } catch (IOException e) {
            XposedBridge.log("Error reading from file " + fullPath);
            return null;
        }
        return fileData;
    }

    public static void writeToFile(String file, byte []data) {

        Context context = (Context) AndroidAppHelper.currentApplication();
        XposedBridge.log("context = " + context);
        String dir = context.getFilesDir().getPath();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
        String currentDateandTime = sdf.format(new Date());

        String file_name = dir + '/' + file+ "_" + currentDateandTime + ".dat";
        try {
            XposedBridge.log("Writing to file " + file_name + ", size = " + (data == null ? 0 : data.length));
            FileOutputStream f = new FileOutputStream(new File(file_name));
            if(data != null) {
                // file will be written with zero length to let the user know what is happening.
                f.write(data);
            }
            f.close();
        }catch (IOException e) {
            XposedBridge.log("Cought exception when trying to write file" + e);
        }
    }

}

class Constants {
    static public final String XDRIP_PLUS_LIBRE_DATA = "com.eveningoutpost.dexdrip.LIBRE_DATA";
    static public final String LIBRE_DATA_BUFFER = "com.eveningoutpost.dexdrip.Extras.DATA_BUFFER";
    static public final String LIBRE_DATA_TIMESTAMP = "com.eveningoutpost.dexdrip.Extras.TIMESTAMP";
    static public final String XDRIP_PLUS_NS_EMULATOR = "com.eveningoutpost.dexdrip.NS_EMULATOR";
}

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
    public void onReceive(Context context, Intent intent) {
        XposedBridge.log("we are inside the broadcast reciever");

        String packet_file = intent.getStringExtra("packet");
        String old_state_file = intent.getStringExtra("old_state");//"/data/local/tmp/new_state_20171002_165943.dat";
        long timestamp;

        XposedBridge.log("packet_file = " + packet_file);
        XposedBridge.log("old_state_file = " + old_state_file);
        byte[] packet;
        if(packet_file != null) {
            packet = Utils.readBinaryFile(packet_file);
        } else {
            packet = intent.getByteArrayExtra(Constants.LIBRE_DATA_BUFFER);
        }
        timestamp = intent.getLongExtra(Constants.LIBRE_DATA_TIMESTAMP, 0);

        XposedBridge.log("byte packet = " + Utils.byteArrayToHex(packet));

        byte[] oldState = Utils.readBinaryFile(old_state_file);
        XposedBridge.log("byte oldState = " + Utils.byteArrayToHex(oldState));

        if(packet == null || oldState == null) {
            return;
        }
        //oldState = null;

        // Create DataProcessingNative
        Class<?> DataProcessingNativeDef = XposedHelpers.findClass("com.abbottdiabetescare.flashglucose.sensorabstractionservice.dataprocessing.DataProcessingNative", classLoader_);
        XposedBridge.log("DataProcessingNativeDef =  " + DataProcessingNativeDef );
        Object DataProcessingNativeInstance = XposedHelpers.newInstance(DataProcessingNativeDef, 1095774808);
        XposedBridge.log("DataProcessingNativeInstance =  " + DataProcessingNativeInstance );
        Object ApplicationRegionInstance = createApplicationRegion();

        byte[] bDat = {(byte)0xdf, 0x00, 0x00, 0x01, 01, 02};
        boolean ret = (boolean)XposedHelpers.callMethod(DataProcessingNativeInstance, "isPatchSupported", bDat, ApplicationRegionInstance);
        XposedBridge.log("return from  isPatchSupported is " + ret);

        // Create AlarmConfiguration
        Class<?> AlarmConfigurationDef = XposedHelpers.findClass("com.abbottdiabetescare.flashglucose.sensorabstractionservice.AlarmConfiguration", classLoader_);
        XposedBridge.log("AlarmConfigurationDef =  " + AlarmConfigurationDef );
        Object AlarmConfigurationInstance = XposedHelpers.newInstance(AlarmConfigurationDef, 70, 180);
        XposedBridge.log("AlarmConfigurationInstance =  " + AlarmConfigurationInstance );

        // Create NonActionableConfiguration
        Class<?> NonActionableConfigurationDef = XposedHelpers.findClass("com.abbottdiabetescare.flashglucose.sensorabstractionservice.NonActionableConfiguration", classLoader_);
        XposedBridge.log("NonActionableConfigurationDef =  " + NonActionableConfigurationDef );
        Object NonActionableConfigurationInstance = XposedHelpers.newInstance(NonActionableConfigurationDef, true, true, 0, 40, 500, -2, 2);
        XposedBridge.log("NonActionableConfigurationInstance =  " + NonActionableConfigurationInstance );


        /*
        byte[] packet = {(byte)0x3a, (byte)0xcf, (byte)0x10, (byte)0x16, (byte)0x03, (byte)0x00, (byte)0x00, (byte)0x00,
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
                (byte)0x4f, (byte)0x11, (byte)0x08, (byte)0x10, (byte)0xad, (byte)0x02, (byte)0xc8, (byte)0xd4,
                (byte)0x5b, (byte)0x00, (byte)0xaa, (byte)0x02, (byte)0xc8, (byte)0xb4, (byte)0x1b, (byte)0x80,
                (byte)0xa9, (byte)0x02, (byte)0xc8, (byte)0x9c, (byte)0x5b, (byte)0x00, (byte)0xa9, (byte)0x02,
                (byte)0xc8, (byte)0x8c, (byte)0x1b, (byte)0x80, (byte)0xb0, (byte)0x02, (byte)0xc8, (byte)0x30,
                (byte)0x5c, (byte)0x80, (byte)0xb0, (byte)0x02, (byte)0x88, (byte)0xe6, (byte)0x9c, (byte)0x80,
                (byte)0xb8, (byte)0x02, (byte)0xc8, (byte)0x3c, (byte)0x9d, (byte)0x80, (byte)0xb8, (byte)0x02,
                (byte)0xc8, (byte)0x60, (byte)0x9d, (byte)0x80, (byte)0xa1, (byte)0x02, (byte)0xc8, (byte)0xdc,
                (byte)0x9e, (byte)0x80, (byte)0xab, (byte)0x02, (byte)0xc8, (byte)0x14, (byte)0x9e, (byte)0x80,
                (byte)0xa9, (byte)0x02, (byte)0xc8, (byte)0xc0, (byte)0x9d, (byte)0x80, (byte)0xab, (byte)0x02,
                (byte)0xc8, (byte)0x78, (byte)0x9d, (byte)0x80, (byte)0xaa, (byte)0x02, (byte)0xc8, (byte)0x40,
                (byte)0x9d, (byte)0x80, (byte)0xa8, (byte)0x02, (byte)0xc8, (byte)0x08, (byte)0x9d, (byte)0x80,
                (byte)0xa8, (byte)0x02, (byte)0xc8, (byte)0x2c, (byte)0x5c, (byte)0x80, (byte)0xad, (byte)0x02,
                (byte)0xc8, (byte)0xf8, (byte)0x5b, (byte)0x00, (byte)0x29, (byte)0x06, (byte)0xc8, (byte)0xf4,
                (byte)0x9b, (byte)0x80, (byte)0xc9, (byte)0x05, (byte)0xc8, (byte)0x8c, (byte)0xde, (byte)0x80,
                (byte)0xc3, (byte)0x05, (byte)0xc8, (byte)0x28, (byte)0x9e, (byte)0x80, (byte)0x2c, (byte)0x06,
                (byte)0xc8, (byte)0xd0, (byte)0x9e, (byte)0x80, (byte)0x7b, (byte)0x06, (byte)0x88, (byte)0xa6,
                (byte)0x9e, (byte)0x80, (byte)0xf9, (byte)0x05, (byte)0xc8, (byte)0xb0, (byte)0x9e, (byte)0x80,
                (byte)0x99, (byte)0x05, (byte)0xc8, (byte)0xf0, (byte)0x9e, (byte)0x80, (byte)0x2e, (byte)0x05,
                (byte)0xc8, (byte)0x00, (byte)0x9f, (byte)0x80, (byte)0x81, (byte)0x04, (byte)0xc8, (byte)0x48,
                (byte)0xa0, (byte)0x80, (byte)0x5d, (byte)0x04, (byte)0xc8, (byte)0x38, (byte)0x9d, (byte)0x80,
                (byte)0x12, (byte)0x04, (byte)0xc8, (byte)0x10, (byte)0x9e, (byte)0x80, (byte)0xcf, (byte)0x03,
                (byte)0xc8, (byte)0x4c, (byte)0x9e, (byte)0x80, (byte)0x6f, (byte)0x03, (byte)0xc8, (byte)0xb8,
                (byte)0x9e, (byte)0x80, (byte)0x19, (byte)0x03, (byte)0xc8, (byte)0x40, (byte)0x9f, (byte)0x80,
                (byte)0xc5, (byte)0x02, (byte)0xc8, (byte)0xf4, (byte)0x9e, (byte)0x80, (byte)0xaa, (byte)0x02,
                (byte)0xc8, (byte)0xf8, (byte)0x5b, (byte)0x00, (byte)0xa2, (byte)0x04, (byte)0xc8, (byte)0x38,
                (byte)0x9a, (byte)0x00, (byte)0xd1, (byte)0x04, (byte)0xc8, (byte)0x28, (byte)0x9b, (byte)0x80,
                (byte)0xe4, (byte)0x04, (byte)0xc8, (byte)0xe0, (byte)0x1a, (byte)0x80, (byte)0x8f, (byte)0x04,
                (byte)0xc8, (byte)0x20, (byte)0x9b, (byte)0x80, (byte)0x22, (byte)0x06, (byte)0xc8, (byte)0x50,
                (byte)0x5b, (byte)0x80, (byte)0xbc, (byte)0x06, (byte)0xc8, (byte)0x54, (byte)0x9c, (byte)0x80,
                (byte)0x7f, (byte)0x05, (byte)0xc8, (byte)0x24, (byte)0x5c, (byte)0x80, (byte)0xc9, (byte)0x05,
                (byte)0xc8, (byte)0x38, (byte)0x5c, (byte)0x80, (byte)0x38, (byte)0x05, (byte)0xc8, (byte)0xf4,
                (byte)0x1a, (byte)0x80, (byte)0x37, (byte)0x07, (byte)0xc8, (byte)0x84, (byte)0x5b, (byte)0x80,
                (byte)0xfb, (byte)0x08, (byte)0xc8, (byte)0x4c, (byte)0x9c, (byte)0x80, (byte)0xfb, (byte)0x09,
                (byte)0xc8, (byte)0x7c, (byte)0x9b, (byte)0x80, (byte)0x77, (byte)0x0a, (byte)0xc8, (byte)0xe4,
                (byte)0x5a, (byte)0x80, (byte)0xdf, (byte)0x09, (byte)0xc8, (byte)0x88, (byte)0x9f, (byte)0x80,
                (byte)0x6d, (byte)0x08, (byte)0xc8, (byte)0x2c, (byte)0x9f, (byte)0x80, (byte)0xc3, (byte)0x06,
                (byte)0xc8, (byte)0xb0, (byte)0x9d, (byte)0x80, (byte)0xd9, (byte)0x11, (byte)0x00, (byte)0x00,
                (byte)0x72, (byte)0xc2, (byte)0x00, (byte)0x08, (byte)0x82, (byte)0x05, (byte)0x09, (byte)0x51,
                (byte)0x14, (byte)0x07, (byte)0x96, (byte)0x80, (byte)0x5a, (byte)0x00, (byte)0xed, (byte)0xa6,
                (byte)0x0e, (byte)0x6e, (byte)0x1a, (byte)0xc8, (byte)0x04, (byte)0xdd, (byte)0x58, (byte)0x6d};

        */

        int sensorStartTimestamp = 0x0e181349;
        int sensorScanTimestamp = 0x0e1c4794;
        //sensorScanTimestamp = sensorStartTimestamp+ 3600 * 2 + 24*3600;
        int currentUtcOffset = 0x0036ee80;
        /*
        byte[] oldState = {(byte)0xd5, (byte)0x11, (byte)0x00, (byte)0x00, (byte)0xb8, (byte)0x25, (byte)0xb5, (byte)0x94,
                (byte)0x94, (byte)0x56, (byte)0xcc, (byte)0x36, (byte)0x25, (byte)0xec, (byte)0x4b, (byte)0x40,
                (byte)0xd5, (byte)0x11, (byte)0x00, (byte)0x00, (byte)0x38, (byte)0xee, (byte)0xe9, (byte)0xae,
                (byte)0x6b, (byte)0x09, (byte)0x20, (byte)0x2b, (byte)0xfe, (byte)0x44, (byte)0xdc, (byte)0xbf};
        */

        Object DataProcessingOutputsInstance =  XposedHelpers.callMethod(DataProcessingNativeInstance, "processScan", AlarmConfigurationInstance, NonActionableConfigurationInstance,
                packet, sensorStartTimestamp, sensorScanTimestamp, currentUtcOffset, oldState);
        XposedBridge.log("return from  processScan is " + DataProcessingOutputsInstance + " " + Utils.objectToString(DataProcessingOutputsInstance));

        java.lang.reflect.Method method = null;
        Object AlgorithmResultsInstance = null;
        Object GlucoseValueInstance = null;


        try {
            method = DataProcessingOutputsInstance.getClass().getMethod("getAlgorithmResults");
        } catch (NoSuchMethodException e) {
            XposedBridge.log("NoSuchMethodException: Exception cought in getAlgorithmResults" + e);
        }
        try {
            AlgorithmResultsInstance = method.invoke(DataProcessingOutputsInstance);
        } catch (IllegalAccessException e) {
            XposedBridge.log("IllegalAccessException: Exception cought in getRealTimeGlucose" + e);
        } catch (InvocationTargetException e) {
            XposedBridge.log("InvocationTargetException: Exception cought in getRealTimeGlucose" + e);
        }
        XposedBridge.log("return from  getAlgorithmResults is AlgorithmResultsInstance = " + AlgorithmResultsInstance + " " + Utils.objectToString(AlgorithmResultsInstance));



        try {
            method = AlgorithmResultsInstance.getClass().getMethod("getRealTimeGlucose");
        } catch (NoSuchMethodException e) {
            XposedBridge.log("NoSuchMethodException: Exception cought in getRealTimeGlucose" + e);
        }
        try {
            GlucoseValueInstance = method.invoke(AlgorithmResultsInstance);
        } catch (IllegalAccessException e) {
            XposedBridge.log("IllegalAccessException: Exception cought in getRealTimeGlucose" + e);
        } catch (InvocationTargetException e) {
            XposedBridge.log("InvocationTargetException: Exception cought in getRealTimeGlucose" + e);
        }
        XposedBridge.log("return from  processScan is getRealTimeGlucose GlucoseValueInstance = " + GlucoseValueInstance + " " + Utils.objectToString(GlucoseValueInstance));

        // Get SGV value:
        int sgv = 38;
        try {
            method = GlucoseValueInstance.getClass().getMethod("getValue");
        } catch (NoSuchMethodException e) {
            XposedBridge.log("NoSuchMethodException: Exception cought in GlucoseValueInstance.getValue" + e);
        }
        try {
            sgv = (int)method.invoke(GlucoseValueInstance);
        } catch (IllegalAccessException e) {
            XposedBridge.log("IllegalAccessException: Exception cought in getRealTimeGlucose" + e);
        } catch (InvocationTargetException e) {
            XposedBridge.log("InvocationTargetException: Exception cought in getRealTimeGlucose" + e);
        }
        XposedBridge.log("return from  processScan sgv = " + sgv);
        BroadcastBack(context, sgv, timestamp);


    }

    void BroadcastBack(Context context, int sgv, long timestamp) {
        // Broadcast the data back to xDrip.
        JSONObject jo = new JSONObject();
        try {
            jo.put("type", "sgv");
            jo.put("sgv", sgv);
            jo.put("date", timestamp);
        }catch (JSONException e) {
            XposedBridge.log("JSONException: Exception cought in jo.put " + e);
        }

        JSONArray ja = new JSONArray();
        ja.put(jo);

        Intent intent = new Intent(Constants.XDRIP_PLUS_NS_EMULATOR);
        Bundle bundle = new Bundle();
        bundle.putString("collection", "entries");
        bundle.putString("data", ja.toString());
        intent.putExtras(bundle);
        intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
        context.sendBroadcast(intent);
    }
}

public class NfcModifier implements IXposedHookLoadPackage {






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
                        XposedBridge.log("We are after tranceiveWithRetries input = " + Utils.byteArrayToHex(input) + " output =" + Utils.byteArrayToHex(result));
                        if(result == null) {
                            return;
                        }
                        if(result.length != 7) {
                            return;
                        }
                        byte[] cmp = {0x00,(byte)0xdf, 0x00, 0x00, 0x08};
                        if(!Utils.comparePartialByteArray(cmp, result)) {
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

        findAndHookConstructor("com.abbottdiabetescare.flashglucose.sensorabstractionservice.dataprocessing.DataProcessingNative", lpparam.classLoader, int.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                XposedBridge.log("We are before DataProcessingNative.DataProcessingNative " + param.args[0] );
            }
        });

        findAndHookMethod("com.abbottdiabetescare.flashglucose.sensorabstractionservice.dataprocessing.DataProcessingNative", lpparam.classLoader,
                "isPatchSupported", byte[].class, "com.abbottdiabetescare.flashglucose.sensorabstractionservice.ApplicationRegion", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                XposedBridge.log("We are before DataProcessingNative.isPatchSupported " + param.args[0] + " " +  param.args[1] + " this = "+ param.thisObject);
                XposedBridge.log("continuing bytes =  " + Utils.byteArrayToHex((byte[])param.args[0]) + "this = " + Utils.objectToString(param.thisObject));
            }

        });

        findAndHookMethod("com.abbottdiabetescare.flashglucose.sensorabstractionservice.dataprocessing.DataProcessingNative", lpparam.classLoader, "processScan",
                "com.abbottdiabetescare.flashglucose.sensorabstractionservice.AlarmConfiguration",
                "com.abbottdiabetescare.flashglucose.sensorabstractionservice.NonActionableConfiguration",
                byte[].class, int.class, int.class, int.class,
                byte[].class, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        XposedBridge.log("We are before DataProcessingNative.processScan AlarmConfiguration " + param.args[0] + " NonActionableConfiguration " + param.args[1] +
                                " packet " + param.args[2] + " sensorStartTimestamp " + param.args[3] + " sensorScanTimestamp " + param.args[4] +
                                " currentUtcOffset " + param.args[5] + " oldState " + param.args[6] +  "this = " + param.thisObject);
                        XposedBridge.log("continuing before DataProcessingNative.processScan AlarmConfiguration " +  Utils.objectToString(param.args[0]) +
                        " NonActionableConfiguration " + Utils.objectToString(param.args[1]));
                        Utils.writeToFile("scan_mem" , (byte[])param.args[2]);
                    }

                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        XposedBridge.log("We are after DataProcessingNative.processScan");
                        Object DataProcessingOutputsInstance = param.getResult();
                        byte[] newState = null;
                        java.lang.reflect.Method method = null;
                        if(DataProcessingOutputsInstance != null) {
                            try {
                                method = DataProcessingOutputsInstance.getClass().getMethod("getNewState");
                            } catch (NoSuchMethodException e) {
                                XposedBridge.log("NoSuchMethodException: Exception cought in getNewState" + e);
                            }
                            try {
                                newState = (byte[]) method.invoke(DataProcessingOutputsInstance);
                            } catch (IllegalAccessException e) {
                                XposedBridge.log("IllegalAccessException: Exception cought in getNewState" + e);
                            } catch (InvocationTargetException e) {
                                XposedBridge.log("InvocationTargetException: Exception cought in getNewState" + e);
                            }
                        } else {
                            XposedBridge.log("We are after DataProcessingNative.processScan return value is null!!!");
                        }
                        XposedBridge.log("We are after DataProcessingNative.processScan newStatis = " + Utils.byteArrayToHex(newState));
                        Utils.writeToFile("new_state" , newState);
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

                IntentFilter filter = new IntentFilter(Constants.XDRIP_PLUS_LIBRE_DATA);
                MyReceiver receiver = new MyReceiver(lpparam.classLoader);
                context.registerReceiver(receiver, filter);

                //?? intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);

                // Code under is probably not needed.
                if (true) return;


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





            }
        });

    }

}
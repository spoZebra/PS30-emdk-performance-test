package com.spozebra.ps30emdkperftest;

import android.content.Context;

import com.symbol.emdk.EMDKBase;
import com.symbol.emdk.EMDKException;
import com.symbol.emdk.EMDKManager;
import com.symbol.emdk.EMDKResults;
import com.symbol.emdk.ProfileManager;
import com.symbol.emdk.barcode.BarcodeManager;
import com.symbol.emdk.barcode.ScanDataCollection;
import com.symbol.emdk.barcode.Scanner;
import com.symbol.emdk.barcode.ScannerInfo;

import java.util.ArrayList;
import java.util.List;

public class EmdkTest implements EMDKManager.EMDKListener {

    private EMDKManager emdkManager = null;
    private BarcodeManager barcodeManager = null;
    private Context applicationContext;
    private IDataListener listener = null;
    private CounterManager counterManager = null;

    public void createInstance(Context applicationContext, IDataListener listener) {
        this.applicationContext = applicationContext;
        this.listener = listener;

        counterManager = new CounterManager(applicationContext);
        int cc = counterManager.getCounter();
        listener.addMsg("--- Tentative #" + cc + " ---");
        initEmdk();
    }

    private void initEmdk(){
        EMDKResults results = EMDKManager.getEMDKManager(applicationContext, this);
        if (results.statusCode != EMDKResults.STATUS_CODE.SUCCESS) {
            throw new RuntimeException("EMDKManager object request failed!");
        }
    }

    @Override
    public void onOpened(EMDKManager emdkManager) {
        listener.addMsg( "EMDK open success!");
        this.emdkManager = emdkManager;
        try {
            emdkManager.getInstanceAsync(EMDKManager.FEATURE_TYPE.BARCODE, new EMDKManager.StatusListener() {
                @Override
                public void onStatus(EMDKManager.StatusData statusData, EMDKBase emdkBase) {
                    listener.addMsg("Init barcodeManager...");
                    if (statusData.getResult() == EMDKResults.STATUS_CODE.SUCCESS) {
                        try {

                            barcodeManager = (BarcodeManager) emdkBase;

                            if(barcodeManager == null) {
                                listener.addMsg("!!!ERROR!!! - BarcodeManager not initialized even if OnOpen was successful");
                                return;
                            }

                            listener.addMsg("BarcodeManager successfully initialized");

                            // Get default scanner and trigger a scan to prove that barcode manager works!
                            Scanner scanner = barcodeManager.getDevice(BarcodeManager.DeviceIdentifier.DEFAULT);

                            scanner.enable();

                            scanner.addDataListener(new Scanner.DataListener() {
                                @Override
                                public void onData(ScanDataCollection scanDataCollection) {
                                    // Nothing to do here
                                }
                            });

                            // Trigger the scan once
                            scanner.triggerType = Scanner.TriggerType.SOFT_ONCE;
                            scanner.read();

                            Thread.sleep(2000);

                            // Stop it.
                            scanner.cancelRead();
                            scanner.disable();

                            listener.addMsg("BarcodeManager works fine!");

                            // Everything's fine -> Increment the counter and reboot one more time
                            counterManager.incrementCounter();
                            reboot();
                        }
                        catch (Exception e) {
                            listener.addMsg("!ERROR! - Barcode Manager should be fine...but something else happened! - please check");
                            listener.addMsg(e.getMessage());
                            e.printStackTrace();
                            onClosed();
                        }
                    }
                    else {
                        listener.addMsg("Open ERROR...Retrying...");
                        onClosed();
                        initEmdk();
                    }
                }
            });
        } catch (Exception e) {
            listener.addMsg("Not expected...");
            listener.addMsg(e.getMessage());
            onClosed();
            e.printStackTrace();
        }
    }


    private void reboot() throws EMDKException {
        emdkManager.getInstanceAsync(EMDKManager.FEATURE_TYPE.PROFILE, new EMDKManager.StatusListener() {
            @Override
            public void onStatus(EMDKManager.StatusData statusData, EMDKBase emdkBase) {
                listener.addMsg("Init Profile manager...");
                if (statusData.getResult() == EMDKResults.STATUS_CODE.SUCCESS) {
                    try {
                        listener.addMsg("Rebooting...");
                        ProfileManager profileManager = (ProfileManager) emdkBase;
                        EMDKResults result = profileManager.processProfile("shutdown", ProfileManager.PROFILE_FLAG.SET, (String[])null);
                        listener.addMsg("OK...");
                    }
                    catch (Exception e) {
                        listener.addMsg(e.getMessage());
                        onClosed();
                        e.printStackTrace();
                    }
                }
                else {
                    listener.addMsg("Ops...");
                }
            }
        });
    }


    @Override
    public void onClosed() {
        // Release all the resources
        if (emdkManager != null) {
            emdkManager.release();
            emdkManager = null;
        }
    }

}
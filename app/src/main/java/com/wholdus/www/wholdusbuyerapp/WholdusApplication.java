package com.wholdus.www.wholdusbuyerapp;

import android.content.Context;
import android.support.multidex.MultiDex;
import android.support.multidex.MultiDexApplication;

import com.facebook.stetho.Stetho;

/**
 * Created by aditya on 20/11/16.
 */

public class WholdusApplication extends MultiDexApplication {

    private String mAccessToken;
    private String mRefreshToken;
    private int mBuyerID;

    @Override
    public void onCreate() {
        super.onCreate();
        // Create an InitializerBuilder
        Stetho.InitializerBuilder initializerBuilder = Stetho.newInitializerBuilder(this);

        // Enable Chrome DevTools
        initializerBuilder.enableWebKitInspector(Stetho.defaultInspectorModulesProvider(this));

        // Use the InitializerBuilder to generate an Initializer
        Stetho.Initializer initializer = initializerBuilder.build();

        // Initialize Stetho with the Initializer
        Stetho.initialize(initializer);

        mBuyerID = -1;
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(WholdusApplication.this);
    }

    public void setTokens(String aToken, String rToken, int buyerID) {
        mAccessToken = aToken;
        mRefreshToken = rToken;
        mBuyerID = buyerID;
    }

    public String getAccessToken() {
        return mAccessToken;
    }

    public String getRefreshToken() {
        return mRefreshToken;
    }

    public int getBuyerID() {
        return mBuyerID;
    }
}

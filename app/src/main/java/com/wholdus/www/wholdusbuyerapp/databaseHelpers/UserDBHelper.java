package com.wholdus.www.wholdusbuyerapp.databaseHelpers;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.wholdus.www.wholdusbuyerapp.databaseContracts.OrdersContract;
import com.wholdus.www.wholdusbuyerapp.databaseContracts.UserProfileContract.BusinessTypesTable;
import com.wholdus.www.wholdusbuyerapp.databaseContracts.UserProfileContract.UserAddressTable;
import com.wholdus.www.wholdusbuyerapp.databaseContracts.UserProfileContract.UserInterestsTable;
import com.wholdus.www.wholdusbuyerapp.databaseContracts.UserProfileContract.UserTable;
import com.wholdus.www.wholdusbuyerapp.databaseContracts.OrdersContract.OrdersTable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by aditya on 25/11/16.
 */

public class UserDBHelper extends BaseDBHelper {

    public UserDBHelper(Context context) {
        super(context);
    }

    public Cursor getUserData(String buyerID) {
        String query = "SELECT * FROM " + UserTable.TABLE_NAME + " WHERE " + UserTable.COLUMN_BUYER_ID + "= " + buyerID + ";";
        return getCursor(query);
    }

    public Cursor getUserAddress(@Nullable String addressID, int _ID, @Nullable List<String> columns) {
        String columnNames;
        if (columns!= null && !columns.isEmpty()){
            columnNames = TextUtils.join(", ", columns);
        } else {columnNames = "*";}

        String query = "SELECT " + columnNames +" FROM " + UserAddressTable.TABLE_NAME;
        if (addressID != null && !TextUtils.isEmpty(addressID)) {
            query += " WHERE " + UserAddressTable.COLUMN_ADDRESS_ID + " = " + addressID;
        } else if (_ID != -1) {
            query += " WHERE " + UserAddressTable._ID + " = " + _ID;
        }
        return getCursor(query);
    }

    public ArrayList<String> getPresentBuyerAddressIDs(){
        ArrayList<String> columns = new ArrayList<>();
        columns.add(UserAddressTable.COLUMN_ADDRESS_ID);
        Cursor cursor = getUserAddress(null, -1, columns);
        ArrayList<String> buyerAddressIDs = new ArrayList<>();
        while (cursor.moveToNext()){
            buyerAddressIDs.add(cursor.getString(cursor.getColumnIndexOrThrow(UserAddressTable.COLUMN_ADDRESS_ID)));
        }
        return buyerAddressIDs;
    }

    public Cursor getUserInterests(@Nullable String buyerInterestID, int _ID) {
        String query = "SELECT * FROM " + UserInterestsTable.TABLE_NAME;
        if (buyerInterestID != null) {
            query += " WHERE " + UserInterestsTable.COLUMN_BUYER_INTEREST_ID + "=" + buyerInterestID;
        } else if (_ID != -1) {
            query += " WHERE " + UserInterestsTable._ID + "=" + _ID;
        }
        return getCursor(query);
    }

    public Cursor getBusinessTypes(@Nullable String businessTypeID) {
        String query = "SELECT * FROM " + BusinessTypesTable.TABLE_NAME;
        if (businessTypeID != null) {
            query += " WHERE " + BusinessTypesTable.COLUMN_BUSINESS_TYPE_ID + " = " + businessTypeID;
        }
        return getCursor(query);
    }

    public long updateUserData(JSONObject data) throws JSONException {

        ContentValues user = new ContentValues();
        user.put(UserTable.COLUMN_NAME, data.getString(UserTable.COLUMN_NAME));
        user.put(UserTable.COLUMN_COMPANY_NAME, data.getString(UserTable.COLUMN_COMPANY_NAME));
        user.put(UserTable.COLUMN_EMAIL, data.getString(UserTable.COLUMN_EMAIL));
        user.put(UserTable.COLUMN_MOBILE_NUMBER, data.getString(UserTable.COLUMN_MOBILE_NUMBER));
        user.put(UserTable.COLUMN_WHATSAPP_NUMBER, data.getString(UserTable.COLUMN_WHATSAPP_NUMBER));
        user.put(UserTable.COLUMN_GENDER, data.getString(UserTable.COLUMN_GENDER));

        String businessType = data.getJSONObject("details").getJSONObject("buyer_type").getString(UserTable.COLUMN_BUSINESS_TYPE);
        user.put(UserTable.COLUMN_BUSINESS_TYPE, businessType);

        String buyerID = data.getString(UserTable.COLUMN_BUYER_ID);
        long rows;
        SQLiteDatabase db = mDatabaseHelper.openDatabase();

        if (getUserData(buyerID).getCount() == 0) {
            user.put(UserTable.COLUMN_BUYER_ID, buyerID);
            rows = db.insert(UserTable.TABLE_NAME, null, user);
        } else {
            rows = db.update(
                    UserTable.TABLE_NAME,
                    user,
                    UserTable.COLUMN_BUYER_ID + "=" + buyerID,
                    null
            );
        }
        mDatabaseHelper.closeDatabase();
        return rows;
    }

    public long updateUserData(String buyerID, ContentValues values) {
        SQLiteDatabase db = mDatabaseHelper.openDatabase();

        long rows = db.update(
                UserTable.TABLE_NAME,
                values,
                UserTable.COLUMN_BUYER_ID + "=" + buyerID,
                null
        );

        mDatabaseHelper.closeDatabase();
        return rows;
    }

    public long updateUserAddressData(JSONObject data) throws JSONException {
        JSONArray address = new JSONArray();

        try {
            address = data.getJSONArray("address");
        } catch (Exception e) {
            address.put(0, data);
        }

        for (int i = 0; i < address.length(); i++) {
            JSONObject currAddress = address.getJSONObject(i);
            updateUserAddressDataFromJSONObject(currAddress, null, null);
        }

        mDatabaseHelper.closeDatabase();
        return 1;
    }

    public void updateUserAddressDataFromJSONObject(JSONObject currAddress,
                                                    @Nullable SQLiteDatabase db, @Nullable Boolean addressPresent) throws JSONException{
        boolean databaseOpened = false;
        if (db== null) {
            db = mDatabaseHelper.openDatabase();
            databaseOpened = true;
        }

        String addressID = currAddress.getString(UserAddressTable.COLUMN_ADDRESS_ID);
        ContentValues values = getBuyerAddressContentValues(currAddress);
        values.put(UserAddressTable.COLUMN_ADDRESS_ID, addressID);

        if (addressPresent==null){
            addressPresent = (getUserAddress(addressID, -1, null).getCount() != 0);
        }

        if (!addressPresent){
            // insert
            db.insert(UserAddressTable.TABLE_NAME, null, values);
        } else {
            String selection = UserAddressTable.COLUMN_ADDRESS_ID + "=" + addressID;
            db.update(UserAddressTable.TABLE_NAME, values, selection, null);
        }
        if (databaseOpened) {mDatabaseHelper.closeDatabase();}
    }

    private ContentValues getBuyerAddressContentValues(JSONObject currAddress) throws JSONException{
        ContentValues values = new ContentValues();
        values.put(UserAddressTable.COLUMN_ADDRESS_ID, currAddress.getString(UserAddressTable.COLUMN_ADDRESS_ID));
        values.put(UserAddressTable.COLUMN_ADDRESS, currAddress.getString(UserAddressTable.COLUMN_ADDRESS));
        values.put(UserAddressTable.COLUMN_ADDRESS_ALIAS, currAddress.getString(UserAddressTable.COLUMN_ADDRESS_ALIAS));
        values.put(UserAddressTable.COLUMN_CITY, currAddress.getString(UserAddressTable.COLUMN_CITY));
        values.put(UserAddressTable.COLUMN_CONTACT_NUMBER, currAddress.getString(UserAddressTable.COLUMN_CONTACT_NUMBER));
        values.put(UserAddressTable.COLUMN_LANDMARK, currAddress.getString(UserAddressTable.COLUMN_LANDMARK));
        values.put(UserAddressTable.COLUMN_PINCODE_ID, currAddress.getString(UserAddressTable.COLUMN_PINCODE_ID));
        values.put(UserAddressTable.COLUMN_PINCODE, currAddress.getString(UserAddressTable.COLUMN_PINCODE));
        values.put(UserAddressTable.COLUMN_STATE, currAddress.getString(UserAddressTable.COLUMN_STATE));
        values.put(UserAddressTable.COLUMN_PRIORITY, currAddress.getString(UserAddressTable.COLUMN_PRIORITY));
        return values;
    }

    public int updateBusinessTypesData(JSONObject data) throws JSONException {
        JSONArray businessTypes = data.getJSONArray(BusinessTypesTable.TABLE_NAME);
        SQLiteDatabase db = mDatabaseHelper.openDatabase();

        for (int i = 0; i < businessTypes.length(); i++) {
            JSONObject bt = businessTypes.getJSONObject(i);
            ContentValues values = new ContentValues();
            String businessTypeID = bt.getString(BusinessTypesTable.COLUMN_BUSINESS_TYPE_ID);
            values.put(BusinessTypesTable.COLUMN_BUSINESS_TYPE_ID, businessTypeID);
            values.put(BusinessTypesTable.COLUMN_BUSINESS_TYPE, bt.getString(BusinessTypesTable.COLUMN_BUSINESS_TYPE));
            values.put(BusinessTypesTable.COLUMN_DESCRIPTION, bt.getString(BusinessTypesTable.COLUMN_DESCRIPTION));

            if (getBusinessTypes(businessTypeID).getCount() == 0) { // insert
                db.insert(BusinessTypesTable.TABLE_NAME, null, values);
            } else { // update
                db.update(BusinessTypesTable.TABLE_NAME,
                        values,
                        BusinessTypesTable.COLUMN_BUSINESS_TYPE_ID + "=" + businessTypeID,
                        null);
            }
        }

        mDatabaseHelper.closeDatabase();
        return businessTypes.length();
    }

    public int updateUserInterestsData(JSONObject data) throws JSONException {
        JSONArray interests = data.getJSONArray(UserInterestsTable.TABLE_NAME);

        SQLiteDatabase db = mDatabaseHelper.openDatabase();

        for (int i = 0; i < interests.length(); i++) {
            JSONObject currInterest = interests.getJSONObject(i);
            ContentValues values = new ContentValues();

            JSONObject category = currInterest.getJSONObject("category");
            String buyerInterestID = currInterest.getString(UserInterestsTable.COLUMN_BUYER_INTEREST_ID);

            int priceFilterApplied = currInterest.getBoolean(UserInterestsTable.COLUMN_PRICE_FILTER_APPLIED) ? 1 : 0;
            values.put(UserInterestsTable.COLUMN_BUYER_INTEREST_ID, buyerInterestID);
            values.put(UserInterestsTable.COLUMN_CATEGORY_ID, category.getString(UserInterestsTable.COLUMN_CATEGORY_ID));
            values.put(UserInterestsTable.COLUMN_CATEGORY_NAME, category.getString(UserInterestsTable.COLUMN_CATEGORY_NAME));
            values.put(UserInterestsTable.COLUMN_MIN_PRICE_PER_UNIT, currInterest.getString(UserInterestsTable.COLUMN_MIN_PRICE_PER_UNIT));
            values.put(UserInterestsTable.COLUMN_MAX_PRICE_PER_UNIT, currInterest.getString(UserInterestsTable.COLUMN_MAX_PRICE_PER_UNIT));
            values.put(UserInterestsTable.COLUMN_PRICE_FILTER_APPLIED, priceFilterApplied);
            values.put(UserInterestsTable.COLUMN_FABRIC_FILTER_TEXT, currInterest.getString(UserInterestsTable.COLUMN_FABRIC_FILTER_TEXT));

            if (getUserInterests(buyerInterestID, -1).getCount() == 0) { // insert
                db.insert(UserInterestsTable.TABLE_NAME, null, values);
            } else {
                String selection = UserInterestsTable.COLUMN_BUYER_INTEREST_ID + "=" + buyerInterestID;
                db.update(UserInterestsTable.TABLE_NAME, values, selection, null);
            }
        }

        mDatabaseHelper.closeDatabase();
        return 0;
    }
}

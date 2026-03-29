package com.example.streat_cafe;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "StreatCafe.db";
    public static final String TABLE_USERS = "users";
    public static final String TABLE_CART = "cart";
    public static final String TABLE_ADDRESSES = "addresses";

    public static final String COL_USER_ID = "ID";
    public static final String COL_EMAIL = "EMAIL";
    public static final String COL_FIRSTNAME = "FIRSTNAME";
    public static final String COL_LASTNAME = "LASTNAME";
    public static final String COL_PHONE = "PHONE";
    public static final String COL_PASSWORD = "PASSWORD";

    public static final String COL_CART_ID = "ID";
    public static final String COL_CART_USER_EMAIL = "USER_EMAIL";
    public static final String COL_PRODUCT_NAME = "PRODUCT_NAME";
    public static final String COL_PRODUCT_PRICE = "PRODUCT_PRICE";
    public static final String COL_PRODUCT_QTY = "QUANTITY";
    public static final String COL_PRODUCT_CATEGORY = "CATEGORY";
    public static final String COL_PRODUCT_IMAGE = "IMAGE_RES";
    public static final String COL_PRODUCT_SIZE = "SIZE";
    public static final String COL_PRODUCT_SWEETNESS = "SWEETNESS";
    public static final String COL_PRODUCT_ADDONS = "ADDONS";

    public static final String COL_ADDR_ID = "ID";
    public static final String COL_ADDR_USER_EMAIL = "USER_EMAIL";
    public static final String COL_ADDR_LABEL = "LABEL";
    public static final String COL_ADDR_PROVINCE = "PROVINCE";
    public static final String COL_ADDR_MUNICIPALITY = "MUNICIPALITY";
    public static final String COL_ADDR_DETAILS = "DETAILS";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, 6);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_USERS + " (ID INTEGER PRIMARY KEY AUTOINCREMENT, EMAIL TEXT UNIQUE, FIRSTNAME TEXT, LASTNAME TEXT, PHONE TEXT, PASSWORD TEXT)");
        db.execSQL("CREATE TABLE " + TABLE_CART + " (ID INTEGER PRIMARY KEY AUTOINCREMENT, USER_EMAIL TEXT, PRODUCT_NAME TEXT, PRODUCT_PRICE DOUBLE, QUANTITY INTEGER, CATEGORY TEXT, IMAGE_RES INTEGER, SIZE TEXT, SWEETNESS TEXT, ADDONS TEXT)");
        db.execSQL("CREATE TABLE " + TABLE_ADDRESSES + " (ID INTEGER PRIMARY KEY AUTOINCREMENT, USER_EMAIL TEXT, LABEL TEXT, PROVINCE TEXT, MUNICIPALITY TEXT, DETAILS TEXT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CART);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_ADDRESSES);
        onCreate(db);
    }

    public boolean insertUser(String email, String firstName, String lastName, String phone, String password) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(COL_EMAIL, email);
        contentValues.put(COL_FIRSTNAME, firstName);
        contentValues.put(COL_LASTNAME, lastName);
        contentValues.put(COL_PHONE, phone);
        contentValues.put(COL_PASSWORD, password);
        long result = db.insert(TABLE_USERS, null, contentValues);
        return result != -1;
    }

    public boolean checkUser(String email, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_USERS + " WHERE EMAIL=? AND PASSWORD=?", new String[]{email, password});
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }

    public boolean checkEmailExists(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_USERS + " WHERE EMAIL=?", new String[]{email});
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }

    public String getFirstName(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT FIRSTNAME FROM " + TABLE_USERS + " WHERE EMAIL=?", new String[]{email});
        if (cursor.moveToFirst()) {
            String name = cursor.getString(0);
            cursor.close();
            return name;
        }
        cursor.close();
        return "User";
    }

    public Cursor getUserData(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE_USERS + " WHERE EMAIL=?", new String[]{email});
    }

    public boolean addToCart(String userEmail, String productName, double price, int qty, String category, int imageRes, String size, String sweetness, String addons) {
        SQLiteDatabase db = this.getWritableDatabase();
        
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_CART + " WHERE USER_EMAIL=? AND PRODUCT_NAME=? AND SIZE=? AND SWEETNESS=? AND ADDONS=?",
                new String[]{userEmail, productName, size, sweetness, addons});
        
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            int currentQty = cursor.getInt(cursor.getColumnIndexOrThrow(COL_PRODUCT_QTY));
            int id = cursor.getInt(cursor.getColumnIndexOrThrow(COL_CART_ID));
            ContentValues cv = new ContentValues();
            cv.put(COL_PRODUCT_QTY, currentQty + qty);
            db.update(TABLE_CART, cv, "ID=?", new String[]{String.valueOf(id)});
            cursor.close();
            return true;
        }
        cursor.close();

        ContentValues contentValues = new ContentValues();
        contentValues.put(COL_CART_USER_EMAIL, userEmail);
        contentValues.put(COL_PRODUCT_NAME, productName);
        contentValues.put(COL_PRODUCT_PRICE, price);
        contentValues.put(COL_PRODUCT_QTY, qty);
        contentValues.put(COL_PRODUCT_CATEGORY, category);
        contentValues.put(COL_PRODUCT_IMAGE, imageRes);
        contentValues.put(COL_PRODUCT_SIZE, size);
        contentValues.put(COL_PRODUCT_SWEETNESS, sweetness);
        contentValues.put(COL_PRODUCT_ADDONS, addons);
        long result = db.insert(TABLE_CART, null, contentValues);
        return result != -1;
    }

    public void updateCartItemQuantity(int cartId, int newQty) {
        SQLiteDatabase db = this.getWritableDatabase();
        if (newQty <= 0) {
            deleteCartItem(cartId);
        } else {
            ContentValues cv = new ContentValues();
            cv.put(COL_PRODUCT_QTY, newQty);
            db.update(TABLE_CART, cv, "ID=?", new String[]{String.valueOf(cartId)});
        }
    }

    public Cursor getCartItems(String userEmail) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE_CART + " WHERE USER_EMAIL=?", new String[]{userEmail});
    }

    public void deleteCartItem(int cartId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_CART, "ID=?", new String[]{String.valueOf(cartId)});
    }

    public boolean insertAddress(String userEmail, String label, String province, String municipality, String details) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_ADDR_USER_EMAIL, userEmail);
        cv.put(COL_ADDR_LABEL, label);
        cv.put(COL_ADDR_PROVINCE, province);
        cv.put(COL_ADDR_MUNICIPALITY, municipality);
        cv.put(COL_ADDR_DETAILS, details);
        long result = db.insert(TABLE_ADDRESSES, null, cv);
        return result != -1;
    }

    public Cursor getAddresses(String userEmail) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE_ADDRESSES + " WHERE USER_EMAIL=?", new String[]{userEmail});
    }
}

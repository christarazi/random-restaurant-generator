package com.chris.randomrestaurantgenerator.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.chris.randomrestaurantgenerator.models.Restaurant;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Database helper class to facilitate interactions with an SQLite DB.
 */
public class RestaurantDBHelper extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "restaurantsDB.db";
    public static final String TABLE_RESTAURANTS = "restaurants";

    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_RESTNAME = "restaurantname";
    public static final String COLUMN_RATING = "rating";
    public static final String COLUMN_RATINGURL = "ratingurl";
    public static final String COLUMN_THUMBNAILURL = "thumbnailurl";
    public static final String COLUMN_URL = "url";
    public static final String COLUMN_CATEGORIES = "categories";
    public static final String COLUMN_ADDRESS = "address";
    public static final String COLUMN_REVIEWCOUNT = "reviewcount";
    public static final String COLUMN_DEAL = "deal";
    public static final String COLUMN_DISTANCE = "distance";
    public static final String COLUMN_LAT = "lat";
    public static final String COLUMN_LON = "lon";

    public RestaurantDBHelper(Context context, SQLiteDatabase.CursorFactory factory) {
        super(context, DATABASE_NAME, factory, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_RESTAURANTS_TABLE = "CREATE TABLE " +
                TABLE_RESTAURANTS + "("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_RESTNAME + " TEXT,"
                + COLUMN_RATING + " REAL,"
                + COLUMN_RATINGURL + " TEXT,"
                + COLUMN_THUMBNAILURL + " TEXT,"
                + COLUMN_URL + " TEXT,"
                + COLUMN_CATEGORIES + " TEXT,"
                + COLUMN_ADDRESS + " TEXT,"
                + COLUMN_REVIEWCOUNT + " INTEGER,"
                + COLUMN_DEAL + " TEXT,"
                + COLUMN_DISTANCE + " REAL,"
                + COLUMN_LAT + " REAL,"
                + COLUMN_LON + " REAL"
                + ")";
        db.execSQL(CREATE_RESTAURANTS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_RESTAURANTS);
        onCreate(db);
    }

    public long insert(Restaurant res) {
        SQLiteDatabase database = this.getWritableDatabase();

        ContentValues cv = new ContentValues();
        cv.put(COLUMN_RESTNAME, res.getName());
        cv.put(COLUMN_RATING, res.getRating());
        cv.put(COLUMN_RATINGURL, res.getRatingImageURL());
        cv.put(COLUMN_THUMBNAILURL, res.getThumbnailURL());
        cv.put(COLUMN_URL, res.getUrl());

        try {
            JSONObject categories = new JSONObject();
            categories.put(COLUMN_CATEGORIES, new JSONArray(res.getCategories()));
            cv.put(COLUMN_CATEGORIES, categories.toString());

            JSONObject address = new JSONObject();
            address.put(COLUMN_ADDRESS, new JSONArray(res.getAddress()));
            cv.put(COLUMN_ADDRESS, address.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        cv.put(COLUMN_REVIEWCOUNT, res.getReviewCount());
        cv.put(COLUMN_DEAL, res.getDeal());
        cv.put(COLUMN_DISTANCE, res.getDistance());
        cv.put(COLUMN_LAT, res.getLat());
        cv.put(COLUMN_LON, res.getLon());

        long ret = database.insert(TABLE_RESTAURANTS, null, cv);
        database.close();

        return ret;
    }

    public ArrayList<Restaurant> getAll() {
        ArrayList<Restaurant> restaurants = new ArrayList<>();

        SQLiteDatabase database = this.getWritableDatabase();
        Cursor cursor = null;
        try {
             cursor = database.rawQuery(String.format("SELECT * FROM %s", TABLE_RESTAURANTS), null);

            if (cursor.moveToFirst()) {

                while (!cursor.isAfterLast()) {
                    JSONArray categories = new JSONObject(cursor.getString(cursor.getColumnIndex(COLUMN_CATEGORIES)))
                            .optJSONArray(COLUMN_CATEGORIES);
                    JSONArray address =  new JSONObject(cursor.getString(cursor.getColumnIndex(COLUMN_ADDRESS)))
                            .optJSONArray(COLUMN_ADDRESS);

                    ArrayList<String> resCategories = new ArrayList<>();
                    ArrayList<String> resAddress = new ArrayList<>();

                    for (int i = 0; i < categories.length(); i++)
                        resCategories.add(categories.getString(i));


                    for (int i = 0; i < address.length(); i++)
                        resAddress.add(address.getString(i));

                    Restaurant restaurant = new Restaurant(
                            cursor.getString(cursor.getColumnIndex(COLUMN_RESTNAME)),
                            cursor.getFloat(cursor.getColumnIndex(COLUMN_RATING)),
                            cursor.getString(cursor.getColumnIndex(COLUMN_RATINGURL)),
                            cursor.getString(cursor.getColumnIndex(COLUMN_THUMBNAILURL)),
                            cursor.getInt(cursor.getColumnIndex(COLUMN_REVIEWCOUNT)),
                            cursor.getString(cursor.getColumnIndex(COLUMN_URL)),
                            resCategories,
                            resAddress,
                            cursor.getString(cursor.getColumnIndex(COLUMN_DEAL)),
                            cursor.getDouble(cursor.getColumnIndex(COLUMN_DISTANCE)),
                            cursor.getDouble(cursor.getColumnIndex(COLUMN_LAT)),
                            cursor.getDouble(cursor.getColumnIndex(COLUMN_LON))
                    );

                    restaurants.add(restaurant);
                    cursor.moveToNext();
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        finally {
            if (cursor != null)
                cursor.close();
            else
                Log.d("RRG", "Cursor is null");
        }

        return restaurants;
    }

    public long delete(Restaurant res) {
        SQLiteDatabase database = this.getWritableDatabase();
        long ret = database.delete(TABLE_RESTAURANTS, COLUMN_RESTNAME + "=?", new String[] { res.getName() });
        database.close();
        return ret;
    }

    public void deleteAll() {
        SQLiteDatabase database = this.getWritableDatabase();
        database.execSQL("delete from " + TABLE_RESTAURANTS);
        database.close();
    }
}

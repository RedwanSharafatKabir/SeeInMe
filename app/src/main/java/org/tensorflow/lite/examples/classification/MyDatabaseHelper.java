package org.tensorflow.lite.examples.classification;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;

import java.util.Locale;

// All functionalities for SQlite Database

public class MyDatabaseHelper extends SQLiteOpenHelper {

    static final String DATABASE_NAME = "Items_Database.db";
    static final String TABLE_NAME = "Items_Details";
    Cursor cursor;
    static final int VERSION_NUMBER = 2;
    static final String ID = "Id";
    static final String ITEMS = "Items";
    static final String PRICE = "Price";
    Context context;
    static final String CREATE_TABLE_COMMAND = "CREATE TABLE " +
            TABLE_NAME + "( " + ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            ITEMS + " VARCHAR(255), " + PRICE + " VARCHAR(255));";

    static final String DROP_TABLE_COMMAND = "DROP TABLE IF EXISTS " + TABLE_NAME;
    static final String SHOW_ALL_DATA_COMMAND = "SELECT * FROM " + TABLE_NAME;

    public MyDatabaseHelper(@Nullable Context context) {
        super(context, DATABASE_NAME, null, VERSION_NUMBER);
        this.context = context;
    }
    // When Database is launched first time...
    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        try{
            sqLiteDatabase.execSQL(CREATE_TABLE_COMMAND);
            Log.i("Database message ", "Database created successfully");

        } catch (Exception e){
            Log.i("Database error ", e.getMessage());
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
        try {
            sqLiteDatabase.execSQL(DROP_TABLE_COMMAND);
            Toast.makeText(context, "Database upgraded successfully", Toast.LENGTH_LONG).show();
            onCreate(sqLiteDatabase);
        } catch (Exception e){
            Toast.makeText(context, "Exception: " + e, Toast.LENGTH_SHORT).show();
        }
    }

    // Insert data into Product.db database
    public long insertData(String items, String prices){
        SQLiteDatabase sqLiteDatabase = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(ITEMS, items);
        contentValues.put(PRICE, prices);
        long rowId = sqLiteDatabase.insert(TABLE_NAME, null, contentValues);

        Toast.makeText(context, "Items Added to Cart", Toast.LENGTH_SHORT).show();
        Log.i("Inserted_items ", items + " " + prices);

        return  rowId;
    }

    // Retrieve data from Product.db database
    public Cursor retrieveData(){
        SQLiteDatabase sqLiteDatabase = this.getWritableDatabase();

        try{
            cursor = sqLiteDatabase.rawQuery(SHOW_ALL_DATA_COMMAND, null);

        } catch (Exception e){
            Log.i("Error", e.getMessage());
        }

        return cursor;
    }

    public boolean updateData(String id, String items){
        SQLiteDatabase sqLiteDatabase = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(ID, id);
        contentValues.put(ITEMS, items);
        sqLiteDatabase.update(TABLE_NAME, contentValues, ID + " = ?", new String[]{id});

        return true;
    }

    public void deleteData(){
        SQLiteDatabase sqLiteDatabase = this.getWritableDatabase();

        try {
            sqLiteDatabase.execSQL(DROP_TABLE_COMMAND);
            onCreate(sqLiteDatabase);

        } catch (Exception e){
            Log.i("Error", e.getMessage());
        }

//        return sqLiteDatabase.delete(TABLE_NAME, ID + " = ?", new String[]{id});
    }
}

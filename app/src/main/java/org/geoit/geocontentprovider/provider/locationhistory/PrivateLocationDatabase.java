package org.geoit.geocontentprovider.provider.locationhistory;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;

public class PrivateLocationDatabase extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "GeoContentProvider.db";
    private static final String TABLE_NAME = "privatelocation";
    private static final String SQL_CREATE = "CREATE TABLE " + TABLE_NAME +
            " (_id INTEGER PRIMARY KEY, timestamp DATETIME , lat TEXT , lon TEXT )";

    private static final String SQL_DROP = "DROP TABLE IF EXISTS " + TABLE_NAME ;

    public PrivateLocationDatabase(Context context) {
        super(context, DATABASE_NAME, null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(SQL_DROP);
        onCreate(db);
    }

    public Cursor getLocation(String id, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        SQLiteQueryBuilder sqliteQueryBuilder = new SQLiteQueryBuilder();
        sqliteQueryBuilder.setTables(TABLE_NAME);

        if (id != null) {
            sqliteQueryBuilder.appendWhere("_id" + " = " + id);
        }

        if (sortOrder == null || sortOrder == "") {
            sortOrder = "TIMESTAMP";
        }
        Cursor cursor = sqliteQueryBuilder.query(getReadableDatabase(),
                projection,
                selection,
                selectionArgs,
                null,
                null,
                sortOrder);
        return cursor;
    }


    public long addNewLocation(ContentValues values) throws SQLException {
        long id = getWritableDatabase().insert(TABLE_NAME, "", values);
        if (id <= 0) {
            throw new SQLException("Failed to add a private location");
        }

        // Insert timestamp
        getWritableDatabase().execSQL("UPDATE " + TABLE_NAME + " SET TIMESTAMP = CURRENT_TIMESTAMP WHERE _id = " + id);

        return id;
    }

    public int deleteLocation(String id) {
        if(id == null) {
            return getWritableDatabase().delete(TABLE_NAME, null , null);
        } else {
            return getWritableDatabase().delete(TABLE_NAME, "_id=?", new String[]{id});
        }
    }

    public int updateLocation(String id, ContentValues values) {
        if(id == null) {
            return getWritableDatabase().update(TABLE_NAME, values, null, null);
        } else {
            return getWritableDatabase().update(TABLE_NAME, values, "_id=?", new String[]{id});
        }
    }
}
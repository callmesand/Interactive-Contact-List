package com.example.mycontacts;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.net.URI;

public class Provider extends ContentProvider {


    public static final int CONTACTS = 100;
    public static final int CONTACTS_ID = 101;
    public static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    static {
        sUriMatcher.addURI(Contract.CONTENT_AUTHORITY, Contract.PATH_CONTACTS, CONTACTS);

        sUriMatcher.addURI(Contract.CONTENT_AUTHORITY, Contract.PATH_CONTACTS + "/#", CONTACTS_ID);

    }

    public Dbhelper mDbhelper;

    @Override
    public boolean onCreate() {
        mDbhelper = new Dbhelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri,  String[] projection, String selection,   String[] selectionArgs,   String sortOrder) {

        SQLiteDatabase database  = mDbhelper.getReadableDatabase();

        Cursor cursor;

        int match = sUriMatcher.match(uri);
        switch (match) {
            case CONTACTS:
                cursor = database.query(Contract.ContactEntry.TABLE_NAME, projection, selection, selectionArgs, null, null, sortOrder);
                break;

            case CONTACTS_ID:

                selection = Contract.ContactEntry._ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                cursor = database.query(Contract.ContactEntry.TABLE_NAME, projection, selection, selectionArgs, null, null, sortOrder);
                break;

            default:
                throw new IllegalArgumentException("Cant Query" + uri);


        }


       cursor.setNotificationUri(getContext().getContentResolver(), uri);
        return cursor;
    }



    @Override
    public String getType( Uri uri) {
        return null;
    }

    @Override
    public Uri insert(  Uri uri,  ContentValues values) {


        int match = sUriMatcher.match(uri);
        switch (match) {
            case CONTACTS:
                return insertContact(uri, values);

            default:
                throw new IllegalArgumentException("Cannot insert contacts" + uri);
        }

    }

    private Uri insertContact(Uri uri, ContentValues values) {

        String name = values.getAsString(Contract.ContactEntry.COLUMN_NAME);
        if (name == null) {
            throw new IllegalArgumentException("Name is required");
        }


        String number = values.getAsString(Contract.ContactEntry.COLUMN_PHONENUMBER);
        if (number == null) {
            throw new IllegalArgumentException("number is required");
        }


        String email = values.getAsString(Contract.ContactEntry.COLUMN_EMAIL);
        if (email == null) {
            throw new IllegalArgumentException("email is required");
        }

        String type = values.getAsString(Contract.ContactEntry.COLUMN_TYPEOFCONTACT);
        if (type == null || !Contract.ContactEntry.isValidType(type)) {
            throw new IllegalArgumentException("type is required");

        }


        SQLiteDatabase database = mDbhelper.getWritableDatabase();
        long id = database.insert(Contract.ContactEntry.TABLE_NAME, null, values);

        if (id == -1) {
            return null;
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return ContentUris.withAppendedId(uri, id);


    }

    @Override
    public int delete ( Uri uri,   String selection,  String[] selectionArgs) {

        int rowsDeleted;
        SQLiteDatabase database = mDbhelper.getWritableDatabase();
        int match = sUriMatcher.match(uri);
        switch (match) {
            case CONTACTS:
                rowsDeleted = database.delete(Contract.ContactEntry.TABLE_NAME, selection, selectionArgs);
                break;

            case CONTACTS_ID:
                selection = Contract.ContactEntry._ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                rowsDeleted = database.delete(Contract.ContactEntry.TABLE_NAME, selection, selectionArgs);
                break;

            default:
                throw new IllegalArgumentException("Cannot delete" + uri);



        }

        if (rowsDeleted!=0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return rowsDeleted;
    }

    @Override
    public int update(  Uri uri,  ContentValues values,  String selection,  String[] selectionArgs) {

        int match = sUriMatcher.match(uri);
        switch (match) {
            case CONTACTS:
                return updateContact(uri, values, selection, selectionArgs);

            case CONTACTS_ID:

                selection = Contract.ContactEntry._ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                return updateContact(uri, values, selection, selectionArgs);

            default:
                throw new IllegalArgumentException(" Cannot update the contact");


        }
    }

    private int updateContact(Uri uri, ContentValues values, String selection, String[] selectionArgs) {

        if (values.containsKey(Contract.ContactEntry.COLUMN_NAME)) {
            String name = values.getAsString(Contract.ContactEntry.COLUMN_NAME);
           if (name == null) {
            throw new IllegalArgumentException("Name is required");
            }
        }

        if (values.containsKey(Contract.ContactEntry.COLUMN_PHONENUMBER)) {

              String number = values.getAsString(Contract.ContactEntry.COLUMN_PHONENUMBER);
              if (number == null) {
                  throw new IllegalArgumentException("number is required");
              }
        }

        if (values.containsKey(Contract.ContactEntry.COLUMN_EMAIL)) {
            String email = values.getAsString(Contract.ContactEntry.COLUMN_EMAIL);
            if (email == null) {
                throw new IllegalArgumentException("email is required");
            }
        }

        if (values.containsKey(Contract.ContactEntry.COLUMN_TYPEOFCONTACT)) {
            String type = values.getAsString(Contract.ContactEntry.COLUMN_TYPEOFCONTACT);
            if (type == null || !Contract.ContactEntry.isValidType(type)) {
                throw new IllegalArgumentException("type is required");

            }
        }

        if (values.size() == 0) {
            return 0;
        }

        SQLiteDatabase database = mDbhelper.getWritableDatabase();
        int rowsUpdated = database.update(Contract.ContactEntry.TABLE_NAME, values, selection, selectionArgs);
        if (rowsUpdated!=0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return rowsUpdated;

    }
}

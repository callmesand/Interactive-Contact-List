package com.example.mycontacts;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NavUtils;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

public class EditorActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    EditText mNameEditText, mNumberEditText, mEmailEditText;
    private Uri mPhotoUri;
    private Uri mCurrentContactUri;
    private String mType = Contract.ContactEntry.TYPEOFCONTACT_PERSONAL;
    ImageView mPhoto;
    private boolean mContactHasChanged = false;
    Spinner mSpinner;
    public static final int LOADER = 0;

    private View.OnTouchListener mOnTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            mContactHasChanged = true;
            return false;
        }
    };

    boolean hasAllRequiredValues = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);

        Intent intent = getIntent();
        mCurrentContactUri = intent.getData();

        mNameEditText = findViewById(R.id.nameEditText);
        mNumberEditText = findViewById(R.id.phoneEditText);
        mEmailEditText = findViewById(R.id.emailEditText);
        mPhoto = findViewById(R.id.profile_image);
        mSpinner = findViewById(R.id.spinner);

        if (mCurrentContactUri == null) {
            mPhoto.setImageResource(R.drawable.photo);
            setTitle("Add a Contact");

            invalidateOptionsMenu();

        } else {
            setTitle("Edit a Contact");
            getLoaderManager().initLoader(LOADER, null, this);

        }

        mNameEditText.setOnTouchListener(mOnTouchListener);
        mNumberEditText.setOnTouchListener(mOnTouchListener);
        mEmailEditText.setOnTouchListener(mOnTouchListener);
        mPhoto.setOnTouchListener(mOnTouchListener);
        mSpinner.setOnTouchListener(mOnTouchListener);

        mPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                trySelector();
                mContactHasChanged = true;
            }
        });

        setUpSpinner();


    }

    private void setUpSpinner() {

        ArrayAdapter spinner = ArrayAdapter.createFromResource(this, R.array.arrayspinner, android.R.layout.simple_spinner_item);
        spinner.setDropDownViewResource(android.R.layout.simple_dropdown_item_1line);
        mSpinner.setAdapter(spinner);

        mSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selection = (String) parent.getItemAtPosition(position);
                if (!TextUtils.isEmpty(selection)) {
                    if (selection.equals(getString(R.string.homephone))) {
                        mType = Contract.ContactEntry.TYPEOFCONTACT_HOME;
                    } else if (selection.equals(getString(R.string.workphone))) {
                        mType = Contract.ContactEntry.TYPEOFCONTACT_WORK;

                    } else {
                        mType = Contract.ContactEntry.TYPEOFCONTACT_PERSONAL;
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                mType = Contract.ContactEntry.TYPEOFCONTACT_PERSONAL;

            }
        });
    }

    public void trySelector() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
            return;
        }
        openSelector();
    }

    private void openSelector() {
        Intent intent;
        if (Build.VERSION.SDK_INT < 19) {
            intent = new Intent(Intent.ACTION_GET_CONTENT);
        } else {
            intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
        }
        intent.setType(getString(R.string.intent_type));
        startActivityForResult(Intent.createChooser(intent, getString(R.string.select_image)), 0);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case 1:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openSelector();
                }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 0 && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                mPhotoUri = data.getData();
                mPhoto.setImageURI(mPhotoUri);
                mPhoto.invalidate();
            }
        }
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menueditor, menu);
        return true;

    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {

        super.onPrepareOptionsMenu(menu);
        if (mCurrentContactUri == null) {
            MenuItem item = (MenuItem) menu.findItem(R.id.delete);
            item.setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {

            case R.id.action_save:
                saveContact();
                if (hasAllRequiredValues == true) {
                    finish();
                }
                return true;

            case R.id.delete:
                showDeleteConfirmationDialog();
                return true;

            case android.R.id.home:
                if (!mContactHasChanged) {


                    NavUtils.navigateUpFromSameTask(EditorActivity.this);
                    return true;

                }

                DialogInterface.OnClickListener discardButton = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        NavUtils.navigateUpFromSameTask(EditorActivity.this);

                    }
                };
                showUnsavedChangesDialog(discardButton);
                return true;



        }
            return super.onOptionsItemSelected(item);
    }

    private boolean saveContact() {



        String name = mNameEditText.getText().toString().trim();
        String email = mEmailEditText.getText().toString().trim();
        String phone = mNumberEditText.getText().toString().trim();


        if (mCurrentContactUri == null && TextUtils.isEmpty(name)
        && TextUtils.isEmpty(email) && TextUtils.isEmpty(phone) && mType == Contract.ContactEntry.TYPEOFCONTACT_PERSONAL && mPhotoUri == null) {

            hasAllRequiredValues = true;
            return hasAllRequiredValues;

        }

        ContentValues values = new ContentValues();

        if (TextUtils.isEmpty(name)) {
            Toast.makeText(this, "Name is Required", Toast.LENGTH_SHORT).show();
            return hasAllRequiredValues;


        } else {
            values.put(Contract.ContactEntry.COLUMN_NAME, name);
        }

        if (TextUtils.isEmpty(email)) {
            Toast.makeText(this, "Email is Required", Toast.LENGTH_SHORT).show();
            return hasAllRequiredValues;


        } else {
            values.put(Contract.ContactEntry.COLUMN_EMAIL, email);
        }

        if (TextUtils.isEmpty(phone)) {
            Toast.makeText(this, "Phone Number is Required", Toast.LENGTH_SHORT).show();
            return hasAllRequiredValues;


        } else {
            values.put(Contract.ContactEntry.COLUMN_PHONENUMBER, phone);
        }



        values.put(Contract.ContactEntry.COLUMN_TYPEOFCONTACT, mType);
        values.put(Contract.ContactEntry.COLUMN_PICTURE, mPhotoUri.toString());

        if (mCurrentContactUri == null) {

            Uri newUri = getContentResolver().insert(Contract.ContactEntry.CONTENT_URI, values);
            if (newUri == null) {
                Toast.makeText(this, "Error with Saving", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Success with Saving", Toast.LENGTH_SHORT).show();

            }
        } else {

            int rowsAffected = getContentResolver().update(mCurrentContactUri, values, null, null);
            if (rowsAffected == 0) {
                Toast.makeText(this, "Error  with Update", Toast.LENGTH_SHORT).show();

            } else {
                Toast.makeText(this, "Success  with Update", Toast.LENGTH_SHORT).show();

            }

        }

        hasAllRequiredValues = true;

        return hasAllRequiredValues;




    }


    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {

       String[] projection = {Contract.ContactEntry._ID,
               Contract.ContactEntry.COLUMN_NAME,
               Contract.ContactEntry.COLUMN_EMAIL,
               Contract.ContactEntry.COLUMN_PICTURE,
               Contract.ContactEntry.COLUMN_PHONENUMBER,
               Contract.ContactEntry.COLUMN_TYPEOFCONTACT
       };

       return new CursorLoader(this, mCurrentContactUri,
               projection, null,
               null,
               null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {

        if (cursor == null || cursor.getCount() < 1) {
            return;
        }

        if (cursor.moveToFirst()) {

            int name = cursor.getColumnIndex(Contract.ContactEntry.COLUMN_NAME);
            int email = cursor.getColumnIndex(Contract.ContactEntry.COLUMN_EMAIL);
            int type = cursor.getColumnIndex(Contract.ContactEntry.COLUMN_TYPEOFCONTACT);
            int number = cursor.getColumnIndex(Contract.ContactEntry.COLUMN_PHONENUMBER);
            int picture = cursor.getColumnIndex(Contract.ContactEntry.COLUMN_PICTURE);

            String contactname = cursor.getString(name);
            String contactemail = cursor.getString(email);
            String contactnumber = cursor.getString(number);
            String contactpicture = cursor.getString(picture);
            String typeof = cursor.getString(type);
            mPhotoUri = Uri.parse(contactpicture);

            mNumberEditText.setText(contactnumber);
            mNameEditText.setText(contactname);
            mEmailEditText.setText(contactemail);
            mPhoto.setImageURI(mPhotoUri);

            switch (typeof) {

                case Contract.ContactEntry.TYPEOFCONTACT_HOME:
                    mSpinner.setSelection(1);
                    break;

                case Contract.ContactEntry.TYPEOFCONTACT_WORK:
                        mSpinner.setSelection(2);
                    break;

                default:
                    mSpinner.setSelection(0);




            }

        }


    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

        mNumberEditText.setText("");
        mNameEditText.setText("");
        mEmailEditText.setText("");
        mPhoto.setImageResource(R.drawable.photo);

    }

    private void showUnsavedChangesDialog(
            DialogInterface.OnClickListener discardButtonClickListener) {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.unsaved_changes_dialog_msg);
        builder.setPositiveButton(R.string.discard, discardButtonClickListener);
        builder.setNegativeButton(R.string.keep_editing, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {

                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });


        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }


    private void showDeleteConfirmationDialog() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.delete_dialog_msg);
        builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {

                deleteProduct();
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {

                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }


    private void deleteProduct() {

        if (mCurrentContactUri != null) {


            int rowsDeleted = getContentResolver().delete(mCurrentContactUri, null, null);


            if (rowsDeleted == 0) {

                Toast.makeText(this, getString(R.string.editor_delete_product_failed),
                        Toast.LENGTH_SHORT).show();
            } else {

                Toast.makeText(this, getString(R.string.editor_delete_product_successful),
                        Toast.LENGTH_SHORT).show();
            }
        }


        finish();
    }

    @Override
    public void onBackPressed() {

        if (!mContactHasChanged) {
            super.onBackPressed();
            return;
        }

        DialogInterface.OnClickListener discardButtonClickListener =
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                        finish();
                    }
                };


        showUnsavedChangesDialog(discardButtonClickListener);
    }
}


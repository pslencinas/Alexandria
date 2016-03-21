package it.jaschke.alexandria;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;


import net.sourceforge.zbar.Symbol;

import it.jaschke.alexandria.Scanner.SimpleScannerActivity;
import it.jaschke.alexandria.Scanner.ZBarConstants;
import it.jaschke.alexandria.Scanner.ZBarScannerActivity;
import it.jaschke.alexandria.data.AlexandriaContract;
import it.jaschke.alexandria.services.BookService;
import it.jaschke.alexandria.services.DownloadImage;
import me.dm7.barcodescanner.zbar.ZBarScannerView;


public class AddBook extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = "INTENT_TO_SCAN_ACTIVITY";
    private EditText ean;
    private final int LOADER_ID = 1;
    private View rootView;
    private final String EAN_CONTENT="eanContent";
    private static final String SCAN_FORMAT = "scanFormat";
    private static final String SCAN_CONTENTS = "scanContents";

    private String mScanFormat = "Format:";
    private String mScanContents = "Contents:";

    public static boolean SCAN = false;
    public static String Content = "";
    public static String BarCode = "";

    public static final String SCAN_MODES = "SCAN_MODES";
    public static final String SCAN_RESULT = "SCAN_RESULT";
    public static final String SCAN_RESULT_TYPE = "SCAN_RESULT_TYPE";
    public static final String ERROR_INFO = "ERROR_INFO";
    private static final int ZBAR_SCANNER_REQUEST = 0;
    private ZBarScannerView mScannerView;

    public static final int NONE = 0;
    public static final int PARTIAL = 1;
    public static final int EAN8 = 8;
    public static final int UPCE = 9;
    public static final int ISBN10 = 10;
    public static final int UPCA = 12;
    public static final int EAN13 = 13;
    public static final int ISBN13 = 14;
    public static final int I25 = 25;
    public static final int DATABAR = 34;
    public static final int DATABAR_EXP = 35;
    public static final int CODABAR = 38;
    public static final int CODE39 = 39;
    public static final int PDF417 = 57;
    public static final int QRCODE = 64;
    public static final int CODE93 = 93;
    public static final int CODE128 = 128;

    public AddBook(){
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if(ean!=null) {
            outState.putString(EAN_CONTENT, ean.getText().toString());
        }
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        rootView = inflater.inflate(R.layout.fragment_add_book, container, false);
        ean = (EditText) rootView.findViewById(R.id.ean);

        ean.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                //no need
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                //no need
            }

            @Override
            public void afterTextChanged(Editable s) {
                String ean =s.toString();
                //catch isbn10 numbers

                if(ean.length()==10 && !ean.startsWith("978")){

                    ean="978"+ean;
                    ean=ISBNConversion(ean);
                }
                if(ean.length()<13){
                    clearFields();
                    return;
                }

                Log.i("AddBook. EAN: ", ean);
                //Once we have an ISBN, start a book intent
                Intent bookIntent = new Intent(getActivity(), BookService.class);
                bookIntent.putExtra(BookService.EAN, ean);
                bookIntent.setAction(BookService.FETCH_BOOK);
                getActivity().startService(bookIntent);
                AddBook.this.restartLoader();
            }
        });

        rootView.findViewById(R.id.scan_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // This is the callback method that the system will invoke when your button is
                // clicked. You might do this by launching another app or by including the
                //functionality directly in this app.
                // Hint: Use a Try/Catch block to handle the Intent dispatch gracefully, if you
                // are using an external app.
                //when you're done, remove the toast below.


                Context context = getActivity();
                //Intent intent = new Intent(context, ZBarScannerActivity.class);
                //intent.putExtra(ZBarConstants.SCAN_MODES, new int[]{Symbol.QRCODE, Symbol.ISBN13, Symbol.ISBN10});
                //startActivityForResult(intent, ZBAR_SCANNER_REQUEST);

                Intent scannerIntent = new Intent(context, SimpleScannerActivity.class);
                startActivity(scannerIntent);

                CharSequence text = "This button should let you scan a book for its barcode!";
                int duration = Toast.LENGTH_SHORT;

                //Toast toast = Toast.makeText(context, text, duration);
                //toast.show();

            }
        });

        rootView.findViewById(R.id.save_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ean.setText("");
            }
        });

        rootView.findViewById(R.id.delete_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent bookIntent = new Intent(getActivity(), BookService.class);
                bookIntent.putExtra(BookService.EAN, ean.getText().toString());
                bookIntent.setAction(BookService.DELETE_BOOK);
                getActivity().startService(bookIntent);
                ean.setText("");
            }
        });

        if(savedInstanceState!=null){
            ean.setText(savedInstanceState.getString(EAN_CONTENT));
            ean.setHint("");
        }

        return rootView;
    }

    private String ISBNConversion (String ean){

        int value=0;
        int control=0;
        int digit=0;

        control = Integer.parseInt(ean.substring(0, 1)) * 1;
        control += Integer.parseInt(ean.substring(1, 2)) * 3;
        control += Integer.parseInt(ean.substring(2, 3)) * 1;
        control += Integer.parseInt(ean.substring(3, 4)) * 3;
        control += Integer.parseInt(ean.substring(4, 5)) * 1;
        control += Integer.parseInt(ean.substring(5, 6)) * 3;
        control += Integer.parseInt(ean.substring(6, 7)) * 1;
        control += Integer.parseInt(ean.substring(7, 8)) * 3;
        control += Integer.parseInt(ean.substring(8, 9)) * 1;
        control += Integer.parseInt(ean.substring(9, 10)) * 3;
        control += Integer.parseInt(ean.substring(10, 11)) * 1;
        control += Integer.parseInt(ean.substring(11, 12)) * 3;

        Log.i("Control: ", control+"");
        int div, rest;
        div = control / 10;
        rest = control % 10;

        if(rest == 0){
            digit = 0;
        }else{
            digit = (div+1)*10 - control;
        }

        Log.i("ISBN10: ", ean);
        Log.i("ISBN13: ", ean.substring(0, 12) + digit);

        return ean.substring(0,12)+digit;
    }

   @Override
    public void onResume() {
        Log.i("onResume", "dentro de onResume");
        if ( SCAN && BarCode.contains("ISBN10")) {

            ean.setText(ISBNConversion("978"+Content));

            CharSequence text = "Barcode: "+BarCode;
            Toast.makeText(getActivity(), text, Toast.LENGTH_LONG).show();

            SCAN = false;
        }
        super.onResume();
    }

    private void restartLoader(){
        getLoaderManager().restartLoader(LOADER_ID, null, this);
    }

    @Override
    public android.support.v4.content.Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if(ean.getText().length()==0){
            return null;
        }
        String eanStr= ean.getText().toString();
        if(eanStr.length()==10 && !eanStr.startsWith("978")){
            eanStr="978"+eanStr;
        }
        return new CursorLoader(
                getActivity(),
                AlexandriaContract.BookEntry.buildFullBookUri(Long.parseLong(eanStr)),
                null,
                null,
                null,
                null
        );
    }

    @Override
    public void onLoadFinished(android.support.v4.content.Loader<Cursor> loader, Cursor data) {



        if (!data.moveToFirst()) {

            updateEmptyView();
            return;
        }


        String bookTitle = data.getString(data.getColumnIndex(AlexandriaContract.BookEntry.TITLE));
        ((TextView) rootView.findViewById(R.id.bookTitle)).setText(bookTitle);

        String bookSubTitle = data.getString(data.getColumnIndex(AlexandriaContract.BookEntry.SUBTITLE));
        ((TextView) rootView.findViewById(R.id.bookSubTitle)).setText(bookSubTitle);

        String authors = data.getString(data.getColumnIndex(AlexandriaContract.AuthorEntry.AUTHOR));
        String[] authorsArr = authors.split(",");
        ((TextView) rootView.findViewById(R.id.authors)).setLines(authorsArr.length);
        ((TextView) rootView.findViewById(R.id.authors)).setText(authors.replace(",","\n"));
        String imgUrl = data.getString(data.getColumnIndex(AlexandriaContract.BookEntry.IMAGE_URL));
        if(Patterns.WEB_URL.matcher(imgUrl).matches()){
            new DownloadImage((ImageView) rootView.findViewById(R.id.bookCover)).execute(imgUrl);
            rootView.findViewById(R.id.bookCover).setVisibility(View.VISIBLE);
        }

        String categories = data.getString(data.getColumnIndex(AlexandriaContract.CategoryEntry.CATEGORY));
        ((TextView) rootView.findViewById(R.id.categories)).setText(categories);

        rootView.findViewById(R.id.save_button).setVisibility(View.VISIBLE);
        rootView.findViewById(R.id.delete_button).setVisibility(View.VISIBLE);
    }

    @Override
    public void onLoaderReset(android.support.v4.content.Loader<Cursor> loader) {

    }

    private void clearFields(){
        ((TextView) rootView.findViewById(R.id.bookTitle)).setText("");
        ((TextView) rootView.findViewById(R.id.bookSubTitle)).setText("");
        ((TextView) rootView.findViewById(R.id.authors)).setText("");
        ((TextView) rootView.findViewById(R.id.categories)).setText("");
        rootView.findViewById(R.id.bookCover).setVisibility(View.INVISIBLE);
        rootView.findViewById(R.id.save_button).setVisibility(View.INVISIBLE);
        rootView.findViewById(R.id.delete_button).setVisibility(View.INVISIBLE);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        activity.setTitle(R.string.scan);
    }


    private void updateEmptyView() {


        if (!Utility.isNetworkAvailable(getActivity())) {
            CharSequence text = "No book list information available. The network is not available.";
            int duration = Toast.LENGTH_SHORT;

            Toast toast = Toast.makeText(getActivity(), text, duration);
            toast.show();
        }



    }


}

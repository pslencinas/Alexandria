package it.jaschke.alexandria.Scanner;

import android.os.Bundle;
import android.os.Handler;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import it.jaschke.alexandria.AddBook;
import it.jaschke.alexandria.R;
import me.dm7.barcodescanner.zbar.BarcodeFormat;
import me.dm7.barcodescanner.zbar.Result;
import me.dm7.barcodescanner.zbar.ZBarScannerView;

public class SimpleScannerActivity extends BaseScannerActivity implements ZBarScannerView.ResultHandler {
    private ZBarScannerView mScannerView;

    private final String LOG_TAG = SimpleScannerActivity.class.getSimpleName();

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_simple_scanner);
        setupToolbar();

        ViewGroup contentFrame = (ViewGroup) findViewById(R.id.content_frame);
        mScannerView = new ZBarScannerView(this);
        contentFrame.addView(mScannerView);
    }

    @Override
    public void onResume() {
        super.onResume();
        mScannerView.setResultHandler(this);
        mScannerView.startCamera();
    }

    @Override
    public void onPause() {
        super.onPause();
        mScannerView.stopCamera();
    }


    @Override
    public void handleResult(Result rawResult) {

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mScannerView.resumeCameraPreview(SimpleScannerActivity.this);
            }
        }, 1000);


        AddBook.SCAN = true;
        AddBook.Content = rawResult.getContents();
        AddBook.BarCode = rawResult.getBarcodeFormat().getName();
        finish();
    }
}

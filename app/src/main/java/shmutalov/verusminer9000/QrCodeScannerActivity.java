// Copyright (c) 2020, Scala
//
// Please see the included LICENSE file for more information.

package shmutalov.verusminer9000;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.journeyapps.barcodescanner.CaptureManager;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;

public class QrCodeScannerActivity extends AppCompatActivity {

    private CaptureManager capture;
    private DecoratedBarcodeView barcodeScannerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_code_scanner);

        barcodeScannerView = findViewById(R.id.barcode);

        capture = new CaptureManager(this, barcodeScannerView);
        capture.initializeFromIntent(getIntent(), savedInstanceState);

        barcodeScannerView.decodeContinuous(result -> {
            if (result != null && result.getText() != null) {
                String scannedAddress = result.getText();
                Log.d("QRCODE", "Barcode read: " + scannedAddress);

                if (Utils.verifyAddress(scannedAddress)) {
                    Config.write("address", scannedAddress);
                    Toast.makeText(this, "Address scanned successfully", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(this, "Invalid Verus address", Toast.LENGTH_SHORT).show();
                }
            }
        });

        findViewById(R.id.stop).setOnClickListener(v -> finish());

        capture.decode();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (capture != null) {
            capture.onResume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (capture != null) {
            capture.onPause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (capture != null) {
            capture.onDestroy();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (capture != null) {
            capture.onSaveInstanceState(outState);
        }
    }
}

// Copyright (c) 2020, Scala
//
// Please see the included LICENSE file for more information.

package shmutalov.verusminer9000;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.zxing.ResultPoint;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;

import java.util.List;

public class QrCodeScannerActivity extends AppCompatActivity {

    private DecoratedBarcodeView barcodeScannerView;
    private boolean resultHandled = false;

    private final BarcodeCallback callback = new BarcodeCallback() {
        @Override
        public void barcodeResult(BarcodeResult result) {
            if (resultHandled) {
                return;
            }

            if (result != null && result.getText() != null) {
                String scannedAddress = result.getText();

                if (Utils.verifyAddress(scannedAddress)) {
                    resultHandled = true;
                    barcodeScannerView.pause();

                    // Return the result to the calling activity
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("scanned_address", scannedAddress);
                    setResult(Activity.RESULT_OK, resultIntent);

                    Toast.makeText(QrCodeScannerActivity.this, "Address scanned successfully", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(QrCodeScannerActivity.this, "Invalid Verus address", Toast.LENGTH_SHORT).show();
                }
            }
        }

        @Override
        public void possibleResultPoints(List<ResultPoint> resultPoints) {
            // Not used
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_code_scanner);

        barcodeScannerView = findViewById(R.id.barcode);
        barcodeScannerView.decodeContinuous(callback);

        findViewById(R.id.stop).setOnClickListener(v -> finish());
    }

    @Override
    protected void onResume() {
        super.onResume();
        barcodeScannerView.resume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        barcodeScannerView.pause();
    }
}

// Copyright (c) 2020, Scala
//
// Please see the included LICENSE file for more information.

package shmutalov.verusminer9000;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class WizardAddressActivity extends BaseActivity {
    private ActivityResultLauncher<Intent> qrScannerLauncher;
    private ActivityResultLauncher<String> cameraPermissionLauncher;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if ((getIntent().getFlags() & Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT) != 0) {
            // Activity was brought to front and not created,
            // Thus finishing this will get us to the last viewed activity
            finish();
            return;
        }

        // Register camera permission result handler
        cameraPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        startQrCodeActivity();
                    } else {
                        Toast.makeText(this, "Camera Permission Denied.", Toast.LENGTH_LONG).show();
                    }
                });

        // Register QR scanner result handler
        qrScannerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        String scannedAddress = result.getData().getStringExtra("scanned_address");
                        if (scannedAddress != null && !scannedAddress.isEmpty()) {
                            View view2 = findViewById(android.R.id.content).getRootView();
                            TextInputEditText tvAddress = view2.findViewById(R.id.addressWizard);
                            tvAddress.setText(scannedAddress);
                        }
                    }
                });

        setContentView(R.layout.fragment_wizard_address);
    }

    public void onPaste(View view) {
        View view2 = findViewById(android.R.id.content).getRootView();

        TextInputEditText etAddress = view2.findViewById(R.id.addressWizard);
        etAddress.setText(Utils.pasteFromClipboard(WizardAddressActivity.this));
    }

    public void onScanQrCode(View view) {
        Context appContext = WizardAddressActivity.this;

        if (ContextCompat.checkSelfPermission(appContext, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        } else {
            startQrCodeActivity();
        }
    }

    private void startQrCodeActivity() {
        try {
            Intent intent = new Intent(this, QrCodeScannerActivity.class);
            qrScannerLauncher.launch(intent);
        } catch (Exception e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    public void onNext(View view) {
        View view2 = findViewById(android.R.id.content).getRootView();

        TextView tvAddress = view2.findViewById(R.id.addressWizard);
        String strAddress = tvAddress.getText().toString();

        TextInputLayout til = view2.findViewById(R.id.addressIL);

        if(strAddress.isEmpty() || !Utils.verifyAddress(strAddress)) {
            til.setErrorEnabled(true);
            til.setError(getResources().getString(R.string.invalidaddress));
            requestFocus(tvAddress);
            return;
        }

        til.setErrorEnabled(false);
        til.setError(null);

        Config.write("address", strAddress);

        startActivity(new Intent(WizardAddressActivity.this, WizardPoolActivity.class));
        finish();
    }

    private void requestFocus(View view) {
        if (view.requestFocus()) {
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        }
    }

    public void onMineScala(View view) {
        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.mine_scala);
        dialog.setCancelable(false);

        Button btnYes = dialog.findViewById(R.id.btnYes);
        btnYes.setOnClickListener(v -> {
            View view2 = findViewById(android.R.id.content).getRootView();
            TextView tvAddress = view2.findViewById(R.id.addressWizard);
            tvAddress.setText(Utils.VERUS_DONATION_ADDRESS);

            dialog.dismiss();
        });

        Button btnNo = dialog.findViewById(R.id.btnNo);
        btnNo.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }
}
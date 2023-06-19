package tech.brainco.zenlitesdk.example;

import android.graphics.drawable.ColorDrawable;
import android.view.KeyEvent;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import tech.brainco.zenlitesdk.ZenLiteDevice;

public class BaseActivity extends AppCompatActivity {

    private AlertDialog alertDialog;

    public static ZenLiteDevice selectedZenLiteDevice;

    public static ZenLiteDevice getSelectedZenLiteDevice() {
        return selectedZenLiteDevice;
    }

    public static void setSelectedZenLiteDevice(ZenLiteDevice device) {
        selectedZenLiteDevice = device;
    }

    public void showLoadingDialog() {
        alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable());
        alertDialog.setCancelable(false);
        alertDialog.setOnKeyListener(
                (dialog, keyCode, event) -> (keyCode == KeyEvent.KEYCODE_SEARCH || keyCode == KeyEvent.KEYCODE_BACK));
        alertDialog.show();
        alertDialog.setContentView(R.layout.view_loading);
        alertDialog.setCanceledOnTouchOutside(false);
    }

    public void dismissLoadingDialog() {
        if (null != alertDialog && alertDialog.isShowing()) {
            alertDialog.dismiss();
        }
    }

    void showMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    void showShortMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    /*
     * public void showError(@NotNull ZenLiteError error){
     * Toast.makeText(this, error.getCode() + ":" + error.getMessage(),
     * Toast.LENGTH_LONG).show();
     * }
     */

    public void deviceNotConnectedAlert() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Please connect your device first");
        builder.setCancelable(true);

        builder.setPositiveButton(
                "OK",
                (dialog, id) -> dialog.cancel());

        AlertDialog alert = builder.create();
        alert.show();
    }
}
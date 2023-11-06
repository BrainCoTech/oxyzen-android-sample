package tech.brainco.zenlitesdk.example;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;

import tech.brainco.zenlitesdk.DeviceInfo;
import tech.brainco.zenlitesdk.IMU;
import tech.brainco.zenlitesdk.ZenLiteDevice;
import tech.brainco.zenlitesdk.ZenLiteDeviceListener;
import tech.brainco.zenlitesdk.ZenLiteSDK;

public class ConfigActivity extends BaseActivity {
    private static final String TAG = "ConfigActivity";

    private ZenLiteDevice device = null;

    MenuItem disconnectButton;
    MenuItem connectButton;
    MenuItem shutDownButton;

    private TextView manufacturerNameText;
    private TextView modelNumberText;
    private TextView serialNumberText;
    private TextView hardwareRevisionText;
    private TextView firmwareRevisionText;

    private Spinner spinnerAfeSampleRate;
    private Spinner spinnerDataChannel;
    private Spinner spinnerLeadOffOption;
    private Spinner spinnerRldChannel;
    private Spinner spinnerLeadOffChannel;
    private Spinner spinnerImuSampleRate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_config);
        setupViews();

        // Setup device
        device = getSelectedZenLiteDevice();
        DeviceListener listener = new DeviceListener();

        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.setTitle(device.getName());
        actionBar.setSubtitle(device.getId());
        device.setListener(listener);

        manufacturerNameText.setText(device.getManufacturerName());
        modelNumberText.setText(device.getModelNumber());
        serialNumberText.setText(device.getSerialNumber());
        hardwareRevisionText.setText(device.getHardwareRevision());
        firmwareRevisionText.setText(device.getFirmwareRevision());
    }

    private void setupViews() {
        manufacturerNameText = findViewById(R.id.device_manufacturer_name);
        modelNumberText = findViewById(R.id.device_model_number);
        serialNumberText = findViewById(R.id.device_serial_number);
        hardwareRevisionText = findViewById(R.id.device_hardware_revision);
        firmwareRevisionText = findViewById(R.id.device_firmware_revision);

        spinnerAfeSampleRate = findViewById(R.id.spinner_afe_sample_rate);
        spinnerDataChannel = findViewById(R.id.spinner_data_channel);
        spinnerLeadOffOption = findViewById(R.id.spinner_lead_off_option);
        spinnerRldChannel = findViewById(R.id.spinner_rld_channel);
        spinnerLeadOffChannel = findViewById(R.id.spinner_lead_off_channel);
        spinnerImuSampleRate = findViewById(R.id.spinner_imu_sample_rate);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_device, menu);
        disconnectButton = menu.findItem(R.id.action_device_disconnect);
        shutDownButton = menu.findItem(R.id.action_shut_down);
        connectButton = menu.findItem(R.id.action_device_connect);

        if (device.isConnected()) {
            disconnectButton.setVisible(true);
            shutDownButton.setVisible(true);
            connectButton.setVisible(false);

        } else {
            connectButton.setVisible(true);
            shutDownButton.setVisible(false);
            disconnectButton.setVisible(false);
        }
        return true;
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_device_connect) {
            device.connect(this);
        } else if (itemId == R.id.action_device_disconnect) {
            device.disconnect();
        } else if (itemId == R.id.action_shut_down) {
            shutDownClick();
            Toast.makeText(getApplicationContext(), "shutting down device...", Toast.LENGTH_SHORT).show();
            Intent myIntent = new Intent(ConfigActivity.this, ScanActivity.class);
            ConfigActivity.this.startActivity(myIntent);
        } else {
            ZenLiteSDK.logW(TAG, "Unknown action");
        }
        return super.onOptionsItemSelected(item);
    }

    private class DeviceListener extends ZenLiteDeviceListener {

        @Override
        public void onConnectivityChange(ZenLiteSDK.Connectivity connectivity) {
            if (connectivity == ZenLiteSDK.Connectivity.CONNECTED) {
                connectButton.setVisible(false);
                disconnectButton.setVisible(true);
                shutDownButton.setVisible(true);
            } else if (connectivity == ZenLiteSDK.Connectivity.DISCONNECTED) {
                connectButton.setVisible(true);
                disconnectButton.setVisible(false);
                shutDownButton.setVisible(false);
            } else if (connectivity == ZenLiteSDK.Connectivity.CONNECTING) {
                connectButton.setTitle("Connecting");
            } else if (connectivity == ZenLiteSDK.Connectivity.DISCONNECTING) {
                disconnectButton.setTitle("Disconnecting");
            }

        }

        @Override
        public void onIMUData(IMU data) {
            ZenLiteSDK.logI(ConfigActivity.class.getSimpleName(), data.toString());
        }

        @Override
        public void onDeviceInfoReady(DeviceInfo info) {
            manufacturerNameText.setText(info.manufacturerName);
            modelNumberText.setText(info.modelNumber);
            serialNumberText.setText(info.serialNumber);
            hardwareRevisionText.setText(info.hardwareRevision);
            firmwareRevisionText.setText(info.firmwareRevision);
        }
    }

    public void sendAFEConfiguration(View v) {
        if (device.isConnected()) {
            try {
                Toast.makeText(getApplicationContext(), "sending AFE configuration...", Toast.LENGTH_SHORT).show();
                device.startEEG(error -> {
                    if (error != null) {
                        ZenLiteSDK.logI(ConfigActivity.class.getSimpleName(), error.toString());
                    }
                });
            } catch (Exception e) {
                ZenLiteSDK.logI("Error", e.getMessage());
            }
        } else {
            deviceNotConnectedAlert();
        }
    }

    public void sendIMUConfiguration(View v) {
        if (device.isConnected()) {
            try {
                Toast.makeText(getApplicationContext(), "sending ACC configuration...", Toast.LENGTH_SHORT).show();
                device.startIMU(error -> {
                    if (error != null) {
                        ZenLiteSDK.logI(ConfigActivity.class.getSimpleName(), error.toString());
                    }
                });
            } catch (Exception e) {
                ZenLiteSDK.logI("Error", e.getMessage());
            }
        } else {
            deviceNotConnectedAlert();
        }
    }

    public void renameClick(View v) {
        if (device.isConnected()) {
            renameDialog();
        } else {
            deviceNotConnectedAlert();
        }
    }

    private void renameDialog() {

        AlertDialog.Builder myDialog = new AlertDialog.Builder(ConfigActivity.this);
        LayoutInflater inflater = LayoutInflater.from(ConfigActivity.this);
        View myView = inflater.inflate(R.layout.device_rename, null);

        final AlertDialog dialog = myDialog.create();
        dialog.setView(myView);

        TextView deviceName = myView.findViewById(R.id.device_name);
        EditText edtDeviceNewName = myView.findViewById(R.id.edt_device_new_name);
        Button btnSave = myView.findViewById(R.id.btn_save);

        deviceName.setText(device.getName());

        btnSave.setOnClickListener(v -> {
            String deviceNewName = edtDeviceNewName.getText().toString().trim();

            if (TextUtils.isEmpty(deviceNewName)) {
                edtDeviceNewName.setError("Required Field..");
                return;
            }

            //device.setDeviceName("Oxyz-Yongle", null);
            device.setDeviceName(deviceNewName, error -> {
                if (error != null) {
                    ZenLiteSDK.logI(TAG, "setDeviceName " + error.getCode() + error.getMessage());
                }
            });

            dialog.dismiss();
        });

        dialog.show();

    }

    public void redClick(View v) {}

    public void greenClick(View v) {}

    public void blueClick(View v) {}

    public void shutDownClick() {
        if (device.isConnected()) {
        } else {
            deviceNotConnectedAlert();
        }
    }
}

package tech.brainco.zenlitesdk.example;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;

import java.util.Arrays;
import java.util.Locale;

import tech.brainco.zenlitesdk.BrainWave;
import tech.brainco.zenlitesdk.DFUCallback;
import tech.brainco.zenlitesdk.DeviceInfo;
import tech.brainco.zenlitesdk.EEG;
import tech.brainco.zenlitesdk.IMU;
import tech.brainco.zenlitesdk.PPG;
import tech.brainco.zenlitesdk.PPGSampleRate;
import tech.brainco.zenlitesdk.ZenLiteDevice;
import tech.brainco.zenlitesdk.ZenLiteDeviceListener;
import tech.brainco.zenlitesdk.ZenLiteOTA;
import tech.brainco.zenlitesdk.ZenLiteSDK;

public class DeviceActivity extends BaseActivity {
    private static final String TAG = "DeviceActivity";
    MenuItem disconnectButton;
    MenuItem connectButton;
    MenuItem shutDownButton;

    private ZenLiteDevice device = null;

    private TextView batteryLevelText;
    private TextView eegMetaDataText;
    private TextView eegDataText;
    private TextView deviceConnectivityText;
    private TextView deviceContactStateText;
    private TextView deviceCalmnessText;
    private TextView deviceMeditationText;

    private TextView deviceDelta;
    private TextView deviceTheta;
    private TextView deviceAlpha;
    private TextView deviceLowBeta;
    private TextView deviceHighBeta;
    private TextView deviceGamma;
    private Button deviceDataStreamButton;
    private Button devicePairButton;
    private TextView imuDataText;

    private boolean paired = false;
    private boolean pairing = false;

    @Override
    protected void onStop() {
        super.onStop();

        Log.i(TAG, "onStop");
        stopEEG();
        stopIMU();
        device.stopPPG(null);
    }

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device);

        setupViews();
        DeviceListener listener = new DeviceListener();

        device = getSelectedZenLiteDevice();
        final int rssi = device.getRssi();
        Log.i(TAG, "BLE device rssi: " + rssi);

        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.setTitle(device.getName());
        actionBar.setSubtitle(device.getId());
        device.setListener(listener);
        deviceDataStreamButton.setOnClickListener(v -> dataStreamClick());
        devicePairButton.setOnLongClickListener(v -> {
            Log.d("TEST_ID", ZenLiteDevice.getDeviceId());
            return true;
        });
        devicePairButton.setOnClickListener(v -> pairAction());
        if (device.isConnected()) {
            Log.i(TAG, "device is connected already, startEEG");
            dataStreamClick();
        }
    }

    private void setupViews() {
        batteryLevelText = findViewById(R.id.battery_level);
        eegMetaDataText = findViewById(R.id.eeg_meta_data);
        eegDataText = findViewById(R.id.eeg_data_text);
        imuDataText = findViewById(R.id.imu_data_text);

        deviceConnectivityText = findViewById(R.id.device_connectivity);
        deviceContactStateText = findViewById(R.id.device_contact_state);
        deviceCalmnessText = findViewById(R.id.device_calmness);
        deviceMeditationText = findViewById(R.id.device_meditation);

        deviceDelta = findViewById(R.id.device_delta);
        deviceTheta = findViewById(R.id.device_theta);
        deviceAlpha = findViewById(R.id.device_alpha);
        deviceLowBeta = findViewById(R.id.device_low_beta);
        deviceHighBeta = findViewById(R.id.device_high_beta);
        deviceGamma = findViewById(R.id.device_gamma);

        devicePairButton = findViewById(R.id.btn_device_pair);
        deviceDataStreamButton = findViewById(R.id.btn_device_data_stream);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_device, menu);
        shutDownButton = menu.findItem(R.id.action_shut_down);
        disconnectButton = menu.findItem(R.id.action_device_disconnect);
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
        switch (item.getItemId()) {
            case R.id.action_device_connect: {
                device.connect(this);
                break;
            }
            case R.id.action_device_disconnect: {
                device.disconnect();
                break;
            }
            case R.id.action_shut_down: {
                shutDownClick();
                Toast.makeText(getApplicationContext(), "shutting down device...", Toast.LENGTH_SHORT).show();
                Intent myIntent = new Intent(DeviceActivity.this, ScanActivity.class);
                DeviceActivity.this.startActivity(myIntent);
                break;
            }
            default:
                Log.d("SearchHeadbandsActivity", "Unknown action");
        }
        return super.onOptionsItemSelected(item);
    }

    private class DeviceListener extends ZenLiteDeviceListener {
        @Override
        public void onDeviceInfoReady(DeviceInfo info) {
            Log.d(TAG, "info=" + info);
        }

        @SuppressLint("SetTextI18n")
        @Override
        public void onBatteryLevelChange(int batteryLevel) {
            Log.d(TAG, "batteryLevel=" + batteryLevel);
            if (batteryLevel >= 0 && batteryLevel <= 100) {
                batteryLevelText.setText("Battery Level: " + batteryLevel + "%");
            } else {
                batteryLevelText.setText("Battery Level: Unknown");
            }
        }

        @SuppressLint("SetTextI18n")
        @Override
        public void onConnectivityChange(ZenLiteSDK.Connectivity connectivity) {
            Log.d(TAG, "connectivity=" + connectivity);
            connectButton.setVisible(false);
            disconnectButton.setVisible(false);
            shutDownButton.setVisible(false);
            connectButton.setTitle("");
            disconnectButton.setTitle("");

            if (connectivity == ZenLiteSDK.Connectivity.CONNECTING) {
                deviceConnectivityText.setText("CONNECTING");
                connectButton.setVisible(true);
                connectButton.setEnabled(false);
                connectButton.setTitle("CONNECTING");

            } else if (connectivity == ZenLiteSDK.Connectivity.CONNECTED) {
                deviceConnectivityText.setText("CONNECTED");
                shutDownButton.setVisible(true);
                disconnectButton.setVisible(true);
                disconnectButton.setEnabled(true);
                disconnectButton.setTitle("Disconnect");
                pairAction();

            } else if (connectivity == ZenLiteSDK.Connectivity.DISCONNECTED) {
                deviceConnectivityText.setText("DISCONNECTED");
                paired = false;
                connectButton.setVisible(true);
                connectButton.setEnabled(true);
                connectButton.setTitle("Connect");

            } else if (connectivity == ZenLiteSDK.Connectivity.DISCONNECTING) {
                deviceConnectivityText.setText("DISCONNECTING");
                disconnectButton.setVisible(true);
                disconnectButton.setEnabled(false);
                disconnectButton.setTitle("DISCONNECTING");
            }
        }

        @SuppressLint("SetTextI18n")
        @Override
        public void onContactStateChange(ZenLiteSDK.ContactState state) {
            Log.i(TAG, "ContactState=" + state);
            if (state.isContacted())
                deviceContactStateText.setText("Contacted");
            else if (state == ZenLiteSDK.ContactState.OFF)
                deviceContactStateText.setText("LeadOff");
            else if (state == ZenLiteSDK.ContactState.UNKNOWN)
                deviceContactStateText.setText("UNKNOWN");
        }

        @Override
        public void onOrientationChange(ZenLiteSDK.Orientation orientation) {
            // Log.d(TAG, "orientation=" + orientation);
        }

        @Override
        public void onIMUData(IMU data) {
            imuDataText.setText(data.toString());
        }

        @Override
        public void onPPGData(PPG ppg) {
            Log.i(TAG, "onPPGData, seq_num=" + ppg.sequence_num);
        }

        @Override
        public void onEEGData(EEG data) {
            eegMetaDataText.setText(String.format(Locale.getDefault(), "SN:%d SR:%.1f",
                    data.getSequenceNumber(),
                    data.getSampleRate()));

            eegDataText.setText(Arrays.toString(data.getEEGData()));
        }

        @Override
        public void onBrainWave(BrainWave wave) {
            deviceDelta.setText(String.format(Locale.getDefault(), "%.3f",
                    wave.getDelta()));
            deviceTheta.setText(String.format(Locale.getDefault(), "%.3f",
                    wave.getTheta()));
            deviceAlpha.setText(String.format(Locale.getDefault(), "%.3f",
                    wave.getAlpha()));
            deviceLowBeta.setText(String.format(Locale.getDefault(), "%.3f",
                    wave.getLowBeta()));
            deviceHighBeta.setText(String.format(Locale.getDefault(), "%.3f",
                    wave.getHighBeta()));
            deviceGamma.setText(String.format(Locale.getDefault(), "%.3f",
                    wave.getGamma()));
        }

        @Override
        public void onAttention(float attention, float weighted_attention) {
            Log.i(TAG, "onAttention, attention=" + attention + ", weighted_attention=" + weighted_attention);
        }

        @Override
        public void onMeditation(float meditation, float calmness, float awareness) {
            Log.i(TAG, "onMeditation, meditation=" + meditation + ", calmness=" + calmness + ", awareness=" + awareness);
            deviceCalmnessText.setText(String.valueOf(calmness));
            deviceMeditationText.setText(String.valueOf(meditation));
        }
    }

    @SuppressLint("SetTextI18n")
    public void dataStreamClick() {
        if (deviceDataStreamButton.getText().toString().equals("Start")) {
            startEEG();
            startIMU();
            startPPG();
        } else {
            stopEEG();
            stopIMU();
            stopPPG();
        }
    }

    private void doDfu() {
        boolean ret = device.isNewFirmwareAvailable();
        Log.i(TAG, "isNewFirmwareAvailable=" + ret);
        Log.i(TAG, "latestVersion=" + ZenLiteOTA.latestVersion);
        Log.i(TAG, "desc=" + ZenLiteOTA.desc);

        device.startDfu(this, new DFUCallback() {
            @Override
            public void onSuccess() {
                Log.i(TAG, "OTA Success");
            }

            @Override
            public void onFailure(Exception e) {
                Log.i(TAG, e.getMessage());
            }

            @Override
            public void onProgress(int progress) {
                Log.i(TAG, "progress=" + progress);
            }
        });
    }

    public void shutDownClick() {
    }

    @SuppressLint("SetTextI18n")
    private void startEEG() {
        device.startEEG(error -> {
            if (error != null) {
                Log.i(TAG, "startEEG:" + error.getCode() + ", message=" + error.getMessage());
            } else {
                Log.i(TAG, "startEEG success");
                deviceDataStreamButton.setText("Stop");
            }
        });
    }

    private void stopEEG() {
        device.stopEEG(null);
    }

    private void startIMU() {
        device.startIMU(error -> {
            if (error != null) {
                Log.i(TAG, "startIMU:" + error.getCode() + ", message=" + error.getMessage());
            } else {
                Log.i(TAG, "startIMU success");
            }
        });
    }

    private void stopIMU() {
        device.stopIMU(error -> {
            if (error != null) {
                Log.i(TAG, "stopIMU:" + error.getCode() + ", message=" + error.getMessage());
            } else {
                Log.i(TAG, "stopIMU success");
            }
        });
    }

    private void startPPG() {
        device.startPPG(PPGSampleRate.SR25, error -> {
//        device.startPPG(PPGSampleRate.SR25, error -> {
            if (error != null) {
                Log.i(TAG, "startPPG:" + error.getCode() + ", message=" + error.getMessage());
            } else {
                Log.i(TAG, "startPPG success");
            }
        });
    }

    private void stopPPG() {
        device.stopPPG(error -> {
            if (error != null) {
                Log.i(TAG, "stopPPG:" + error.getCode() + ", message=" + error.getMessage());
            } else {
                Log.i(TAG, "stopPPG success");
            }
        });
    }

    @SuppressLint("SetTextI18n")
    private void pairAction() {
        if (device.isConnected()) {
            if (!pairing) {
                if (paired) {
                    showShortMessage("Already paired");
                } else {
                    pairing = true;
                    devicePairButton.setText("Pairing");
                    if (device.inPairingMode()) {
                        device.pair(error -> {
                            pairing = false;
                            if (error == null) {
                                paired = true;
                                dataStreamClick();
                                devicePairButton.setText("Paired");
                            } else {
                                devicePairButton.setText("Pair");

                                if (error.getCode() == 3) {
                                    showShortMessage("配对失败");
                                } else {
                                    showShortMessage("Pair failed " + error.getMessage());
                                }
                            }
                        });
                    } else if (device.isInNormalMode()) {
                        device.validatePairInfo(error -> {
                            pairing = false;
                            if (error == null) {
                                paired = true;
                                dataStreamClick();
                                devicePairButton.setText("Paired");
                            } else {
                                devicePairButton.setText("Pair");
                                if (error.getCode() == 4) {
                                    showMessage("检验配对信息失败，去重新配对");
                                } else {
                                    showMessage("Validate pair info failed " + error.getMessage());
                                }
                            }
                        });
                    } else {
                        pairing = false;
                        showShortMessage("Unknown device mode");
                        devicePairButton.setText("Pair");
                    }
                }
            } else {
                showShortMessage("Pairing");
            }
        } else {
            showMessage("Must be connected before pair");
        }
    }
}
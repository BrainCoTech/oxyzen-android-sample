package tech.brainco.zenlitesdk.example;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import tech.brainco.zenlitesdk.ZenLiteDevice;
import tech.brainco.zenlitesdk.ZenLiteDeviceScanListener;
import tech.brainco.zenlitesdk.ZenLiteError;
import tech.brainco.zenlitesdk.ZenLitePermissions;
import tech.brainco.zenlitesdk.ZenLiteSDK;

public class ScanActivity extends BaseActivity {
    private final String TAG = "ScanActivity";
    private List<ZenLiteDevice> devices = new ArrayList<>();
    private DeviceListAdapter deviceListAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);

        RecyclerView deviceListView = findViewById(R.id.device_list);
        deviceListView.setHasFixedSize(true);
        deviceListAdapter = new DeviceListAdapter(this);
        deviceListView.setAdapter(deviceListAdapter);
        deviceListView.setLayoutManager(new LinearLayoutManager(this));
        ZenLiteSDK.setLogLevel(ZenLiteSDK.LogLevel.INFO);
        ZenLiteSDK.registerBLEStateReceiver(this);
        Log.i(TAG, "OxyZenSDK Version: " + ZenLiteSDK.getSdkVersion());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        ZenLiteSDK.unregisterBLEStateReceiver(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_scan, menu);
        return true;
    }

    private void scanDevices() {
        if (ZenLiteSDK.isScanning) {
            dismissLoadingDialog();
            ZenLiteSDK.stopScan();
            return;
        }

        if (!ZenLitePermissions.checkBluetoothFeature(this)) {
            showMessage("BLE not supported");
            return;
        }

        BluetoothAdapter adapter = ((BluetoothManager) this.getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter();
        if (adapter == null || !adapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            this.startActivity(enableBtIntent);
            return;
        }

        // check Permissions
        if (!ZenLitePermissions.checkPermissions(this)) {
            ZenLitePermissions.requestPermissions(this);
            return;
        }
        if (!isLocationServiceEnabled(this)) {
            showMessage("Please enable location service");
            return;
        }

        showLoadingDialog();
        ZenLiteSDK.startScan(new ZenLiteDeviceScanListener() {
            @Override
            public void onBluetoothAdapterStateChange(int state) {
                ZenLiteSDK.logI(TAG, "BluetoothAdapter state=" + state);
                switch (state) {
                    case BluetoothAdapter.STATE_ON:
                        // restart scan
                        scanDevices();
                        break;
                    case BluetoothAdapter.STATE_OFF:
                        ZenLiteSDK.stopScan();
                        break;
                    default:
                        break;
                }
            }

            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onFoundDevices(List<ZenLiteDevice> results) {
                if (!results.isEmpty())
                    dismissLoadingDialog();

                // TODO: removeAll scan results which not in pairing mode and not exists paired success record
                devices = results;
                deviceListAdapter.notifyDataSetChanged();
            }

            @Override
            public void onError(ZenLiteError error) {
                dismissLoadingDialog();
                showMessage(error.getMessage());
            }
        }, null);
    }

    public boolean isLocationServiceEnabled(Context context) {
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if (locationManager != null) {
            return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        }
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_scan_device) {
            devices.clear();
            deviceListAdapter.notifyDataSetChanged();
            scanDevices();
        }
        return super.onOptionsItemSelected(item);
    }

    public static class DeviceListAdapter extends RecyclerView.Adapter<DeviceListAdapter.DeviceViewHolder> {
        private final ScanActivity context;

        static class DeviceViewHolder extends RecyclerView.ViewHolder {
            // each data item is just a string in this case
            TextView nameTextView;
            TextView idTextView;

            TextView inPairingTextView;
            TextView rssiTextView;

            DeviceViewHolder(View view) {
                super(view);
                nameTextView = view.findViewById(R.id.item_device_info_name);
                idTextView = view.findViewById(R.id.item_device_info_id);
                inPairingTextView = view.findViewById(R.id.item_inPairingMode);
                rssiTextView = view.findViewById(R.id.item_rssi);
            }
        }

        // Provide a suitable constructor (depends on the kind of dataset)
        DeviceListAdapter(ScanActivity context) {
            this.context = context;
        }

        @SuppressLint("InflateParams")
        @NonNull
        @Override
        public DeviceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_device_info, null);
            view.setLayoutParams(new RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT,
                    RecyclerView.LayoutParams.WRAP_CONTENT));
            return new DeviceViewHolder(view);
        }

        @Override
        public void onBindViewHolder(DeviceViewHolder holder, int position) {
            final ZenLiteDevice device = context.devices.get(position);
            holder.nameTextView.setText(device.getName());
            holder.idTextView.setText(device.getId());
            holder.rssiTextView.setText(String.valueOf(device.getRssi()));
            holder.inPairingTextView.setText(device.inPairingMode() ? "配对模式" : "普通模式");
            holder.itemView.setOnClickListener(v -> {
                ZenLiteSDK.stopScan();
                setSelectedZenLiteDevice(device);
                Intent intent = new Intent(context, DeviceActivity.class);
                context.startActivity(intent);
                // clear scan results
                context.devices.clear();
                context.deviceListAdapter.notifyDataSetChanged();
            });
        }

        @Override
        public int getItemCount() {
            return context.devices.size();
        }
    }
}
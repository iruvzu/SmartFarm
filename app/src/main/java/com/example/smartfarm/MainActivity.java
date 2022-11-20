package com.example.smartfarm;

import static android.content.ContentValues.TAG;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    public static final int REQUEST_CODE_MENU = 101;

    UUID BT_MODULE_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); // "random" unique identifier

    BluetoothAdapter btAdapter;
    private final static int REQUEST_ENABLE_BT = 1;

    TextView textStatus, plantMoisture1, plantMoisture2, plantMoisture3, plantBrighter;
    Button btnPaired, btnSend1, btnSend2, btnSend3;
    ListView listView;

    Set<BluetoothDevice> pairedDevices;
    ArrayAdapter<String> btArrayAdapter;
    ArrayList<String> deviceAddressArray;

    BluetoothSocket btSocket = null;
    ConnectedThread connectedThread;

    int pltmoisture1;
    int pltmoisture2;
    int pltmoisture3;
    int pltbrighter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //main 액션바 제거
        ActionBar actionBar = getSupportActionBar();
        actionBar.hide();

        //권한체크
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_DENIED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 2);
                return;
            }
        }

        // Enable bluetooth
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!btAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        // variables
        textStatus = (TextView) findViewById(R.id.text_status);
        btnPaired = (Button) findViewById(R.id.btn_paired);
        //센서값
        plantMoisture1 = (TextView) findViewById(R.id.plantmoisture1);
        plantMoisture2 = (TextView) findViewById(R.id.plantmoisture2);
        plantMoisture3 = (TextView) findViewById(R.id.plantmoisture3);
        plantBrighter = (TextView) findViewById(R.id.plantbrighter);
        //btnSearch = (Button) findViewById(R.id.btn_search);
        btnSend1 = (Button) findViewById(R.id.btn_send1);
        btnSend2 = (Button) findViewById(R.id.btn_send2);
        btnSend3 = (Button) findViewById(R.id.btn_send3);
        listView = findViewById(R.id.listview);

        // show paired devices
        btArrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        deviceAddressArray = new ArrayList<>();
        listView.setAdapter(btArrayAdapter);

        listView.setOnItemClickListener(new myOnItemClickListener());
    }

    public void onClickButtonPaired(View view) {
        //권한체크
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_DENIED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 2);
                return;
            }
        }

        btArrayAdapter.clear();
        if (deviceAddressArray != null && !deviceAddressArray.isEmpty()) {
            deviceAddressArray.clear();
        }
        pairedDevices = btAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            // There are paired devices. Get the name and address of each paired device.
            for (BluetoothDevice device : pairedDevices) {
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress(); // MAC address
                btArrayAdapter.add(deviceName);
                deviceAddressArray.add(deviceHardwareAddress);
            }
        }
    }

    // Create a BroadcastReceiver for ACTION_FOUND.
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            //권한체크
            if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_DENIED) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 2);
                    return;
                }
            }

            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Discovery has found a device. Get the BluetoothDevice
                // object and its info from the Intent.
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress(); // MAC address
                btArrayAdapter.add(deviceName);
                deviceAddressArray.add(deviceHardwareAddress);
                btArrayAdapter.notifyDataSetChanged();
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Don't forget to unregister the ACTION_FOUND receiver.
        unregisterReceiver(receiver);
    }

    //블루투스 기기와 통신
    public class myOnItemClickListener implements AdapterView.OnItemClickListener {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            //권한체크
            if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_DENIED) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 2);
                    return;
                }
            }

            textStatus.setText("connecting");
            Toast.makeText(getApplicationContext(), btArrayAdapter.getItem(position), Toast.LENGTH_SHORT).show();

            final String name = btArrayAdapter.getItem(position); // get name
            final String address = deviceAddressArray.get(position); // get address
            boolean flag = true;

            BluetoothDevice device = btAdapter.getRemoteDevice(address);

            // create & connect socket
            try {
                btSocket = createBluetoothSocket(device);
                btSocket.connect();
            } catch (IOException e) {
                flag = false;
                textStatus.setText("connection failed!");
                e.printStackTrace();
            }

            if (flag) {
                textStatus.setText("connected to " + name);
                connectedThread = new ConnectedThread(btSocket);
                connectedThread.start();


            }

        }
    }

    public void onClickButtonSend1(View view){
        if(connectedThread!=null){ connectedThread.write("1"); }
    }

    public void onClickButtonSend2(View view){
        if(connectedThread!=null){ connectedThread.write("2"); }
    }

    public void onClickButtonSend3(View view){
        if(connectedThread!=null){ connectedThread.write("3"); }
    }

    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
        try {
            final Method m = device.getClass().getMethod("createInsecureRfcommSocketToServiceRecord", UUID.class);
            return (BluetoothSocket) m.invoke(device, BT_MODULE_UUID);
        } catch (Exception e) {
            Log.e(TAG, "Could not create Insecure RFComm Connection", e);
        }
        //권한체크
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_DENIED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 2);
            }
        }
        return device.createRfcommSocketToServiceRecord(BT_MODULE_UUID);
    }

    // https://popcorn16.tistory.com/192

    public void plantOnClick(View view)  {
        Intent intent = new Intent(getApplicationContext(), PlantListActivity.class);
        startActivityForResult(intent, REQUEST_CODE_MENU);
        overridePendingTransition(0, 0);
    }
}
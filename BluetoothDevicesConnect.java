package teamsully.sullypiwas;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;

public class BluetoothDevicesConnect extends BaseActivity
{
    //    private final String DEVICE_ADDRESS="98:D3:36:00:AF:10"; // Pilot Warning System 1 Bluetooth module's MAC address
    private final String DEVICE_ADDRESS="98:D3:36:00:C0:2D"; // Pilot Warning System 3 Bluetooth module's MAC address
    private final UUID PORT_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb"); //Serial Port Service ID

    private static final int ONE_MEGABYTE = 1048576;   //To limit the size of the log file

    private Set<BluetoothDevice> bondedDevices;
    private ArrayList<String> deviceNames;
    private ArrayList<String> macAddresses;
    private ListView bluetoothDeviceListView;
    private BluetoothDeviceListAdapter bluetoothDeviceListAdapter;

    Button disconnectButton;
    boolean stopThread;
    private boolean fullMessageReceived;
    private boolean readingFromBuffer;
    private int warning_bitmap;
    private int previous_warning_bitmap = 0x00;

    private final int START_FLAG = 0xFF;
    private final int END_FLAG = 0xAA;
    private final int PING_RESPONSE_TYPE = 0x11;
    private final int WARNING_STATUS_TYPE = 0x12;

    private int state = 0;
    private int messageType;
    private int numBytes;
    private int checksum;

    int count;
    private int[] received_msg;

    //Register a BroadcastReceiver to listen for any changes in the state of the BluetoothAdapter as a private instance variable in your Activity or in a separate class file
    private final BroadcastReceiver mReceiver = new BroadcastReceiver()    //This changes the text on the editText field in case of any changes in the state of the BluetoothAdapter
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            final String action = intent.getAction();

            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED))
            {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                        BluetoothAdapter.ERROR);
                switch (state)
                {
                    case BluetoothAdapter.STATE_OFF:
                        Toast.makeText(getApplicationContext(),"Bluetooth off.",Toast.LENGTH_LONG).show();
                        recreateActivityCompat(BluetoothDevicesConnect.this);   //Restart the activity
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        Toast.makeText(getApplicationContext(),"Turning Bluetooth off...",Toast.LENGTH_LONG).show();
                        break;
                    case BluetoothAdapter.STATE_ON:
                        Toast.makeText(getApplicationContext(),"Bluetooth on",Toast.LENGTH_LONG).show();
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        Toast.makeText(getApplicationContext(),"Turning Bluetooth on",Toast.LENGTH_LONG).show();
                        break;
                }
            }
        }
    };

    public BluetoothDevicesConnect()
    {
        received_msg = new int[256];
        deviceNames = new ArrayList<String>();
        macAddresses = new ArrayList<String>();
        count = 0;
    }

    // This will use savedInstanceState since the activity is not killed (read activity lifecycle documentation)

    // back button KILLS the activity (removes from memory), so we can't use savedInstanceState
    // use SharedPreferences instead.
    @Override
    public void onBackPressed()
    {
        Log.d("onBackPressed", "called");
        savePreferences();
        super.onBackPressed();
    }

    public void savePreferences()
    {
        SharedPreferences sharedPreferences = getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("STOP_BUTTON_STATE", disconnectButton.isEnabled());
        editor.apply();
    }

    public void loadPreferences()
    {
        SharedPreferences sharedPreferences = getPreferences(MODE_PRIVATE);
        disconnectButton.setEnabled(sharedPreferences.getBoolean("STOP_BUTTON_STATE", false));
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedState)
    {
        super.onRestoreInstanceState(savedState);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_devices_connect);
        disconnectButton = findViewById(R.id.buttonDisconnect);
        TextView bondedDevices = (TextView) findViewById(R.id.bondedDevices);

        // Register for broadcasts on BluetoothAdapter state change
        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(mReceiver, filter);

        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        myToolbar.setTitleTextColor(getResources().getColor(android.R.color.white));
        myToolbar.setTitle("Connect to PIWAS Platform");
        setSupportActionBar(myToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        createBluetoothDeviceListView();

        // TODO: restore ListView of devices (may need to use an SQlite database)
        if(device == null)
        {
            setUiEnabled(false);
        }
        else
        {
            loadPreferences(); // load button states
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId()) {
            case android.R.id.home: // back button
                savePreferences();
                finish();
                return true;
            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    public void createBluetoothDeviceListView()
    {
        bluetoothDeviceListView = (ListView) findViewById(R.id.bluetoothDeviceList);
        bluetoothDeviceListAdapter = new BluetoothDeviceListAdapter(this, deviceNames, macAddresses);
        bluetoothDeviceListView.setAdapter(bluetoothDeviceListAdapter);

        bluetoothDeviceListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.d("onItemClick()", "Clicked on " + deviceNames.get(position));
                for (BluetoothDevice iterator : bondedDevices)   // find the device among the set of bondedDevices
                {
                    // does the iterator device name equal the device name that was clicked in the list?
                    if(iterator.getName().equals(deviceNames.get(position)))
                    {
//                        if(device == null) // if a device isn't already connected...
//                        {
                        device = iterator;
                        break;
//                        }
//                        else // a device is already connected
//                        {
//                            Toast.makeText(getApplicationContext(),"A device is already connected (try reconnecting)", Toast.LENGTH_LONG).show();
//                            return;
//                        }
                    }
                }

                // create alert dialog to see if user wants to REALLY connect to device
                AlertDialog.Builder builder = new AlertDialog.Builder(BluetoothDevicesConnect.this);
                builder.setMessage("Are you sure you want to connect to " + device.getName() + "?");
                builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // User clicked Yes, attempt to connect to device
                        BluetoothConnectTask task = new BluetoothConnectTask();
                        task.execute(); // call async task to offload main thread
                    }
                });
                builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // User clicked No
                        Toast.makeText(getApplicationContext(),"Connection cancelled", Toast.LENGTH_LONG).show();
                    }
                });
                AlertDialog dialog = builder.create();
                dialog.show();
            }
        });
        bluetoothDeviceListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                Log.d("onItemLongClick()", "Long click");
                return true;
            }
        });
    }

    private class BluetoothConnectTask extends AsyncTask<Void, Void, Boolean>
    {
        @Override
        protected Boolean doInBackground(Void... voids) // this method CANNOT work on the UI
        {
            return BTconnect(); // call BTconnect with the device that was selected
        }

        @Override
        protected void onPostExecute(Boolean result) // this method receives the result from doInBackground and can work on the UI
        {
            if(result)
            {
                deviceConnected = true;
                Toast.makeText(getApplicationContext(),"Successfully connected to " + device.getName(), Toast.LENGTH_LONG).show();

                bluetoothDeviceListAdapter.notifyDataSetChanged(); // TODO: indicate that the device is connected
                setUiEnabled(true);
                beginListenForData(); // TODO: is it fine to call this while(1) task from the async task?
            }
            else
            {
                Toast.makeText(getApplicationContext(),"Unable to connect to " + device.getName(), Toast.LENGTH_LONG).show();
                device = null;
            }
        }
    }

    public void setUiEnabled(boolean bool)
    {
        disconnectButton.setEnabled(bool);
    }

    public boolean BTinit()
    {
        boolean found = false;
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();   //Returns a BluetoothAdapter that represents the device's own Bluetooth adapter (the Bluetooth radio)
        if (bluetoothAdapter == null)   // Device doesn't support Bluetooth
        {
            Toast.makeText(getApplicationContext(),"Device doesn't support Bluetooth",Toast.LENGTH_LONG).show();
        }

        if(!bluetoothAdapter.isEnabled())   //Need to ensure that Bluetooth is enabled
        {
            Intent enableAdapter = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableAdapter, 0);   //This call issues a request to enable Bluetooth through the system settings (without stopping your application)
            try
            {
                Thread.sleep(1000);
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }
        }

        bondedDevices = bluetoothAdapter.getBondedDevices();
        if(bondedDevices.isEmpty())
        {
            Toast.makeText(getApplicationContext(),"Please pair the device first", Toast.LENGTH_LONG).show();
            return false;
        }
        else
        {
            for (BluetoothDevice iterator : bondedDevices)   //Querying the set of paired devices to see if the desired device is already known
            {
                Log.d("BTinit()", "Bonded device: " + iterator.getName());

                // store the device info that will be used in BluetoothDeviceListAdapter
                // don't add to the list if it already exists in the list
                if(!macAddresses.contains(iterator.getAddress()))
                {
                    deviceNames.add(iterator.getName());
                    macAddresses.add(iterator.getAddress());
                }
            }
            bluetoothDeviceListAdapter.notifyDataSetChanged(); // update list view with device names
        }
        return true;
    }


    //Initiate a connection with a remote device that is accepting connections on an open server socket
    public boolean BTconnect()
    {
        boolean connected = true;
        try
        {
            socket = device.createRfcommSocketToServiceRecord(PORT_UUID);  //Connect as client to the Bluetooth module and a socket has to be created to handle the outgoing connection
            socket.connect();   //Initiate the connection (blocking call)
        }
        catch (IOException e)
        {
            e.printStackTrace();
            connected = false;
        }
        if(connected)
        {
            try
            {
                outputStream = socket.getOutputStream();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
            try
            {
                inputStream = socket.getInputStream();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
        return connected;
    }

    public void onClickStart(View view)
    {
        Toast.makeText(getApplicationContext(),"Showing bonded devices",Toast.LENGTH_SHORT).show();
        BTinit(); // show list of bonded devices to user
    }

    void beginListenForData()
    {
        final Handler handler = new Handler();
        stopThread = false;
        fullMessageReceived = false;
        readingFromBuffer = false;
        //Since data can be received at any point of time, running a thread to listen for data would be best
        Thread thread  = new Thread(new Runnable()
        {
            public void run()
            {
                int rawByte;
                int dataCount = 0;

                while(!Thread.currentThread().isInterrupted() && !stopThread)
                {
                    try
                    {
                        if(!readingFromBuffer) // write to the buffer only if it's not being read from
                        {
                            // The read function returns -1 if there is nothing more to read from the input stream
                            rawByte = inputStream.read();

                            // TODO: handle all parsing in the handler
                            while(rawByte != -1)
                            {
                                received_msg[count++] = rawByte;

                                switch(state)
                                {
                                    case 0: // find open flag
                                        if(rawByte == START_FLAG)
                                        {
                                            state = 1;
                                        }
                                        else
                                        {
                                            state = 0;
                                            count = 0;
                                        }
                                        break;
                                    case 1: // find message type
                                        messageType = rawByte;
                                        if(messageType == PING_RESPONSE_TYPE)
                                        {
                                            state = 2;
                                        }
                                        else if(messageType == WARNING_STATUS_TYPE)
                                        {
                                            state = 2;
                                        }
                                        else // invalid message type, go back and look for open flag
                                        {
                                            state = 0;
                                            count = 0;
                                        }
                                        break;
                                    case 2: // get num bytes
                                        numBytes = rawByte;
                                        if(numBytes == 0) // if message has no data, skip to the checksum
                                        {
                                            state = 4;
                                        }
                                        else // get data
                                        {
                                            state = 3;
                                        }
                                        break;
                                    case 3: // get data
                                        ++dataCount;
                                        if(dataCount == numBytes)
                                        {
                                            dataCount = 0;
                                            state = 4;
                                        }
                                        else
                                        {
                                            state = 3; // wait for more data
                                        }
                                        break;
                                    case 4: // get checksum
                                        checksum = rawByte;
                                        state = 5;
                                        break;
                                    case 5: // get end flag
                                        if(rawByte == END_FLAG) // full message received
                                        {
                                            Log.d("beginListenForData()", "Valid message structure received");
                                            count = 0;
                                            fullMessageReceived = true;
                                            readingFromBuffer = true; // don't write into buffer while it is being read from
                                        }
                                        state = 0;
                                        count = 0;
                                        break;
                                    default:
                                        break;
                                }

                                if(fullMessageReceived)
                                {
                                    break;
                                }

                                rawByte = inputStream.read();
                            }
                        }

                        //A handler to take actions on the UI depending on whether a message was received
                        handler.post(new Runnable()
                        {
                            public void run()
                            {
                                if(fullMessageReceived)
                                {
                                    Log.d("beginListenForData()", "Full message received");
                                    if (messageType == PING_RESPONSE_TYPE)
                                    {
                                        //Check if message is not corrupted
                                        if (checksum != (received_msg[1] + received_msg[2]))
                                        {
                                            Log.d("beginListenForData()", "Ping message is corrupted");
                                            count = 0;
                                            state = 0; // reset the state machine
                                            fullMessageReceived = false;
                                            readingFromBuffer = false;
                                            return;
                                        }
                                        connectionStatus = true;
                                        messageReceivedInTime = true;
                                        Log.d("beginListenForData()", "Ping message received");
//                                        showNotification("Ping message received", 0);
//                                        writeToLogFile("ping message received.");
                                    }
                                    else if (messageType == WARNING_STATUS_TYPE)
                                    {
                                        //Warning message
                                        //Check if message is not corrupted
                                        if (checksum != (received_msg[1] + received_msg[2] + received_msg[3]))
                                        {
                                            Log.d("beginListenForData()", "Warning message is corrupted");
                                            count = 0;
                                            state = 0; // reset the state machine
                                            fullMessageReceived = false;
                                            readingFromBuffer = false;
                                            return;
                                        }
                                        else
                                        {
                                            warning_bitmap = received_msg[3];
                                            Log.d("Data", "Warning bitmap: " + "0b" + Integer.toBinaryString(warning_bitmap));

                                            for (int j = 0; j < 6; j++)
                                            {
                                                if ((warning_bitmap & (1 << j)) == (1 << j)) // check which bits are set
                                                {
                                                    Log.d("beginListenForData()", "warning " + j + " received");
                                                    if(warnings.getWarningObjectWithHwInput(j) != null) // make sure the warning exists in the system
                                                    {
                                                        if(!warnings.getWarningObjectWithHwInput(j).isActive()) // if the warning isn't already active, alert the pilot immediately
                                                        {
                                                            writeToLogFile("Warning message received: " + warnings.getWarningObjectWithHwInput(j).getWarningMsg() + ".");
                                                            Log.d("Bluetooth Handler", "Warning input " + j + " is active");
                                                            warnings.getWarningObjectWithHwInput(j).setActive(true); //the warning is ACTIVE
                                                            showNotification("Warning input " + j + " is active!", warnings.getWarningObjectWithHwInput(j).getWarningMsg(), j);


                                                            // get the row of the ListView based on where the warning input is positioned in the list
                                                            // e.g., if warning 2 is the first row in the list, do not access index 2 (correct index is 0)
                                                            rowView = warningListView.getChildAt(warnings.getIndexOfWarningObjectInSet(warnings.getWarningObjectWithHwInput(j)));
                                                            ImageView warningImg = rowView.findViewById(R.id.imageView1ID);        //get the image from the row
                                                            warningImg.setColorFilter(Color.rgb(255,0,0));         //set warning image to red
                                                            tts.speakMsg(warnings.getWarningObjectWithHwInput(j).getWarningMsg()); //speak the warning message
                                                        }
                                                    }
                                                }
                                                else // bit is 0, clear warning if needed
                                                {
                                                    if(warnings.getWarningObjectWithHwInput(j) != null) // make sure the warning exists in the system
                                                    {
                                                        if(warnings.getWarningObjectWithHwInput(j).isActive()) // deassert the warning only if it was previously active (this avoids redundantly clearing it)
                                                        {
                                                            Log.d("Bluetooth Handler", "Warning input " + j + " no longer active");
                                                            showNotification("Warning input " + j + " is no longer active!", warnings.getWarningObjectWithHwInput(j).getWarningMsg(), j);
                                                            warnings.getWarningObjectWithHwInput(j).setActive(false); //the warning is not ACTIVE (also reset durationActive to 0)

                                                            // get the row of the ListView based on where the warning input is positioned in the list
                                                            // e.g., if warning 2 is the first row in the list, do not access index 2 (correct index is 0)
                                                            rowView = warningListView.getChildAt(warnings.getIndexOfWarningObjectInSet(warnings.getWarningObjectWithHwInput(j)));   //get the row of the list view
                                                            ImageView warningImg = rowView.findViewById(R.id.imageView1ID); //get the image from the row
                                                            warningImg.setColorFilter(Color.rgb(0,190,0));  //set warning image to green
                                                            tts.speakMsg( "Warning with title " +warnings.getWarningObjectWithHwInput(j).getTitle() + " is now inactive!");    //Say warning is inactive.
                                                        }
                                                    }
                                                }
                                            }
                                            messageReceivedInTime = true;
                                            previous_warning_bitmap = warning_bitmap;
                                        }
                                    }
                                    fullMessageReceived = false;
                                    readingFromBuffer = false;
                                }
                            }
                        });
                    }
                    catch (IOException ex)
                    {
                        stopThread = true;
                    }
                }
            }
        });

        thread.start();
    }

    public void onClickDisconnect(View view) throws IOException
    {
        if(device != null)
        {
            Toast.makeText(getApplicationContext(),"Connection with the hardware platform has been closed", Toast.LENGTH_LONG).show();
        }
        else
        {
            Toast.makeText(getApplicationContext(),"No device currently connected", Toast.LENGTH_LONG).show();
        }
        stopThread = true;
        outputStream.close();
        inputStream.close();
        socket.close();
        setUiEnabled(false);
        deviceConnected = false;
        device = null;
        connectionStatus = false;

    }

    public static final void recreateActivityCompat(final Activity a)
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
        {
            a.recreate();
        }
        else
        {
            final Intent intent = a.getIntent();
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            a.finish();
            a.overridePendingTransition(0, 0);
            a.startActivity(intent);
            a.overridePendingTransition(0, 0);
        }
    }

    @Override
    protected void onDestroy()
    {
        unregisterReceiver(mReceiver);

        super.onDestroy();
//        if(tts != null)
//        {
//            tts.stop();
//            tts.shutdown();
//        }

    }

    //    @RequiresApi(api = Build.VERSION_CODES.N)
    public void writeToLogFile(String statement)
    {
        try
        {
            // Creates a file in the primary external storage space of the current application.

            // If the file does not exist, it is created.
            File logFile = new File(this.getExternalFilesDir(null), "LogFile.txt");
            if (!logFile.exists())
                logFile.createNewFile();

            //Get current date and time
            Date currentDate = new Date();
            TimeZone estTime = TimeZone.getTimeZone("EST");
            SimpleDateFormat dateTime = new SimpleDateFormat ("E MM/dd/yyyy 'at' hh:mm:ss a");
            dateTime.setTimeZone(estTime);
            //Calendar c = Calendar.getInstance();
            //SimpleDateFormat df = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
            //String formattedDate = df.format(c.getTime());

            // Adds a line to the trace file
            int stringLength = statement.getBytes("UTF-8").length;
            String dateTimeString = dateTime.toString();
            int dateTimeLength = dateTimeString.getBytes("UTF-8").length;
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFile, true /*append*/)))
            {
                if (logFile.length() + stringLength + dateTimeLength < ONE_MEGABYTE)
                {
                    writer.write(dateTime.format(currentDate) + ": " + statement + "\n");
                    //writer.write(formattedDate + " : " + statement + "\n");
//                    writer.flush();
                }
                else
                {
                    Toast.makeText(getApplicationContext(),"Log file size exceeded. Log file cleared and overwritten.", Toast.LENGTH_LONG).show();
                    writer.write("");
                    writer.write(dateTime.format(currentDate) + " : " + statement + "\n");
                }
                writer.close();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
        catch (IOException e)
        {
            Log.e("ReadWriteFile", "Unable to write to the LogFile.txt file.");
        }
    }
}

package teamsully.sullypiwas;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.PorterDuffColorFilter;
import android.net.Uri;
import android.os.Bundle;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends BaseActivity
{
    private ArrayList<String> warningNames = new ArrayList<>();
    private ArrayList<String> warningInfo = new ArrayList<>();
    TextView connectionStatusString;
    private final int HARDWARE_STATUS_PERIOD = 5000;
    private boolean wasDisconnected = false;

    private ConstraintLayout mainLayout;
    private PopupWindow pilotInformationPopup;

    static boolean sayDisconnectedOnce = true;
    @SuppressLint("HandlerLeak")
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        Log.d("MainActivity", "onCreate()");

        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); // keep the screen turned on
        warnings = new WarningSet(); //instantiate WarningSet
        Toolbar myToolbar = findViewById(R.id.my_toolbar);
        connectionStatusString = findViewById(R.id.textView2);
        myToolbar.setTitleTextColor(getResources().getColor(android.R.color.white));
        setSupportActionBar(myToolbar);
        Intent intent = new Intent(this, PilotInformation.class);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getApplicationContext(), PilotInformation.class));
            }
        });

        if(tts == null) // only make a new TTS if one doesn't exist
        {
            tts = new TextToSpeechHelper(getApplicationContext()); //text to speech engine
        }

        loadPreferences(); // load warning info from a previous session if it exists

        // Explanation: in WarningListAdapter, a message is sent to this handler when a button in
        // the ListView is pressed. The message is sent with key:value pairs in the form of a Bundle
        // object. The specific warning number can then be extracted from this message.
        snoozeHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                Log.d("snoozeHandler", "msg received");
                Bundle data = msg.getData();
                final String rowNumber = (String) data.get("snooze");
                Log.d("snoozeHandler", "warning " + rowNumber + " has been requested to be snoozed");

                if(warnings.getWarningObjectAtIndex(Integer.parseInt(rowNumber)).isActive())
                {
                    if(warnings.getWarningObjectAtIndex(Integer.parseInt(rowNumber)).isSilenced())
                    {
                        Toast.makeText(getApplicationContext(),"Warning (" +  warnings.getWarningObjectAtIndex(Integer.parseInt(rowNumber)).getTitle() + ") is already snoozed.",Toast.LENGTH_LONG).show();
                    }
                    else
                    {
                        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                        builder.setMessage("Are you sure you want to snooze warning: " + warnings.getWarningObjectAtIndex(Integer.parseInt(rowNumber)).getTitle() + "?");
                        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // User clicked Yes,snooze warning
                                warnings.getWarningObjectAtIndex(Integer.parseInt(rowNumber)).setSilenced(true); //silence the warning's audio warning message
                                rowView = warningListView.getChildAt(Integer.parseInt(rowNumber));
//                                TextView warningName = rowView.findViewById(R.id.warningName);        //get the warning title from the row
//                                warningName.setText(warningName.getText() + " (Snoozed)");
                                ImageView warningImg = rowView.findViewById(R.id.imageView1ID);        //get the warning Image from the row
                                warningImg.setColorFilter(Color.rgb(244,223,66));
                                int snoozePeriodToast = warnings.getWarningObjectAtIndex(Integer.parseInt(rowNumber)).getSnoozePeriod();
                                Toast.makeText(getApplicationContext(), "Warning " + warnings.getWarningObjectAtIndex(Integer.parseInt(rowNumber)).getTitle() + " has been snoozed for " + snoozePeriodToast + " minutes", Toast.LENGTH_LONG).show();
                            }
                        });
                        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // User clicked NO, no action
                            }
                        });
                        AlertDialog dialog = builder.create();
                        dialog.show();
                    }
                }
                else
                {
                    Toast.makeText(getApplicationContext(),"Warning (" +  warnings.getWarningObjectAtIndex(Integer.parseInt(rowNumber)).getTitle() + ") is not active.",Toast.LENGTH_LONG).show();
                }

            }
        };

        connectionStatusString.setText("Not connected to Pilot Warning System");
        connectionStatusString.setTextColor(getResources().getColor(android.R.color.holo_red_dark));

        handler.postDelayed(hardwareStatusRunnable, 5000);  // this MUST be AFTER the tts is initialized
        handler.postDelayed(warningStatusRunnable, 20000);  // speaks the warnings
        handler.postDelayed(unsnoozeWarningRunnable, 1000); // check warning active duration every second
    }

    @Override
    protected void onStart() //called when the MainActivity is resumed from being on another activity
    {
        super.onStart();
        Log.d("onStart()", "onStart() called");
        createWarningListView();
    }

    @Override
    protected void onStop()
    {
        super.onStop();
        Log.d("onStop()", "onStop() called");
        savePreferences();
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        Log.d("onDestroy()", "onDestroy() called");
    }

    @Override
    public void onBackPressed() // don't destroy the task when the back button is clicked
    {
        moveTaskToBack(true);
    }

    public void savePreferences()
    {
        SharedPreferences sharedPreferences = getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        Log.d("savePreferences()", "Saving warning information...");
        Log.d("savePreferences()", "NUM_WARNINGS: " + Integer.toString(warnings.getWarningCount()));
        editor.putBoolean("LOAD_PREVIOUS_SESSION", true); // the next time the app runs, it will know to load the data
        editor.putInt("NUM_WARNINGS", warnings.getWarningCount());
        editor.putString("CONNECTION_STATUS", (String) connectionStatusString.getText());
        for(int i = 0; i < warnings.getWarningCount(); i++)
        {
            editor.putString("WARNING_" + i + "_TITLE", warnings.getWarningObjectAtIndex(i).getTitle());
            editor.putString("WARNING_" + i + "_MESSAGE", warnings.getWarningObjectAtIndex(i).getWarningMsg());
            editor.putInt("WARNING_" + i + "_INPUT", warnings.getWarningObjectAtIndex(i).getHwInput());
            editor.putInt("WARNING_" + i + "_PRIORITY", warnings.getWarningObjectAtIndex(i).getPriority());
            //editor.putBoolean("WARNING_" + i + "_ACTIVE", warnings.getWarningObjectAtIndex(i).isActive());
            editor.putInt("WARNING " + i + "_SNOOZE_PERIOD", warnings.getWarningObjectAtIndex(i).getSnoozePeriod());

            Log.d("savePreferences()", "WARNING_" + i + "_TITLE: " + warnings.getWarningObjectAtIndex(i).getTitle());
            Log.d("savePreferences()", "WARNING_" + i + "_MESSAGE: " + warnings.getWarningObjectAtIndex(i).getWarningMsg());
            Log.d("savePreferences()", "WARNING_" + i + "_INPUT: " + warnings.getWarningObjectAtIndex(i).getHwInput());
            Log.d("savePreferences()", "WARNING_" + i + "_PRIORITY: " + warnings.getWarningObjectAtIndex(i).getPriority());
            Log.d("savePreferences()", "WARNING_" + i + "_ACTIVE: " + warnings.getWarningObjectAtIndex(i).getPriority());
        }
        editor.apply();
    }

    public void loadPreferences()
    {
        SharedPreferences sharedPreferences = getPreferences(MODE_PRIVATE);

        // if we need to load data from the previous session, do so
        if(sharedPreferences.contains("LOAD_PREVIOUS_SESSION"))
        {
            Log.d("loadPreferences()", "Loading warning information from previous session...");
            int numWarnings = sharedPreferences.getInt("NUM_WARNINGS", 0);
            String statusOfConnection = sharedPreferences.getString("CONNECTION_STATUS" , "");
            connectionStatusString.setText(statusOfConnection);
            for(int i = 0; i < numWarnings; i++)
            {
                // read in the data for each warning and add a new warning object to the WarningSet
                String warningTitle = sharedPreferences.getString("WARNING_" + i + "_TITLE", "Warning Title " + Integer.toString(i));
                String warningMessage = sharedPreferences.getString("WARNING_" + i + "_MESSAGE", "Warning Message " + Integer.toString(i));
                int warningInput = sharedPreferences.getInt("WARNING_" + i + "_INPUT", 0);
                int warningPriority = sharedPreferences.getInt("WARNING_" + i + "_PRIORITY", 0);
                //boolean warningActive = sharedPreferences.getBoolean("WARNING_" + i + "_ACTIVE", false);
                int warningSnoozePeriod = sharedPreferences.getInt("WARNING_" + i + "_SNOOZE_PERIOD", 1);
                Warning warning = new Warning(warningTitle, warningMessage, warningPriority, warningInput, warningSnoozePeriod);
                //warning.setActive(warningActive);
                warnings.addWarning(warning);

                Log.d("loadPreferences()", "WARNING_" + i + "_TITLE: " + warningTitle);
                Log.d("loadPreferences()", "WARNING_" + i + "_MESSAGE: " + warningMessage);
                Log.d("loadPreferences()", "WARNING_" + i + "_INPUT: " + warningInput);
                Log.d("loadPreferences()", "WARNING_" + i + "_PRIORITY: " + warningPriority);
                //Log.d("loadPreferences()", "WARNING_" + i + "_ACTIVE: " + warningActive);
            }
        }
        else
        {
            Log.d("loadPreferences", "Not loading preferences");
        }
    }

    private final int interval = HARDWARE_STATUS_PERIOD;
    private Handler handler = new Handler();
    private Runnable hardwareStatusRunnable = new Runnable()
    {
        public void run()
        {
            Log.d("hardwareStatusRunnable", "handler running");
            if(!deviceConnected)  // is PIWAS even paired?
            {
                handler.postDelayed(hardwareStatusRunnable, interval);
                connectionStatusString.setText("Not connected to Pilot Warning System");
                connectionStatusString.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                Log.d("MainActivity", "Device not connected");
                return;
            }

            if(messageReceivedInTime) // gets set in BluetoothDevicesConnect activity
            {
                messageReceivedInTime = false; // reset the flag
                if(connectionStatus == true)
                {
                    if(wasDisconnected) // only play the audible warning if we have reconnected to PIWAS
                    {
                        wasDisconnected = false;
                        Log.d("tts", "speaking");
                        tts.speakMsg("Successfully reconnected to Pilot Warning System");
                        showNotification("Connection reestablished", "Connection with Pilot Warning System reestablished", 0);
                        sayDisconnectedOnce = true;
//                        showNotification("Successfully reconnected to Pilot Warning System", 0);
                    }
                    connectionStatusString.setText("Connected to Pilot Warning System");
                    connectionStatusString.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                }
                else
                {
                    wasDisconnected = true;
                    connectionStatusString.setText("Not connected to Pilot Warning System");
                    connectionStatusString.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                    tts.speakMsg("Connection lost with Pilot Warning System");
//                    showNotification("Connection lost with Pilot Warning System", 0);
                }
            }
            else // message not received within timeout window, assume platform is disconnected
            {
                connectionStatus = false;
                wasDisconnected = true;
                connectionStatusString.setText("Not connected to Pilot Warning System");
                connectionStatusString.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                if(sayDisconnectedOnce)
                {
                    tts.speakMsg("Connection lost with Pilot Warning System");
                    showNotification("Connection Lost", "Connection lost with Pilot Warning System", 0);
                    sayDisconnectedOnce = false;
                }
            }

            handler.postDelayed(hardwareStatusRunnable, interval);
        }
    };

    private Runnable warningStatusRunnable = new Runnable()
    {
        @Override
        public void run()
        {
            Log.d("warningStatusRunnable", "handler running");
            //tts.speakMsg("Entered warning status runnable.");
            if(!deviceConnected)  // is PIWAS even paired?
            {
                handler.postDelayed(warningStatusRunnable, 10000);
                connectionStatusString.setText("Not connected to Pilot Warning System");
                connectionStatusString.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                Log.d("MainActivity", "Device not connected");
                return;
            }

            // loop through each of the warnings in the priority list to speak them (speaks warnings from HIGH to LOW priority)
            for(int i = 0; i < warnings.getWarningCount(); i++)
            {
                int indexOfCurrWarnObj = warnings.getIndexOfWarningObjectInSet(warnings.getWarningObjectInPriorityListAtIndex(i)); //finds the index of warning in the set
                rowView = warningListView.getChildAt(indexOfCurrWarnObj);
                ImageView warningImg = rowView.findViewById(R.id.imageView1ID);
                int hwInput = warnings.getWarningObjectInPriorityListAtIndex(i).getHwInput();

                // if a warning is active and it is not set to snooze currently,
                // queue the warning in the TTS
                if(warnings.getWarningObjectInPriorityListAtIndex(i).isActive() && !warnings.getWarningObjectInPriorityListAtIndex(i).isSilenced())
                {
                    tts.speakMsg(warnings.getWarningObjectInPriorityListAtIndex(i).getWarningMsg());
                    showNotification("Warning input " + hwInput + " is active!", warnings.getWarningObjectInPriorityListAtIndex(i).getWarningMsg(), i);
                }
            }
            handler.postDelayed(warningStatusRunnable, 20000);
        }
    };

    private Runnable unsnoozeWarningRunnable = new Runnable()
    {
        @Override
        public void run()
        {
            Log.d("unsnoozeWarningRunnable", "handler running");
            //tts.speakMsg("Entered snooze warning runnable.");
            if(warnings.isAnyWarningActive()) //if there is an active warning
            {
                for(int i = 0; i < warnings.getWarningCount(); i++)
                {
                    if(warnings.getWarningObjectAtIndex(i).isSilenced()) //if warning is silenced
                    {
                        int currWarnDurationActive = warnings.getWarningObjectAtIndex(i).getDurationActive(); //capture durationActive
                        int currSnoozePeriod = warnings.getWarningObjectAtIndex(i).getSnoozePeriod();   //capture snooze period for the current warning
                        if(currWarnDurationActive + 1 >= currSnoozePeriod*60)         //if warning has been snoozed for the specified number of minutes
                        {
                            warnings.getWarningObjectAtIndex(i).setDurationActive(0); //reset to 0 minutes
                            warnings.getWarningObjectAtIndex(i).setSilenced(false);  //unsnooze warning
                            rowView = warningListView.getChildAt(warnings.getIndexOfWarningObjectInSet(warnings.getWarningObjectAtIndex(i)));
//                            TextView warningName = rowView.findViewById(R.id.warningName);        //get the warning title from the row
//                            warningName.setText(warnings.getWarningObjectAtIndex(i).getTitle());  //Remove (Snoozed) from the title
                            ImageView warningImg = rowView.findViewById(R.id.imageView1ID);        //get the warning Image from the row
                            warningImg.setColorFilter(Color.rgb(255, 0, 0));
                        }
                        else //warning has not been snoozed for the time specified by the user
                        {
                            warnings.getWarningObjectAtIndex(i).setDurationActive(currWarnDurationActive + 1); //increment minute count by 1
                        }
                    }
                }
            }
            handler.postDelayed(unsnoozeWarningRunnable, 1000); //run in 1 seconds
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.action_bar, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        Intent intent;
        switch (item.getItemId()) {
            case R.id.action_add:
                // go to the add new warning activity
                intent = new Intent(this, AddNewWarningActivity.class);
                startActivity(intent);
                return true;
            case R.id.action_log:
                // go to the log activity
                intent = new Intent(this, logFile_Activity.class);
                startActivity(intent);
                return true;
            case R.id.action_bt:
                // go to the BT connect activity
                intent = new Intent(this, BluetoothDevicesConnect.class);
                startActivity(intent);
                return true;
            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);
        }
    }

    public void createWarningListView()
    {
        warningListView = findViewById(R.id.warningList);
        warningNames.clear();
        warningInfo.clear();

        for(int i = 0; i < warnings.getWarningCount(); i++)
        {
            warningNames.add(warnings.getWarningObjectAtIndex(i).getTitle());
            warningInfo.add("Input " + Integer.toString(warnings.getWarningObjectAtIndex(i).getHwInput()));

            if(warnings.getWarningObjectAtIndex(i).isSilenced())
            {
                rowView = warningListView.getChildAt(warnings.getIndexOfWarningObjectInSet(warnings.getWarningObjectAtIndex(i)));
                ImageView warningImg = rowView.findViewById(R.id.imageView1ID);        //get the warning Image from the row
                warningImg.setColorFilter(Color.rgb(244,223,66));
            }
        }

        final WarningListAdapter warningListAdapter = new WarningListAdapter(this, warningNames, warningInfo);
        warningListView.setAdapter(warningListAdapter);
        warningListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.d("onItemClick()", "Clicked on " + warningNames.get(position));
            }
        });
        warningListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                Log.d("onItemLongClick()", "Long click");
                final int indexOfWarningClicked = position;
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setMessage("Are you sure you want to delete warning: " + warningNames.get(position) + "?");
                builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // User clicked Yes, delete the warning from the set, and from the list view
                        if(warnings.getWarningObjectAtIndex(indexOfWarningClicked).isActive())
                        {
                            Toast.makeText(getApplicationContext(),"Cannot delete active warnings.",Toast.LENGTH_LONG).show();
                        }
                        else
                        {
                            warnings.deleteWarning(warnings.getWarningObjectAtIndex(indexOfWarningClicked));
                            warningNames.remove(indexOfWarningClicked);
                            warningInfo.remove(indexOfWarningClicked);
                            warningListAdapter.notifyDataSetChanged();
                            Toast.makeText(getApplicationContext(), "Warning deleted.", Toast.LENGTH_LONG).show();
                        }
                    }
                });
                builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // User clicked NO, no action
                    }
                });
                AlertDialog dialog = builder.create();
                dialog.show();
                return true;
            }
        });
    }
}
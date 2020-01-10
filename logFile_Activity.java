package teamsully.sullypiwas;


import android.content.Intent;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Locale;

public class logFile_Activity extends BaseActivity
{
    private Button showLogFile;
    private Button clearLogFile;
    TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activy_log_file);

        showLogFile = findViewById(R.id.showLog);
        clearLogFile = findViewById(R.id.clearLog);
        textView = findViewById(R.id.history);
        textView.setText("Press SHOW LOG to see warning log!");
        textView.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        textView.setTextSize(24);
        textView.setGravity(Gravity.CENTER);
        Toolbar myToolbar = findViewById(R.id.my_toolbar);
        myToolbar.setTitleTextColor(getResources().getColor(android.R.color.white));
        myToolbar.setTitle("Log File");
        setSupportActionBar(myToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: // back button
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

    public void readFromLogFile(View view)
    {
        String textToDisplay = "";
        TextView FileContentTextView = findViewById(R.id.history);

        // Gets the file from the primary external storage space of the
        // current application.
        File logFile = new File(this.getExternalFilesDir(null), "LogFile.txt");
        if (logFile == null)
        {
            FileContentTextView.setText(textToDisplay);
            return;
        }

        // Reads the data from the file
        BufferedReader reader = null;
        try
        {
            if (logFile.length() == 0)
            {
                FileContentTextView.setText("Press SHOW LOG to see warning log!");
                FileContentTextView.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                FileContentTextView.setTextSize(24);
                Toast.makeText(getApplicationContext(),"Log file is empty.",Toast.LENGTH_LONG).show();
            }
            else
            {
                reader = new BufferedReader(new FileReader(logFile));
                String line;

                while ((line = reader.readLine()) != null)
                {
                    textToDisplay += line.toString();
                    textToDisplay += "\n";
                }
                reader.close();
                // Set the text read from the file to the textview
                FileContentTextView.setText(textToDisplay);
                FileContentTextView.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
                FileContentTextView.setTextSize(18);
                FileContentTextView.setGravity(Gravity.LEFT);
            }
        }
        catch (Exception e)
        {
            Log.e("ReadWriteFile", "Unable to read the LogFile.txt file.");
        }
    }

    public void clearLogFile(View view)
    {
        try
        {
            // Creates a file in the primary external storage space of the
            // current application.
            // If the file does not exists, it is created.
            File logFile = new File(this.getExternalFilesDir(null), "LogFile.txt");
            if (!logFile.exists())
                logFile.createNewFile();

            // Clear the trace file
            BufferedWriter writer = new BufferedWriter(new FileWriter(logFile));
            writer.write("");
            textView.setText("");
            textView.setText("Press SHOW LOG to see warning log!");
            textView.setTextSize(24);
            textView.setGravity((Gravity.CENTER));
            textView.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
            writer.close();
            Toast.makeText(getApplicationContext(),"Log file has been successfully cleared.",Toast.LENGTH_LONG).show();
        }
        catch (IOException e)
        {
            Log.e("ReadWriteFile", "Unable to clear the LogFile.txt file.");
        }

    }
}

package teamsully.sullypiwas;

import android.content.Intent;
import android.graphics.Color;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Locale;

public class AddNewWarningActivity extends BaseActivity
{
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.add_new_warning);

        //Initialize Spinner for priority selection
        String[] prioritySpinArray = new String[] {"Select priority", "High", "Low"};
        String[] snoozeSpinArray = new String[] {"Select snooze duration (minutes)", "1", "5", "10", "15", "20", "25", "30", "35", "40"};
        Spinner snoozeSpinner = findViewById(R.id.snoozeSpinner);
        Spinner prioritySpinner = findViewById(R.id.newWarningPriorityLevel);
        ArrayAdapter<String> priorityAdapter = new ArrayAdapter<>(this,android.R.layout.simple_spinner_item, prioritySpinArray);
        ArrayAdapter<String> snoozeAdapter = new ArrayAdapter<>(this,android.R.layout.simple_spinner_item, snoozeSpinArray);
        priorityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        snoozeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        snoozeSpinner.setAdapter(snoozeAdapter);
        prioritySpinner.setAdapter(priorityAdapter);

        //Initialize Spinner for hardware input selection
        repopulateHwInputSpinner();

        Button cancelButton = findViewById(R.id.button2);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish(); //cancel button goes back to main activity
            }
        });

        Toolbar myToolbar = findViewById(R.id.my_toolbar);
        myToolbar.setTitleTextColor(getResources().getColor(android.R.color.white));
        myToolbar.setTitle("Add New Warning");
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

    public void createWarning(View view)
    {
        boolean formError = false;

        EditText newWarningTitle = findViewById(R.id.newWarningTitle); // grab the name of the warning
        EditText newWarningMsg = findViewById(R.id.newWarningMsg); // grab the name of the warning
        Spinner newWarningPriorityLevel = findViewById(R.id.newWarningPriorityLevel); // grab the priority level
        Spinner newWarningHwInput = findViewById(R.id.newWarningHardwareInput); //grab the hardware input
        Spinner newWarningSnoozePeriod = findViewById(R.id.snoozeSpinner);

        String newTitle = newWarningTitle.getText().toString();
        String newMsg = newWarningMsg.getText().toString();
        String newPriorityLevel = newWarningPriorityLevel.getSelectedItem().toString();
        String newHwInput = newWarningHwInput.getSelectedItem().toString();
        String newSnoozePeriod = newWarningSnoozePeriod.getSelectedItem().toString();

        if(newTitle.isEmpty())
        {
            formError = true;
            newWarningTitle.setError("The warning title cannot be empty.");
        }

        if(newMsg.isEmpty())
        {
            formError = true;
            newWarningMsg.setError("The warning message cannot be empty.");
        }
        if(newMsg.length() > 200)
        {
            formError = true;
            newWarningMsg.setError("The warning message must be less than 200 characters.");
        }
        if(newPriorityLevel.equals("Select priority"))
        {
            formError = true;
            TextView errorText = (TextView)newWarningPriorityLevel.getSelectedView();
            errorText.setError("");
            errorText.setTextColor(Color.RED);//just to highlight that this is an error
            errorText.setText("Select either High or Low priority.");//changes the selected item text to this
            //newWarningPriorityLevel.setError("Select either High or Low priority.");
        }
        if(newHwInput.equals("Select hardware input"))
        {
            formError = true;
            TextView errorText = (TextView)newWarningHwInput.getSelectedView();
            errorText.setError("");
            errorText.setTextColor(Color.RED);//just to highlight that this is an error
            errorText.setText("Select a hardware input.");//changes the selected item text to this
        }
        if(newSnoozePeriod.equals("Select snooze duration (minutes)"))
        {
            formError = true;
            TextView errorText = (TextView)newWarningSnoozePeriod.getSelectedView();
            errorText.setError("");
            errorText.setTextColor(Color.RED);//just to highlight that this is an error
            errorText.setText("Select a snooze duration.");//changes the selected item text to this
        }
        if(formError)
        {
            return;
        }

        //Convert the priority level to either HIGH or LOW (1 or 0)
        int newPriorityLevelValue = (newPriorityLevel.equals("High")) ? Warning.HIGH : Warning.LOW;

        // All fields have been populated, so create a new warning
        Warning newWarning = new Warning(newTitle,
                                        newMsg,
                                        newPriorityLevelValue,
                                        Integer.parseInt(newHwInput),
                                        Integer.parseInt(newSnoozePeriod));

        if(warnings.addWarning(newWarning))
        {
            Toast.makeText(getApplicationContext(), "\"" + newWarning.getTitle() + "\" warning created", Toast.LENGTH_LONG).show();
            repopulateHwInputSpinner();
        }
        else
        {
            Toast.makeText(getApplicationContext(), "Warning not created: limit exceeded", Toast.LENGTH_LONG).show();
        }

        finish(); //go back to the Main Activity
    }

    private void repopulateHwInputSpinner()
    {
        ArrayList<String> hwInputSpinArray = new ArrayList<>();
        hwInputSpinArray.add("Select hardware input");
        for(int i = 0; i < warnings.MAX_WARNING_COUNT; i++)
        {
            if(!warnings.hwInputBitMapEntryIsSet(i)) //if an input is still unoccupied
            {
                hwInputSpinArray.add(Integer.toString(i)); //add input number to Spinner drop down
            }
        }

        //Repopulate Spinner UI
        Spinner hwInputSpinner = findViewById(R.id.newWarningHardwareInput);
        ArrayAdapter<String> hwInputAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, hwInputSpinArray);
        hwInputAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        hwInputSpinner.setAdapter(hwInputAdapter);
    }
}

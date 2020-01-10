package teamsully.sullypiwas;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

import static teamsully.sullypiwas.BaseActivity.snoozeHandler;
import static teamsully.sullypiwas.BaseActivity.warnings;

public class WarningListAdapter extends ArrayAdapter
{
    private final Activity context;
    private final ArrayList<String> names;
    private final ArrayList<String> info;

    private int[] drawableIds = {R.drawable.low_fuel, R.drawable.landing_gear, R.drawable.canopy, R.drawable.generic_warning, R.drawable.generic_warning, R.drawable.generic_warning};
    public WarningListAdapter(Activity context, ArrayList<String> names, ArrayList<String> info)
    {
        super(context,R.layout.warning_list_row , names);
        this.context = context;
        this.names = names;
        this.info = info;
    }

    public View getView(int position, View view, ViewGroup parent)
    {
        LayoutInflater inflater = context.getLayoutInflater();
        View rowView = inflater.inflate(R.layout.warning_list_row, null, true);

        // get references to the objects in the warning_list_row.xml file
        TextView warningName = rowView.findViewById(R.id.warningName);
        TextView warningInfo = rowView.findViewById(R.id.warningInfo);
        ImageView warningImg = rowView.findViewById(R.id.imageView1ID);
        Button snooze = rowView.findViewById(R.id.snooze);
        snooze.setId(position); // associate the button with the row

        snooze.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("setOnClickListener", "Button is in row " + v.getId());
                Bundle bundle = new Bundle();
                bundle.putString("snooze", Integer.toString(v.getId())); // key:value pair
                Message msg = Message.obtain();
                msg.setData(bundle);
                snoozeHandler.sendMessage(msg);
            }
        });

        Log.d("WarningListAdapter", "getView");
        // set the row items
        warningName.setText(names.get(position));
        warningInfo.setText(info.get(position));
        if(warnings.getWarningObjectAtIndex(position).isActive())
        {
            if(warnings.getWarningObjectAtIndex(position).isSilenced())
            {
                warningImg.setColorFilter(Color.rgb(244,223,66)); // warning is snoozed
            }
            else
            {
                warningImg.setColorFilter(Color.rgb(255,0,0)); // warning is active
            }
        }
        else
        {
            warningImg.setColorFilter(Color.rgb(0,190,0));  // warning is inactive
        }

        return rowView;
    }
}
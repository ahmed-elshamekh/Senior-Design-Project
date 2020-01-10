package teamsully.sullypiwas;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

public class BluetoothDeviceListAdapter extends ArrayAdapter
{
    private final Activity context;
    private final ArrayList<String> deviceNames;
    private final ArrayList<String> macAddresses;

    public BluetoothDeviceListAdapter(Activity context, ArrayList<String> deviceNames, ArrayList<String> macAddresses)
    {
        super(context,R.layout.bluetooth_device_list_row , deviceNames);
        this.context = context;
        this.deviceNames = deviceNames;
        this.macAddresses = macAddresses;
    }

    public View getView(int position, View view, ViewGroup parent)
    {
        LayoutInflater inflater = context.getLayoutInflater();
        View rowView = inflater.inflate(R.layout.bluetooth_device_list_row, null, true);

        // get references to the objects in the warning_list_row.xml file
        TextView deviceName = rowView.findViewById(R.id.deviceName);
        TextView macAddress = rowView.findViewById(R.id.macAddress);

        // set the row items
        deviceName.setText(deviceNames.get(position));
        macAddress.setText(macAddresses.get(position));

        return rowView;
    }
}

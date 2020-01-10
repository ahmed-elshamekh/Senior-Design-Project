package teamsully.sullypiwas;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Handler;
import android.os.Message;
import android.speech.tts.TextToSpeech;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import java.io.InputStream;
import java.io.OutputStream;


// this class is used for storing any global data
public class BaseActivity extends AppCompatActivity
{
    protected static String global_test = "Hello test";

    protected static Handler snoozeHandler;
    protected static boolean connectionStatus = false; // to update the connection status in the mainActivity
    protected static boolean messageReceivedInTime = false; // used to determine if messages are received within the timeout window

    protected static boolean deviceConnected = false;
    protected static BluetoothDevice device = null;
    protected static BluetoothSocket socket;
    protected static OutputStream outputStream;
    protected static InputStream inputStream;

    protected static TextToSpeechHelper tts;

    protected static WarningSet warnings;

    protected static ListView warningListView;

    protected static View rowView;

    protected static void stopAudibleWarning(TextToSpeech tts, String text)
    {
        tts.stop();
    }

    protected void showNotification(String title, String msg, int id)
    {
        //Get an instance of NotificationManager//
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this, "1");
        mBuilder.setSmallIcon(R.drawable.ic_warning_white_48dp);
        mBuilder.setContentTitle(title);
        mBuilder.setContentText(msg);
        mBuilder.setDefaults(Notification.DEFAULT_ALL);
        mBuilder.setPriority(NotificationManager.IMPORTANCE_HIGH);

        final Intent notificationIntent = new Intent(this, MainActivity.class); //TODO: test if clicking on the notification creates a new instance of the Main Activity
        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addParentStack(MainActivity.class);   //Requires API level 16 minimum
        stackBuilder.addNextIntent(notificationIntent);
        mBuilder.setContentIntent(pendingIntent);

        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O)
        {
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel notificationChannel = new NotificationChannel("1", "WARNINGS", importance);
            notificationChannel.enableLights(true);
            notificationChannel.setLightColor(Color.RED);
            notificationChannel.enableVibration(true);
            notificationChannel.setVibrationPattern(new long[]{100, 200, 300, 400, 500, 400, 300, 200, 400});
            assert mNotificationManager != null;
            mBuilder.setChannelId("1");
            mNotificationManager.createNotificationChannel(notificationChannel);
        }

        mNotificationManager.notify(id, mBuilder.build());
    }
}
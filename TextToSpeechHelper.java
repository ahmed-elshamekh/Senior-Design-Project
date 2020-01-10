package teamsully.sullypiwas;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import java.util.Locale;

public class TextToSpeechHelper implements TextToSpeech.OnInitListener
{
    private TextToSpeech tts;
    private boolean ready = false;
    private boolean allowed = false;

    public TextToSpeechHelper(Context context)
    {
        tts = new TextToSpeech(context,this);
    }

    public boolean isReady()
    {
        return this.ready;
    }
    public void setReady(boolean ready)
    {
        this.ready = ready;
    }


    public boolean isAllowed()
    {
        return this.allowed;
    }
    public void setAllowed(boolean allowed)
    {
        this.allowed = allowed;
    }

    @Override
    public void onInit(int status)
    {
        if(status == TextToSpeech.SUCCESS)
        {
            tts.setLanguage(Locale.US);
            setReady(true);
            setAllowed(true);
        }
        else if (status == TextToSpeech.ERROR)
        {
            setReady(false);
        }
    }

    public void speakMsg(String text)
    {
        if(isReady() && isAllowed())
        {
            tts.speak(text, TextToSpeech.QUEUE_ADD, null);
        }
    }
}

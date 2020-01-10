package teamsully.sullypiwas;


public class Warning
{
    public static final int HIGH = 1;
    public static final int LOW = 0;

    //attributes
    private String title;
    private int priority;
    private String warningMsg;
    private boolean silenced; //silenced == true means warning's audio message is silenced
    private int hwInput;      //used to identify the warning with the hardware input (TBD)
    private boolean active;   //active == true means warning is active
    private int durationActive; //how long the warning has been active
    private  int snoozePeriod; //For how long the warning is snoozed

    //constructor
    public Warning()
    {
        this.title = "";
        this.priority = LOW;
        this.warningMsg = "";
        this.silenced = false;
        this.hwInput = -1;
        this.active = false;
        this.durationActive = 0;
        this.snoozePeriod = 0;
    }

    public Warning(String title, String warningMsg, int priority, int hwInput, int snoozePeriod)
    {
        this.title = title;
        this.warningMsg = warningMsg;
        this.priority = priority;
        this.silenced = false; //warning audio message starts out not silenced by default
        this.hwInput = hwInput;
        this.active = false;   //warning assumed to be not active upon creation
        this.durationActive = 0;
        this.snoozePeriod = snoozePeriod;
    }

    //methods
    public void setTitle(String title)
    {
        this.title = title;
    }
    public String getTitle()
    {
        return this.title;
    }

    public void setPriority(int priority)
    {
        this.priority = priority;
    }
    public int getPriority()
    {
        return this.priority;
    }

    public void setWarningMsg(String warningMsg)
    {
        this.warningMsg = warningMsg;
    }
    public String getWarningMsg()
    {
        return this.warningMsg;
    }

    public void setSilenced(boolean silenced)
    {
        this.silenced = silenced;
    }
    public boolean isSilenced()
    {
        return this.silenced;
    }

    public void setHwInput(int hwInput)
    {
        this.hwInput = hwInput;
    }
    public int getHwInput()
    {
        return this.hwInput;
    }

    public void setActive(boolean active)
    {
        this.active = active;
        if(!active)
        {
            this.durationActive = 0; //reset duration active
        }
    }
    public boolean isActive()
    {
        return this.active;
    }

    public void setDurationActive(int duration)
    {
        this.durationActive = duration;
    }
    public int getDurationActive()
    {
        return this.durationActive;
    }

    public int getSnoozePeriod()
    {
        return this.snoozePeriod;
    }
}

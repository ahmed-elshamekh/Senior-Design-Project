package teamsully.sullypiwas;

import java.util.ArrayList;
import java.util.List;

public class WarningSet
{
    //attributes
    public final int MAX_WARNING_COUNT = 6;
    private ArrayList<Warning> warningSet;
    private ArrayList<Warning> priorityList;
    private int warningCount;
    private boolean[] hwInputBitMap; //Indexes 0 to MAX_WARNING_COUNT - 1.

    //constructor
    public WarningSet()
    {
        this.warningSet = new ArrayList<Warning>();
        this.priorityList = new ArrayList<Warning>();
        this.hwInputBitMap = new boolean[MAX_WARNING_COUNT + 1];
        for(int i = 0; i < MAX_WARNING_COUNT; i++)
        {
            hwInputBitMap[i] = false; //initialize all to false, no hardware inputs connected yet.
        }
        this.warningCount = 0;
//        warningSet.add(new Warning("Low Fuel", "Warning, you are low on fuel!", Warning.HIGH, 0, 1));
//        hwInputBitMap[0] = true;
//        warningSet.add(new Warning("Landing Gear Not Down", "Warning, landing gear is not deployed!", Warning.HIGH, 1, 1));
//        hwInputBitMap[1] = true;
//        warningSet.add(new Warning("Canopy Is Not Latched", "Warning, the canopy is not latched!", Warning.HIGH, 2, 1));
//        hwInputBitMap[2] = true;
//        this.warningCount = 3;
    }

    //methods
    public boolean addWarning(Warning w)
    {
        if(warningCount >= MAX_WARNING_COUNT) //exceeded the max set capacity
        {
            return false;
        }
        else
        {
            insertInList(warningSet, w);           //insert warning based on hardware input
            addToPriorityList(priorityList, w);    //insert warning to priority list
            hwInputBitMap[w.getHwInput()] = true;  //set bitmap for that hardware input
            warningCount++;                        //increment warning count
            return true;
        }
    }

    private void insertInList(ArrayList<Warning> wSet, Warning w)
    {
        //insert w into the sorted ArrayList using its hardware input number. The ArrayList will
        // still be sorted by increasing hardware input number after the insertion.
        int i = 0;
        while(i < warningCount)
        {
            if (w.getHwInput() < wSet.get(i).getHwInput()) //compare with ith warning in ArrayList
            {
                wSet.add(i, w); //insert warning at index i
                return;
            }
            else  //check the next warning in ArrayList
            {
                i++;
            }
        }
        wSet.add(w); //w's hardware input number is the largest, append w to end of ArrayList
                     //Or, the list is empty, so add the warning to the empty ArrayList.
    }

    private void addToPriorityList(ArrayList<Warning> wSet, Warning w)
    {
        if(w.getPriority() == Warning.HIGH)
        {
            wSet.add(0, w);   //add HIGH priority warning to front of ArrayList
        }
        else
        {
            wSet.add(w);        //add LOW priority warning to back of ArrayList
        }
    }

    public void deleteWarning(Warning w)
    {
        hwInputBitMap[w.getHwInput()] = false; //the input was removed from the hardware
        warningSet.remove(w);                  //remove the Warning object from the ArrayList
        priorityList.remove(w);                //remove the warning from the PriorityList
        warningCount--;                        //decrement warning count
    }

    public int getWarningCount()
    {
        return this.warningCount;
    }

    public boolean hwInputBitMapEntryIsSet(int index)
    {
        return this.hwInputBitMap[index];
    }

    public void setHwInputBitMapEntry(int index, boolean bool)
    {
        this.hwInputBitMap[index] = bool;
    }

    public Warning getWarningObjectAtIndex(int index)
    {
        if(index < this.warningCount)
        {
            return warningSet.get(index);
        }
        else
        {
            return null;
        }
    }

    public Warning getWarningObjectInPriorityListAtIndex(int index)
    {
        if(index < this.warningCount)
        {
            return priorityList.get(index);
        }
        else
        {
            return null;
        }
    }

    public Warning getWarningObjectWithHwInput(int hardwareInput)
    {
        if(hardwareInput < MAX_WARNING_COUNT)
        {
            for(int i = 0; i < warningCount; i++) //linear search the warning set
            {
                if(warningSet.get(i).getHwInput() == hardwareInput)
                {
                    return warningSet.get(i);
                }
            }
            return null;
        }
        else
        {
            return null;
        }
    }

    public int getIndexOfWarningObjectInSet(Warning warning)
    {
        if(warning != null)
        {
            return this.warningSet.indexOf(warning);
        }
        else
        {
            return -1;
        }
    }

    public boolean isAnyWarningActive()
    {
        for(int i = 0; i < warningCount; i++)
        {
            if(warningSet.get(i).isActive())
            {
                return true; //warning i is currently active
            }
        }
        return false; //no warning is currently active
    }
}

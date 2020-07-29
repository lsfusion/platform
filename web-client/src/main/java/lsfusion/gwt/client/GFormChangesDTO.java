package lsfusion.gwt.client;

import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.property.GClassViewType;
import lsfusion.gwt.client.form.property.GPropertyReaderDTO;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

public class GFormChangesDTO implements Serializable {
    public int requestIndex;

    public int[] objectsGroupIds;
    public GGroupObjectValue[] objects;

    public int[] gridObjectsGroupIds;
    public ArrayList<GGroupObjectValue>[] gridObjects;

    public int[] parentObjectsGroupIds;
    public ArrayList<GGroupObjectValue>[] parentObjects;

    public int[] expandablesGroupIds;
    public GGroupObjectValue[][] expandableKeys;
    public Boolean[][] expandableValues;

    public GPropertyReaderDTO[] properties;
    public GGroupObjectValue[][] propertiesValueKeys;
    public Serializable[][] propertiesValueValues;

    public int[] dropPropertiesIds;

    public int[] updateStateObjectsGroupIds;
    public boolean[] updateStateObjectsGroupValues;

    public int[] activateTabsIds;
    public int[] activatePropsIds;
}

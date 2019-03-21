package lsfusion.gwt.shared.form;

import lsfusion.gwt.shared.form.property.GClassViewType;
import lsfusion.gwt.shared.form.object.GGroupObjectValue;
import lsfusion.gwt.shared.form.property.GPropertyReaderDTO;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

public class GFormChangesDTO implements Serializable {
    public int requestIndex;

    public int[] classViewsGroupIds;
    public GClassViewType[] classViews;

    public int[] objectsGroupIds;
    public GGroupObjectValue[] objects;

    public int[] gridObjectsGroupIds;
    public ArrayList<GGroupObjectValue>[] gridObjects;

    public int[] parentObjectsGroupIds;
    public ArrayList<GGroupObjectValue>[] parentObjects;

    public int[] expandablesGroupIds;
    public HashMap<GGroupObjectValue, Boolean>[] expandables;

    public GPropertyReaderDTO[] properties;
    public HashMap<GGroupObjectValue, Object>[] propertiesValues;

    public int[] panelPropertiesIds;
    public int[] dropPropertiesIds;

    public int[] activateTabsIds;
    public int[] activatePropsIds;
}

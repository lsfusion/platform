package platform.gwt.form.shared.view.changes.dto;

import platform.gwt.form.shared.view.GClassViewType;
import platform.gwt.form.shared.view.changes.GGroupObjectValue;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

public class GFormChangesDTO implements Serializable {
    public int[] classViewsGroupIds;
    public GClassViewType[] classViews;

    public int[] objectsGroupIds;
    public GGroupObjectValue[] objects;

    public int[] gridObjectsGroupIds;
    public ArrayList<GGroupObjectValue>[] gridObjects;

    public int[] parentObjectsGroupIds;
    public ArrayList<GGroupObjectValue>[] parentObjects;

    public GPropertyReaderDTO[] properties;
    public HashMap<GGroupObjectValue, Object>[] propertiesValues;

    public int[] panelPropertiesIds;

    public int[] dropPropertiesIds;
}

package lsfusion.gwt.client.controller.remote.action.form;

import lsfusion.gwt.client.form.GUpdateMode;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.property.GPropertyGroupType;

public class ChangeMode extends FormRequestCountingAction<ServerResponseResult> {
    public int groupObjectID;
    
    // group mode
    public boolean setGroup;
    public int[] propertyIDs;
    public GGroupObjectValue[] columnKeys;
    public GPropertyGroupType[] types;

    public Integer pageSize;
    public boolean forceRefresh;
    public GUpdateMode updateMode;

    public ChangeMode() {}

    public ChangeMode(int groupObjectID, boolean setGroup, int[] propertyIDs, GGroupObjectValue[] columnKeys, GPropertyGroupType[] types, Integer pageSize, boolean forceRefresh, GUpdateMode updateMode) {
        this.groupObjectID = groupObjectID;
        this.setGroup = setGroup;
        this.propertyIDs = propertyIDs;
        this.columnKeys = columnKeys;
        this.types = types;
        this.pageSize = pageSize;
        this.forceRefresh = forceRefresh;
        this.updateMode = updateMode;
    }
}

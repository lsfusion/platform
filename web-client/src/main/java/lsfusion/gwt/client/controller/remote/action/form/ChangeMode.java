package lsfusion.gwt.client.controller.remote.action.form;

import lsfusion.gwt.client.form.GUpdateMode;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.grid.view.GGridViewType;
import lsfusion.gwt.client.form.property.GPropertyGroupType;

public class ChangeMode extends FormRequestCountingAction<ServerResponseResult> {
    public int groupObjectID;
    
    // group mode
    public boolean setGroup;
    public int[] propertyIDs;
    public GGroupObjectValue[] columnKeys;
    public int aggrProps;
    public GPropertyGroupType aggrType;

    public Integer pageSize;
    public boolean forceRefresh;
    public GUpdateMode updateMode;

    public GGridViewType viewType;

    public ChangeMode() {}

    public ChangeMode(int groupObjectID, boolean setGroup, int[] propertyIDs, GGroupObjectValue[] columnKeys, int aggrProps, GPropertyGroupType aggrType, Integer pageSize, boolean forceRefresh, GUpdateMode updateMode, GGridViewType viewType) {
        this.groupObjectID = groupObjectID;
        this.setGroup = setGroup;
        this.propertyIDs = propertyIDs;
        this.columnKeys = columnKeys;
        this.aggrProps = aggrProps;
        this.aggrType = aggrType;
        this.pageSize = pageSize;
        this.forceRefresh = forceRefresh;
        this.updateMode = updateMode;
        this.viewType = viewType;
    }
}

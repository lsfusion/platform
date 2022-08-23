package lsfusion.server.language.form.object;

import lsfusion.interop.form.object.table.grid.ListViewType;
import lsfusion.interop.form.property.ClassViewType;
import lsfusion.interop.form.property.PivotOptions;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.logics.LogicsModule;
import lsfusion.server.logics.form.interactive.UpdateType;
import lsfusion.server.logics.form.struct.action.ActionObjectEntity;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;
import lsfusion.server.logics.form.struct.property.PropertyObjectEntity;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.util.List;

public class ScriptingGroupObject {
    public String groupName;
    public List<String> objects;
    public List<String> classes;
    public List<LocalizedString> captions;
    public List<ActionObjectEntity> events;
    public List<String> integrationSIDs;
    public ClassViewType viewType;
    public ListViewType listViewType;
    public PivotOptions pivotOptions;
    public Integer pageSize;
    public UpdateType updateType;
    public String propertyGroupName;

    public String integrationSID;
    public boolean integrationKey;

    public boolean isSubReport;
    public PropertyObjectEntity subReportPath;

    public GroupObjectEntity neighbourGroupObject;
    public LogicsModule.InsertType insertType;

    public ScriptingLogicsModule.FormLPUsage background;
    public ScriptingLogicsModule.FormLPUsage foreground;

    public String customRenderFunction;
    public ScriptingLogicsModule.FormLPUsage customOptions;
    public String mapTileProvider;

    public ScriptingGroupObject(String name, List<String> objects, List<String> classes, List<LocalizedString> captions, List<ActionObjectEntity> events, List<String> integrationSIDs) {
        assert objects.size() == classes.size() && classes.size() == captions.size() && captions.size() == events.size();

        groupName = name;
        this.objects = objects;
        this.classes = classes;
        this.captions = captions;
        this.events = events;
        this.integrationSIDs = integrationSIDs;
    }

    public void setViewType(ClassViewType viewType, ListViewType listViewType) {
        this.viewType = viewType;
        this.listViewType = listViewType;
    }

    public void setPivotOptions(PivotOptions pivotOptions) {
        if(this.pivotOptions != null) {
            this.pivotOptions.merge(pivotOptions);
        } else {
            this.pivotOptions = pivotOptions;
        }
    }

    public void setCustomTypeRenderFunction(String customRenderFunction) {
        this.customRenderFunction = customRenderFunction;
    }

    public void setCustomOptions(ScriptingLogicsModule.FormLPUsage customOptions) {
        this.customOptions = customOptions;
    }

    public void setMapTileProvider(String mapTileProvider) {
        this.mapTileProvider = mapTileProvider;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    public void setUpdateType(UpdateType updateType) {
        this.updateType = updateType; 
    }
    
    public void setNeighbourGroupObject(GroupObjectEntity neighbourGroupObject, LogicsModule.InsertType insertType) {
        this.neighbourGroupObject = neighbourGroupObject;
        this.insertType = insertType;
    }

    public void setBackground(ScriptingLogicsModule.FormLPUsage background) {
        this.background = background;
    }

    public void setForeground(ScriptingLogicsModule.FormLPUsage foreground) {
        this.foreground = foreground;
    }

    public void setPropertyGroupName(String propertyGroupName) {
        this.propertyGroupName = propertyGroupName;
    }

    public void setIntegrationSID(String integrationSID) {
        this.integrationSID = integrationSID;
    }

    public void setIntegrationKey(boolean integrationKey) {
        this.integrationKey = integrationKey;
    }

    public void setSubReport(PropertyObjectEntity subReportPath) {
        this.isSubReport = true;
        this.subReportPath = subReportPath;
    }
}

package lsfusion.server.language.form.object;

import lsfusion.interop.form.object.table.grid.ListViewType;
import lsfusion.interop.form.property.ClassViewType;
import lsfusion.interop.form.property.PivotOptions;
import lsfusion.server.base.version.ComplexLocation;
import lsfusion.server.language.ScriptingLogicsModule;

import lsfusion.server.logics.form.interactive.UpdateType;
import lsfusion.server.logics.form.struct.action.ActionObjectEntity;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.form.struct.property.PropertyObjectEntity;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.util.List;

public class ScriptingGroupObject {
    public List<ObjectEntity> objects;

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

    public ComplexLocation<GroupObjectEntity> location;

    public ScriptingLogicsModule.FormLPUsage background;
    public ScriptingLogicsModule.FormLPUsage foreground;

    public String customRenderFunction;
    public ScriptingLogicsModule.FormLPUsage customOptions;
    public String mapTileProvider;

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
    
    public void setLocation(ComplexLocation<GroupObjectEntity> location) {
        this.location = location;
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

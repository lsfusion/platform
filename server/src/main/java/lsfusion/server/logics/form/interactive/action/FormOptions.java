package lsfusion.server.logics.form.interactive.action;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.interop.form.ShowFormType;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.logics.form.interactive.ManageSessionType;
import lsfusion.server.logics.form.struct.filter.ContextFilterInstance;
import lsfusion.server.logics.form.struct.object.ObjectEntity;

public class FormOptions {
    public Boolean noCancel;
    public ManageSessionType manageSession;
    public ImMap<ObjectEntity, ? extends ObjectValue> mapObjects;
    public ShowFormType type;
    public ImSet<ContextFilterInstance> contextFilters;
    public boolean showReadonly;
    public boolean forbidDuplicate;
    public boolean syncType;
    public String formId;

    public FormOptions(Boolean noCancel, ManageSessionType manageSession, ImMap<ObjectEntity, ? extends ObjectValue> mapObjects, ShowFormType type, ImSet<ContextFilterInstance> contextFilters, boolean showReadonly, boolean forbidDuplicate, boolean syncType, String formId) {
        this.noCancel = noCancel;
        this.manageSession = manageSession;
        this.mapObjects = mapObjects;
        this.type = type;
        this.contextFilters = contextFilters;
        this.showReadonly = showReadonly;
        this.forbidDuplicate = forbidDuplicate;
        this.syncType = syncType;
        this.formId = formId;
    }
}

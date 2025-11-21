package lsfusion.server.logics.form.interactive.action;

import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.interop.form.ShowFormType;
import lsfusion.interop.form.WindowFormType;
import lsfusion.server.logics.form.interactive.ManageSessionType;
import lsfusion.server.logics.form.struct.filter.ContextFilterInstance;

public class FormOptions {
    public Boolean noCancel;
    public ManageSessionType manageSession;
    public ShowFormType type;
    public ImSet<ContextFilterInstance> contextFilters;
    public boolean showReadonly;
    public boolean forbidDuplicate;
    public boolean syncType;
    public String formId;

    public FormOptions(Boolean noCancel, ManageSessionType manageSession, ShowFormType type, ImSet<ContextFilterInstance> contextFilters, boolean showReadonly, boolean forbidDuplicate, boolean syncType, String formId) {
        this.noCancel = noCancel;
        this.manageSession = manageSession;
        this.type = type;
        this.contextFilters = contextFilters;
        this.showReadonly = showReadonly;
        this.forbidDuplicate = forbidDuplicate;
        this.syncType = syncType;
        this.formId = formId;
    }

    public WindowFormType getWindowFormType() {
        return type.getWindowType();
    }
}

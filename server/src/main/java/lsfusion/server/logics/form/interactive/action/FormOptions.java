package lsfusion.server.logics.form.interactive.action;

import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.interop.form.ShowFormType;
import lsfusion.interop.form.WindowFormType;
import lsfusion.server.logics.action.implement.ActionValueImplement;
import lsfusion.server.logics.form.interactive.ManageSessionType;
import lsfusion.server.logics.form.struct.filter.ContextFilterInstance;
import lsfusion.server.logics.form.struct.object.ObjectEntity;

import java.util.HashSet;
import java.util.Set;

public class FormOptions {
    public Boolean noCancel;
    public ManageSessionType manageSession;
    public ShowFormType type;
    public ImSet<ObjectEntity> inputObjects;
    public ImSet<ContextFilterInstance> contextFilters;
    public boolean showReadonly;
    public boolean forbidDuplicate;
    public boolean syncType;
    public boolean showDrop;
    public boolean checkOnOk;
    public String formId;
    public ActionValueImplement<?> initAction;

    public FormOptions(Boolean noCancel, ManageSessionType manageSession, ShowFormType type,
                       ImSet<ObjectEntity> inputObjects, ImSet<ContextFilterInstance> contextFilters,
                       boolean showReadonly, boolean forbidDuplicate, boolean syncType, boolean showDrop, boolean checkOnOk, String formId,
                       ActionValueImplement<?> initAction) {
        this.noCancel = noCancel;
        this.manageSession = manageSession;
        this.type = type;
        this.inputObjects = inputObjects;
        this.contextFilters = contextFilters;
        this.showReadonly = showReadonly;
        this.forbidDuplicate = forbidDuplicate;
        this.syncType = syncType;
        this.showDrop = showDrop;
        this.checkOnOk = checkOnOk;
        this.formId = formId;
        this.initAction = initAction;
    }

    public WindowFormType getWindowFormType() {
        return type.getWindowType();
    }

    public Set<Integer> getInputGroupObjects() {
        Set<Integer> result = new HashSet<>();
        if(inputObjects != null) {
            for (ObjectEntity objectEntity : inputObjects) {
                result.add(objectEntity.groupTo.getID());
            }
        }
        return result;
    }
}

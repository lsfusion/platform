package lsfusion.server.logics.action.flow;

import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.logics.form.struct.FormEntity;

// has changes in this session or other opening form
public class FormChangeFlowType extends ChangeFlowType {
    public final ImSet<FormEntity> recursionGuard;

    public static final FormChangeFlowType INSTANCE = new FormChangeFlowType(SetFact.EMPTY());

    public FormChangeFlowType(ImSet<FormEntity> recursionGuard) {
        this.recursionGuard = recursionGuard;
    }

    public boolean equals(Object o) {
        return this == o || o instanceof FormChangeFlowType && recursionGuard.equals(((FormChangeFlowType) o).recursionGuard);
    }

    public int hashCode() {
        return recursionGuard.hashCode();
    }
}

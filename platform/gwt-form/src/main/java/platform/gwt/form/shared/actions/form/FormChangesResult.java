package platform.gwt.form.shared.actions.form;

import net.customware.gwt.dispatch.shared.Result;
import platform.gwt.view.changes.dto.GFormChangesDTO;

public class FormChangesResult implements Result {
    public GFormChangesDTO changes;

    public FormChangesResult() {
    }

    public FormChangesResult(GFormChangesDTO changes) {
        this.changes = changes;
    }
}
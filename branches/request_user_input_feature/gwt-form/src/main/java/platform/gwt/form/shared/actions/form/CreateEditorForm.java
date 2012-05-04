package platform.gwt.form.shared.actions.form;

import platform.gwt.form.shared.actions.GetFormResult;

public class CreateEditorForm extends FormBoundAction<GetFormResult> {
    public int propertyId;

    public CreateEditorForm() {
    }

    public CreateEditorForm(int propertyId) {
        this.propertyId = propertyId;
    }
}

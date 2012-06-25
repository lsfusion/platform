package platform.gwt.main.shared.actions.form;

import platform.gwt.main.shared.actions.GetFormResult;

//todo: remove
public class CreateEditorForm extends FormBoundAction<GetFormResult> {
    public int propertyId;

    public CreateEditorForm() {
    }

    public CreateEditorForm(int propertyId) {
        this.propertyId = propertyId;
    }
}

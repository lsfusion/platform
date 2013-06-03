package lsfusion.client.descriptor.editor;

import lsfusion.base.context.ApplicationContextProvider;
import lsfusion.client.descriptor.FormDescriptor;
import lsfusion.client.descriptor.GroupObjectDescriptor;
import lsfusion.client.descriptor.increment.editor.IncrementDialogEditor;

public class PropertyObjectEditor extends IncrementDialogEditor {

    protected Object dialogValue(Object currentValue) {
        return new SimplePropertyFilter(form, groupObject).getPropertyObject();
    }

    private final FormDescriptor form;
    private final GroupObjectDescriptor groupObject;

    public PropertyObjectEditor(ApplicationContextProvider object, String field, FormDescriptor form, GroupObjectDescriptor groupObject) {
        super(object, field);

        this.form = form;
        this.groupObject = groupObject;
    }
}

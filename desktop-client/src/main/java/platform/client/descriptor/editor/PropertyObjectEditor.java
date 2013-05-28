package platform.client.descriptor.editor;

import platform.base.context.ApplicationContextProvider;
import platform.client.descriptor.FormDescriptor;
import platform.client.descriptor.GroupObjectDescriptor;
import platform.client.descriptor.increment.editor.IncrementDialogEditor;

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

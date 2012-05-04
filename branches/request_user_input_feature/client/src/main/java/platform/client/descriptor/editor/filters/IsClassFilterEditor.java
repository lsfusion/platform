package platform.client.descriptor.editor.filters;

import platform.client.ClientResourceBundle;
import platform.client.descriptor.FormDescriptor;
import platform.client.descriptor.GroupObjectDescriptor;
import platform.client.descriptor.editor.base.TitledPanel;
import platform.client.descriptor.filter.IsClassFilterDescriptor;
import platform.client.descriptor.increment.editor.IncrementDialogEditor;
import platform.client.form.classes.ClassDialog;
import platform.client.logics.classes.ClientObjectClass;
import platform.client.logics.classes.ClientObjectType;
import platform.client.Main;

public class IsClassFilterEditor extends PropertyFilterEditor {

    public IsClassFilterEditor(GroupObjectDescriptor group, IsClassFilterDescriptor descriptor, FormDescriptor form) {
        super(group, descriptor, form);

        add(new TitledPanel(ClientResourceBundle.getString("descriptor.filters.class"), new IncrementDialogEditor(descriptor, "objectClass") {
            protected Object dialogValue(Object currentValue) {
                return ClassDialog.dialogObjectClass(this, Main.getBaseClass(), (ClientObjectClass) currentValue, false);
            }
        }));
    }
}

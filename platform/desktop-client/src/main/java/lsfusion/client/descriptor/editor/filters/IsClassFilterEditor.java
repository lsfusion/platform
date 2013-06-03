package lsfusion.client.descriptor.editor.filters;

import lsfusion.client.ClientResourceBundle;
import lsfusion.client.descriptor.FormDescriptor;
import lsfusion.client.descriptor.GroupObjectDescriptor;
import lsfusion.client.descriptor.editor.base.TitledPanel;
import lsfusion.client.descriptor.filter.IsClassFilterDescriptor;
import lsfusion.client.descriptor.increment.editor.IncrementDialogEditor;
import lsfusion.client.form.classes.ClassDialog;
import lsfusion.client.logics.classes.ClientObjectClass;
import lsfusion.client.Main;

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

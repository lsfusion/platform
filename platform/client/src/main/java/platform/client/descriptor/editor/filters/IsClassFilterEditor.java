package platform.client.descriptor.editor.filters;

import platform.client.descriptor.FormDescriptor;
import platform.client.descriptor.increment.editor.IncrementDialogEditor;
import platform.client.descriptor.editor.base.TitledPanel;
import platform.client.descriptor.editor.ValueClassEditor;
import platform.client.descriptor.filter.IsClassFilterDescriptor;
import platform.client.form.classes.ClassDialog;
import platform.client.logics.classes.ClientObjectType;
import platform.client.logics.classes.ClientObjectClass;
import platform.interop.serialization.RemoteDescriptorInterface;

public class IsClassFilterEditor extends PropertyFilterEditor {

    public IsClassFilterEditor(IsClassFilterDescriptor descriptor, FormDescriptor form) {
        super(descriptor, form);

        add(new TitledPanel("Класс", new IncrementDialogEditor(descriptor, "objectClass") {
            protected Object dialogValue(Object currentValue) {
                return ClassDialog.dialogObjectClass(this, ClientObjectType.baseClass, (ClientObjectClass) currentValue, false);
            }
        }));
    }
}

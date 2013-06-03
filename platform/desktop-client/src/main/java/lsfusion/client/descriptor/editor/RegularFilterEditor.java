package lsfusion.client.descriptor.editor;

import lsfusion.client.ClientResourceBundle;
import lsfusion.client.descriptor.FormDescriptor;
import lsfusion.client.descriptor.GroupObjectDescriptor;
import lsfusion.client.descriptor.editor.base.TitledPanel;
import lsfusion.client.descriptor.filter.RegularFilterDescriptor;
import lsfusion.client.descriptor.increment.editor.IncrementTextEditor;

public class RegularFilterEditor extends GroupElementEditor {

    public RegularFilterEditor(GroupObjectDescriptor groupObject, RegularFilterDescriptor regularFilter, FormDescriptor form) {
        super(groupObject);

        addTab(ClientResourceBundle.getString("descriptor.editor.common"), new TitledPanel(ClientResourceBundle.getString("descriptor.editor.name"), new IncrementTextEditor(regularFilter, "caption")));
    }
}

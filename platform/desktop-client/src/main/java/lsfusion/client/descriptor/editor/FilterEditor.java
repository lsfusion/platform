package lsfusion.client.descriptor.editor;

import lsfusion.client.ClientResourceBundle;
import lsfusion.client.descriptor.editor.base.NorthBoxPanel;
import lsfusion.client.descriptor.editor.base.TitledPanel;
import lsfusion.client.descriptor.increment.editor.IncrementCheckBox;
import lsfusion.client.logics.ClientFilter;

public class FilterEditor extends ComponentEditor {
    public FilterEditor(ClientFilter component) {
        super(component);

        int index = indexOfTab(ClientResourceBundle.getString("descriptor.editor.display"));

        setComponentAt(index, new NorthBoxPanel(defaultComponentEditor, sizesEditor, designEditor,
                new TitledPanel(null, new IncrementCheckBox(ClientResourceBundle.getString("descriptor.editor.filter.visible"), component, "visible"))
        ));
    }
}

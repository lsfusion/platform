package platform.client.descriptor.editor;

import platform.client.ClientResourceBundle;
import platform.client.descriptor.editor.base.NorthBoxPanel;
import platform.client.descriptor.editor.base.TitledPanel;
import platform.client.descriptor.increment.editor.IncrementCheckBox;
import platform.client.logics.ClientFilter;

public class FilterEditor extends ComponentEditor {
    public FilterEditor(ClientFilter component) {
        super(component);

        int index = indexOfTab(ClientResourceBundle.getString("descriptor.editor.display"));

        setComponentAt(index, new NorthBoxPanel(defaultComponentEditor, sizesEditor, designEditor,
                new TitledPanel(null, new IncrementCheckBox(ClientResourceBundle.getString("descriptor.editor.filter.visible"), component, "visible"))
        ));
    }
}

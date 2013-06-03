package lsfusion.client.descriptor.editor;

import lsfusion.client.ClientResourceBundle;
import lsfusion.client.descriptor.editor.base.NorthBoxPanel;
import lsfusion.client.descriptor.editor.base.TitledPanel;
import lsfusion.client.descriptor.increment.editor.IncrementCheckBox;
import lsfusion.client.logics.ClientToolbar;

public class ToolbarEditor extends ComponentEditor {
    public ToolbarEditor(ClientToolbar component) {
        super(component);

        int index = indexOfTab(ClientResourceBundle.getString("descriptor.editor.display"));

        setComponentAt(index, new NorthBoxPanel(defaultComponentEditor, sizesEditor, designEditor,
                new TitledPanel(null, new IncrementCheckBox(ClientResourceBundle.getString("descriptor.editor.toolbar.visible"), component, "visible")),
                new TitledPanel(null, new IncrementCheckBox(ClientResourceBundle.getString("descriptor.editor.toolbar.showgroupchange"), component, "showGroupChange")),
                new TitledPanel(null, new IncrementCheckBox(ClientResourceBundle.getString("descriptor.editor.toolbar.showcountquantity"), component, "showCountRows")),
                new TitledPanel(null, new IncrementCheckBox(ClientResourceBundle.getString("descriptor.editor.toolbar.showcalculatesum"), component, "showCalculateSum")),
                new TitledPanel(null, new IncrementCheckBox(ClientResourceBundle.getString("descriptor.editor.toolbar.showgroup"), component, "showGroupReport"))
        ));
    }
}

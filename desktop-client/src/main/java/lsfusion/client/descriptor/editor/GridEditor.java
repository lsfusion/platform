package lsfusion.client.descriptor.editor;

import lsfusion.client.ClientResourceBundle;
import lsfusion.client.descriptor.editor.base.NorthBoxPanel;
import lsfusion.client.descriptor.editor.base.TitledPanel;
import lsfusion.client.descriptor.increment.editor.IncrementCheckBox;
import lsfusion.client.logics.ClientGrid;

public class GridEditor extends ComponentEditor {
    public GridEditor(ClientGrid component) {
        super(component);

        int index = indexOfTab(ClientResourceBundle.getString("descriptor.editor.display"));

        setComponentAt(index, new NorthBoxPanel(defaultComponentEditor, sizesEditor, designEditor,
                new TitledPanel(null, new IncrementCheckBox(ClientResourceBundle.getString("descriptor.editor.vertical.tabulation"), component, "tabVertical")),
                new TitledPanel(null, new IncrementCheckBox(ClientResourceBundle.getString("descriptor.editor.autohide"), component, "autoHide"))
        ));
    }
}

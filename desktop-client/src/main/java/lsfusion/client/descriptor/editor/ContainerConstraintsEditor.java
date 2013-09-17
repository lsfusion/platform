package lsfusion.client.descriptor.editor;

import lsfusion.client.descriptor.editor.base.TitledPanel;
import lsfusion.client.descriptor.increment.editor.IncrementIntegerEditor;
import lsfusion.client.logics.ClientContainer;

import static lsfusion.client.ClientResourceBundle.getString;

public class ContainerConstraintsEditor extends ComponentConstraintsEditor {

    public ContainerConstraintsEditor(ClientContainer container) {
        super(container);
        initialize();
    }

    private void initialize() {
        panel.add(new AlignmentEditor(getString("descriptor.editor.location.limit.children.alignment"), "childrenAlignment"));
        panel.add(new TitledPanel(getString("descriptor.editor.location.limit.columns"), new IncrementIntegerEditor(component, "columns")));
    }
}
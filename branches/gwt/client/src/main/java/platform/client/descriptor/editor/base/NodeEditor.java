package platform.client.descriptor.editor.base;

import javax.swing.*;

public interface NodeEditor {
    public JComponent getComponent();
    public boolean validateEditor();
}

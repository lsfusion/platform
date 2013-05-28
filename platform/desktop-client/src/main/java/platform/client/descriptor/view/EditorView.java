package platform.client.descriptor.view;

import platform.client.descriptor.editor.base.NodeEditor;

import javax.swing.*;
import java.awt.*;

public class EditorView extends JPanel {
    private NodeEditor editor;

    public EditorView() {
        super(new BorderLayout());
    }

    public boolean setEditor(NodeEditor iEditor) {
        if (!validateEditor()) {
            return false;
        }

        editor = iEditor;

        removeAll();
        if (editor != null) {
            add(editor.getComponent(), BorderLayout.NORTH);
        }
        validate();
        updateUI();

        return true;
    }

    public boolean validateEditor() {
        return editor == null || editor.validateEditor();
    }

    public void removeEditor() {
        editor = null;
        removeAll();
        validate();
        updateUI();
    }
}

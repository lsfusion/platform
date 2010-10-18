package platform.client.descriptor.view;

import platform.client.descriptor.editor.base.NodeEditor;

import javax.swing.*;

public class EditorView extends JPanel {
    private NodeEditor editor;

    public EditorView() {
    }

    public boolean setEditor(NodeEditor iEditor) {
        if (!validateEditor()) {
            return false;
        }
        
        editor = iEditor;

        removeAll();
        if (editor != null) {
            add(editor.getComponent());
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

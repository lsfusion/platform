package platform.client.descriptor.view;

import platform.client.descriptor.editor.base.NodeEditor;

import javax.swing.*;
import java.awt.*;

public class EditorView extends JPanel {
    private NodeEditor editor;

    public EditorView() {
        setLayout(new BorderLayout());
    }

    public boolean setEditor(NodeEditor iEditor) {
        if (!validateEditor()) {
            return false;
        }
        
        editor = iEditor;

        removeAll();
        if (editor != null) {
            JPanel editorPanel = new JPanel();
            editorPanel.add(editor.getComponent());

            add(new JScrollPane(editorPanel));
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

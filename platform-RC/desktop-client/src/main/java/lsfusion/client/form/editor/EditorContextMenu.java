package lsfusion.client.form.editor;

import lsfusion.client.ClientResourceBundle;

import javax.swing.*;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.JTextComponent;
import java.awt.event.KeyEvent;

public class EditorContextMenu extends JPopupMenu {
    public EditorContextMenu(JTextComponent tc) {
        JMenuItem cutItem = new JMenuItem(new DefaultEditorKit.CutAction());
        cutItem.setText(ClientResourceBundle.getString("form.editor.cut"));
        cutItem.setMnemonic(KeyEvent.VK_X);

        JMenuItem copyItem = new JMenuItem(new DefaultEditorKit.CopyAction());
        copyItem.setText(ClientResourceBundle.getString("form.editor.copy"));
        copyItem.setMnemonic(KeyEvent.VK_C);

        JMenuItem pasteItem = new JMenuItem(new DefaultEditorKit.PasteAction());
        pasteItem.setText(ClientResourceBundle.getString("form.editor.paste"));
        pasteItem.setMnemonic(KeyEvent.VK_V);
        
        if (!tc.isEditable()) {
            cutItem.setEnabled(false);
            pasteItem.setEnabled(false);
        }
        
        if (tc.getSelectedText() == null) {
            cutItem.setEnabled(false);
            copyItem.setEnabled(false);
        }

        add(cutItem);
        add(copyItem);
        add(pasteItem);
    }
}

package lsfusion.client.form.property.cell.classes.controller;

import lsfusion.client.ClientResourceBundle;

import javax.swing.*;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.JTextComponent;
import java.awt.event.KeyEvent;

public class EditorContextMenu extends JPopupMenu {
    public EditorContextMenu(JComponent tc) {
        ActionMap actionMap = tc.getActionMap();

        JMenuItem cutItem = new JMenuItem(actionMap.get("cut-to-clipboard"));
        cutItem.setText(ClientResourceBundle.getString("form.editor.cut"));
        cutItem.setMnemonic(KeyEvent.VK_X);
        add(cutItem);

        JMenuItem copyItem = new JMenuItem(actionMap.get("copy-to-clipboard"));
        copyItem.setText(ClientResourceBundle.getString("form.editor.copy"));
        copyItem.setMnemonic(KeyEvent.VK_C);
        add(copyItem);

        JMenuItem pasteItem = new JMenuItem(actionMap.get("paste-from-clipboard"));
        pasteItem.setText(ClientResourceBundle.getString("form.editor.paste"));
        pasteItem.setMnemonic(KeyEvent.VK_V);
        add(pasteItem);
    }
}

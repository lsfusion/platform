package org.jdesktop.swingx;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeEvent;

// JXTable.editCellAt installs JXTable.CellEditorRemover, which checks the focus owner's ancestors with instanceof java.applet.Applet,
// and java.applet is removed since Java 26 (NoClassDefFoundError on every cell edit), so the same logic without that check is installed instead
public class JXTableCellEditorRemover extends JXTable.CellEditorRemover {
    private final JXTable table;

    private JXTableCellEditorRemover(JXTable table) {
        table.super();
        this.table = table;
    }

    // to be called before JXTable.editCellAt (it creates its own remover only when there is none)
    public static void install(JXTable table) {
        if (table.editorRemover == null)
            table.editorRemover = new JXTableCellEditorRemover(table);
    }

    @Override
    public void propertyChange(PropertyChangeEvent ev) {
        if (ev == null || !"permanentFocusOwner".equals(ev.getPropertyName()) || !table.isEditing() || !table.isTerminateEditOnFocusLost())
            return;

        Component c = focusManager.getPermanentFocusOwner();
        while (c != null) {
            if (c instanceof JPopupMenu) {
                c = ((JPopupMenu) c).getInvoker();
            } else {
                if (c == table) // focus remains inside the table
                    return;
                if (c instanceof Window) {
                    if (c == SwingUtilities.getRoot(table)) {
                        if (!table.getCellEditor().stopCellEditing())
                            table.getCellEditor().cancelCellEditing();
                    }
                    break;
                }
                c = c.getParent();
            }
        }
    }
}

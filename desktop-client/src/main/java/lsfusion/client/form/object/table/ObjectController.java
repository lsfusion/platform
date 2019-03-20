package lsfusion.client.form.object.table;

import lsfusion.client.ClientResourceBundle;
import lsfusion.client.form.controller.ClientFormController;
import lsfusion.client.form.classes.ClassChooserController;
import lsfusion.client.form.classes.ClassChooserView;
import lsfusion.client.form.layout.view.ClientFormLayout;
import lsfusion.client.form.object.ClientObject;
import lsfusion.client.form.filter.user.QueryConditionView;
import lsfusion.client.form.object.table.grid.user.toolbar.ToolbarGridButton;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

public class ObjectController {
    private static final ImageIcon sideExpandIcon = new ImageIcon(QueryConditionView.class.getResource("/images/side_expand.png"));
    private static final ImageIcon sideCollapseIcon = new ImageIcon(QueryConditionView.class.getResource("/images/side_collapse.png"));

    private final ClientObject object;

    // объект, при помощи которого будет происходить общение с внешним миром
    private final ClientFormController form;

    // управление классами
    public final ClassChooserController classChooserController;

    private final JButton toolbarButton;

    public ObjectController(ClientObject iobject, ClientFormController iform) throws IOException {
        object = iobject;
        form = iform;

        classChooserController = new ClassChooserController(object, form);
        toolbarButton = new ToolbarGridButton(sideExpandIcon, ClientResourceBundle.getString("form.tree.show", object.caption)) {
            public void addListener() {
                addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        ClassChooserView chooserView = classChooserController.getClassChooserView();
                        if (!chooserView.isExpanded) {
                            chooserView.expandTree();
                            setIcon(sideCollapseIcon);
                            setToolTipText(ClientResourceBundle.getString("form.tree.hide", object.caption));
                        } else {
                            chooserView.collapseTree();
                            setIcon(sideExpandIcon);
                            setToolTipText(ClientResourceBundle.getString("form.tree.show", object.caption));
                        }
                    }
                });
            }
        };
    }

    public void addView(ClientFormLayout formLayout) {
        classChooserController.addView(formLayout);
    }

    public void setVisible(boolean visible) {
        classChooserController.setVisible(visible);
    }

    public JButton getToolbarButton() {
        return toolbarButton;
    }
}
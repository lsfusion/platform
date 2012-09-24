package platform.client.form;

import platform.client.ClientResourceBundle;
import platform.client.form.classes.ClassChooserController;
import platform.client.form.classes.ClassChooserView;
import platform.client.form.queries.QueryConditionView;
import platform.client.form.queries.ToolbarGridButton;
import platform.client.logics.ClientObject;
import platform.interop.ClassViewType;

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

    private ClassViewType classView = ClassViewType.HIDE;

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
        if (classChooserController.allowedEditObjects()) {
            classChooserController.addView(formLayout);
        }
    }

    public void changeClassView(ClassViewType classView) {
        this.classView = classView;

        if (classView.equals(ClassViewType.GRID)) {
            if (classChooserController != null) {
                classChooserController.showViews();
            }
        } else {
            if (classChooserController != null) {
                classChooserController.hideViews();
            }
        }
    }

    public void hideViews() {
        classChooserController.hideViews();
    }

    public void showViews() {
        if (classView.equals(ClassViewType.GRID)) {
            classChooserController.showViews();
        }
    }

    public JButton getToolbarButton() {
        return toolbarButton;
    }
}
package lsfusion.client.form.classes.view;

import lsfusion.client.ClientResourceBundle;
import lsfusion.client.controller.remote.RmiQueue;
import lsfusion.client.form.controller.ClientFormController;
import lsfusion.client.form.design.view.JComponentPanel;
import lsfusion.client.form.object.ClientObject;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;

public class ClassChooserView extends JComponentPanel {
    public boolean isExpanded;
    private ClientFormController form;

    public ClassChooserView(final ClientFormController form, final ClientObject object) {
        this.form = form;
        // создаем дерево для отображения классов
        ClassTree view = new ClassTree(object.getID(), object.baseClass) {
            protected void currentClassChanged() {
                RmiQueue.runAction(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            form.changeGridClass(object, getSelectionClass());
                        } catch (IOException e) {
                            throw new RuntimeException(ClientResourceBundle.getString("errors.error.changing.current.class"), e);
                        }
                    }
                });

            }
        };

        JScrollPane pane = new JScrollPane(view);
        add(pane, BorderLayout.CENTER);

        // по умолчанию прячем дерево
        collapseTree();
    }

    public void expandTree() {
        isExpanded = true;
        setVisible(true);
    }

    public void collapseTree() {
        isExpanded = false;
        setVisible(false);
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible && isExpanded);
    }
}

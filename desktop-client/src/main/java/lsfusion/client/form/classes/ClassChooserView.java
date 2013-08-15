package lsfusion.client.form.classes;

import lsfusion.client.ClientResourceBundle;
import lsfusion.client.form.ClientFormController;
import lsfusion.client.logics.ClientObject;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;

public class ClassChooserView extends JPanel {
    public boolean isExpanded;
    private ClientFormController form;

    public ClassChooserView(final ClientFormController form, final ClientObject object) {
        this.form = form;
        // создаем дерево для отображения классов
        ClassTree view = new ClassTree(object.getID(), object.baseClass) {
            protected void currentClassChanged() {
                try {
                    form.changeGridClass(object, getSelectionClass());
                } catch (IOException e) {
                    throw new RuntimeException(ClientResourceBundle.getString("errors.error.changing.current.class"), e);
                }
            }
        };

        setLayout(new BorderLayout());

        JScrollPane pane = new JScrollPane(view);
        add(pane, BorderLayout.CENTER);

        // по умолчанию прячем дерево
        collapseTree();
    }

    public void expandTree() {
        isExpanded = true;
        setVisible(true);
        form.dropLayoutCaches();
    }

    public void collapseTree() {
        isExpanded = false;
        setVisible(false);
        form.dropLayoutCaches();
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible && isExpanded);
    }
}

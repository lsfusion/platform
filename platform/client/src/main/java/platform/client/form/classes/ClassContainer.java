package platform.client.form.classes;

import javax.swing.*;
import java.awt.*;

public abstract class ClassContainer extends JPanel {
    public boolean isExpanded;

    public ClassContainer(ClassTree view) {
        setLayout(new BorderLayout());

        JScrollPane pane = new JScrollPane(view);
        add(pane, BorderLayout.CENTER);

        // по умолчанию прячем дерево
        collapseTree();
    }

    public void expandTree() {
        isExpanded = true;
        setVisible(true);
        needToBeRevalidated();
    }

    public void collapseTree() {
        isExpanded = false;
        setVisible(false);
        needToBeRevalidated();
    }

    @Override
    public void setVisible(boolean aFlag) {
        super.setVisible(aFlag && isExpanded);
    }

    protected abstract void needToBeRevalidated();
}

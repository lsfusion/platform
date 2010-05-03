package platform.client.form.classes;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

public abstract class ClassContainer extends JPanel {

    private JScrollPane pane;

    private JButton expandButton;
    private JPanel buttonContainer;

    public ClassContainer(ClassTree view) {

        setLayout(new BorderLayout());

        pane = new JScrollPane(view);
        add(pane, BorderLayout.CENTER);

        JButton collapseButton = new JButton("<<");
        collapseButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                collapseTree();
            }
        });
        collapseButton.setFont(getFont().deriveFont(Font.BOLD));

        expandButton = new JButton(">");
        expandButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                expandTree();
            }
        });
        
        Insets insets = expandButton.getInsets();
        insets.left = 0; insets.right = 0;
        expandButton.setMargin(insets); // экономим на спичках

        expandButton.setFont(getFont().deriveFont(Font.BOLD));
        expandButton.setVisible(false); // по умолчанию невидима

        add(expandButton, BorderLayout.EAST);

        JButton widthDecButton = new JButton("<");
        widthDecButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                widthDecreased();
                invalidate();
                needToBeValidated();
            }
        });
        widthDecButton.setFont(getFont().deriveFont(Font.BOLD));

        JButton widthIncButton = new JButton(">");
        widthIncButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                widthIncreased();
                invalidate();
                needToBeValidated();
            }
        });
        widthIncButton.setFont(getFont().deriveFont(Font.BOLD));

        buttonContainer = new JPanel();
        buttonContainer.setLayout(new BoxLayout(buttonContainer, BoxLayout.X_AXIS));
        buttonContainer.add(collapseButton);
        buttonContainer.add(Box.createHorizontalGlue());
        buttonContainer.add(widthDecButton);
        buttonContainer.add(widthIncButton);

        add(buttonContainer, BorderLayout.SOUTH);

        // по умолчанию показываем дерево свернутым
        collapseTree();
    }

    private Dimension maxSize;

    private void expandTree() {

        expandButton.setVisible(false);

        pane.setVisible(true);
        buttonContainer.setVisible(true);

        setMaximumSize(maxSize);

        needToBeValidated();
    }

    protected void collapseTree() {

        expandButton.setVisible(true);
        pane.setVisible(false);
        buttonContainer.setVisible(false);

        maxSize = getMaximumSize();

        Dimension newMaxSize = new Dimension(maxSize);
        newMaxSize.width = expandButton.getMinimumSize().width;
        setMaximumSize(newMaxSize);

        needToBeValidated();
    }

    protected abstract void needToBeValidated();
    protected abstract void widthDecreased();
    protected abstract void widthIncreased();
}

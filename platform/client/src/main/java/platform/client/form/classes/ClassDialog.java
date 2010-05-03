package platform.client.form.classes;

import platform.client.logics.ClientObjectImplementView;
import platform.client.logics.filter.ClientPropertyFilter;
import platform.client.logics.classes.ClientConcreteClass;
import platform.client.logics.classes.ClientClass;
import platform.client.logics.classes.ClientObjectClass;
import platform.client.SwingUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

public class ClassDialog extends JDialog {

    ClassTree tree;

    public ClassDialog(Component owner, ClientObjectImplementView object, ClientConcreteClass value) {
        super(SwingUtils.getWindow(owner), Dialog.ModalityType.DOCUMENT_MODAL);

        setLayout(new BorderLayout());
        setUndecorated(true);

        tree = new ClassTree(0, object.baseClass) {
            protected void currentClassChanged() {
                okPressed();
            }
        };
        tree.setSelectedClass(value);
        add(tree);

        JPanel buttonContainer = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        JButton okButton = new JButton("OK");
        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                okPressed();
            }
        });

        AbstractAction cancelAction = new AbstractAction("Отмена") {
            public void actionPerformed(ActionEvent ae) {
                ClassDialog.this.setVisible(false);
            }
        };

        JButton cancelButton = new JButton(cancelAction);

        getRootPane().getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "closeDialog");
        getRootPane().getActionMap().put("closeDialog", cancelAction);

        buttonContainer.add(okButton);
        buttonContainer.add(cancelButton);
        add(buttonContainer, BorderLayout.SOUTH);

    }

    private ClientObjectClass chosenClass = null;
    public ClientObjectClass getChosenClass() {
        return chosenClass;
    }

    private void okPressed() {
        
        ClientClass selectedClass = tree.getSelectedClass();
        if (selectedClass instanceof ClientConcreteClass) {
            chosenClass = tree.getSelectedClass();
            ClassDialog.this.setVisible(false);
        }
    }
}

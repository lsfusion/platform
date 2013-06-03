package lsfusion.client.form.classes;

import lsfusion.client.ClientResourceBundle;
import lsfusion.client.SwingUtils;
import lsfusion.client.logics.classes.ClientClass;
import lsfusion.client.logics.classes.ClientObjectClass;
import lsfusion.interop.KeyStrokes;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ClassDialog extends JDialog {

    ClassTree tree;
    boolean concrete;

    public ClassDialog(Component owner, ClientObjectClass baseClass, ClientObjectClass value, boolean concrete) {
        super(SwingUtils.getWindow(owner), Dialog.ModalityType.DOCUMENT_MODAL);

        this.concrete = concrete;
        setLayout(new BorderLayout());

        // делаем, чтобы не выглядел как диалог
        setUndecorated(true);
        getRootPane().setBorder(BorderFactory.createLineBorder(Color.gray, 1));

        setSize(500, 400);

        tree = new ClassTree(0, baseClass) {
            protected void currentClassChanged() {
                okPressed();
            }
        };
        add(new JScrollPane(tree));

        if(value!=null)
            tree.setSelectedClass(value);

        JPanel buttonContainer = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        JButton okButton = new JButton(ClientResourceBundle.getString("descriptor.editor.okbutton"));
        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                okPressed();
            }
        });

        AbstractAction cancelAction = new AbstractAction(ClientResourceBundle.getString("descriptor.editor.cancelbutton")) {
            public void actionPerformed(ActionEvent ae) {
                chosenClass = null;
                ClassDialog.this.setVisible(false);
            }
        };

        JButton cancelButton = new JButton(cancelAction);

        getRootPane().getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStrokes.getEscape(), "closeDialog");
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
        
        ClientClass selectedClass = tree.getSelectionClass();
        if (!concrete || (selectedClass instanceof ClientObjectClass && ((ClientObjectClass) selectedClass).isConcreate())) {
            chosenClass = tree.getSelectionClass();
            ClassDialog.this.setVisible(false);
        }
    }

    public static ClientObjectClass dialogObjectClass(Component owner, ClientObjectClass baseClass, ClientObjectClass value, boolean concrete) {

        ClassDialog dialog = new ClassDialog(owner, baseClass, value, concrete);
        SwingUtils.requestLocation(dialog, MouseInfo.getPointerInfo().getLocation());
        dialog.setVisible(true);
        return dialog.getChosenClass();
    }
}

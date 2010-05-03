package platform.client.form.classes;

import platform.client.logics.ClientObjectImplementView;
import platform.client.logics.classes.ClientObjectClass;
import platform.client.logics.classes.ClientConcreteClass;
import platform.client.form.ClientForm;
import platform.client.form.ClientFormLayout;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import javax.swing.*;
import java.io.IOException;
import java.awt.event.*;

public class ClassController {

    // компоненты для отображения
    private ClassContainer classContainer;
    private ClassTree view;

    // данные по объекту, класс которого обрабатывается
    private final ClientObjectImplementView object;

    // объект, при помощи которого будет происходить общение с внешним миром
    private final ClientForm form;

    public ClassController(ClientObjectImplementView iobject, ClientForm iform) throws IOException {

        this.object = iobject;
        this.form = iform;
    }

    private JButton buttonChangeClass;

    public void addView(ClientFormLayout formLayout) {

        // создаем дерево для отображения классов
        view = new ClassTree(object.getID(), object.baseClass) {

            protected void currentClassChanged() {
                try {
                    form.changeGridClass(object, view.getSelectedClass());
                } catch (IOException e) {
                    throw new RuntimeException("Ошибка при изменении текущего класса", e);
                }
            }
        };

        classContainer = new ClassContainer(view) {

            protected void needToBeValidated() {
                form.validate();
            }

            protected void widthDecreased() {
                object.classView.constraints.fillHorizontal *= 0.95 ;
            }

            protected void widthIncreased() {
                object.classView.constraints.fillHorizontal = 0.95 * object.classView.constraints.fillHorizontal + 0.05;
            }
        };
        classContainer.setVisible(false); // по умолчанию компонент невидим

        // добавляем кнопку для изменения классов
        buttonChangeClass = new JButton("Изменить класс");
        buttonChangeClass.setFocusable(false);
        buttonChangeClass.setVisible(false);
        buttonChangeClass.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ae) {
                changeClass();
            }

        });

        formLayout.add(object.classView, classContainer);
        formLayout.add(object.changeClassView, buttonChangeClass);
    }

    public void showViews() {

        if (object.classView.show) {

            if (classContainer != null)
                classContainer.setVisible(true);

            if (buttonChangeClass != null)
                buttonChangeClass.setVisible(true);
        }
    }

    public void hideViews() {

        if (classContainer != null)
            classContainer.setVisible(false);

        if (buttonChangeClass != null)
            buttonChangeClass.setVisible(false);
    }

    // нужно для того, что если объект типа дата, то для него не будет возможностей добавлять объекты
    public boolean allowedEditObjects() { return object.baseClass instanceof ClientObjectClass; }

    private DefaultMutableTreeNode getSelectedNode() {

        TreePath path = view.getSelectionModel().getLeadSelectionPath();
        if (path == null) return null;

        return (DefaultMutableTreeNode) path.getLastPathComponent();
    }

    public ClientObjectClass getDerivedClass() {

        DefaultMutableTreeNode selNode = getSelectedNode();
        if (selNode == null || !view.getCurrentNode().isNodeChild(selNode)) return (ClientObjectClass) view.getCurrentClass();

        return (ClientObjectClass) selNode.getUserObject();
    }

    public ClientObjectClass getSelectedClass() {

        DefaultMutableTreeNode selNode = getSelectedNode();
        if (selNode == null) return (ClientObjectClass) view.getCurrentClass();

        return (ClientObjectClass) selNode.getUserObject();
    }

    public void changeClass() {

        ClientObjectClass selectedClass = getSelectedClass();

        if (!(selectedClass instanceof ClientConcreteClass)) {
            selectedClass = ClassDialog.dialogConcreteClass(form, object, selectedClass);
            if (selectedClass == null) return;
        }

        try {
            form.changeClass(object, (ClientConcreteClass)selectedClass);
        } catch (IOException e) {
            throw new RuntimeException("Ошибка при изменении класса объекта", e);
        }
    }
}

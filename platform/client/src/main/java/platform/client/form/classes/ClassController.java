package platform.client.form.classes;

import platform.client.logics.ClientObjectImplementView;
import platform.client.logics.classes.ClientClass;
import platform.client.logics.classes.ClientObjectClass;
import platform.client.logics.classes.ClientConcreteClass;
import platform.client.form.ClientForm;
import platform.client.form.ClientFormLayout;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import javax.swing.*;
import java.io.IOException;
import java.awt.event.*;
import java.awt.*;

public class ClassController {

    // базовый класс для объекта
    private ClientClass rootClass;

    // компоненты для отображения
    private JScrollPane pane;
    private ClassTree view;

    // данные по объекту, класс которого обрабатывается
    private final ClientObjectImplementView object;

    // объект, при помощи которого будет происходить общение с внешним миром
    private final ClientForm form;

    public ClassController(ClientObjectImplementView iobject, ClientForm iform) throws IOException {

        this.object = iobject;
        this.form = iform;
        
        // тут теоретически можно оптимизировать, а то получается, что при создании любой формы на каждом объекте идет обращение к серверу
        rootClass = form.getBaseClass(object);

    }

    private JButton buttonChangeClass;

    public void addView(ClientFormLayout formLayout) {

        // создаем дерево для отображения классов
        view = new ClassTree(object.getID(), rootClass) {

            protected void currentClassChanged() throws IOException {
                form.changeGridClass(object, view.getSelectedClass());
            }

            protected java.util.List<ClientClass> getChildClasses(ClientObjectClass parentClass) throws IOException {
                return form.getChildClasses(object, parentClass);
            }
        };
        pane = new JScrollPane(view);
        pane.setVisible(false); // по умолчанию компонент невидим

        // добавляем кнопку для изменения классов
        buttonChangeClass = new JButton("Изменить класс");
        buttonChangeClass.setFocusable(false);
        buttonChangeClass.setVisible(false);
        buttonChangeClass.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ae) {
                ClientObjectClass selectedClass = getSelectedClass();
                if(selectedClass instanceof ClientConcreteClass) {
                    try {
                        form.changeClass(object, (ClientConcreteClass)selectedClass);
                    } catch (IOException e) {
                        throw new RuntimeException("Ошибка при изменении класса объекта", e);
                    }
                }
            }

        });

        formLayout.add(object.classView, pane);
        formLayout.add(object.changeClassView, buttonChangeClass);
    }

    public void showViews() {

        if (rootClass.hasChilds()) {

            if (pane != null)
                pane.setVisible(true);

            if (buttonChangeClass != null)
                buttonChangeClass.setVisible(true);
        }
    }

    public void hideViews() {

        if (pane != null)
            pane.setVisible(false);

        if (buttonChangeClass != null)
            buttonChangeClass.setVisible(false);
    }

    // нужно для того, что если объект типа дата, то для него не будет возможностей добавлять объекты
    public boolean allowedEditObjects() { return rootClass instanceof ClientObjectClass; }

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

}

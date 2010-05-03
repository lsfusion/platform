package platform.client.form;

import platform.client.logics.ClientObjectImplementView;
import platform.client.logics.classes.ClientObjectClass;
import platform.client.logics.classes.ClientConcreteClass;
import platform.client.form.classes.ClassController;
import platform.client.form.classes.ClassDialog;
import platform.interop.ClassViewType;

import javax.swing.*;
import java.io.IOException;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

class ObjectController {

    // объект, при помощи которого будет происходить общение с внешним миром
    private final ClientForm form;

    private final ClientObjectImplementView object;

    // управление классами
    public ClassController classController;

    private JButton buttonAdd;
    private JButton buttonDel;

    public ObjectController(ClientObjectImplementView iobject, ClientForm iform) throws IOException {

        object = iobject;
        form = iform;

        classController = new ClassController(object, form);
    }

    public void addView(ClientFormLayout formLayout) {

        if (classController.allowedEditObjects() && object.objectCellView.show && !form.isReadOnly()) {

            buttonAdd = new JButton("Добавить");
            buttonAdd.setFocusable(false);
            buttonAdd.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    addObject();
                }
            });

            buttonDel = new JButton("Удалить");
            buttonDel.setFocusable(false);
            buttonDel.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent ae) {
                    deleteObject();
                }

            });

            formLayout.add(object.addView, buttonAdd);
            formLayout.add(object.delView, buttonDel);

            classController.addView(formLayout);
        }

    }

    Byte classView = ClassViewType.GRID;
    public void changeClassView(Byte classView) {

        this.classView = classView;

        if (classView.equals(ClassViewType.GRID)) {
            
            if (classController != null)
                classController.showViews();
        } else {

            if (classController != null)
                classController.hideViews();
        }

    }

    public void hideViews() {

        classController.hideViews();

        if (buttonAdd != null)
            buttonAdd.setVisible(false);

        if (buttonDel != null)
            buttonDel.setVisible(false);
    }

    public void showViews() {

        if (classView.equals(ClassViewType.GRID))
            classController.showViews();

        if (buttonAdd != null)
            buttonAdd.setVisible(true);

        if (buttonDel != null)
            buttonDel.setVisible(true);
    }

    public void addObject() {

        ClientObjectClass derivedClass = classController.getDerivedClass();

        if (!(derivedClass instanceof ClientConcreteClass)) {
            derivedClass = ClassDialog.dialogConcreteClass(form, object, derivedClass);
            if (derivedClass == null) return;
        }

        try {
            form.addObject(object, (ClientConcreteClass)derivedClass);
        } catch (IOException e) {
            throw new RuntimeException("Ошибка при добавлении объекта", e);
        }
    }

    public void deleteObject() {

        try {
            form.changeClass(object, null);
        } catch (IOException e) {
            throw new RuntimeException("Ошибка при удалении объекта", e);
        }
    }
}
package platform.client.form;

import platform.client.logics.ClientObjectImplementView;
import platform.client.logics.classes.ClientObjectClass;
import platform.client.logics.classes.ClientConcreteClass;
import platform.client.form.classes.ClassController;

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

    public ObjectController(ClientObjectImplementView iobject, ClientForm iform) throws IOException {

        object = iobject;
        form = iform;

        classController = new ClassController(object, form);
    }

    public void addView(ClientFormLayout formLayout) {

        if (classController.allowedEditObjects() && object.objectIDView.show && !form.isReadOnly()) {

            JButton buttonAdd = new JButton("Добавить");
            buttonAdd.setFocusable(false);
            buttonAdd.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent ae) {
                    ClientObjectClass derivedClass = classController.getDerivedClass();
                    if(derivedClass instanceof ClientConcreteClass) {
                        try {
                            form.addObject(object, (ClientConcreteClass)derivedClass);
                        } catch (IOException e) {
                            throw new RuntimeException("Ошибка при добавлении объекта", e);
                        }
                    }
                }
            });

            JButton buttonDel = new JButton("Удалить");
            buttonDel.setFocusable(false);
            buttonDel.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent ae) {
                    try {
                        form.changeClass(object, null);
                    } catch (IOException e) {
                        throw new RuntimeException("Ошибка при удалении объекта", e);
                    }
                }

            });

            formLayout.add(object.addView, buttonAdd);
            formLayout.add(object.delView, buttonDel);

            classController.addView(formLayout);
        }

    }

    public void changeClassView(Boolean classView) {

        if (classView) {
            if (classController != null)
                classController.showViews();
        } else {

            if (classController != null)
                classController.hideViews();
        }

    }

}
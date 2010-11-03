package platform.client.descriptor.editor;

import platform.client.descriptor.ObjectDescriptor;
import platform.client.descriptor.editor.base.NodeEditor;
import platform.client.descriptor.editor.base.TitledPanel;
import platform.client.descriptor.increment.editor.IncrementCheckBox;
import platform.client.descriptor.increment.editor.IncrementTextEditor;

import javax.swing.*;
import java.awt.*;

public class ObjectEditor extends JPanel implements NodeEditor {

    private final ObjectDescriptor object;

    public ObjectEditor(ObjectDescriptor object) {
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        this.object = object;

        add(new TitledPanel("Заголовок", new IncrementTextEditor(object, "caption")));
        add(Box.createRigidArea(new Dimension(5, 5)));

        add(new TitledPanel(null, new IncrementCheckBox("Добавлять новый объект при транзакции", object, "addOnTransaction")));
        add(Box.createRigidArea(new Dimension(5, 5)));
        add(new TitledPanel(null, new IncrementCheckBox("Показывать объект", object.client.classChooser, "show")));
        add(Box.createRigidArea(new Dimension(5, 5)));

        add(new TitledPanel("Класс", new ValueClassEditor(object, "baseClass")));
    }

    public JComponent getComponent() {
        return this;
    }

    public boolean validateEditor() {
        if (object.getBaseClass() == null) {
            JOptionPane.showMessageDialog(this, "Выберите класс объекта!", "Ошибка", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        return true;
    }
}

package platform.client.descriptor.editor;

import platform.client.descriptor.editor.base.NodeEditor;
import platform.client.descriptor.editor.base.TitledPanel;
import platform.client.descriptor.increment.editor.IncrementTextEditor;
import platform.client.logics.ClientContainer;

import javax.swing.*;

public class ContainerEditor extends JPanel implements NodeEditor {

    public ContainerEditor(ClientContainer descriptor) {

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        add(new TitledPanel("Заголовок", new IncrementTextEditor(descriptor, "title")));
        add(new TitledPanel("Описание", new IncrementTextEditor(descriptor, "description")));
        add(new TitledPanel("Идентификатор", new IncrementTextEditor(descriptor, "sID")));

        TitledPanel arrangement = new TitledPanel("Настройка расположения");
        arrangement.setLayout(new BoxLayout(arrangement, BoxLayout.Y_AXIS));
        arrangement.add(new ComponentIntersectsEditor("Взаимное расположение компонентов", descriptor, "intersects"));
        arrangement.add(new ContainerConstraintsEditor(descriptor, "constraints"));

        add(arrangement);

        add(new ComponentEditor("Компонентные свойства", descriptor));
    }

    public JComponent getComponent() {
        return this;
    }

    public boolean validateEditor() {
        return true;
    }
}

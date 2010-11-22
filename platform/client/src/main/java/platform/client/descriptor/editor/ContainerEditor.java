package platform.client.descriptor.editor;

import platform.client.descriptor.editor.base.NorthBoxPanel;
import platform.client.descriptor.editor.base.NodeEditor;
import platform.client.descriptor.editor.base.TitledPanel;
import platform.client.descriptor.increment.editor.IncrementCheckBox;
import platform.client.descriptor.increment.editor.IncrementTextEditor;
import platform.client.logics.ClientContainer;

import javax.swing.*;
import java.awt.*;


public class ContainerEditor extends JTabbedPane implements NodeEditor {

    public ContainerEditor(ClientContainer descriptor) {

        addTab("Общее", new NorthBoxPanel(new TitledPanel("Заголовок", new IncrementTextEditor(descriptor, "title")),
                new TitledPanel("Описание", new IncrementTextEditor(descriptor, "description")),
                new TitledPanel("Идентификатор", new IncrementTextEditor(descriptor, "sID"))));

        JPanel defaultComponent = new JPanel();
        defaultComponent.setLayout(new FlowLayout(FlowLayout.LEFT));
        defaultComponent.add(new IncrementCheckBox("Компонент по умолчанию", descriptor, "defaultComponent"));

        addTab("Отображение", new NorthBoxPanel(defaultComponent,
                new ComponentDesignEditor("Дизайн", descriptor.design)));
        
        addTab("Расположение", new NorthBoxPanel(new ComponentIntersectsEditor("Взаимное расположение компонентов", descriptor, "intersects"),
                new ContainerConstraintsEditor(descriptor.constraints)));

    }

    public JComponent getComponent() {
        return this;
    }

    public boolean validateEditor() {
        return true;
    }
}

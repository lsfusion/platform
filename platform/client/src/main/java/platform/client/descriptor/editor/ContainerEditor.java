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

        addTab("Отображение", new NorthBoxPanel(
                new TitledPanel(null, new IncrementCheckBox("Панель закладок", descriptor, "tabbedPane")),
                new TitledPanel(null, new IncrementCheckBox("Компонент по умолчанию", descriptor, "defaultComponent")),
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

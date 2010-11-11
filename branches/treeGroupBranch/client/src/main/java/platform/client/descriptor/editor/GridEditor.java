package platform.client.descriptor.editor;

import platform.client.descriptor.editor.base.TitledPanel;
import platform.client.descriptor.increment.editor.IncrementCheckBox;
import platform.client.logics.ClientGrid;

import javax.swing.*;
import java.awt.*;

public class GridEditor extends ComponentEditor {
    public GridEditor(ClientGrid component) {
        super("Таблица", component);

        add(new TitledPanel(null, new IncrementCheckBox("Показывать поиск", component, "showFind")));
        add(Box.createRigidArea(new Dimension(5, 5)));
        add(new TitledPanel(null, new IncrementCheckBox("Показывать фильтр", component, "showFilter")));
        add(Box.createRigidArea(new Dimension(5, 5)));
        add(new TitledPanel(null, new IncrementCheckBox("Вертикальная табуляция", component, "tabVertical")));
        add(Box.createRigidArea(new Dimension(5, 5)));
        add(new TitledPanel(null, new IncrementCheckBox("Автоскрытие", component, "autoHide")));
        add(Box.createRigidArea(new Dimension(5, 5)));

        //todo: minRowCount
    }
}

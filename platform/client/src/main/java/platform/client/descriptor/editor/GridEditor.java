package platform.client.descriptor.editor;

import platform.client.descriptor.editor.base.NorthBoxPanel;
import platform.client.descriptor.editor.base.TitledPanel;
import platform.client.descriptor.increment.editor.IncrementCheckBox;
import platform.client.descriptor.increment.editor.IncrementColorEditor;
import platform.client.logics.ClientGrid;

public class GridEditor extends ComponentEditor {
    public GridEditor(ClientGrid component) {
        super(component);

        int index = indexOfTab("Отображение");

        setComponentAt(index, new NorthBoxPanel(defaultComponentEditor, designEditor,
                new TitledPanel(null, new IncrementCheckBox("Показывать поиск", component, "showFind")),
                new TitledPanel(null, new IncrementCheckBox("Показывать фильтр", component, "showFilter")),
                new TitledPanel(null, new IncrementCheckBox("Вертикальная табуляция", component, "tabVertical")),
                new TitledPanel(null, new IncrementCheckBox("Автоскрытие", component, "autoHide"))
        ));

        //todo: minRowCount
    }
}

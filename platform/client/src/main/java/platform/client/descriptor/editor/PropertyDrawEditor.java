package platform.client.descriptor.editor;

import platform.client.descriptor.FormDescriptor;
import platform.client.descriptor.GroupObjectDescriptor;
import platform.client.descriptor.PropertyDrawDescriptor;
import platform.client.descriptor.PropertyObjectDescriptor;
import platform.client.descriptor.editor.base.NorthBoxPanel;
import platform.client.descriptor.editor.base.TitledPanel;
import platform.client.descriptor.increment.editor.*;
import platform.interop.ClassViewType;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class PropertyDrawEditor extends GroupElementEditor {
    private final PropertyDrawDescriptor descriptor;

    public PropertyDrawEditor(final GroupObjectDescriptor groupObject, final PropertyDrawDescriptor descriptor, final FormDescriptor form) {
        super(groupObject);
        this.descriptor = descriptor;

        TitledPanel captionPanel = new TitledPanel("Стат. заголовок", new IncrementTextEditor(descriptor, "caption"));

        TitledPanel propertyObjectPanel = new TitledPanel("Реализация", new PropertyObjectEditor(descriptor, "propertyObject", form, groupObject));

        TitledPanel groupObjectPanel = new TitledPanel("Группа объектов", new JComboBox(new IncrementSingleListSelectionModel(descriptor, "toDraw", true) {
            public List<?> getSingleList() {
                PropertyObjectDescriptor propertyObject = descriptor.getPropertyObject();
                return propertyObject != null
                        ? propertyObject.getGroupObjects(form.groupObjects)
                        : new ArrayList();
            }

            public void fillListDependencies() {
                form.addDependency(descriptor, "propertyObject", this);
                form.addDependency(form, "groupObjects", this);
            }
        }));

        // columnGroupObjects из списка mapping'ов (полных) !!! без toDraw
        TitledPanel columnGroupObjectsPanel = new TitledPanel("Группы в колонки", new IncrementMultipleListEditor(
                new IncrementMultipleListSelectionModel(descriptor, "columnGroupObjects") {
            public List<?> getList() {
                return descriptor.getUpGroupObjects(form.groupObjects);
            }

            public void fillListDependencies() {
                form.addDependency(descriptor, "propertyObject", this);
                form.addDependency(descriptor, "toDraw", this);
                form.addDependency(form, "groupObjects", this);
            }
        }));

        // propertyCaption из списка columnGroupObjects (+objects без toDraw)
        TitledPanel propertyCaptionPanel = new TitledPanel("Динам. заголовок", new IncrementDialogEditor(descriptor, "propertyCaption") {
            protected Object dialogValue(Object currentValue) {
                return new ListGroupObjectEditor(descriptor.getColumnGroupObjects()).getPropertyObject();
            }
        });

        TitledPanel propertyHighlightPanel = new TitledPanel("Свойство выделения", new IncrementDialogEditor(descriptor, "propertyHighlight") {
            protected Object dialogValue(Object currentValue) {
                return new ListGroupObjectEditor(descriptor.getColumnGroupObjects()).getPropertyObject();
            }
        });

        TitledPanel shouldBeLastPanel = new TitledPanel(null, new IncrementCheckBox("Должно быть последним", descriptor, "shouldBeLast"));
        TitledPanel readOnlyPanel = new TitledPanel(null, new IncrementCheckBox("Только для чтения", descriptor, "readOnly"));
        TitledPanel focusablePanel = new TitledPanel(null, new IncrementTristateCheckBox("Может иметь фокус", descriptor, "focusable"));

        TitledPanel forceTypePanel = new TitledPanel("Тип просмотра", new JComboBox(new IncrementSingleListSelectionModel(descriptor, "forceViewType") {
            public List<?> getSingleList(){
                return ClassViewType.typeNameList();
            }
        }));

        TitledPanel editKeyPanel = new TitledPanel("Клавиши редактирования", new IncrementKeyStrokeEditor(descriptor.client, "editKey"));

        JPanel defaultComponent = new JPanel(new FlowLayout(FlowLayout.LEFT));
        defaultComponent.add(new IncrementCheckBox("Компонент по умолчанию", descriptor.client, "defaultComponent"));

        addTab("Общее", new NorthBoxPanel(captionPanel,
                propertyObjectPanel,
                groupObjectPanel,
                columnGroupObjectsPanel,
                propertyCaptionPanel,
                propertyHighlightPanel,
                shouldBeLastPanel,
                readOnlyPanel,
                focusablePanel,
                forceTypePanel,
                editKeyPanel));

        addTab("Отображение", new NorthBoxPanel(defaultComponent,
                new TitledPanel(null, new IncrementColorEditor("Цвет подсветки", descriptor, "highlightColor")),
                new SizesEditor(descriptor.client),
                new ComponentDesignEditor("Дизайн", descriptor.client.design)));

        addTab("Расположение", new NorthBoxPanel(new ComponentConstraintsEditor(descriptor.client.constraints)));
    }

    @Override
    public boolean validateEditor() {
        if (descriptor.getPropertyObject() == null) {
            JOptionPane.showMessageDialog(this, "Выберите реализацию!", "Ошибка", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        return true;
    }
}

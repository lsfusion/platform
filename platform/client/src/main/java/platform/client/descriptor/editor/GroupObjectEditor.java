package platform.client.descriptor.editor;

import platform.client.descriptor.FormDescriptor;
import platform.client.descriptor.GroupObjectDescriptor;
import platform.client.descriptor.editor.base.NodeEditor;
import platform.client.descriptor.editor.base.TitledPanel;
import platform.client.descriptor.increment.editor.IncrementMultipleListEditor;
import platform.client.descriptor.increment.editor.IncrementMultipleListSelectionModel;
import platform.client.descriptor.increment.editor.IncrementSingleListSelectionModel;
import platform.interop.ClassViewType;

import javax.swing.*;
import java.util.Arrays;
import java.util.List;

public class GroupObjectEditor extends JPanel implements NodeEditor {

    public GroupObjectEditor(final GroupObjectDescriptor group, final FormDescriptor form) {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        add(new TitledPanel("Вид при инициализации", new JComboBox(new IncrementSingleListSelectionModel(group, "initClassView") {
            public List<?> getSingleList() {
                return Arrays.asList(ClassViewType.values());
            }
        })));

        add(new TitledPanel("Запрещённый вид", new IncrementMultipleListEditor(
                new IncrementMultipleListSelectionModel(group, "banClassViewList") {
            public List<?> getList() {
                return Arrays.asList(ClassViewType.values());
            }
        })));

        add(new TitledPanel("Свойство выделения", new PropertyObjectEditor(group, "propertyHighlight", form, group)));

        add(new GroupPropertyObjectEditor(form, group));
        
        add(group.client.grid.getPropertiesEditor());
        add(group.client.showType.getPropertiesEditor());

        add(new TitledPanel("Порядки по умолчанию", new DefaultOrdersEditor(form, group)));
    }

    public JComponent getComponent() {
        return this;
    }

    public boolean validateEditor() {
        return true;
    }
}

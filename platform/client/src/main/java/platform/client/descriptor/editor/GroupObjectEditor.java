package platform.client.descriptor.editor;

import platform.client.descriptor.FormDescriptor;
import platform.client.descriptor.GroupObjectDescriptor;
import platform.client.descriptor.editor.base.NodeEditor;
import platform.client.descriptor.editor.base.TitledPanel;
import platform.client.descriptor.increment.editor.IncrementMultipleListEditor;
import platform.client.descriptor.increment.editor.IncrementMultipleListSelectionModel;
import platform.client.descriptor.increment.editor.IncrementSingleListSelectionModel;
import platform.client.GroupPropertyObjectEditor;
import platform.interop.ClassViewType;

import javax.swing.*;
import java.util.Arrays;
import java.util.List;

public class GroupObjectEditor extends JPanel implements NodeEditor {

    public GroupObjectEditor(final GroupObjectDescriptor descriptor, final FormDescriptor form) {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        add(new TitledPanel("Вид при инициализации", new JComboBox(new IncrementSingleListSelectionModel(descriptor, "initClassView") {
            public List<?> getList() {
                return Arrays.asList(
                        ClassViewType.PANEL,
                        ClassViewType.GRID,
                        ClassViewType.HIDE);
            }
        })));

        add(new TitledPanel("Запрещённый вид", new IncrementMultipleListEditor(new IncrementMultipleListSelectionModel(descriptor, "banClassViewList") {
            public List<?> getList() {
                return Arrays.asList(
                        ClassViewType.PANEL,
                        ClassViewType.GRID,
                        ClassViewType.HIDE);
            }
        })));

        add(new TitledPanel("Свойство выделения", new PropertyObjectEditor(descriptor, "propertyHighlight", form, descriptor)));

        add(descriptor.client.grid.getPropertiesEditor());
        add(descriptor.client.showType.getPropertiesEditor());

        add(new GroupPropertyObjectEditor(form, descriptor));
        
    }

    public JComponent getComponent() {
        return this;
    }

    public boolean validateEditor() {
        return true;
    }
}

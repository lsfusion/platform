package platform.client.descriptor.editor;

import platform.base.context.ApplicationContext;
import platform.base.context.ApplicationContextProvider;
import platform.base.context.IncrementView;
import platform.client.descriptor.FormDescriptor;
import platform.client.descriptor.GroupObjectDescriptor;
import platform.client.descriptor.ObjectDescriptor;
import platform.client.descriptor.PropertyObjectDescriptor;
import platform.client.descriptor.editor.base.NamedContainer;
import platform.client.descriptor.editor.base.NodeEditor;
import platform.client.descriptor.editor.base.TitledPanel;
import platform.client.descriptor.increment.editor.IncrementMultipleListEditor;
import platform.client.descriptor.increment.editor.IncrementMultipleListSelectionModel;
import platform.client.descriptor.increment.editor.IncrementSingleListSelectionModel;
import platform.interop.ClassViewType;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class GroupObjectEditor extends JPanel implements NodeEditor {
    private final GroupObjectDescriptor group;
    private final FormDescriptor form;

    public GroupObjectEditor(final GroupObjectDescriptor group, final FormDescriptor form) {
        this.group = group;
        this.form = form;
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

        if (group.getParent() != null) {
            add(new IsParentEditor());
        }
    }

    public class IsParentEditor extends TitledPanel implements IncrementView {
        public IsParentEditor() {
            super("Свойства задания родительского узла");
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

            form.addDependency(group, "objects", this);
        }

        public void update(Object updateObject, String updateField) {
            removeAll();
            for (ObjectDescriptor object : group.objects) {
                IsParentElementEditor editor = new IsParentElementEditor(object, group.getIsParent());
                add(editor);
            }
            revalidate();
        }
    }

    public class IsParentElementEditor extends JPanel implements ApplicationContextProvider {
        private final ObjectDescriptor object;
        private final Map<ObjectDescriptor, PropertyObjectDescriptor> isParent;

        public IsParentElementEditor(ObjectDescriptor object, Map<ObjectDescriptor, PropertyObjectDescriptor> isParent) {
            super(new BorderLayout());

            this.object = object;
            this.isParent = isParent;

            JPanel panel = new NamedContainer(object.toString() + ": ", false, new PropertyObjectEditor(this, "isParentProperty", form, group));

            add(panel);
        }

        public void setIsParentProperty(PropertyObjectDescriptor isParentProperty) {
            isParent.put(object, isParentProperty);
            getContext().updateDependency(this, "isParentProperty");
        }

        public PropertyObjectDescriptor getIsParentProperty() {
            return isParent.get(object);
        }

        public ApplicationContext getContext() {
            return form.getContext();
        }
    }

    public JComponent getComponent() {
        return this;
    }

    public boolean validateEditor() {
        return true;
    }
}

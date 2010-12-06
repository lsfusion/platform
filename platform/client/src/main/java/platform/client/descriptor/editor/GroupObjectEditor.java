package platform.client.descriptor.editor;

import platform.base.context.ApplicationContext;
import platform.base.context.ApplicationContextProvider;
import platform.base.context.IncrementView;
import platform.client.code.CodeGenerator;
import platform.client.descriptor.FormDescriptor;
import platform.client.descriptor.GroupObjectDescriptor;
import platform.client.descriptor.ObjectDescriptor;
import platform.client.descriptor.PropertyObjectDescriptor;
import platform.client.descriptor.editor.base.NamedContainer;
import platform.client.descriptor.editor.base.NodeEditor;
import platform.client.descriptor.editor.base.NorthBoxPanel;
import platform.client.descriptor.editor.base.TitledPanel;
import platform.client.descriptor.increment.editor.*;
import platform.interop.ClassViewType;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class GroupObjectEditor extends JTabbedPane implements NodeEditor {
    private final GroupObjectDescriptor group;
    private final FormDescriptor form;
    
    public GroupObjectEditor(final GroupObjectDescriptor group, final FormDescriptor form) {
        this.group = group;
        this.form = form;

        TitledPanel initClassViewPanel = new TitledPanel("Вид при инициализации", new JComboBox(new IncrementSingleListSelectionModel(group, "initClassView") {
            public List<?> getSingleList() {
                return Arrays.asList(ClassViewType.values());
            }
        }));

        TitledPanel banClassViewPanel =new TitledPanel("Запрещённый вид", new IncrementMultipleListEditor(
                new IncrementMultipleListSelectionModel(group, "banClassViewList") {
                    public List<?> getList() {
                        return Arrays.asList(ClassViewType.values());
                    }
                }));

        TitledPanel propertyHighlightPanel = new TitledPanel("Свойство выделения", new PropertyObjectEditor(group, "propertyHighlight", form, group));

        TitledPanel pageSizePanel = new TitledPanel("Размер страницы", new IncrementTextEditor(group, "pageSize"));

        GroupPropertyObjectEditor groupPropertyObjectPanel = new GroupPropertyObjectEditor(form, group);

        JTabbedPane propertiesPanel = new JTabbedPane();
        propertiesPanel.addTab("Таблица", new NorthBoxPanel(group.client.grid.getPropertiesEditor()));
        propertiesPanel.addTab("Выбор вида", new NorthBoxPanel(group.client.showType.getPropertiesEditor()));

        DefaultOrdersEditor defaultOrdersPanel = new DefaultOrdersEditor(form, group);
        addTab("Код", CodeGenerator.getComponent(form));
        if (group.getParent() != null){
            addTab("Общее", new NorthBoxPanel(initClassViewPanel, banClassViewPanel, propertyHighlightPanel, pageSizePanel, new IsParentEditor()));
        }
        else{
            addTab("Общее", new NorthBoxPanel(initClassViewPanel, banClassViewPanel, propertyHighlightPanel, pageSizePanel));
        }
        addTab("Свойства", new NorthBoxPanel(groupPropertyObjectPanel));
        addTab("Отображение", new NorthBoxPanel(new TitledPanel(null, new IncrementColorEditor("Цвет подсветки", group, "highlightColor")), propertiesPanel));
        addTab("Порядки по умолчанию", new NorthBoxPanel(defaultOrdersPanel));

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

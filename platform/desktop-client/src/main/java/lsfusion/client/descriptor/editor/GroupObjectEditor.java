package lsfusion.client.descriptor.editor;

import lsfusion.base.context.ApplicationContext;
import lsfusion.base.context.ApplicationContextProvider;
import lsfusion.base.context.IncrementView;
import lsfusion.client.ClientResourceBundle;
import lsfusion.client.descriptor.FormDescriptor;
import lsfusion.client.descriptor.GroupObjectDescriptor;
import lsfusion.client.descriptor.ObjectDescriptor;
import lsfusion.client.descriptor.PropertyObjectDescriptor;
import lsfusion.client.descriptor.editor.base.NamedContainer;
import lsfusion.client.descriptor.editor.base.NodeEditor;
import lsfusion.client.descriptor.editor.base.NorthBoxPanel;
import lsfusion.client.descriptor.editor.base.TitledPanel;
import lsfusion.client.descriptor.increment.editor.*;
import lsfusion.interop.ClassViewType;

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

        TitledPanel initClassViewPanel = new TitledPanel(ClientResourceBundle.getString("descriptor.editor.view.initialization.view"), new JComboBox(new IncrementSingleListSelectionModel(group, "initClassView") {
            public List<?> getSingleList() {
                return Arrays.asList(ClassViewType.values());
            }
        }));

        TitledPanel banClassViewPanel =new TitledPanel(ClientResourceBundle.getString("descriptor.editor.view.prohibited.view"), new IncrementMultipleListEditor(
                new IncrementMultipleListSelectionModel(group, "banClassViewList") {
                    public List<?> getList() {
                        return Arrays.asList(ClassViewType.values());
                    }
                }));

        TitledPanel filterPropertyPanel = new TitledPanel(ClientResourceBundle.getString("descriptor.editor.view.default.view.for.filter"), new JComboBox(new IncrementSingleListSelectionModel(group, "filterProperty", true) {
            public List<?> getSingleList() {
                return form.getGroupPropertyDraws(group);
            }

            @Override
            public void fillListDependencies() {
                form.addDependency(form, "propertyDraws", this);
            }
        }));

        TitledPanel propertyBackgroundPanel = new TitledPanel(ClientResourceBundle.getString("descriptor.editor.object.editor.selection.property.background"), new PropertyObjectEditor(group, "propertyBackground", form, group));

        TitledPanel propertyForegroundPanel = new TitledPanel(ClientResourceBundle.getString("descriptor.editor.object.editor.selection.property.foreground"), new PropertyObjectEditor(group, "propertyForeground", form, group));

        TitledPanel pageSizePanel = new TitledPanel(ClientResourceBundle.getString("descriptor.editor.view.selection.pagesize"), new IncrementTextEditor(group, "pageSize"));

        GroupPropertyObjectEditor groupPropertyObjectPanel = new GroupPropertyObjectEditor(form, group);

        JTabbedPane propertiesPanel = new JTabbedPane();
        propertiesPanel.addTab(ClientResourceBundle.getString("descriptor.editor.view.grid"), new NorthBoxPanel(group.client.grid.getPropertiesEditor()));
        propertiesPanel.addTab(ClientResourceBundle.getString("descriptor.editor.view.toolbar"), new NorthBoxPanel(group.client.toolbar.getPropertiesEditor()));
        propertiesPanel.addTab(ClientResourceBundle.getString("descriptor.editor.view.filter"), new NorthBoxPanel(group.client.filter.getPropertiesEditor()));
        propertiesPanel.addTab(ClientResourceBundle.getString("descriptor.editor.view.selection"), new NorthBoxPanel(group.client.showType.getPropertiesEditor()));

        DefaultOrdersEditor defaultOrdersPanel = new DefaultOrdersEditor(form, group);

        if (group.getParent() != null){
            addTab(ClientResourceBundle.getString("descriptor.editor.view.common"), new NorthBoxPanel(initClassViewPanel, banClassViewPanel, propertyBackgroundPanel, propertyForegroundPanel, filterPropertyPanel, pageSizePanel, new IsParentEditor()));
        }
        else{
            addTab(ClientResourceBundle.getString("descriptor.editor.view.common"), new NorthBoxPanel(initClassViewPanel, banClassViewPanel, propertyBackgroundPanel, propertyForegroundPanel, filterPropertyPanel, pageSizePanel));
        }
        addTab(ClientResourceBundle.getString("descriptor.properties"), new NorthBoxPanel(groupPropertyObjectPanel));
        addTab(ClientResourceBundle.getString("descriptor.editor.view.display"), new NorthBoxPanel(propertiesPanel));
        addTab(ClientResourceBundle.getString("descriptor.editor.view.order.by.default"), new NorthBoxPanel(defaultOrdersPanel));

    }                                        

    public class IsParentEditor extends TitledPanel implements IncrementView {
        public IsParentEditor() {
            super(ClientResourceBundle.getString("descriptor.editor.view.parent.node.set.properties"));
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

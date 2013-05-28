package platform.client.descriptor.editor;

import platform.client.ClientResourceBundle;
import platform.client.descriptor.FormDescriptor;
import platform.client.descriptor.GroupObjectDescriptor;
import platform.client.descriptor.PropertyDrawDescriptor;
import platform.client.descriptor.PropertyObjectDescriptor;
import platform.client.descriptor.editor.base.NorthBoxPanel;
import platform.client.descriptor.editor.base.TitledPanel;
import platform.client.descriptor.increment.editor.*;
import platform.interop.ClassViewType;
import platform.interop.PropertyEditType;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class PropertyDrawEditor extends GroupElementEditor {
    private final PropertyDrawDescriptor descriptor;

    public PropertyDrawEditor(final GroupObjectDescriptor groupObject, final PropertyDrawDescriptor descriptor, final FormDescriptor form) {
        super(groupObject);
        this.descriptor = descriptor;

        TitledPanel captionPanel = new TitledPanel(ClientResourceBundle.getString("descriptor.editor.object.editor.static.title"), new IncrementTextEditor(descriptor, "caption"));

        TitledPanel propertyObjectPanel = new TitledPanel(ClientResourceBundle.getString("descriptor.editor.object.editor.realization"), new PropertyObjectEditor(descriptor, "propertyObject", form, groupObject));

        TitledPanel groupObjectPanel = new TitledPanel(ClientResourceBundle.getString("descriptor.editor.object.editor.group.object"), new JComboBox(new IncrementSingleListSelectionModel(descriptor, "toDraw", true) {
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
        TitledPanel columnGroupObjectsPanel = new TitledPanel(ClientResourceBundle.getString("descriptor.editor.object.editor.group.in.column"), new IncrementMultipleListEditor(
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
        TitledPanel propertyCaptionPanel = new TitledPanel(ClientResourceBundle.getString("descriptor.editor.object.editor.dynamic.title"), new IncrementDialogEditor(descriptor, "propertyCaption") {
            protected Object dialogValue(Object currentValue) {
                return new ListGroupObjectEditor(descriptor.getColumnGroupObjects()).getPropertyObject();
            }
        });

        TitledPanel propertyBackgroundPanel = new TitledPanel(ClientResourceBundle.getString("descriptor.editor.object.editor.selection.property.background"), new IncrementDialogEditor(descriptor, "propertyBackground") {
            protected Object dialogValue(Object currentValue) {
                return new ListGroupObjectEditor(descriptor.getColumnGroupObjects()).getPropertyObject();
            }
        });

        TitledPanel propertyForegroundPanel = new TitledPanel(ClientResourceBundle.getString("descriptor.editor.object.editor.selection.property.foreground"), new IncrementDialogEditor(descriptor, "propertyForeground") {
            protected Object dialogValue(Object currentValue) {
                return new ListGroupObjectEditor(descriptor.getColumnGroupObjects()).getPropertyObject();
            }
        });

        TitledPanel shouldBeLastPanel = new TitledPanel(null, new IncrementCheckBox(ClientResourceBundle.getString("descriptor.editor.object.editor.should.be.last"), descriptor, "shouldBeLast"));
        TitledPanel editTypePanel = new TitledPanel(ClientResourceBundle.getString("descriptor.editor.object.editor.edit.type"), new JComboBox(new IncrementSingleListSelectionModel(descriptor, "editType") {
            public List<?> getSingleList() {
                return PropertyEditType.typeNameList();
            }
        }));
        TitledPanel focusablePanel = new TitledPanel(null, new IncrementTristateCheckBox(ClientResourceBundle.getString("descriptor.editor.object.editor.focusable"), descriptor, "focusable"));

        TitledPanel forceTypePanel = new TitledPanel(ClientResourceBundle.getString("descriptor.editor.object.editor.viewtype"), new JComboBox(new IncrementSingleListSelectionModel(descriptor, "forceViewType") {
            public List<?> getSingleList(){
                return ClassViewType.typeNameList();
            }
        }));

        TitledPanel editKeyPanel = new TitledPanel(ClientResourceBundle.getString("descriptor.editor.object.editor.editing.keys"), new IncrementKeyStrokeEditor(descriptor.client, "editKey"));

        JPanel defaultComponent = new JPanel(new FlowLayout(FlowLayout.LEFT));
        defaultComponent.add(new IncrementCheckBox(ClientResourceBundle.getString("descriptor.editor.object.editor.default.component"), descriptor.client, "defaultComponent"));

        addTab(ClientResourceBundle.getString("descriptor.editor.common"), new NorthBoxPanel(captionPanel,
                propertyObjectPanel,
                groupObjectPanel,
                columnGroupObjectsPanel,
                propertyCaptionPanel,
                propertyBackgroundPanel,
                propertyForegroundPanel,
                shouldBeLastPanel,
                editTypePanel,
                focusablePanel,
                forceTypePanel,
                editKeyPanel));

        addTab(ClientResourceBundle.getString("descriptor.editor.object.editor.display"), new NorthBoxPanel(defaultComponent,
                new SizesEditor(descriptor.client),
                new ComponentDesignEditor(ClientResourceBundle.getString("descriptor.editor.view.design"), descriptor.client.design)));

        addTab(ClientResourceBundle.getString("descriptor.editor.arrangement"), new NorthBoxPanel(new ComponentConstraintsEditor(descriptor.client.constraints)));
    }

    @Override
    public boolean validateEditor() {
        if (descriptor.getPropertyObject() == null) {
            JOptionPane.showMessageDialog(this, ClientResourceBundle.getString("descriptor.editor.object.editor.choose.realization"), ClientResourceBundle.getString("descriptor.editor.object.editor.error"), JOptionPane.ERROR_MESSAGE);
            return false;
        }

        return true;
    }
}

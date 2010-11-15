package platform.client.descriptor.nodes;

import platform.client.descriptor.FormDescriptor;
import platform.client.descriptor.GroupObjectDescriptor;
import platform.client.descriptor.PropertyDrawDescriptor;
import platform.interop.context.ApplicationContext;
import platform.interop.context.ApplicationContextHolder;
import platform.interop.context.ApplicationContextProvider;
import platform.interop.context.IncrementDependency;
import platform.client.tree.ClientTree;
import platform.base.BaseUtils;

import javax.swing.*;

public class PropertyDrawFolder extends GroupElementFolder<PropertyDrawFolder> implements ApplicationContextProvider {

    private FormDescriptor form;

    public ApplicationContext getContext() {
        return form.getContext();
    }

    public PropertyDrawFolder(GroupObjectDescriptor group, FormDescriptor form) {
        super(group, "Свойства");

        this.form = form;

        // добавим новые свойства, предполагается что оно одно, но пока не будем вешать assertion
        for (PropertyDrawDescriptor propertyDraw : BaseUtils.mergeList(form.getGroupPropertyDraws(group), form.getAddPropertyDraws(group)))
            add(new PropertyDrawNode(group, propertyDraw, form));

        addCollectionReferenceActions(this, "propertyDraws", new String[] {""}, new Class[] {PropertyDrawDescriptor.class});
    }

    public boolean addToPropertyDraws(PropertyDrawDescriptor propertyDraw) { // usage через reflection
        propertyDraw.addGroup = groupObject;
        form.addToPropertyDraws(propertyDraw);
        return true;
    }

    public boolean removeFromPropertyDraws(PropertyDrawDescriptor propertyDraw) {
        form.removeFromPropertyDraws(propertyDraw);
        return true;
    }

    @Override
    public boolean canImport(TransferHandler.TransferSupport info) {
        return ClientTree.getNode(info) instanceof PropertyDrawNode;
    }

    @Override
    public boolean importData(ClientTree tree, TransferHandler.TransferSupport info) {
        return form.movePropertyDraw((PropertyDrawDescriptor)ClientTree.getNode(info).getTypedObject(), ClientTree.getChildIndex(info));
    }
}

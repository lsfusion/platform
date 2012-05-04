package platform.client.descriptor.nodes;

import platform.client.ClientResourceBundle;
import platform.client.descriptor.FormDescriptor;
import platform.client.descriptor.GroupObjectDescriptor;
import platform.client.descriptor.PropertyDrawDescriptor;
import platform.base.context.ApplicationContext;
import platform.base.context.ApplicationContextProvider;
import platform.client.tree.ClientTree;
import platform.base.BaseUtils;

import javax.swing.*;

public class PropertyDrawFolder extends GroupElementFolder<PropertyDrawFolder> implements ApplicationContextProvider {

    private FormDescriptor form;

    public ApplicationContext getContext() {
        return form.getContext();
    }

    public PropertyDrawFolder(GroupObjectDescriptor group, FormDescriptor form) {
        super(group, ClientResourceBundle.getString("descriptor.properties"));

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
        // приходится так извращаться, поскольку indexTo scoped относительно groupObject у FormDescriptor'а в propertyDraws у него абсолютно другой индекс
        PropertyDrawNode nodeFrom = (PropertyDrawNode)ClientTree.getNode(info);
        int indexTo = ClientTree.getChildIndex(info);
        if (indexTo == -1) {
            // кинули на сам Folder
            return form.movePropertyDraw(nodeFrom.getTypedObject(), -1);
        } else {
            if (getIndex(nodeFrom) < indexTo) indexTo--;
            return form.movePropertyDraw(nodeFrom.getTypedObject(), ((PropertyDrawNode)getChildAt(indexTo)).getTypedObject());
        }
    }
}

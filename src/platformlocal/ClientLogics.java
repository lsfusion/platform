/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package platformlocal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.awt.*;
import java.text.Format;
import java.text.NumberFormat;
import java.io.Serializable;

class ClientGroupObjectImplement extends ArrayList<ClientObjectImplement>
                                 implements Serializable {

    Integer GID = 0;

    ClientGridView gridView = new ClientGridView();
    ClientFunctionView addView = new ClientFunctionView();
    ClientFunctionView changeClassView = new ClientFunctionView();
    ClientFunctionView delView = new ClientFunctionView();

    @Override
    public boolean equals(Object o) {
        return this == o;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 23 * hash + (this.GID != null ? this.GID.hashCode() : 0);
        return hash;
    }
    
    public ClientGroupObjectImplement() {
    }
}

class ClientGroupObjectMap<T> extends HashMap<ClientObjectImplement,T>
                              implements Serializable {

   
/*  На самом деле не надо - так как сравнивать как раз надо именно по значениям
    @Override
    public boolean equals(Object o) {
        return this == o;
    }

    // Здесь по хорошему нужно hashcode когда новые свойства появятся перегрузить
    @Override
    public int hashCode() {
        int hash = 7;
        return hash;
    } */

}

class ClientGroupObjectValue extends ClientGroupObjectMap<Integer>
                             implements Serializable {
    
}

class ClientObjectImplement implements Serializable {
    
    Integer ID = 0;
    
    ClientGroupObjectImplement groupObject;
 
    String caption = "";
    
    ClientObjectView objectIDView = new ClientObjectView();
    ClientClassView classView = new ClientClassView();
    
    public ClientObjectImplement() {
    }

    public String toString() { return caption; }
}

class ClientComponentView implements Serializable {
    
    ClientContainerView container;
    SimplexConstraints constraints = new SimplexConstraints();
 
    String outName = "";
    
}

class ClientContainerView extends ClientComponentView {

    String title;

    LayoutManager layout;
}

class ClientGridView extends ClientComponentView {

}

class ClientClassView extends ClientComponentView {
    
}

class ClientFunctionView extends ClientComponentView {
    
}

abstract class ClientCellView extends ClientComponentView {
    
    Integer ID = 0;

    ClientGroupObjectImplement groupObject;
    
    Dimension minimumSize;
    Dimension maximumSize;
    Dimension preferredSize;

    String caption;

    public int getMinimumWidth() {
        return getPreferredWidth();
    }

    public int getMinimumHeight() {
        return getPreferredHeight();
    }

    public Dimension getMinimumSize() {

        if (minimumSize != null) return minimumSize;
        return new Dimension(getMinimumWidth(), getMinimumHeight());
    }

    public int getPreferredWidth() {
        return 50;
    }
    
    public int getPreferredHeight() {
        return 15;
    }
    
    public Dimension getPreferredSize() {
        
        if (preferredSize != null) return preferredSize;
        return new Dimension(getPreferredWidth(), getPreferredHeight());
    }
    
    transient protected PropertyRendererComponent renderer;
    abstract public PropertyRendererComponent getRendererComponent(ClientForm form);
    abstract public PropertyEditorComponent getEditorComponent(ClientForm form);

    Format format;
    public Format getFormat() { return format; }

    public String toString() { return caption; }

}

class ClientPropertyView extends ClientCellView {

    public ClientClass baseClass;

/*    public ClientClass getBaseClass(ClientForm form) {

       if (baseClass == null) baseClass = ByteArraySerializer.deserializeClientClass(form.remoteForm.getPropertyClassByteArray(ID));

       return baseClass;
    }*/

    public int getMinimumWidth() {

        return baseClass.getMinimumWidth();
    }

    public int getPreferredWidth() {

        return baseClass.getPreferredWidth();
    }

    public PropertyRendererComponent getRendererComponent(ClientForm form) {
        
        if (renderer == null) renderer = baseClass.getRendererComponent(getFormat());

        return renderer;
    }
    
    public PropertyEditorComponent getEditorComponent(ClientForm form) {

        return baseClass.getEditorComponent(form, getFormat());
    }

}

class ClientObjectView extends ClientCellView {

    public PropertyRendererComponent getRendererComponent(ClientForm form) {
        
        if (renderer == null) {
            renderer = new QuantityPropertyRenderer(getFormat());
        }
        
        return renderer;
        
    }
    
    public PropertyEditorComponent getEditorComponent(ClientForm form) {
        
        form.switchClassView(groupObject);
        return null;
    }
    
}

class ClientFormChanges extends AbstractFormChanges<ClientGroupObjectImplement,ClientGroupObjectValue,ClientPropertyView> {

}


class ClientFormView implements Serializable {
    
    List<ClientGroupObjectImplement> groupObjects = new ArrayList();
    List<ClientObjectImplement> objects = new ArrayList();
    List<ClientPropertyView> properties = new ArrayList();
    
    List<ClientContainerView> containers = new ArrayList();

    ClientFunctionView printView = new ClientFunctionView();
    ClientFunctionView applyView = new ClientFunctionView();
    ClientFunctionView cancelView = new ClientFunctionView();
    ClientFunctionView okView = new ClientFunctionView();
    ClientFunctionView closeView = new ClientFunctionView();

    List<ClientCellView> order = new ArrayList();

    public ClientFormView() {
    }

    public ClientGroupObjectImplement getGroupObject(int id) {
        for (ClientGroupObjectImplement groupObject : groupObjects)
            if (groupObject.GID == id) return groupObject;
        return null;
    }

    public ClientPropertyView getPropertyView(int id) {
        for (ClientPropertyView property : properties)
            if (property.ID == id) return property;
        return null;
    }
}

class DefaultClientFormView extends ClientFormView {

    public DefaultClientFormView(RemoteForm remoteForm) {

        ClientContainerView mainContainer = new ClientContainerView();
        mainContainer.outName = "mainContainer";
        mainContainer.constraints.childConstraints = SingleSimplexConstraint.TOTHE_BOTTOM;

        containers.add(mainContainer);

        Map<GroupObjectImplement, ClientGroupObjectImplement> mgroupObjects = new HashMap();
        Map<ObjectImplement, ClientObjectImplement> mobjects = new HashMap();
        Map<PropertyView, ClientPropertyView> mproperties = new HashMap();

        Map<ClientGroupObjectImplement, ClientContainerView> groupContainers = new HashMap();
        Map<ClientGroupObjectImplement, ClientContainerView> panelContainers = new HashMap();

        for (GroupObjectImplement group : (List<GroupObjectImplement>)remoteForm.Groups) {

            ClientGroupObjectImplement clientGroup = new ClientGroupObjectImplement();
            clientGroup.GID = group.GID;

            mgroupObjects.put(group, clientGroup);
            groupObjects.add(clientGroup);

            ClientContainerView groupContainer = new ClientContainerView();
            groupContainer.outName = "groupContainer " + group.get(0).OutName;
            groupContainer.container = mainContainer;
            groupContainer.constraints.order = remoteForm.Groups.indexOf(group);
            groupContainer.constraints.childConstraints = SingleSimplexConstraint.TOTHE_BOTTOM;
            groupContainer.constraints.insetsInside = new Insets(0,0,4,0);

            groupContainers.put(clientGroup, groupContainer);
            containers.add(groupContainer);

            ClientContainerView gridContainer = new ClientContainerView();
            gridContainer.outName = "gridContainer " + group.get(0).OutName;
            gridContainer.container = groupContainer;
            gridContainer.constraints.order = 0;
            gridContainer.constraints.childConstraints = SingleSimplexConstraint.TOTHE_RIGHT;

            containers.add(gridContainer);

            ClientContainerView panelContainer = new ClientContainerView();
            panelContainer.outName = "panelContainer " + group.get(0).OutName;
            panelContainer.container = groupContainer;
            panelContainer.constraints.order = 1;
            panelContainer.constraints.childConstraints = SingleSimplexConstraint.TOTHE_RIGHTBOTTOM;

            panelContainer.title = group.get(0).OutName;

            panelContainers.put(clientGroup, panelContainer);
            containers.add(panelContainer);

            ClientContainerView buttonContainer = new ClientContainerView();
            buttonContainer.outName = "buttonContainer " + group.get(0).OutName;
            buttonContainer.container = groupContainer;
            buttonContainer.constraints.order = 2;
            buttonContainer.constraints.childConstraints = SingleSimplexConstraint.TOTHE_RIGHT;

            containers.add(buttonContainer);

            clientGroup.gridView.container = gridContainer;
            clientGroup.gridView.constraints.order = 1;
            clientGroup.gridView.constraints.fillVertical = 1;
            clientGroup.gridView.constraints.fillHorizontal = 1;

            clientGroup.addView.container = buttonContainer;
            clientGroup.addView.constraints.order = 0;

            clientGroup.delView.container = buttonContainer;
            clientGroup.delView.constraints.order = 1;

            clientGroup.changeClassView.container = buttonContainer;
            clientGroup.changeClassView.constraints.order = 2;

            for (ObjectImplement object : group) {

                ClientObjectImplement clientObject = new ClientObjectImplement();
                clientObject.ID = object.ID;
                clientObject.groupObject = clientGroup;

                clientObject.objectIDView.ID = object.ID;
                clientObject.objectIDView.groupObject = clientGroup;

                clientObject.objectIDView.container = panelContainer;
                clientObject.objectIDView.constraints.order = -1000 + group.indexOf(object);
                clientObject.objectIDView.constraints.insetsSibling = new Insets(0,0,2,2);

                clientObject.caption = object.OutName;
                clientObject.objectIDView.caption = object.OutName;

                clientObject.classView.container = gridContainer;
                clientObject.classView.constraints.order = 0;
                clientObject.classView.constraints.fillVertical = 1;
                clientObject.classView.constraints.fillHorizontal = 0.2;

                clientGroup.add(clientObject);

                mobjects.put(object, clientObject);
                objects.add(clientObject);

                order.add(clientObject.objectIDView);
            }
        }

        for (PropertyView property : (List<PropertyView>)remoteForm.Properties) {

            ClientPropertyView clientProperty = new ClientPropertyView();
            clientProperty.ID = property.ID;

            clientProperty.groupObject = mgroupObjects.get(property.ToDraw);
            clientProperty.constraints.order = remoteForm.Properties.indexOf(property);
            clientProperty.constraints.insetsSibling = new Insets(0,0,2,2);

            //временно
            clientProperty.caption = property.View.Property.OutName;
            clientProperty.baseClass = ByteArraySerializer.deserializeClientClass(
                                       ByteArraySerializer.serializeClass(property.View.Property.GetValueClass(property.View.Property.GetClassSet(null).get(0))));

            mproperties.put(property, clientProperty);
            properties.add(clientProperty);

            clientProperty.container = panelContainers.get(clientProperty.groupObject);

            order.add(clientProperty);
        }

        ClientContainerView formButtonContainer = new ClientContainerView();
        formButtonContainer.container = mainContainer;
        formButtonContainer.constraints.order = remoteForm.Groups.size();
        formButtonContainer.constraints.childConstraints = SingleSimplexConstraint.TOTHE_RIGHT;

        containers.add(formButtonContainer);

        printView.container = formButtonContainer;
        printView.constraints.order = 0;
        printView.constraints.directions = new SimplexComponentDirections(0,0.01,0.01,0);

        applyView.container = formButtonContainer;
        applyView.constraints.order = 1;
        applyView.constraints.directions = new SimplexComponentDirections(0,0,0.01,0.01);

        cancelView.container = formButtonContainer;
        cancelView.constraints.order = 2;
        cancelView.constraints.directions = new SimplexComponentDirections(0,0,0.01,0.01);

        okView.constraints.insetsSibling = new Insets(0, 8, 0, 0);
        
        okView.container = formButtonContainer;
        okView.constraints.order = 3;
        okView.constraints.directions = new SimplexComponentDirections(0,0,0.01,0.01);

        closeView.container = formButtonContainer;
        closeView.constraints.order = 4;
        closeView.constraints.directions = new SimplexComponentDirections(0,0,0.01,0.01);

    }

}

// -------------------------------------- Классы ------------------------------ //

abstract class ClientClass implements Serializable {

    int ID;
    String caption;

    boolean hasChilds;

    public String toString() { return caption; }

    public int getMinimumWidth() {
        return getPreferredWidth();
    }
    public int getPreferredWidth() {
        return 50;
    }

    abstract public PropertyRendererComponent getRendererComponent(Format format);
    abstract public PropertyEditorComponent getEditorComponent(ClientForm form, Format format);
}

class ClientObjectClass extends ClientClass {

    public int getPreferredWidth() { return 45; }

    public PropertyRendererComponent getRendererComponent(Format format) { return new QuantityPropertyRenderer(format); } ;
    public PropertyEditorComponent getEditorComponent(ClientForm form, Format format) { return new ObjectPropertyEditor(form); }
}

class ClientStringClass extends ClientClass {

    public int getMinimumWidth() { return 30; }
    public int getPreferredWidth() { return 250; }

    public PropertyRendererComponent getRendererComponent(Format format) { return new StringPropertyRenderer(format); } ;
    public PropertyEditorComponent getEditorComponent(ClientForm form, Format format) { return new StringPropertyEditor(); }
}

class ClientQuantityClass extends ClientClass {

    public int getPreferredWidth() { return 45; }

    public PropertyRendererComponent getRendererComponent(Format format) { return new QuantityPropertyRenderer(format); } ;
    public PropertyEditorComponent getEditorComponent(ClientForm form, Format format) { return new QuantityPropertyEditor((NumberFormat)format); }
}

class ClientDateClass extends ClientClass {

    public int getPreferredWidth() { return 70; }

    public PropertyRendererComponent getRendererComponent(Format format) { return new DatePropertyRenderer(format); } ;
    public PropertyEditorComponent getEditorComponent(ClientForm form, Format format) { return new DatePropertyEditor(); }
}

class ClientBitClass extends ClientClass {

    public int getPreferredWidth() { return 35; }

    public PropertyRendererComponent getRendererComponent(Format format) { return new BitPropertyRenderer(); } ;
    public PropertyEditorComponent getEditorComponent(ClientForm form, Format format) { return new BitPropertyEditor(); }
}

// -------------------------------------- Фильтры ------------------------------ //

class ClientFilter {

    ClientPropertyView property;
    ClientValueLink value;

    int compare;
    
}

abstract class ClientValueLink {

}

class ClientUserValueLink extends ClientValueLink {

    Object value;

    public String toString() { return "Значение"; }
}

class ClientObjectValueLink extends ClientValueLink {

    ClientObjectImplement object;

    public String toString() { return "Объект"; }
}

class ClientPropertyValueLink extends ClientValueLink {

    ClientPropertyView property;

    public String toString() { return "Свойство"; }
}
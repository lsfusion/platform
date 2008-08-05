/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package platformlocal;

import java.awt.Dimension;
import java.awt.LayoutManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class ClientGroupObjectImplement extends ArrayList<ClientObjectImplement> {

    Integer GID = 0;
    
    ClientGridView gridView = new ClientGridView();
    ClientFunctionView addView = new ClientFunctionView();
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

class ClientGroupObjectMap<T> extends HashMap<ClientObjectImplement,T> {

   
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

class ClientGroupObjectValue extends ClientGroupObjectMap<Integer> {
    
}

class ClientObjectImplement {
    
    Integer GID = 0;
    
    ClientGroupObjectImplement groupObject;
 
    String caption = "";
    
    ClientObjectView objectIDView = new ClientObjectView();
    
    public ClientObjectImplement() {
    }
}

class ClientComponentView {
    
    ClientContainerView container;
    SimplexConstraints constraints = new SimplexConstraints();
 
    String outName = "";
    
}

class ClientContainerView extends ClientComponentView {
    
    LayoutManager layout;
}

class ClientGridView extends ClientComponentView {
    
}

class ClientFunctionView extends ClientComponentView {
    
}

abstract class ClientCellView extends ClientComponentView {
    
    Integer GID = 0;

    ClientGroupObjectImplement groupObject;
    
    Dimension minimumSize;
    Dimension maximumSize;
    Dimension preferredSize;

    String caption;

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
    
    abstract public PropertyRendererComponent getRendererComponent(ClientForm form);
    abstract public PropertyEditorComponent getEditorComponent(ClientForm form);

}

class ClientPropertyView extends ClientCellView {

    String type;
    
    public int getPreferredWidth() {
        
        int res = 15;
        
        if (type.equals("integer")) res = 9;
        if (type.equals("char(50)")) res = 50;
        
        return res * 5;
    }
    
    private PropertyRendererComponent renderer;
    public PropertyRendererComponent getRendererComponent(ClientForm form) {
        
        if (renderer == null) {
            
            if (type.equals("integer")) renderer = new IntegerPropertyRenderer();
            if (type.equals("char(50)")) renderer = new StringPropertyRenderer();

            if (renderer == null) renderer = new StringPropertyRenderer();
            
        }
        
        return renderer;
        
    }
    
    public PropertyEditorComponent getEditorComponent(ClientForm form) {
        
        if (type.equals("integer")) return new IntegerPropertyEditor();
        if (type.equals("char(50)")) return new StringPropertyEditor();
        
        return new StringPropertyEditor();
        
    }

}

class ClientObjectView extends ClientCellView {

    private PropertyRendererComponent renderer;
    public PropertyRendererComponent getRendererComponent(ClientForm form) {
        
        if (renderer == null) {
            renderer = new IntegerPropertyRenderer();
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


class ClientFormView {
    
    List<ClientGroupObjectImplement> groupObjects = new ArrayList();
    List<ClientObjectImplement> objects = new ArrayList();
    List<ClientPropertyView> properties = new ArrayList();
    
    List<ClientContainerView> containers = new ArrayList();

    ClientFunctionView printView = new ClientFunctionView();
    ClientFunctionView applyView = new ClientFunctionView();
    ClientFunctionView cancelView = new ClientFunctionView();

    List<ClientCellView> order = new ArrayList();
    
    public ClientFormView() {
    }

}

class ClientFormBean {
    
    RemoteForm formBean;
    
    Map<GroupObjectImplement, ClientGroupObjectImplement> groupObjects = new HashMap();
    Map<ObjectImplement, ClientObjectImplement> objects = new HashMap();
    Map<PropertyView, ClientPropertyView> properties = new HashMap();
    Map<GroupObjectValue, ClientGroupObjectValue> objectValues = new HashMap();

    List<ClientGroupObjectImplement> listGroups = new ArrayList();
    List<ClientObjectImplement> listObjects = new ArrayList();
    List<ClientPropertyView> listProperties = new ArrayList();

    List<ClientContainerView> listContainers = new ArrayList();

    ClientContainerView mainContainer;
    Map<ClientGroupObjectImplement, ClientContainerView> groupContainers = new HashMap();
    Map<ClientGroupObjectImplement, ClientContainerView> panelContainers = new HashMap();
    
    List<ClientCellView> listOrder = new ArrayList();

    ClientFunctionView printView;
    ClientFunctionView applyView;
    ClientFunctionView cancelView;
    
    public ClientGroupObjectImplement client(GroupObjectImplement groupObject) {
        return groupObjects.get(groupObject);
    }

    public ClientObjectImplement client(ObjectImplement object) {
        return objects.get(object);
    }
    
    public ClientPropertyView client(PropertyView property) {
        return properties.get(property);
    }
    
    public ClientFormBean(RemoteForm iformBean) {
        
        formBean = iformBean;
        
        mainContainer = new ClientContainerView();
        mainContainer.outName = "mainContainer";
        mainContainer.constraints.childConstraints = SingleSimplexConstraint.TOTHE_BOTTOM;
        
        listContainers.add(mainContainer);
        
        for (GroupObjectImplement group : formBean.Groups) {
            
            ClientGroupObjectImplement clientGroup = new ClientGroupObjectImplement();
            groupObjects.put(group, clientGroup);
            listGroups.add(clientGroup);
            
            ClientContainerView groupContainer = new ClientContainerView();
            groupContainer.outName = "groupContainer " + group.get(0).OutName;
            groupContainer.container = mainContainer;
            groupContainer.constraints.order = formBean.Groups.indexOf(group);
            groupContainer.constraints.childConstraints = SingleSimplexConstraint.TOTHE_BOTTOM;
            
            groupContainers.put(clientGroup, groupContainer);
            listContainers.add(groupContainer);

            ClientContainerView panelContainer = new ClientContainerView();
            panelContainer.outName = "panelContainer " + group.get(0).OutName;
            panelContainer.container = groupContainer;
            panelContainer.constraints.order = 1;
            panelContainer.constraints.childConstraints = SingleSimplexConstraint.TOTHE_RIGHTBOTTOM;
      
            panelContainers.put(clientGroup, panelContainer);
            listContainers.add(panelContainer);
            
            ClientContainerView buttonContainer = new ClientContainerView();
            buttonContainer.outName = "buttonContainer " + group.get(0).OutName;
            buttonContainer.container = groupContainer;
            buttonContainer.constraints.order = 2;
            buttonContainer.constraints.childConstraints = SingleSimplexConstraint.TOTHE_RIGHT;

            listContainers.add(buttonContainer);

            clientGroup.gridView.container = groupContainer;
            clientGroup.gridView.constraints.order = 0;
            clientGroup.gridView.constraints.fillVertical = SimplexConstraints.MAXIMUM;
            clientGroup.gridView.constraints.fillHorizontal = SimplexConstraints.MAXIMUM;

            clientGroup.addView.container = buttonContainer;
            clientGroup.addView.constraints.order = 0;
            
            clientGroup.delView.container = buttonContainer;
            clientGroup.delView.constraints.order = 1;
            
            for (ObjectImplement object : group) {

                ClientObjectImplement clientObject = new ClientObjectImplement();
                clientObject.groupObject = clientGroup;
                clientObject.objectIDView.groupObject = clientGroup;
                
                clientObject.objectIDView.container = panelContainer;
                clientObject.objectIDView.constraints.order = -1000 + group.indexOf(object);
                
                clientObject.caption = object.OutName;
                clientObject.objectIDView.caption = object.OutName;
                
                clientGroup.add(clientObject);
                
                objects.put(object, clientObject);
                listObjects.add(clientObject);

                listOrder.add(clientObject.objectIDView);
            }
        }
        
        for (PropertyView property : formBean.Properties) {
            
            ClientPropertyView clientProperty = new ClientPropertyView();
            clientProperty.groupObject = groupObjects.get(property.ToDraw);
            clientProperty.constraints.order = formBean.Properties.indexOf(property);
            
            //временно
            clientProperty.caption = property.View.Property.OutName;
            clientProperty.type = property.View.Property.GetDBType();
            
            properties.put(property, clientProperty);
            listProperties.add(clientProperty);
            
            clientProperty.container = panelContainers.get(clientProperty.groupObject);
            
            listOrder.add(clientProperty);
        }
        
        ClientContainerView formButtonContainer = new ClientContainerView();
        formButtonContainer.container = mainContainer;
        formButtonContainer.constraints.order = formBean.Groups.size();
        formButtonContainer.constraints.childConstraints = SingleSimplexConstraint.TOTHE_RIGHT;

        listContainers.add(formButtonContainer);

        printView = new ClientFunctionView();
        printView.container = formButtonContainer;
        printView.constraints.order = 0;
        printView.constraints.directions = new SimplexComponentDirections(0,0.01,0.01,0);

        applyView = new ClientFunctionView();
        applyView.container = formButtonContainer;
        applyView.constraints.order = 1;
        applyView.constraints.directions = new SimplexComponentDirections(0,0,0.01,0.01);

        cancelView = new ClientFunctionView();
        cancelView.container = formButtonContainer;
        cancelView.constraints.order = 2;
        cancelView.constraints.directions = new SimplexComponentDirections(0,0,0.01,0.01);
        
    }

    ClientFormChanges convertFormChangesToClient (FormChanges formChanges)
    {
        ClientFormChanges clientChanges = new ClientFormChanges();

        for (GroupObjectImplement group : formChanges.Objects.keySet()) {

            ClientGroupObjectImplement clientGroup = groupObjects.get(group);
            
            GroupObjectValue objectValue = formChanges.Objects.get(group);
            ClientGroupObjectValue clientObjectValue = new ClientGroupObjectValue();
            
            for (ObjectImplement object : objectValue.keySet()) {

                ClientObjectImplement clientObject = objects.get(object);
                
                clientObjectValue.put(clientObject, objectValue.get(object));
            }
            
            objectValues.put(objectValue, clientObjectValue);
            clientChanges.Objects.put(clientGroup, clientObjectValue);
        }

        for (GroupObjectImplement group : formChanges.GridObjects.keySet()) {

            ClientGroupObjectImplement clientGroup = groupObjects.get(group);
            
            List<GroupObjectValue> listObjectValue = formChanges.GridObjects.get(group);
            List<ClientGroupObjectValue> listClientObjectValue = new ArrayList();
            
            for (GroupObjectValue objectValue : listObjectValue) {

                ClientGroupObjectValue clientObjectValue = new ClientGroupObjectValue();
            
                for (ObjectImplement object : objectValue.keySet()) {

                    ClientObjectImplement clientObject = objects.get(object);

                    clientObjectValue.put(clientObject, objectValue.get(object));
                }
                
                objectValues.put(objectValue, clientObjectValue);
                listClientObjectValue.add(clientObjectValue);
            }
            
            clientChanges.GridObjects.put(clientGroup, listClientObjectValue);
        }
        
        for (PropertyView property : formChanges.PanelProperties.keySet()) {
            
            ClientPropertyView clientProperty = properties.get(property);
            
            clientChanges.PanelProperties.put(clientProperty, formChanges.PanelProperties.get(property));
        }

        for (PropertyView property : formChanges.GridProperties.keySet()) {

            ClientPropertyView clientProperty = properties.get(property);
            
            Map<GroupObjectValue, Object> mapObjectValue = formChanges.GridProperties.get(property);
            Map<ClientGroupObjectValue, Object> clientMapObjectValue = new HashMap();
            
            for (GroupObjectValue objectValue : mapObjectValue.keySet()) {

                ClientGroupObjectValue clientObjectValue = objectValues.get(objectValue);
                clientMapObjectValue.put(clientObjectValue, mapObjectValue.get(objectValue));
            }
            
            clientChanges.GridProperties.put(clientProperty, clientMapObjectValue);
        }
        
        for (PropertyView property : formChanges.DropProperties) {

            ClientPropertyView clientProperty = properties.get(property);
            
            clientChanges.DropProperties.add(clientProperty);
        }
        
        return clientChanges;
    
    }
    
    public ClientFormView getClientFormView() {
        
        ClientFormView formView = new ClientFormView();
  
        for (ClientGroupObjectImplement groupObject : listGroups)
            formView.groupObjects.add(groupObject);
        
        for (ClientObjectImplement object : listObjects)
            formView.objects.add(object);
            
        for (ClientPropertyView property : listProperties)
            formView.properties.add(property);

        for (ClientContainerView container : listContainers)
            formView.containers.add(container);
            
        for (ClientCellView view : listOrder)
            formView.order.add(view);

        formView.printView = printView;
        formView.applyView = applyView;
        formView.cancelView = cancelView;
        
        return formView;
        
    }

    // Здесь тестинг, заглушка на простую перекачку FormChanges в ClientFormChanges
    
    ClientFormChanges changeObject(ClientGroupObjectImplement groupObject, ClientGroupObjectValue objectValue) {
        
        MapUtils<GroupObjectImplement, ClientGroupObjectImplement> mgu = new MapUtils();
        GroupObjectImplement GroupObject = mgu.getKey(groupObjects, groupObject);

        MapUtils<GroupObjectValue, ClientGroupObjectValue> mvu = new MapUtils();
        GroupObjectValue GroupVal = mvu.getKey(objectValues, objectValue);
        
        try {
            formBean.ChangeObject(GroupObject, GroupVal);
        } catch(SQLException e) {
            
        }
        
        FormChanges formChanges = null;
        try {
            formChanges = formBean.EndApply();
        } catch (SQLException e) {
            System.out.println(e);
        }
        
//        formChanges.Out(formBean);

        System.out.println(objectValue.toString());
        
        return convertFormChangesToClient(formChanges);
        
    }

    ClientFormChanges changeProperty(ClientPropertyView clientProperty, Object value) {
        
        MapUtils<PropertyView, ClientPropertyView> pvu = new MapUtils();
        PropertyView property = pvu.getKey(properties, clientProperty);
        
        try {
            formBean.ChangePropertyView(property, value);
        } catch(SQLException e) {
            
        }
        
        FormChanges formChanges = null;
        try {
            formChanges = formBean.EndApply();
        } catch (SQLException e) {
            System.out.println(e);
        }
        
        System.out.println("Change : " + value.toString());
//        formChanges.Out(formBean);

        return convertFormChangesToClient(formChanges);
        
    }

    ClientFormChanges addObject(ClientObjectImplement clientObject) {
        
        MapUtils<ObjectImplement, ClientObjectImplement> mou = new MapUtils();
        ObjectImplement object = mou.getKey(objects, clientObject);
        
        try {
            formBean.AddObject(object);
        } catch(SQLException e) {
            
        }
        
        FormChanges formChanges = null;
        try {
            formChanges = formBean.EndApply();
        } catch (SQLException e) {
            System.out.println(e);
        }
        
//        System.out.println("Change : " + value.toString());
//        formChanges.Out(formBean);

        return convertFormChangesToClient(formChanges);
        
    }

    ClientFormChanges changeClass(ClientObjectImplement clientObject) {
        
        MapUtils<ObjectImplement, ClientObjectImplement> mou = new MapUtils();
        ObjectImplement object = mou.getKey(objects, clientObject);
        
        try {
            formBean.ChangeClass(object, null);
        } catch(SQLException e) {
            
        }
        
        FormChanges formChanges = null;
        try {
            formChanges = formBean.EndApply();
        } catch (SQLException e) {
            System.out.println(e);
        }
        
//        System.out.println("Change : " + value.toString());
//        formChanges.Out(formBean);

        return convertFormChangesToClient(formChanges);
        
    }

    ClientFormChanges changeClassView(ClientGroupObjectImplement groupObject, Boolean classView) {
 
        MapUtils<GroupObjectImplement, ClientGroupObjectImplement> mgu = new MapUtils();
        GroupObjectImplement GroupObject = mgu.getKey(groupObjects, groupObject);

        formBean.ChangeClassView(GroupObject, classView);
        
        FormChanges formChanges = null;
        try {
            formChanges = formBean.EndApply();
        } catch (SQLException e) {
            System.out.println(e);
        }
        
//        formChanges.Out(formBean);

        return convertFormChangesToClient(formChanges);
        
    }

    ClientFormChanges saveChanges() {
 
        try {
            System.out.println("Save changes : " + formBean.SaveChanges());
        } catch(SQLException e) {
            
        }
        
        FormChanges formChanges = null;
        try {
            formChanges = formBean.EndApply();
        } catch (SQLException e) {
            System.out.println(e);
        }
        
//        formChanges.Out(formBean);

        return convertFormChangesToClient(formChanges);
        
    }
    
    ClientFormChanges cancelChanges() {
 
        formBean.CancelChanges();
        
        FormChanges formChanges = null;
        try {
            formChanges = formBean.EndApply();
        } catch (SQLException e) {
            System.out.println(e);
        }
        
//        formChanges.Out(formBean);

        return convertFormChangesToClient(formChanges);
        
    }
    
}
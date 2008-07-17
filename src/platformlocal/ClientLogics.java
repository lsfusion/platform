/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package platformlocal;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

class ClientGroupObjectImplement extends ArrayList<ClientObjectImplement> {

    Integer GID = 0;
    
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
    
}

class ClientGroupPropertyView {
    
}

class ClientPropertyView {
    
    Integer GID = 0;

    ClientGroupPropertyView groupProperty;
    ClientGroupObjectImplement groupObject;

    String type;
    
    Dimension minimumSize;
    Dimension maximumSize;
    Dimension preferredSize;
    
    public int getPreferredWidth() {
        
        int res = 15;
        
        if (type.equals("integer")) res = 9;
        if (type.equals("char(50)")) res = 50;
        
        return res * 5;
    }
    
    public int getPreferredHeight() {
        return 15;        
    }
    
    public Dimension getPreferredSize() {
        
        if (preferredSize != null) return preferredSize;
        return new Dimension(getPreferredWidth(), getPreferredHeight());
    }
    
    String caption;

}

class ClientFormChanges extends AbstractFormChanges<ClientGroupObjectImplement,ClientGroupObjectValue,ClientPropertyView> {
    
}


class ClientFormInit {
    
    List<ClientGroupObjectImplement> GroupObjects;
    List<ClientObjectImplement> Objects;
    List<ClientGroupPropertyView> GroupProperties;
    List<ClientPropertyView> Properties;
    
    public ClientFormInit() {
        
        GroupObjects = new ArrayList();
        Objects = new ArrayList();
        GroupProperties = new ArrayList();
        Properties = new ArrayList();
        
    }

}

class ClientFormBean {
    
    FormBeanView formBean;
    
    Map<GroupObjectImplement, ClientGroupObjectImplement> groupObjects = new HashMap();
    Map<ObjectImplement, ClientObjectImplement> objects = new HashMap();
    Map<PropertyView, ClientPropertyView> properties = new HashMap();
    Map<GroupObjectValue, ClientGroupObjectValue> objectValues = new HashMap();
    
    public ClientGroupObjectImplement client(GroupObjectImplement groupObject) {
        return groupObjects.get(groupObject);
    }

    public ClientObjectImplement client(ObjectImplement object) {
        return objects.get(object);
    }
    
    public ClientPropertyView client(PropertyView property) {
        return properties.get(property);
    }
    
    public ClientFormBean(FormBeanView iformBean) {
        
        formBean = iformBean;

        groupObjects = new HashMap();
        objects = new HashMap();
        properties = new HashMap();
        
        for (GroupObjectImplement group : formBean.Groups) {
            
            ClientGroupObjectImplement clientGroup = new ClientGroupObjectImplement();
            groupObjects.put(group, clientGroup);

            for (ObjectImplement object : group) {

                ClientObjectImplement clientObject = new ClientObjectImplement();
                clientObject.groupObject = clientGroup;
                
                objects.put(object, clientObject);
            }
        }
        
        for (PropertyView property : formBean.Properties) {
            
            ClientPropertyView clientProperty = new ClientPropertyView();
            clientProperty.groupObject = groupObjects.get(property.ToDraw);
            
            //временно
            clientProperty.caption = property.View.Property.OutName;
            clientProperty.type = property.View.Property.GetDBType();
            
            properties.put(property, clientProperty);
        }
        
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
    
    public ClientFormInit getClientFormInit() {
        
        ClientFormInit formInit = new ClientFormInit();
  
        for (GroupObjectImplement groupObject : groupObjects.keySet())
            formInit.GroupObjects.add(groupObjects.get(groupObject));
        
        for (ObjectImplement object : objects.keySet())
            formInit.Objects.add(objects.get(object));
            
        for (PropertyView property : properties.keySet())
            formInit.Properties.add(properties.get(property));
        
        return formInit;
        
    }

    // Здесь тестинг, заглушка на простую перекачку FormChanges в ClientFormChanges
    
    ClientFormChanges changeObject(ClientGroupObjectImplement groupObject, ClientGroupObjectValue objectValue) {
        
        MapUtils<GroupObjectImplement, ClientGroupObjectImplement> mgu = new MapUtils();
        GroupObjectImplement GroupObject = mgu.getKey(groupObjects, groupObject);

        MapUtils<GroupObjectValue, ClientGroupObjectValue> mvu = new MapUtils();
        GroupObjectValue GroupVal = mvu.getKey(objectValues, objectValue);
        
        try {
            formBean.ChangeObject(GroupObject, GroupVal);
        } catch(Exception e) {
            
        }
        
        FormChanges formChanges = null;
        try {
            formChanges = formBean.EndApply();
        } catch (SQLException e) {
            System.out.println(e);
        }
        
        formChanges.Out(formBean);

        return convertFormChangesToClient(formChanges);
        
    }

    ClientFormChanges changeProperty(ClientPropertyView clientProperty, Object value) {
        
        MapUtils<PropertyView, ClientPropertyView> pvu = new MapUtils();
        PropertyView property = pvu.getKey(properties, clientProperty);
        
        try {
            formBean.ChangePropertyView(property, value);
        } catch(Exception e) {
            
        }
        
        FormChanges formChanges = null;
        try {
            formChanges = formBean.EndApply();
        } catch (SQLException e) {
            System.out.println(e);
        }
        
        formChanges.Out(formBean);

        return convertFormChangesToClient(formChanges);
        
    }
    
}
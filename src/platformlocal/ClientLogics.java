/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package platformlocal;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.awt.Dimension;
import java.awt.LayoutManager;
import java.io.ByteArrayInputStream;
import java.io.IOException;

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
    
    Integer ID = 0;
    
    ClientGroupObjectImplement groupObject;
 
    String caption = "";
    
    ClientObjectView objectIDView = new ClientObjectView();
    ClientClassView classView = new ClientClassView();
    
    public ClientObjectImplement() {
    }
}

class ClientClass {

    int ID;
    String caption;

    boolean hasChilds;

    public String toString() { return caption; }
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
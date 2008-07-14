/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package platformlocal;

import java.awt.GridBagConstraints;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

class ClientGroupObjectImplement extends ArrayList<ClientObjectImplement> {

    Integer GID = 0;
    
    GridBagConstraints PanelConstraint;
    GridBagConstraints GridConstraint;

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
    
    ClientGroupObjectImplement GroupObject;
    
}

class ClientGroupPropertyView {
    
}

class ClientPropertyView {
    
    Integer GID = 0;
    
    ClientGroupPropertyView GroupProperty;
    ClientGroupObjectImplement GroupObject;
    
    String Caption;
    GridBagConstraints PanelConstraint;
    
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

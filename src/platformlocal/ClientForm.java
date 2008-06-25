/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package platformlocal;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import org.jdesktop.application.FrameView;
import org.jdesktop.application.SingleFrameApplication;

class ClientGroupObjectImplement extends ArrayList<ClientObjectImplement> {

    Integer GID = 0;
    
    GridBagConstraints PanelConstraint;
    GridBagConstraints GridConstraint;
    
}

class ClientGroupObjectMap<T> extends HashMap<ClientObjectImplement,T> {
    
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

public class ClientForm extends FrameView {

    ClientFormInit Objects;
    ClientFormState State;

    JPanel mainPanel;
    GridBagLayout mainLayout;
    
    public ClientForm(SingleFrameApplication app) {
        super(app);
            
        TestClientForm tcf = new TestClientForm();
        Objects = tcf.getClientObjectCache();
        
        mainPanel = new JPanel();
        mainLayout = new GridBagLayout();
        mainPanel.setLayout(mainLayout);

        JLabel label = new JLabel();
        label.setText("Hello World");
        
        CreateViewObjects();
        
        setComponent(mainPanel);
        
    }
    
    Map<ClientGroupObjectImplement, JTable> ObjectGrids;
    Map<ClientGroupObjectImplement, JPanel> ObjectPanels;
    
    Map<ClientPropertyView, JPanel> PropertyPanels;
    
    protected void CreateViewObjects() {
        
        Iterator<ClientGroupObjectImplement> ig = Objects.GroupObjects.iterator();
        
        ObjectGrids = new HashMap();
        ObjectPanels = new HashMap();
        
        while (ig.hasNext()) {
            
            ClientGroupObjectImplement GroupObject = ig.next();
            
            JTable Grid = new JTable();
            JPanel Panel = new JPanel();
            
            ObjectPanels.put(GroupObject, Panel);
            ObjectGrids.put(GroupObject, Grid);
            
            mainPanel.add(Panel, GroupObject.PanelConstraint);
            mainPanel.add(Grid, GroupObject.GridConstraint);
            
        }
        
    }
    
    protected void ApplyFormChanges(ClientFormChanges FormChanges) {
        
        // Сначала меняем виды объектов
    
        Iterator<ClientPropertyView> ip = FormChanges.PanelProperties.keySet().iterator();
        while (ip.hasNext())
        {
            ClientPropertyView Property = ip.next();
            
            JPanel GroupObjectPanel = ObjectPanels.get(Property.GroupObject);
            
            JPanel PropertyPanel = PropertyPanels.get(Property);
            if (PropertyPanel == null)
            {
                PropertyPanel = new ClientPropertyPanel();
                PropertyPanels.put(Property, PropertyPanel);
           
                GroupObjectPanel.add(PropertyPanel, Property.PanelConstraint);
            }
        }
            
        // Затем подгружаем новые данные
    
        
    }
    
    protected void PlaceViewObjects() {
        
    }
    
}


class ClientPropertyPanel extends JPanel {
    
    
    
}
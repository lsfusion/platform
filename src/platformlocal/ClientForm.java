/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package platformlocal;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.TableColumn;
import org.jdesktop.application.FrameView;
import org.jdesktop.application.SingleFrameApplication;


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

public class ClientForm extends FrameView {

    ClientFormInit Objects;

    JPanel mainPanel;
    GridBagLayout mainLayout;
    
    public ClientForm(SingleFrameApplication app) {
        super(app);

        mainPanel = new JPanel();
        mainLayout = new GridBagLayout();
        mainPanel.setLayout(mainLayout);

        setComponent(mainPanel);

        TestClientForm tcf = new TestClientForm();
        Objects = tcf.getClientObjectCache();
        CreateViewObjects();
        
        ApplyFormChanges(tcf.getFormChanges());
        
    }
    
    Map<ClientGroupObjectImplement, JTable> ObjectGrids;
    Map<ClientGroupObjectImplement, JPanel> ObjectPanels;
    
    Map<ClientPropertyView, ClientPropertyPanel> PropertyPanels;
    Map<ClientPropertyView, TableColumn> PropertyColumns;
    
    protected void CreateViewObjects() {
        
        ObjectGrids = new HashMap();
        ObjectPanels = new HashMap();
        
        PropertyPanels = new HashMap();
        PropertyColumns = new HashMap();

        Iterator<ClientGroupObjectImplement> ig = Objects.GroupObjects.iterator();

        int count = 0;
        
        while (ig.hasNext()) {
            
            ClientGroupObjectImplement GroupObject = ig.next();
            
            JTable Grid = new JTable();
            JPanel Panel = new JPanel();
            
            count++;
            Panel.setBackground(new Color((count * 50)%255,((count+1) * 160)%255,((count+2) * 50)%255));

            Panel.setLayout(new GridBagLayout());
            
            ObjectPanels.put(GroupObject, Panel);
            ObjectGrids.put(GroupObject, Grid);
            
            mainPanel.add(Panel, GroupObject.PanelConstraint);
            mainPanel.add(Grid, GroupObject.GridConstraint);
            
        }
        
    }
    
    void ApplyFormChanges(ClientFormChanges FormChanges) {
        
        // Сначала меняем виды объектов
    
        Iterator<ClientPropertyView> ip = FormChanges.PanelProperties.keySet().iterator();
        while (ip.hasNext())
        {
            ClientPropertyView Property = ip.next();
            
            JPanel GroupObjectPanel = ObjectPanels.get(Property.GroupObject);
            
            ClientPropertyPanel PropertyPanel = PropertyPanels.get(Property);
            if (PropertyPanel == null)
            {
                PropertyPanel = new ClientPropertyPanel();
                PropertyPanel.setLabel(Property.Caption);
                        
                PropertyPanels.put(Property, PropertyPanel);
           
                GroupObjectPanel.add(PropertyPanel, Property.PanelConstraint);
            }
       }

       ip = FormChanges.GridProperties.keySet().iterator();
       while (ip.hasNext())
       {
            ClientPropertyView Property = ip.next();
            
            JTable GroupObjectGrid = ObjectGrids.get(Property.GroupObject);
            
            TableColumn PropertyColumn = PropertyColumns.get(Property);
            if (PropertyColumn == null)
            {
                PropertyColumn = new TableColumn();
                PropertyColumn.setHeaderValue(Property.Caption);
                        
                PropertyColumns.put(Property, PropertyColumn);
           
                GroupObjectGrid.addColumn(PropertyColumn);
            }
       }

       ip = FormChanges.DropProperties.iterator();
       while (ip.hasNext())
       {
           ClientPropertyView Property = ip.next();

           JPanel GroupObjectPanel = ObjectPanels.get(Property.GroupObject);
           JTable GroupObjectGrid = ObjectGrids.get(Property.GroupObject);

           ClientPropertyPanel PropertyPanel = PropertyPanels.get(Property);
           TableColumn PropertyColumn = PropertyColumns.get(Property);
           
           GroupObjectPanel.remove(PropertyPanel);
           PropertyPanels.remove(Property);

           GroupObjectGrid.removeColumn(PropertyColumn);
           PropertyColumns.remove(Property);
       }

        // Затем подгружаем новые данные
        
    }
    
    protected void PlaceViewObjects() {
        
    }
    
}


class ClientPropertyPanel extends JPanel {
    
    JLabel Label;
    JTextField TextField;
    
    public void setLabel(String str) {
        Label.setText(str);
    }
    
    public void setTextField(Object obj) {
        TextField.setText(obj.toString());
    }
    
    public ClientPropertyPanel() {
        
        setLayout(new GridBagLayout());
        
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        c.insets = new Insets(4,4,4,4);
        
        Label = new JLabel();
        c.gridx = 0;
        add(Label, c);
        
        TextField = new JTextField();
        c.gridx = 1;
        add(TextField, c);
        
    }
    
}
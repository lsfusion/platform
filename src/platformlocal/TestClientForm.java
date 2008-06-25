/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package platformlocal;

import java.awt.GridBagConstraints;

/**
 *
 * @author NewUser
 */
public class TestClientForm {

    ClientGroupObjectImplement goDocument, goGood;
    ClientObjectImplement Document, Good;
    
    ClientGroupPropertyView gpMain;
    
    ClientPropertyView DocName, GoodName, GoodQuantity;
    
    ClientFormInit getClientObjectCache() {
        
        ClientFormInit cache = new ClientFormInit();
        
        goDocument = new ClientGroupObjectImplement();
        goDocument.GID = 1;
        cache.GroupObjects.add(goDocument);

        goGood = new ClientGroupObjectImplement();
        goGood.GID = 2;
        cache.GroupObjects.add(goGood);
   
        Document = new ClientObjectImplement();
        Document.GID = 3;
        Document.GroupObject = goDocument;
        cache.Objects.add(Document);
        
        Good = new ClientObjectImplement();
        Good.GID = 4;
        Good.GroupObject = goGood;
        cache.Objects.add(Good);
        
        gpMain = new ClientGroupPropertyView();
        cache.GroupProperties.add(gpMain);
        
        DocName = new ClientPropertyView();
        DocName.GID = 5;
        DocName.GroupProperty = gpMain;
        DocName.GroupObject = goDocument;
        DocName.Caption = "Номер документа";
        cache.Properties.add(DocName);

        GoodName = new ClientPropertyView();
        GoodName.GID = 6;
        GoodName.GroupProperty = gpMain;
        GoodName.GroupObject = goGood;
        GoodName.Caption = "Наименование товара";
        cache.Properties.add(GoodName);
        
        GoodQuantity = new ClientPropertyView();
        GoodQuantity.GID = 6;
        GoodQuantity.GroupProperty = gpMain;
        GoodQuantity.GroupObject = goGood;
        GoodQuantity.Caption = "Кол-во товара";
        cache.Properties.add(GoodQuantity);

        // отрисовка

        goDocument.PanelConstraint = generatePanelConstraint(0);
        goDocument.GridConstraint = generatePanelConstraint(1);
        goGood.PanelConstraint = generatePanelConstraint(2);
        goGood.GridConstraint = generatePanelConstraint(3);
        
        DocName.PanelConstraint = generatePropConstraint(0);
        GoodName.PanelConstraint = generatePropConstraint(1);
        GoodQuantity.PanelConstraint = generatePropConstraint(2);
        
        return cache;
        
    }
    
    
    ClientFormChanges getFormChanges() {
        
        ClientFormChanges changes = new ClientFormChanges();
        
        changes.PanelProperties.put(DocName, "");
        changes.PanelProperties.put(GoodName, "");
        changes.PanelProperties.put(GoodQuantity, 0);
        
        return changes;
        
    }

    private GridBagConstraints generatePanelConstraint(int i) {

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1;
        c.weighty = 1;
        c.gridy = i;
        
        return c;
    }

    private GridBagConstraints generatePropConstraint(int i) {
        
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        c.gridx = i;
        
        return c;
        
    }
    
}

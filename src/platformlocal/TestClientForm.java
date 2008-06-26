/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package platformlocal;

import java.awt.GridBagConstraints;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author NewUser
 */
public class TestClientForm {

    ClientGroupObjectImplement goDocument, goGood;
    ClientObjectImplement Document, Good;
    
    ClientGroupPropertyView gpMain;
    
    ClientPropertyView DocName, GoodName, GoodQuantity;
    
    ArrayList<ClientGroupObjectValue> Docs, Goods;
    
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

        goDocument.PanelConstraint = generatePanelConstraint(0,0.0);
        goDocument.GridConstraint = generatePanelConstraint(1,1.0);
        goGood.PanelConstraint = generatePanelConstraint(2,0.0);
        goGood.GridConstraint = generatePanelConstraint(3,1.0);
        
        DocName.PanelConstraint = generatePropConstraint(0);
        GoodName.PanelConstraint = generatePropConstraint(1);
        GoodQuantity.PanelConstraint = generatePropConstraint(2);
        
        return cache;
        
    }
    
    
    ClientFormChanges getFormChanges() {
        
        ClientFormChanges changes = new ClientFormChanges();

        Map<ClientGroupObjectValue,Object> PropValue;
        
        Docs = new ArrayList();
        Docs.add(new ClientGroupObjectValue());
        Docs.add(new ClientGroupObjectValue());
        
        changes.GridObjects.put(goDocument, Docs);
        
        PropValue = new HashMap();
        PropValue.put(Docs.get(0), "НД1");
        PropValue.put(Docs.get(1), "НД2");
        changes.GridProperties.put(DocName, PropValue);

        Goods = new ArrayList();
        Goods.add(new ClientGroupObjectValue());
        Goods.add(new ClientGroupObjectValue());
        Goods.add(new ClientGroupObjectValue());
        
        changes.GridObjects.put(goGood, Goods);
        
        PropValue = new HashMap();
        PropValue.put(Goods.get(0), "Молоко 1.5%");
        PropValue.put(Goods.get(1), "Колбаса прима");
        PropValue.put(Goods.get(2), "Сок юнит");
        changes.GridProperties.put(GoodName, PropValue);

        PropValue = new HashMap();
        PropValue.put(Goods.get(0), 10);
        PropValue.put(Goods.get(1), 20);
        PropValue.put(Goods.get(2), 3);
        changes.GridProperties.put(GoodQuantity, PropValue);
        
//        Docs.get(0).
        
        
        changes.PanelProperties.put(DocName, "СА324235");
        changes.PanelProperties.put(GoodName, "Молоко 1.5%");
        changes.PanelProperties.put(GoodQuantity, 5);
        
/*        changes.GridProperties.put(DocName, new HashMap());
        changes.GridProperties.put(GoodName, new HashMap());
        changes.GridProperties.put(GoodQuantity, new HashMap());*/
        
        return changes;
        
    }

    private GridBagConstraints generatePanelConstraint(int i, double weighty) {

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1;
        c.weighty = weighty;
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

package platform.server.logics.linear;

import platform.interop.ClassViewType;
import platform.server.data.where.classes.ClassWhere;
import platform.server.form.entity.PropertyObjectEntity;
import platform.server.form.entity.PropertyObjectInterfaceEntity;
import platform.server.logics.DataObject;
import platform.server.logics.panellocation.PanelLocation;
import platform.server.logics.property.Property;
import platform.server.logics.property.PropertyInterface;

import javax.swing.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class LP<T extends PropertyInterface, P extends Property<T>> {

    public P property;
    public List<T> listInterfaces;
    private String creationScript = null;
    private String creationPath = null;

    public <IT extends PropertyInterface> boolean intersect(LP<IT, ?> lp) {
        assert listInterfaces.size()==lp.listInterfaces.size();
        Map<IT,T> map = new HashMap<IT,T>();
        for(int i=0;i<listInterfaces.size();i++)
            map.put(lp.listInterfaces.get(i),listInterfaces.get(i));
        return property.intersect(lp.property,map);
    }

    public LP(P property) {
        this.property = property;
        listInterfaces = new ArrayList<T>(property.interfaces);
    }

    public LP(P property, List<T> listInterfaces) {
        this.property = property;
        this.listInterfaces = listInterfaces;
    }

    public Map<T, DataObject> getMapValues(DataObject... objects) {
        Map<T, DataObject> mapValues = new HashMap<T, DataObject>();
        for(int i=0;i<listInterfaces.size();i++)
            mapValues.put(listInterfaces.get(i),objects[i]);
        return mapValues;
    }

    public ClassWhere<Integer> getClassWhere() {
        ClassWhere<T> classWhere = property.getClassWhere();
        Map<T, Integer> mapping = new HashMap<T, Integer>();
        for (int i = 0; i < listInterfaces.size(); i++)
            mapping.put(listInterfaces.get(i), i+1);
        return new ClassWhere<Integer>(classWhere, mapping);
    }

    public void setMinimumWidth(int charWidth) {
        property.minimumCharWidth = charWidth;
    }

    public void setPreferredWidth(int charWidth) {
        property.preferredCharWidth = charWidth;
    }

    public void setMaximumWidth(int charWidth) {
        property.maximumCharWidth = charWidth;
    }

    public <U> Map<T, U> getMap(U... mapping) {
        Map<T,U> propertyMapping = new HashMap<T, U>();
        for(int i=0;i<listInterfaces.size();i++)
            propertyMapping.put(listInterfaces.get(i), mapping[i]);
        return propertyMapping;
    }

    public <U> Map<T, U> getMap(List<U> mapping) {
        Map<T,U> propertyMapping = new HashMap<T, U>();
        for(int i=0;i<listInterfaces.size();i++)
            propertyMapping.put(listInterfaces.get(i), mapping.get(i));
        return propertyMapping;
    }
    /*
    public <L extends PropertyInterface> void follows(LP<L> lp, int... mapping) {
        Map<L, T> mapInterfaces = new HashMap<L, T>();
        for(int i=0;i<lp.listInterfaces.size();i++)
            mapInterfaces.put(lp.listInterfaces.get(i), listInterfaces.get(mapping[i]-1));
        property.addFollows(new CalcPropertyMapImplement<L, T>(lp.property, mapInterfaces));
    }

    public void followed(LP... lps) {
        int[] mapping = new int[listInterfaces.size()];
        for(int i=0;i<mapping.length;i++)
            mapping[i] = i+1;
        for(LP lp : lps)
            lp.follows(this, mapping);
    }
    */
    
    public void setMinimumCharWidth(int charWidth) {
        property.minimumCharWidth = charWidth;
    }

    public void setPreferredCharWidth(int charWidth) {
        property.preferredCharWidth = charWidth;
    }

    public void setMaximumCharWidth(int charWidth) {
        property.maximumCharWidth = charWidth;
    }

    public void setFixedCharWidth(int charWidth) {
        property.setFixedCharWidth(charWidth);
    }

    public void setLoggable(boolean loggable) {
        property.loggable = loggable;
    }

    public void setLogFormProperty(LAP logFormPropertyProp) {
        property.setLogFormProperty(logFormPropertyProp);
    }

    public void setImage(String name) {
        property.setImage(name);
    }

    public void setEditKey(KeyStroke editKey) {
        property.editKey = editKey;
    }

    public void setShowEditKey(boolean showEditKey) {
        property.showEditKey = showEditKey;
    }

    public void setRegexp(String regexp) {
        property.regexp = regexp;
    }

    public void setRegexpMessage(String regexpMessage) {
        property.regexpMessage = regexpMessage;
    }

    public void setEchoSymbols(boolean echoSymbols) {
        property.echoSymbols = echoSymbols;
    }

    public void setPanelLocation(PanelLocation panelLocation) {
        property.panelLocation = panelLocation;
    }

    public void setShouldBeLast(boolean shouldBeLast) {
        property.shouldBeLast = shouldBeLast;
    }

    public void setForceViewType(ClassViewType forceViewType) {
        property.forceViewType = forceViewType;
    }

    public void setAskConfirm(boolean askConfirm) {
        property.askConfirm = askConfirm;
    }

    public String getCreationScript() {
        return creationScript;
    }

    public void setCreationScript(String creationScript) {
        this.creationScript = creationScript;
    }

    public String getCreationPath() {
        return creationPath;
    }

    public void setCreationPath(String creationPath) {
        this.creationPath = creationPath;
    }

    public PropertyObjectEntity<T, ?> createObjectEntity(PropertyObjectInterfaceEntity... objects) {
        return PropertyObjectEntity.create(property, getMap(objects), creationScript, creationPath);
    }
}

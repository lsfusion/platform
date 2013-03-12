package platform.server.logics.linear;

import platform.base.col.interfaces.immutable.ImList;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.immutable.ImOrderSet;
import platform.base.col.interfaces.immutable.ImRevMap;
import platform.base.col.interfaces.mutable.mapvalue.GetIndex;
import platform.interop.ClassViewType;
import platform.server.data.where.classes.ClassWhere;
import platform.server.form.entity.PropertyObjectEntity;
import platform.server.form.entity.PropertyObjectInterfaceEntity;
import platform.server.logics.DataObject;
import platform.server.logics.property.Property;
import platform.server.logics.property.PropertyInterface;

import javax.swing.*;

public abstract class LP<T extends PropertyInterface, P extends Property<T>> {

    public P property;
    public ImOrderSet<T> listInterfaces;
    private String creationScript = null;
    private String creationPath = null;

    public <IT extends PropertyInterface> boolean intersect(LP<IT, ?> lp) {
        assert listInterfaces.size()==lp.listInterfaces.size();
        return property.intersect(lp.property,lp.getRevMap(listInterfaces));
    }

    public LP(P property) {
        this.property = property;
        listInterfaces = property.getOrderInterfaces();
    }

    public LP(P property, ImOrderSet<T> listInterfaces) {
        this.property = property;
        this.listInterfaces = listInterfaces;
    }

    public ImMap<T, DataObject> getMapValues(final DataObject... objects) {
        return getMap(objects);
    }

    public ClassWhere<Integer> getClassWhere() {
        ClassWhere<T> classWhere = property.getClassWhere();
        ImRevMap<T,Integer> mapping = listInterfaces.mapOrderRevValues(new GetIndex<Integer>() {
            public Integer getMapValue(int i) {
                return i+1;
            }});
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

    public <U> ImMap<T, U> getMap(final U... mapping) {
        return listInterfaces.mapOrderValues(new GetIndex<U>() {
            public U getMapValue(int i) {
                return mapping[i];
            }});
    }

    public <U> ImMap<T, U> getMap(final ImList<U> mapping) {
        return listInterfaces.mapOrderValues(new GetIndex<U>() {
            public U getMapValue(int i) {
                return mapping.get(i);
            }});
    }

    public <U> ImRevMap<T, U> getRevMap(final U... mapping) {
        return listInterfaces.mapOrderRevValues(new GetIndex<U>() {
            public U getMapValue(int i) {
                return mapping[i];
            }});
    }

    public <U> ImRevMap<T, U> getRevMap(final ImOrderSet<U> mapping) {
        return listInterfaces.mapOrderRevValues(new GetIndex<U>() {
            public U getMapValue(int i) {
                return mapping.get(i);
            }});
    }

    public <U> ImRevMap<T, U> getRevMap(final ImOrderSet<U> list, final Integer... mapping) {
        return listInterfaces.mapOrderRevValues(new GetIndex<U>() {
            public U getMapValue(int i) {
                return list.get(mapping[i] - 1);
            }});
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

    public void setDrawToToolbar(boolean drawToToolbar) {
        property.drawToToolbar = drawToToolbar;
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

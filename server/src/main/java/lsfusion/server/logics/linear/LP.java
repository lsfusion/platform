package lsfusion.server.logics.linear;

import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetIndex;
import lsfusion.interop.ClassViewType;
import lsfusion.server.classes.sets.ResolveClassSet;
import lsfusion.server.form.entity.PropertyObjectEntity;
import lsfusion.server.form.entity.PropertyObjectInterfaceEntity;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.PropertyInterface;

import javax.swing.*;
import java.util.List;

public abstract class LP<T extends PropertyInterface, P extends Property<T>> {

    public P property;
    public ImOrderSet<T> listInterfaces;
    private String creationScript = null;
    private String creationPath = null;

    public LP(P property) {
        this.property = property;
        listInterfaces = property.getFriendlyPropertyOrderInterfaces();
    }

    public LP(P property, ImOrderSet<T> listInterfaces) {
        this.property = property;
        this.listInterfaces = listInterfaces;
    }

    public ImMap<T, ObjectValue> getMapValues(final ObjectValue... objects) {
        return getMap(objects);
    }

    public ImMap<T, DataObject> getMapDataValues(final DataObject... objects) {
        return getMap(objects);
    }

    public void setMinimumWidth(int charWidth) {
        property.drawOptions.setMinimumCharWidth(charWidth);
    }

    public void setPreferredWidth(int charWidth) {
        property.drawOptions.setPreferredCharWidth(charWidth);
    }

    public void setMaximumWidth(int charWidth) {
        property.drawOptions.setMaximumCharWidth(charWidth);
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
        property.drawOptions.setMinimumCharWidth(charWidth);
    }

    public void setPreferredCharWidth(int charWidth) {
        property.drawOptions.setPreferredCharWidth(charWidth);
    }

    public void setMaximumCharWidth(int charWidth) {
        property.drawOptions.setMaximumCharWidth(charWidth);
    }

    public void setFixedCharWidth(int charWidth) {
        property.drawOptions.setFixedCharWidth(charWidth);
    }

    public void setImage(String name) {
        property.drawOptions.setImage(name);
    }

    public void setEditKey(KeyStroke editKey) {
        property.drawOptions.setEditKey(editKey);
    }

    public void setShowEditKey(boolean showEditKey) {
        property.drawOptions.setShowEditKey(showEditKey);
    }

    public void setRegexp(String regexp) {
        property.drawOptions.setRegexp(regexp);
    }

    public void setRegexpMessage(String regexpMessage) {
        property.drawOptions.setRegexpMessage(regexpMessage);
    }

    public void setEchoSymbols(boolean echoSymbols) {
        property.drawOptions.setEchoSymbols(echoSymbols);
    }

    public void setDrawToToolbar(boolean drawToToolbar) {
        property.drawOptions.setDrawToToolbar(drawToToolbar);
    }

    public void setShouldBeLast(boolean shouldBeLast) {
        property.drawOptions.setShouldBeLast(shouldBeLast);
    }

    public void setForceViewType(ClassViewType forceViewType) {
        property.drawOptions.setForceViewType(forceViewType);
    }

    public void setAskConfirm(boolean askConfirm) {
        property.drawOptions.setAskConfirm(askConfirm);
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

    public void setExplicitClasses(List<ResolveClassSet> signature) {
        property.setExplicitClasses(listInterfaces, signature);
    }

    @Override
    public String toString() {
        return property.toString();
    }
}

package lsfusion.server.language.property.oraction;

import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetIndex;
import lsfusion.interop.form.property.ClassViewType;
import lsfusion.server.data.DataObject;
import lsfusion.server.data.ObjectValue;
import lsfusion.server.logics.classes.user.set.ResolveClassSet;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.form.struct.property.ActionOrPropertyObjectEntity;
import lsfusion.server.logics.property.oraction.ActionOrProperty;
import lsfusion.server.logics.property.oraction.PropertyInterface;

import javax.swing.*;
import java.util.List;

public abstract class LAP<T extends PropertyInterface, P extends ActionOrProperty<T>> {

    public P property;
    public ImOrderSet<T> listInterfaces;
    private String creationScript = null;
    private String creationPath = null;

    public LAP(P property) {
        this.property = property;
        listInterfaces = property.getFriendlyOrderInterfaces();
    }

    public LAP(P property, ImOrderSet<T> listInterfaces) {
        this.property = property;
        this.listInterfaces = listInterfaces;
        assert property.interfaces.size() == listInterfaces.size();
    }

    public ImMap<T, ObjectValue> getMapValues(final ObjectValue... objects) {
        return getMap(objects);
    }

    public ImMap<T, DataObject> getMapDataValues(final DataObject... objects) {
        return getMap(objects);
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
    public <L extends PropertyInterface> void follows(LAP<L> lp, int... mapping) {
        Map<L, T> mapInterfaces = new HashMap<L, T>();
        for(int i=0;i<lp.listInterfaces.size();i++)
            mapInterfaces.put(lp.listInterfaces.get(i), listInterfaces.get(mapping[i]-1));
        property.addFollows(new PropertyMapImplement<L, T>(lp.property, mapInterfaces));
    }

    public void followed(LAP... lps) {
        int[] mapping = new int[listInterfaces.size()];
        for(int i=0;i<mapping.length;i++)
            mapping[i] = i+1;
        for(LAP lp : lps)
            lp.follows(this, mapping);
    }
    */
    
    public void setCharWidth(int charWidth) {
        property.drawOptions.setCharWidth(charWidth);
    }

    public void setFixedCharWidth(int charWidth) {
        property.drawOptions.setFixedCharWidth(charWidth);
    }

    public void setImage(String name) {
        property.drawOptions.setImage(name);
    }

    public void setDefaultCompare(String defaultCompare) {
        property.drawOptions.setDefaultCompare(defaultCompare);
    }

    public void setChangeKey(KeyStroke editKey) {
        property.drawOptions.setChangeKey(editKey);
    }

    public void setShowChangeKey(boolean showEditKey) {
        property.drawOptions.setShowChangeKey(showEditKey);
    }
    
    public void addProcessor(ActionOrProperty.DefaultProcessor processor) {
        property.drawOptions.addProcessor(processor);
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

    public ActionOrPropertyObjectEntity<T, ?> createObjectEntity(ImOrderSet<ObjectEntity> objects) {
        return ActionOrPropertyObjectEntity.create(property, getRevMap(objects), creationScript, creationPath);
    }

    public List<ResolveClassSet> getExplicitClasses() {
        return property.getExplicitClasses(listInterfaces);
    }

    public void setExplicitClasses(List<ResolveClassSet> signature) {
        property.setExplicitClasses(listInterfaces, signature);
    }

    @Override
    public String toString() {
        return property.toString();
    }
}

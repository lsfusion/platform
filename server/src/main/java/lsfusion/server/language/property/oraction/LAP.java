package lsfusion.server.language.property.oraction;

import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.interop.form.event.MouseInputEvent;
import lsfusion.interop.form.property.ClassViewType;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.logics.classes.user.set.ResolveClassSet;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.form.struct.property.oraction.ActionOrPropertyObjectEntity;
import lsfusion.server.logics.property.oraction.ActionOrProperty;
import lsfusion.server.logics.property.oraction.PropertyInterface;

import javax.swing.*;
import java.util.List;

public abstract class LAP<T extends PropertyInterface, P extends ActionOrProperty<T>> {

    public ImOrderSet<T> listInterfaces;
    private String creationScript = null;
    private String creationPath = null;

    public LAP(P property) {
        listInterfaces = property.getFriendlyOrderInterfaces();
    }

    public LAP(P property, ImOrderSet<T> listInterfaces) {
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
        return listInterfaces.mapOrderValues((int i) -> mapping[i]);
    }

    public <U> ImMap<T, U> getMap(final ImList<U> mapping) {
        return listInterfaces.mapOrderValues(mapping::get);
    }

    public <U> ImRevMap<T, U> getRevMap(final U... mapping) {
        return listInterfaces.mapOrderRevValues(i -> mapping[i]);
    }

    public <U> ImRevMap<T, U> getRevMap(final ImOrderSet<U> mapping) {
        return listInterfaces.mapOrderRevValues(mapping::get);
    }

    public <U> ImRevMap<T, U> getRevMap(final ImOrderSet<U> list, final Integer... mapping) {
        return listInterfaces.mapOrderRevValues(i -> list.get(mapping[i] - 1));
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
        getActionOrProperty().drawOptions.setCharWidth(charWidth);
    }

    public void setFlexCharWidth(int charWidth, Boolean flex) {
        getActionOrProperty().drawOptions.setFlexCharWidth(charWidth, flex);
    }

    public void setImage(String name) {
        getActionOrProperty().drawOptions.setImage(name);
    }

    public void setDefaultCompare(String defaultCompare) {
        getActionOrProperty().drawOptions.setDefaultCompare(defaultCompare);
    }

    public void setChangeKey(KeyStroke editKey) {
        getActionOrProperty().drawOptions.setChangeKey(editKey);
    }

    public void setShowChangeKey(boolean showEditKey) {
        getActionOrProperty().drawOptions.setShowChangeKey(showEditKey);
    }

    public void setChangeMouse(MouseInputEvent editMouse) {
        getActionOrProperty().drawOptions.setChangeMouse(editMouse);
    }

    public void setChangeMousePriority(Integer editMousePriority) {
        getActionOrProperty().drawOptions.setChangeMousePriority(editMousePriority);
    }
    
    public void addProcessor(ActionOrProperty.DefaultProcessor processor) {
        getActionOrProperty().drawOptions.addProcessor(processor);
    }

    public void setRegexp(String regexp) {
        getActionOrProperty().drawOptions.setRegexp(regexp);
    }

    public void setRegexpMessage(String regexpMessage) {
        getActionOrProperty().drawOptions.setRegexpMessage(regexpMessage);
    }

    public void setEchoSymbols(boolean echoSymbols) {
        getActionOrProperty().drawOptions.setEchoSymbols(echoSymbols);
    }

    public void setShouldBeLast(boolean shouldBeLast) {
        getActionOrProperty().drawOptions.setShouldBeLast(shouldBeLast);
    }

    public void setForceViewType(ClassViewType forceViewType) {
        getActionOrProperty().drawOptions.setForceViewType(forceViewType);
    }

    public void setAskConfirm(boolean askConfirm) {
        getActionOrProperty().drawOptions.setAskConfirm(askConfirm);
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
        return ActionOrPropertyObjectEntity.create(getActionOrProperty(), getRevMap(objects), creationScript, creationPath);
    }

    public List<ResolveClassSet> getExplicitClasses() {
        return getActionOrProperty().getExplicitClasses(listInterfaces);
    }

    public void setExplicitClasses(List<ResolveClassSet> signature) {
        getActionOrProperty().setExplicitClasses(listInterfaces, signature);
    }

    @Override
    public String toString() {
        return getActionOrProperty().toString();
    }

    public abstract P getActionOrProperty();
}

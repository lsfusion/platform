package platform.server.logics.property;

import platform.base.ListPermutations;
import platform.base.NotFunctionSet;
import platform.base.Pair;
import platform.base.col.ListFact;
import platform.base.col.MapFact;
import platform.base.col.SetFact;
import platform.base.col.interfaces.immutable.*;
import platform.base.col.interfaces.mutable.LongMutable;
import platform.base.col.interfaces.mutable.MExclMap;
import platform.base.col.interfaces.mutable.MList;
import platform.base.col.interfaces.mutable.MMap;
import platform.base.col.interfaces.mutable.mapvalue.GetIndex;
import platform.base.col.interfaces.mutable.mapvalue.GetIndexValue;
import platform.base.col.interfaces.mutable.mapvalue.GetValue;
import platform.interop.ClassViewType;
import platform.interop.form.ServerResponse;
import platform.server.Settings;
import platform.server.caches.IdentityLazy;
import platform.server.caches.ManualLazy;
import platform.server.classes.ActionClass;
import platform.server.classes.LogicalClass;
import platform.server.classes.ValueClass;
import platform.server.classes.sets.AndClassSet;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.where.cases.CaseExpr;
import platform.server.data.query.MapKeysInterface;
import platform.server.data.type.Type;
import platform.server.data.where.classes.AbstractClassWhere;
import platform.server.data.where.classes.ClassWhere;
import platform.server.form.entity.ActionPropertyObjectEntity;
import platform.server.form.entity.FormEntity;
import platform.server.form.entity.PropertyDrawEntity;
import platform.server.form.entity.PropertyObjectInterfaceEntity;
import platform.server.form.view.DefaultFormView;
import platform.server.form.view.PropertyDrawView;
import platform.server.logics.linear.LAP;
import platform.server.logics.linear.LP;
import platform.server.logics.panellocation.PanelLocation;
import platform.server.logics.panellocation.ShortcutPanelLocation;
import platform.server.logics.property.group.AbstractGroup;
import platform.server.logics.property.group.AbstractNode;
import platform.server.serialization.ServerIdentitySerializable;
import platform.server.serialization.ServerSerializationPool;
import platform.server.session.Modifier;
import platform.server.session.PropertyChange;
import platform.server.session.PropertyChanges;

import javax.swing.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.*;

public abstract class Property<T extends PropertyInterface> extends AbstractNode implements MapKeysInterface<T>, ServerIdentitySerializable {
    private String sID;
    private String name;
    // вот отсюда идут свойства, которые отвечают за логику представлений и подставляются автоматически для PropertyDrawEntity и PropertyDrawView

    public static final GetIndex<PropertyInterface> genInterface = new GetIndex<PropertyInterface>() {
        public PropertyInterface getMapValue(int i) {
            return new PropertyInterface(i);
        }};


    public String caption;
    public String toolTip;

    public int minimumCharWidth;
    public int maximumCharWidth;
    public int preferredCharWidth;

    public boolean loggable;
    public LAP logFormProperty;

    public void setFixedCharWidth(int charWidth) {
        minimumCharWidth = charWidth;
        maximumCharWidth = charWidth;
        preferredCharWidth = charWidth;
    }

    public void inheritFixedCharWidth(Property property) {
        minimumCharWidth = property.minimumCharWidth;
        maximumCharWidth = property.maximumCharWidth;
        preferredCharWidth = property.preferredCharWidth;
    }

    private ImageIcon image;
    private String iconPath;

    public void inheritImage(Property property) {
        image = property.image;
        iconPath = property.iconPath;
    }

    public void setImage(String iconPath) {
        this.iconPath = iconPath;
        this.image = new ImageIcon(Property.class.getResource("/images/" + iconPath));
    }

    public KeyStroke editKey;
    public Boolean showEditKey;

    public String regexp;
    public String regexpMessage;
    public Boolean echoSymbols;

    public PanelLocation panelLocation;

    public Boolean shouldBeLast;

    public ClassViewType forceViewType;

    public Boolean askConfirm;
    public String askConfirmMessage;

    public String eventID;

    public String toString() {
        return caption + " (" + sID + ")";
    }

    public int ID = 0;

    public String getCode() {
        return getSID();
    }

    public boolean isField() {
        return false;
    }

    public int getID() {
        return ID;
    }

    public void setID(int iID) {
        ID = iID;
    }

    public LP getLogFormProperty() {
        return logFormProperty;
    }

    public void setLogFormProperty(LAP logFormProperty) {
        this.logFormProperty = logFormProperty;
    }


    public Type getType() {
        return getValueClass().getType();
    }

    public abstract ValueClass getValueClass();

    public ValueClass[] getInterfaceClasses(ImOrderSet<T> listInterfaces) {
        return listInterfaces.mapOrder(getInterfaceClasses()).toArray(new ValueClass[listInterfaces.size()]);
    }
    public ImMap<T, ValueClass> getInterfaceClasses() {
        return getInterfaceClasses(false);
    }
    public abstract ImMap<T, ValueClass> getInterfaceClasses(boolean full);
    public ClassWhere<T> getClassWhere() {
        return getClassWhere(false);
    }
    public abstract ClassWhere<T> getClassWhere(boolean full);

    public boolean check() {
        return !getClassWhere().isFalse();
    }

    public <P extends PropertyInterface> boolean intersect(Property<P> property, ImRevMap<P, T> map) {
        return !getClassWhere().and(new ClassWhere<T>(property.getClassWhere(), map)).isFalse();
    }

    @IdentityLazy
    public boolean cacheIsInInterface(ImMap<T, ? extends AndClassSet> interfaceClasses, boolean isAny) { // для всех подряд свойств не имеет смысла
        return isInInterface(interfaceClasses, isAny);
    }

    public boolean isInInterface(ImMap<T, ? extends AndClassSet> interfaceClasses, boolean isAny) {
        return isAny ? anyInInterface(interfaceClasses) : allInInterface(interfaceClasses);
    }

    private boolean allInInterface(ImMap<T, ? extends AndClassSet> interfaceClasses) {
        return new ClassWhere<T>(interfaceClasses).meansCompatible(getClassWhere(true));
    }

    private boolean anyInInterface(ImMap<T, ? extends AndClassSet> interfaceClasses) {
        return !getClassWhere(true).andCompatible(new ClassWhere<T>(interfaceClasses)).isFalse();
    }

    public boolean isFull(ImCol<T> checkInterfaces) {
        ClassWhere<T> classWhere = getClassWhere();
        if(classWhere.isFalse())
            return false;
        for (AbstractClassWhere.And<T> where : classWhere.wheres) {
            for (T i : checkInterfaces)
                if(where.get(i)==null)
                    return false;
        }
        return true;
    }
    
    private boolean calculateIsFull() {
        return isFull(interfaces);
    }
    private Boolean isFull;
    private static ThreadLocal<Boolean> isFullRunning = new ThreadLocal<Boolean>();
    @ManualLazy
    public boolean isFull() {
        if(isFull==null) {
            if(isFullRunning.get()!=null)
                return false;
            isFullRunning.set(true);

            try {
            isFull = calculateIsFull();
            } finally {
                isFullRunning.set(null);
            }
        }
        return isFull;
    }

    public Property(String sID, String caption, ImOrderSet<T> interfaces) {
        this.setSID(sID);
        this.caption = caption;
        this.interfaces = interfaces.getSet();
        this.orderInterfaces = interfaces;
    }

    public final ImSet<T> interfaces;
    private final ImOrderSet<T> orderInterfaces;
    public ImOrderSet<T> getOrderInterfaces() {
        return orderInterfaces;
    }

    @IdentityLazy
    public ImRevMap<T, KeyExpr> getMapKeys() {
        return KeyExpr.getMapKeys(interfaces);
    }

    public static Modifier defaultModifier = new Modifier() {
        public PropertyChanges getPropertyChanges() {
            return PropertyChanges.EMPTY;
        }
    };

    @IdentityLazy
    public Type getInterfaceType(T propertyInterface) { // true потому как может быть old не полный (в частности NewSessionAction)
        return getInterfaceClasses(true).get(propertyInterface).getType();
    }

    public String getSID() {
        return sID;
    }

    private boolean canChangeSID = true;

    public void setSID(String sID) {
        if (canChangeSID) {
            this.sID = sID;
        } else {
            throw new RuntimeException(String.format("Can't change property SID [%s] after freezing", sID));
        }
    }

    public void freezeSID() {     // todo [dale]: Отрефакторить установку SID
        canChangeSID = false;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean cached = false;

    private Object editActions = MapFact.mMap(MapFact.override());

    @LongMutable
    private ImMap<String, ActionPropertyMapImplement<?, T>> getEditActions() {
        return (ImMap<String, ActionPropertyMapImplement<?, T>>)editActions;
    }

    public void setEditAction(String editActionSID, ActionPropertyMapImplement<?, T> editActionImplement) {
        ((MMap<String, ActionPropertyMapImplement<?, T>>)editActions).add(editActionSID, editActionImplement);
    }

    public ActionPropertyMapImplement<?, T> getEditAction(String editActionSID) {
        return getEditAction(editActionSID, null);
    }

    public ActionPropertyMapImplement<?, T> getEditAction(String editActionSID, CalcProperty filterProperty) {
        ActionPropertyMapImplement<?, T> editAction = getEditActions().get(editActionSID);
        if (editAction != null) {
            return editAction;
        }

        if (editActionSID.equals(ServerResponse.GROUP_CHANGE)) {
            //будем определять на уровне PropertyDraw
            assert false;
        } else if (editActionSID.equals(ServerResponse.CHANGE_WYS)) {
//            возвращаем дефолт
        }

        return getDefaultEditAction(editActionSID, filterProperty);
    }

    public abstract ActionPropertyMapImplement<?, T> getDefaultEditAction(String editActionSID, CalcProperty filterProperty);

    public boolean checkEquals() {
        return this instanceof CalcProperty;
    }

    public ImRevMap<T, T> getIdentityInterfaces() {
        return interfaces.toRevMap();
    }

    // по умолчанию заполняет свойства
    // assert что entity этого свойства
    public void proceedDefaultDraw(PropertyDrawEntity<T> entity, FormEntity<?> form) {
        if (loggable && logFormProperty != null) {
            ActionPropertyObjectEntity logFormPropertyObject =
                    form.addPropertyObject(logFormProperty, getOrderInterfaces().mapOrderMap(entity.propertyObject.mapping).valuesList().toArray(new PropertyObjectInterfaceEntity[interfaces.size()]));
            entity.setContextMenuEditAction(logFormProperty.property.caption, logFormProperty.property.getSID(), logFormPropertyObject);
        }

        if (shouldBeLast != null)
            entity.shouldBeLast = shouldBeLast;
        if (forceViewType != null)
            entity.forceViewType = forceViewType;
        if (askConfirm != null)
            entity.askConfirm = askConfirm;
        if (askConfirmMessage != null)
            entity.askConfirmMessage = askConfirmMessage;
        if (eventID != null)
            entity.eventID = eventID;
        if (panelLocation != null && panelLocation.isToolbarLocation()) {
            entity.setDrawToToolbar(true);
        }

        //перемещаем свойство в контекстном меню в тот же groupObject, что и свойство, к которому оно привязано
        if (panelLocation != null && panelLocation.isShortcutLocation()) {
            //todo: пока просто скрываем это свойство ... в будущем надо сделать, чтобы его вообще не нужно было добавлять в форму...
            entity.forceViewType = ClassViewType.HIDE;

            ShortcutPanelLocation shortcutLocation = (ShortcutPanelLocation) panelLocation;

            Property onlyProperty = shortcutLocation.getOnlyProperty();

            if (onlyProperty != null) {
                for (PropertyDrawEntity drawEntity : form.getProperties(onlyProperty)) {
                    if (drawEntity.toDraw != null) {
                        entity.toDraw = drawEntity.toDraw;
                    }

                    //добавляем в контекстное меню...
                    if (shortcutLocation.isDefault()) {
                        drawEntity.setContextMenuAction(caption, ServerResponse.CHANGE);
                        drawEntity.setEditAction(ServerResponse.CHANGE, (ActionPropertyObjectEntity<T>) entity.propertyObject);
                    } else {
                        drawEntity.setContextMenuEditAction(caption, getSID(), (ActionPropertyObjectEntity<T>) entity.propertyObject);
                    }
                }
            }
        }
    }

    public void proceedDefaultDesign(PropertyDrawView propertyView, DefaultFormView view) {
        if (iconPath != null) {
            propertyView.design.iconPath = iconPath;
            propertyView.design.setImage(image);
        }

        if (editKey != null)
            propertyView.editKey = editKey;
        if (showEditKey != null)
            propertyView.showEditKey = showEditKey;
        if (regexp != null)
            propertyView.regexp = regexp;
        if (regexpMessage != null)
            propertyView.regexpMessage = regexpMessage;
        if (echoSymbols != null)
            propertyView.echoSymbols = echoSymbols;

        if(propertyView.getType() instanceof LogicalClass)
            propertyView.editOnSingleClick = Settings.instance.getEditLogicalOnSingleClick();
        if(propertyView.getType() instanceof ActionClass)
            propertyView.editOnSingleClick = Settings.instance.getEditActionClassOnSingleClick();
    }

    public boolean hasChild(Property prop) {
        return prop.equals(this);
    }

    public ImOrderSet<Property> getProperties() {
        return SetFact.singletonOrder((Property) this);
    }

    @Override
    public ImList<PropertyClassImplement> getProperties(ImCol<ImSet<ValueClassWrapper>> classLists, boolean anyInInterface) {
        MList<PropertyClassImplement> mResultList = ListFact.mList();
        if (isFull()) {
            for (ImSet<ValueClassWrapper> classes : classLists) {
                if (interfaces.size() == classes.size()) {
                    final ImOrderSet<ValueClassWrapper> orderClasses = classes.toOrderSet();
                    for (ImOrderSet<T> mapping : new ListPermutations<T>(getOrderInterfaces())) {
                        ImMap<T, AndClassSet> propertyInterface = mapping.mapOrderValues(new GetIndexValue<AndClassSet, T>() {
                            public AndClassSet getMapValue(int i, T value) {
                                return orderClasses.get(i).valueClass.getUpSet();
                            }});
                        if (isInInterface(propertyInterface, anyInInterface)) {
                            mResultList.add(createClassImplement(orderClasses, mapping));
                        }
                    }
                }
            }
        }
        return mResultList.immutableList();
    }
    
    protected abstract PropertyClassImplement<T, ?> createClassImplement(ImOrderSet<ValueClassWrapper> classes, ImOrderSet<T> mapping);

    @Override
    public Property getProperty(String sid) {
        return this.getSID().equals(sid) ? this : null;
    }

    public T getInterfaceById(int iID) {
        for (T inter : interfaces) {
            if (inter.getID() == iID) {
                return inter;
            }
        }

        return null;
    }

    public void customSerialize(ServerSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        outStream.writeUTF(getSID());
        outStream.writeUTF(caption);
        outStream.writeBoolean(toolTip != null);
        if (toolTip != null)
            outStream.writeUTF(toolTip);
        outStream.writeUTF(getCode());
        outStream.writeBoolean(isField());

        pool.serializeCollection(outStream, getOrderInterfaces().toJavaList());
        pool.serializeObject(outStream, getParent());
    }

    public void customDeserialize(ServerSerializationPool pool, DataInputStream inStream) throws IOException {
        //десериализация не нужна, т.к. вместо создания объекта, происходит поиск в BL
    }

    @Override
    public List<AbstractGroup> fillGroups(List<AbstractGroup> groupsList) {
        return groupsList;
    }

    protected boolean finalized = false;
    public void finalizeInit() {
        assert !finalized;
        finalized = true;
    }

    public void finalizeAroundInit() {
        links = null;
        editActions = ((MMap<String, ActionPropertyMapImplement<?, T>>)editActions).immutable();
    }

    @IdentityLazy
    public PropertyChange<T> getNoChange() {
        return new PropertyChange<T>(getMapKeys(), CaseExpr.NULL);
    }
    
    public abstract void prereadCaches();

    protected abstract ImCol<Pair<Property<?>, LinkType>> calculateLinks();

    private ImSet<Link> links;
    @ManualLazy
    public ImSet<Link> getLinks() { // чисто для лексикографики
        if(links==null) {
            links = calculateLinks().mapColSetValues(new GetValue<Link, Pair<Property<?>, LinkType>>() {
                public Link getMapValue(Pair<Property<?>, LinkType> value) {
                    return new Link(Property.this, value.first, value.second);
                }});
        }
        return links;
    }
    public abstract ImSet<SessionCalcProperty> getSessionCalcDepends();

    public ImSet<OldProperty> getOldDepends() {
        return getSessionCalcDepends().mapSetValues(new GetValue<OldProperty, SessionCalcProperty>() {
            public OldProperty getMapValue(SessionCalcProperty value) {
                return value.getOldProperty();
            }});
    }

    // не сильно структурно поэтому вынесено в метод
    public <V> ImRevMap<T, V> getMapInterfaces(final ImOrderSet<V> list) {
        return getOrderInterfaces().mapOrderRevValues(new GetIndexValue<V, T>() {
            public V getMapValue(int i, T value) {
                return list.get(i);
            }
        });
    }
}

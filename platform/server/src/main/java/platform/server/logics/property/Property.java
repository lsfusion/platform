package platform.server.logics.property;

import platform.base.BaseUtils;
import platform.base.ListPermutations;
import platform.base.Pair;
import platform.base.QuickSet;
import platform.interop.ClassViewType;
import platform.interop.PropertyEditType;
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
import platform.server.form.entity.*;
import platform.server.form.view.DefaultFormView;
import platform.server.form.view.PropertyDrawView;
import platform.server.form.view.panellocation.PanelLocationView;
import platform.server.form.view.panellocation.ShortcutPanelLocationView;
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

    public String caption;
    public String toolTip;

    public int minimumCharWidth;
    public int maximumCharWidth;
    public int preferredCharWidth;

    public boolean loggable;
    public LP logFormProperty;

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

    public void setLogFormProperty(LP logFormProperty) {
        this.logFormProperty = logFormProperty;
    }

    public final Collection<T> interfaces;

    public Type getType() {
        return getValueClass().getType();
    }

    public abstract ValueClass getValueClass();

    public ValueClass[] getInterfaceClasses(List<T> listInterfaces) {
        return BaseUtils.mapList(listInterfaces, getInterfaceClasses()).toArray(new ValueClass[listInterfaces.size()]);
    }
    public Map<T, ValueClass> getInterfaceClasses() {
        return getInterfaceClasses(false);
    }
    public abstract Map<T, ValueClass> getInterfaceClasses(boolean full);
    public ClassWhere<T> getClassWhere() {
        return getClassWhere(false);
    }
    public abstract ClassWhere<T> getClassWhere(boolean full);

    public boolean check() {
        return !getClassWhere().isFalse();
    }

    public <P extends PropertyInterface> boolean intersect(Property<P> property, Map<P, T> map) {
        return !getClassWhere().and(new ClassWhere<T>(property.getClassWhere(), map)).isFalse();
    }

    public boolean isInInterface(Map<T, ? extends AndClassSet> interfaceClasses, boolean isAny) {
        return isAny ? anyInInterface(interfaceClasses) : allInInterface(interfaceClasses);
    }

    @IdentityLazy
    private boolean allInInterface(Map<T, ? extends AndClassSet> interfaceClasses) {
        return new ClassWhere<T>(interfaceClasses).meansCompatible(getClassWhere(true));
    }

    @IdentityLazy
    private boolean anyInInterface(Map<T, ? extends AndClassSet> interfaceClasses) {
        return !getClassWhere(true).andCompatible(new ClassWhere<T>(interfaceClasses)).isFalse();
    }

    public boolean isFull(Collection<T> checkInterfaces) {
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

    public Property(String sID, String caption, List<T> interfaces) {
        this.setSID(sID);
        this.caption = caption;
        this.interfaces = interfaces;
    }

    @IdentityLazy
    public Map<T, KeyExpr> getMapKeys() {
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

    private Map<String, ActionPropertyMapImplement<?, T>> editActions = new HashMap<String, ActionPropertyMapImplement<?, T>>();

    public void setEditAction(String editActionSID, ActionPropertyMapImplement<?, T> editActionImplement) {
        editActions.put(editActionSID, editActionImplement);
    }

    public ActionPropertyMapImplement<?, T> getEditAction(String editActionSID) {
        return getEditAction(editActionSID, null);
    }

    public ActionPropertyMapImplement<?, T> getEditAction(String editActionSID, CalcProperty filterProperty) {
        ActionPropertyMapImplement<?, T> editAction = editActions.get(editActionSID);
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

    public Map<T, T> getIdentityInterfaces() {
        return BaseUtils.toMap(new HashSet<T>(interfaces));
    }

    // по умолчанию заполняет свойства
    // assert что entity этого свойства
    public void proceedDefaultDraw(PropertyDrawEntity<T> entity, FormEntity<?> form) {
        if (loggable && logFormProperty != null) {
            form.addPropertyDraw(logFormProperty, BaseUtils.orderMap(entity.propertyObject.mapping, interfaces).values().toArray(new PropertyObjectInterfaceEntity[0]));
            form.setForceViewType(logFormProperty, ClassViewType.PANEL);
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

        //перемещаем свойство в контекстном меню в тот же groupObject, что и свойство, к которому оно привязано
        if (panelLocation != null && panelLocation.isShortcutLocation() && ((ShortcutPanelLocation) panelLocation).getOnlyProperty() != null) {
            Property onlyProperty = ((ShortcutPanelLocation) panelLocation).getOnlyProperty();
            for (PropertyDrawEntity drawEntity : form.getProperties(onlyProperty)) {
                if (drawEntity.toDraw != null) {
                    entity.toDraw = drawEntity.toDraw;
                }

                //добавляем в контекстное меню...
                drawEntity.setContextMenuEditAction(caption, getSID(), (ActionPropertyObjectEntity<T>) entity.propertyObject);
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

        if (panelLocation != null) {
            PanelLocationView panelLocationView = panelLocation.convertToView();
            if (panelLocationView.isShortcutLocation()) {
                Property onlyProperty = ((ShortcutPanelLocation) panelLocation).getOnlyProperty();
                if (onlyProperty != null) {
                    for (PropertyDrawView prop : view.properties) {
                        if (prop.entity.propertyObject.property.equals(onlyProperty) &&
                        (view.getGroupObject(propertyView.entity.toDraw) == null || view.getGroupObject(propertyView.entity.toDraw).equals(view.getGroupObject(prop.entity.toDraw)))) {
                            ((ShortcutPanelLocationView) panelLocationView).setOnlyProperty(prop);
                            break;
                        }
                    }
                    if (((ShortcutPanelLocationView) panelLocationView).getOnlyProperty() == null)
                        panelLocationView = null;
                }
            }
            if (panelLocationView != null) {
                propertyView.entity.forceViewType = ClassViewType.PANEL;
                propertyView.setPanelLocation(panelLocationView);
            }
        }
        
        if(propertyView.getType() instanceof LogicalClass)
            propertyView.editOnSingleClick = Settings.instance.getEditLogicalOnSingleClick();
        if(propertyView.getType() instanceof ActionClass)
            propertyView.editOnSingleClick = Settings.instance.getEditActionClassOnSingleClick();

        if (loggable && logFormProperty != null) {
            PropertyDrawView logPropertyView = view.get(view.entity.getPropertyDraw(logFormProperty));
            GroupObjectEntity groupObject = propertyView.entity.getToDraw(view.entity);
            if (groupObject != null) {
                logPropertyView = BaseUtils.nvl(view.get(view.entity.getPropertyDraw(logFormProperty.property, groupObject)), logPropertyView);
            }
            if (logPropertyView != null) {
                logPropertyView.entity.setEditType(PropertyEditType.EDITABLE); //бывает, что проставляют READONLY для всего groupObject'а
                logPropertyView.setPanelLocation(new ShortcutPanelLocationView(propertyView));
            }
        }
    }

    public boolean hasChild(Property prop) {
        return prop.equals(this);
    }

    public List<Property> getProperties() {
        return Collections.singletonList((Property) this);
    }

    @Override
    public List<PropertyClassImplement> getProperties(Collection<List<ValueClassWrapper>> classLists, boolean anyInInterface) {
        List<PropertyClassImplement> resultList = new ArrayList<PropertyClassImplement>();
        if (isFull()) {
            for (List<ValueClassWrapper> classes : classLists) {
                if (interfaces.size() == classes.size()) {
                    for (List<T> mapping : new ListPermutations<T>(interfaces)) {
                        Map<T, AndClassSet> propertyInterface = new HashMap<T, AndClassSet>();
                        int interfaceCount = 0;
                        for (T iface : mapping) {
                            ValueClass propertyClass = classes.get(interfaceCount++).valueClass;
                            propertyInterface.put(iface, propertyClass.getUpSet());
                        }

                        if (isInInterface(propertyInterface, anyInInterface)) {
                            resultList.add(createClassImplement(classes, mapping));
                        }
                    }
                }
            }
        }
        return resultList;
    }
    
    protected abstract PropertyClassImplement<T, ?> createClassImplement(List<ValueClassWrapper> classes, List<T> mapping);

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

        pool.serializeCollection(outStream, interfaces);
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

    @IdentityLazy
    public PropertyChange<T> getNoChange() {
        return new PropertyChange<T>(getMapKeys(), CaseExpr.NULL);
    }
    
    public void prereadCaches() {
    }

    protected abstract Collection<Pair<Property<?>, LinkType>> calculateLinks();

    private QuickSet<Link> links;
    @ManualLazy
    public QuickSet<Link> getLinks() {
        if(links==null) {
            links = new QuickSet<Link>();
            for(Pair<Property<?>, LinkType> link : calculateLinks())
                links.add(new Link(this, link.first, link.second));
        }
        return links;
    }

    public abstract Set<SessionCalcProperty> getSessionCalcDepends();

    public Set<OldProperty> getOldDepends() {
        Set<OldProperty> result = new HashSet<OldProperty>();
        for(SessionCalcProperty sessionCalc : getSessionCalcDepends())
            result.add(sessionCalc.getOldProperty());
        return result;
    }

}

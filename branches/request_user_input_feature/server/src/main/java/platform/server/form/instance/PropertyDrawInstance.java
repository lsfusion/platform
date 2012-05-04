package platform.server.form.instance;

import platform.base.BaseUtils;
import platform.base.Result;
import platform.interop.ClassViewType;
import platform.interop.form.PropertyReadType;
import platform.server.form.entity.PropertyDrawEntity;
import platform.server.logics.DataObject;
import platform.server.logics.ServerResourceBundle;
import platform.server.logics.property.Property;
import platform.server.logics.property.PropertyInterface;
import platform.server.logics.property.ObjectValueProperty;
import platform.server.logics.BusinessLogics;

import java.sql.SQLException;
import java.util.*;

// представление св-ва
public class PropertyDrawInstance<P extends PropertyInterface> extends CellInstance<PropertyDrawEntity> implements PropertyReaderInstance {

    public PropertyObjectInstance<P> propertyObject;

    // в какой "класс" рисоваться, ессно один из Object.GroupTo должен быть ToDraw
    public GroupObjectInstance toDraw;
    public List<GroupObjectInstance> columnGroupObjects;

    // предполагается что propertyCaption ссылается на все из propertyObject но без toDraw (хотя опять таки не обязательно)
    public final PropertyObjectInstance<?> propertyCaption;
    public final PropertyObjectInstance<?> propertyReadOnly;
    public final PropertyObjectInstance<?> propertyFooter;
    public final PropertyObjectInstance<?> propertyBackground;
    public final PropertyObjectInstance<?> propertyForeground;

    // извращенное множественное наследование
    public CaptionReaderInstance captionReader = new CaptionReaderInstance();
    public FooterReaderInstance footerReader = new FooterReaderInstance();
    public BackgroundReaderInstance backgroundReader = new BackgroundReaderInstance();
    public ForegroundReaderInstance foregroundReader = new ForegroundReaderInstance();

    public PropertyDrawInstance(PropertyDrawEntity<P> entity, PropertyObjectInstance<P> propertyObject, GroupObjectInstance toDraw, List<GroupObjectInstance> columnGroupObjects,
                                PropertyObjectInstance<?> propertyCaption, PropertyObjectInstance<?> propertyReadOnly, PropertyObjectInstance<?> propertyFooter,
                                PropertyObjectInstance<?> propertyBackground, PropertyObjectInstance<?> propertyForeground) {
        super(entity);
        this.propertyObject = propertyObject;
        this.toDraw = toDraw;
        this.columnGroupObjects = columnGroupObjects;
        this.propertyCaption = propertyCaption;
        this.propertyReadOnly = propertyReadOnly;
        this.propertyFooter = propertyFooter;
        this.propertyBackground = propertyBackground;
        this.propertyForeground = propertyForeground;
    }

    public PropertyObjectInstance getPropertyObjectInstance() {
        return propertyObject;
    }

    public byte getTypeID() {
        return PropertyReadType.DRAW;
    }

    public List<ObjectInstance> getKeysObjectsList() {
        List<GroupObjectInstance> result = new ArrayList<GroupObjectInstance>();
        for (GroupObjectInstance columnGroupObject : columnGroupObjects) {
            if (columnGroupObject.curClassView == ClassViewType.GRID) {
                result.add(columnGroupObject);
            }
        }
        return GroupObjectInstance.getObjects(result);
    }

    public List<ObjectInstance> getKeysObjectsList(Set<PropertyReaderInstance> panelProperties) {
        List<ObjectInstance> result = getKeysObjectsList();
        if (!panelProperties.contains(this)) {
            result = BaseUtils.mergeList(GroupObjectInstance.getObjects(toDraw.getUpTreeGroups()), result);
        }
        return result;
    }

    public PropertyObjectInstance<?> getChangeInstance(BusinessLogics<?> BL, FormInstance form) throws SQLException {
        return getChangeInstance(new Result<Property>(), true, BL, form);
    }
    public PropertyObjectInstance<?> getChangeInstance(Result<Property> aggProp, BusinessLogics<?> BL, FormInstance form) throws SQLException {
        return getChangeInstance(aggProp, true, BL, form);
    }
    public PropertyObjectInstance<?> getChangeInstance(boolean aggValue, BusinessLogics<?> BL, Map<ObjectInstance, DataObject> mapValues, FormInstance form) throws SQLException {
        return getChangeInstance(new Result<Property>(), aggValue, BL, form).getRemappedPropertyObject(mapValues);
    }

    public boolean isReadOnly() {
        return entity.isReadOnly() || (entity.isSelector() && !isSingleSimplePanel());
    }

    private boolean isSingleSimplePanel() { // дебильновато но временно так
        return !(propertyObject.property instanceof ObjectValueProperty)
            && toDraw != null && toDraw.curClassView == ClassViewType.PANEL && toDraw.objects.size() == 1
            && propertyObject.mapping.values().size() == 1
            && propertyObject.mapping.values().iterator().next() == toDraw.objects.iterator().next()
            && toDraw.objects.iterator().next().entity.addOnEvent.isEmpty();
    }

    public PropertyObjectInstance<?> getChangeInstance(Result<Property> aggProp, boolean aggValue, BusinessLogics<?> BL, FormInstance form) throws SQLException {
        PropertyObjectInstance<?> change = propertyObject.getChangeInstance(aggProp, aggValue, form.session, form);

        // если readOnly свойство лежит в groupObject в виде панели с одним входом, то показываем диалог выбора объекта
        if (entity.isSelector() && isSingleSimplePanel()) {
            ObjectInstance singleObject = BaseUtils.single(toDraw.objects);
            ObjectValueProperty objectValueProperty = BL.getObjectValueProperty(singleObject.getBaseClass());

            aggProp.set(propertyObject.property);
            return objectValueProperty.getImplement().mapObjects(
                    Collections.singletonMap(
                            BaseUtils.single(objectValueProperty.interfaces),
                            singleObject));
        }
        return change;
    }

    public ClassViewType getForceViewType() {
        return entity.forceViewType;
    }

    public String toString() {
        return propertyObject.toString();
    }

    public class CaptionReaderInstance implements PropertyReaderInstance {
        public PropertyObjectInstance getPropertyObjectInstance() {
            return propertyCaption;
        }

        public byte getTypeID() {
            return PropertyReadType.CAPTION;
        }

        public int getID() {
            return PropertyDrawInstance.this.getID();
        }

        public List<ObjectInstance> getKeysObjectsList(Set<PropertyReaderInstance> panelProperties) {
            return PropertyDrawInstance.this.getKeysObjectsList();
        }

        @Override
        public String toString() {
            return ServerResourceBundle.getString("logics.property.caption") + "(" + PropertyDrawInstance.this.toString() + ")";
        }
    }

    public class FooterReaderInstance implements PropertyReaderInstance {
        public PropertyObjectInstance getPropertyObjectInstance() {
            return propertyFooter;
        }

        public byte getTypeID() {
            return PropertyReadType.FOOTER;
        }

        public int getID() {
            return PropertyDrawInstance.this.getID();
        }

        public List<ObjectInstance> getKeysObjectsList(Set<PropertyReaderInstance> panelProperties) {
            return PropertyDrawInstance.this.getKeysObjectsList();
        }

        @Override
        public String toString() {
            return ServerResourceBundle.getString("logics.property.footer") + "(" + PropertyDrawInstance.this.toString() + ")";
        }
    }

    public class BackgroundReaderInstance implements PropertyReaderInstance {
        public PropertyObjectInstance getPropertyObjectInstance() {
            return propertyBackground;
        }

        public byte getTypeID() {
            return PropertyReadType.CELL_BACKGROUND;
        }

        public int getID() {
            return PropertyDrawInstance.this.getID();
        }

        public List<ObjectInstance> getKeysObjectsList(Set<PropertyReaderInstance> panelProperties) {
            List<ObjectInstance> result = PropertyDrawInstance.this.getKeysObjectsList();
            if (!panelProperties.contains(this)) {
                result = BaseUtils.mergeList(GroupObjectInstance.getObjects(toDraw.getUpTreeGroups()), result);
            }
            return result;
        }

        @Override
        public String toString() {
            return ServerResourceBundle.getString("logics.background") + "(" + PropertyDrawInstance.this.toString() + ")";
        }
    }

    public class ForegroundReaderInstance implements PropertyReaderInstance {
        public PropertyObjectInstance getPropertyObjectInstance() {
            return propertyForeground;
        }

        public byte getTypeID() {
            return PropertyReadType.CELL_FOREGROUND;
        }

        public int getID() {
            return PropertyDrawInstance.this.getID();
        }

        public List<ObjectInstance> getKeysObjectsList(Set<PropertyReaderInstance> panelProperties) {
            List<ObjectInstance> result = PropertyDrawInstance.this.getKeysObjectsList();
            if (!panelProperties.contains(this)) {
                result = BaseUtils.mergeList(GroupObjectInstance.getObjects(toDraw.getUpTreeGroups()), result);
            }
            return result;
        }

        @Override
        public String toString() {
            return ServerResourceBundle.getString("logics.foreground") + "(" + PropertyDrawInstance.this.toString() + ")";
        }
    }
}

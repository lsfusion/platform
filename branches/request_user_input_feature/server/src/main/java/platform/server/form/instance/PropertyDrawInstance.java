package platform.server.form.instance;

import platform.base.BaseUtils;
import platform.base.Result;
import platform.interop.ClassViewType;
import platform.interop.form.PropertyReadType;
import platform.server.form.entity.PropertyDrawEntity;
import platform.server.logics.BusinessLogics;
import platform.server.logics.DataObject;
import platform.server.logics.ServerResourceBundle;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ObjectValueProperty;
import platform.server.logics.property.Property;
import platform.server.logics.property.PropertyInterface;
import platform.server.logics.property.actions.DialogChangeObjectActionProperty;
import platform.server.logics.property.actions.flow.ListActionProperty;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

// представление св-ва
public class PropertyDrawInstance<P extends PropertyInterface> extends CellInstance<PropertyDrawEntity> implements PropertyReaderInstance {

    private Map<String, PropertyObjectInstance<?>> editActions;

    public PropertyObjectInstance<?> getEditAction(String actionId) {
        if(isReadOnly())
            return null;

        PropertyObjectInstance<?> editAction = editActions.get(actionId);
        if(editAction!=null)
            return editAction;

        if(entity.isSelector()) {
            Map<P, ObjectInstance> groupObjects = BaseUtils.filterValues(propertyObject.mapping, toDraw.objects); // берем нижний объект в toDraw
            for(ObjectInstance objectInstance : groupObjects.values())
                if(objectInstance instanceof CustomObjectInstance) {
                    CustomObjectInstance customObjectInstance = (CustomObjectInstance)objectInstance;
                    DialogChangeObjectActionProperty dialogAction = new DialogChangeObjectActionProperty(propertyObject.property, customObjectInstance.getBaseClass().getBaseClass());
                    return new PropertyObjectInstance<ClassPropertyInterface>(dialogAction,
                            Collections.singletonMap(BaseUtils.single(dialogAction.interfaces), customObjectInstance));
                }
        }

        return propertyObject.getEditAction(actionId);
    }

    public PropertyObjectInstance<P> propertyObject;

    // в какой "класс" рисоваться, ессно один из Object.GroupTo должен быть ToDraw
    public GroupObjectInstance toDraw; // не null в FormInstance проставляется
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

    public PropertyDrawInstance(PropertyDrawEntity<P> entity,
                                PropertyObjectInstance<P> propertyObject,
                                GroupObjectInstance toDraw,
                                Map<String, PropertyObjectInstance<?>> editActions,
                                List<GroupObjectInstance> columnGroupObjects,
                                PropertyObjectInstance<?> propertyCaption,
                                PropertyObjectInstance<?> propertyReadOnly,
                                PropertyObjectInstance<?> propertyFooter,
                                PropertyObjectInstance<?> propertyBackground,
                                PropertyObjectInstance<?> propertyForeground) {
        super(entity);
        this.propertyObject = propertyObject;
        this.toDraw = toDraw;
        this.columnGroupObjects = columnGroupObjects;
        this.editActions = editActions;
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

    public boolean isReadOnly() {
        return entity.isReadOnly() || (entity.isSelector() && !isSingleSimplePanel());
    }

    private boolean isSingleSimplePanel() { // дебильновато но временно так
        return !(propertyObject.property instanceof ObjectValueProperty)
                && toDraw.curClassView == ClassViewType.PANEL && toDraw.objects.size() == 1
                && propertyObject.mapping.values().size() == 1
                && propertyObject.mapping.values().iterator().next() == toDraw.objects.iterator().next()
                && toDraw.objects.iterator().next().entity.addOnEvent.isEmpty();
    }

    public ClassViewType getForceViewType() {
        return entity.forceViewType;
    }

    public String toString() {
        return propertyObject.toString();
    }
    
    public PropertyDrawEntity getEntity() {
        return entity;
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

        @Override
        public String toString() {
            return ServerResourceBundle.getString("logics.foreground") + "(" + PropertyDrawInstance.this.toString() + ")";
        }
    }
}

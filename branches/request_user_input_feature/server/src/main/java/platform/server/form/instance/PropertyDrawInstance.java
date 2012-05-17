package platform.server.form.instance;

import platform.base.BaseUtils;
import platform.interop.ClassViewType;
import platform.interop.form.PropertyReadType;
import platform.interop.form.ServerResponse;
import platform.server.form.entity.PropertyDrawEntity;
import platform.server.logics.ServerResourceBundle;
import platform.server.logics.property.*;
import platform.server.logics.property.actions.ChangeObjectActionProperty;

import java.util.Collections;
import java.util.List;
import java.util.Map;

// представление св-ва
public class PropertyDrawInstance<P extends PropertyInterface> extends CellInstance<PropertyDrawEntity> implements PropertyReaderInstance {

    private final Map<String, ActionPropertyObjectInstance> editActions;

    public ActionPropertyObjectInstance getEditAction(String actionId) {
        // ?? тут или нет
        if (isReadOnly()) {
            return null;
        }

        ActionPropertyObjectInstance editAction = editActions.get(actionId);
        if (editAction != null) {
            return editAction;
        }

        Property<P> property = propertyObject.property;
        if (actionId.equals(ServerResponse.GROUP_CHANGE)) {
            ActionPropertyObjectInstance<?> changeInstance = getEditAction(ServerResponse.CHANGE);

            assert changeInstance != null;

            ActionPropertyObjectInstance<?> groupChangeActionInstance = changeInstance.getGroupChange();

            editActions.put(ServerResponse.GROUP_CHANGE, groupChangeActionInstance);

            return groupChangeActionInstance;
        }

        if (entity.isSelector()) {
            Map<P, ObjectInstance> groupObjects = BaseUtils.filterValues(propertyObject.mapping, toDraw.objects); // берем нижний объект в toDraw
            for (ObjectInstance objectInstance : groupObjects.values()) {
                if (objectInstance instanceof CustomObjectInstance) {
                    CustomObjectInstance customObjectInstance = (CustomObjectInstance) objectInstance;
                    ChangeObjectActionProperty dialogAction = new ChangeObjectActionProperty((CalcProperty) property, customObjectInstance.getBaseClass().getBaseClass());
                    return new ActionPropertyObjectInstance<ClassPropertyInterface>(dialogAction,
                                                            Collections.singletonMap(BaseUtils.single(dialogAction.interfaces), customObjectInstance));
                }
            }
        }

        ActionPropertyMapImplement<?, P> editActionImplement = propertyObject.property.getEditAction(actionId);
        return editActionImplement == null ? null : editActionImplement.mapObjects(propertyObject.mapping);
    }

    public PropertyObjectInstance<P, ?> propertyObject;

    // в какой "класс" рисоваться, ессно один из Object.GroupTo должен быть ToDraw
    public GroupObjectInstance toDraw; // не null, кроме когда без параметров в FormInstance проставляется

    public ClassViewType getCurClassView() {
        return toDraw != null ? toDraw.curClassView : ClassViewType.PANEL;
    }

    public List<GroupObjectInstance> columnGroupObjects;

    // предполагается что propertyCaption ссылается на все из propertyObject но без toDraw (хотя опять таки не обязательно)
    public final CalcPropertyObjectInstance<?> propertyCaption;
    public final CalcPropertyObjectInstance<?> propertyReadOnly;
    public final CalcPropertyObjectInstance<?> propertyFooter;
    public final CalcPropertyObjectInstance<?> propertyBackground;
    public final CalcPropertyObjectInstance<?> propertyForeground;

    // извращенное множественное наследование
    public CaptionReaderInstance captionReader = new CaptionReaderInstance();
    public FooterReaderInstance footerReader = new FooterReaderInstance();
    public BackgroundReaderInstance backgroundReader = new BackgroundReaderInstance();
    public ForegroundReaderInstance foregroundReader = new ForegroundReaderInstance();

    public PropertyDrawInstance(PropertyDrawEntity<P> entity,
                                PropertyObjectInstance<P, ?> propertyObject,
                                GroupObjectInstance toDraw,
                                Map<String, ActionPropertyObjectInstance> editActions,
                                List<GroupObjectInstance> columnGroupObjects,
                                CalcPropertyObjectInstance<?> propertyCaption,
                                CalcPropertyObjectInstance<?> propertyReadOnly,
                                CalcPropertyObjectInstance<?> propertyFooter,
                                CalcPropertyObjectInstance<?> propertyBackground,
                                CalcPropertyObjectInstance<?> propertyForeground) {
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
        return entity.isReadOnly();
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

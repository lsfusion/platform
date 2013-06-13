package lsfusion.server.form.instance;

import lsfusion.base.SFunctionSet;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.interop.ClassViewType;
import lsfusion.interop.form.PropertyReadType;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.form.entity.ActionPropertyObjectEntity;
import lsfusion.server.form.entity.FormEntity;
import lsfusion.server.form.entity.PropertyDrawEntity;
import lsfusion.server.logics.ServerResourceBundle;
import lsfusion.server.logics.property.NullValueProperty;
import lsfusion.server.logics.property.PropertyInterface;

import static lsfusion.interop.ClassViewType.GRID;

// представление св-ва
public class PropertyDrawInstance<P extends PropertyInterface> extends CellInstance<PropertyDrawEntity> implements PropertyReaderInstance {

    public ActionPropertyObjectInstance getEditAction(String actionId, InstanceFactory instanceFactory, FormEntity entity) {
        ActionPropertyObjectEntity editAction = this.entity.getEditAction(actionId, entity);
        if(editAction!=null)
            return instanceFactory.getInstance(editAction);
        return null;
    }

    public PropertyObjectInstance<P, ?> propertyObject;

    // в какой "класс" рисоваться, ессно один из Object.GroupTo должен быть ToDraw
    public GroupObjectInstance toDraw; // не null, кроме когда без параметров в FormInstance проставляется

    public ClassViewType getCurClassView() {
        return toDraw != null ? toDraw.curClassView : ClassViewType.PANEL;
    }

    private final ImOrderSet<GroupObjectInstance> columnGroupObjects;
    public ImSet<GroupObjectInstance> getColumnGroupObjects() {
        return columnGroupObjects.getSet();
    }
    public ImOrderSet<GroupObjectInstance> getOrderColumnGroupObjects() {
        return columnGroupObjects;
    }
    public ImSet<GroupObjectInstance> getColumnGroupObjectsInGridView() {
        return getColumnGroupObjects().filterFn(new SFunctionSet<GroupObjectInstance>() {
            public boolean contains(GroupObjectInstance element) {
                return element.curClassView == GRID;
            }
        });
    }

    public ValueClass getValueClass() {
        return propertyObject.property.getValueClass();
    }

    // предполагается что propertyCaption ссылается на все из propertyObject но без toDraw (хотя опять таки не обязательно)
    public final CalcPropertyObjectInstance<?> propertyCaption;
    public final CalcPropertyObjectInstance<?> propertyShowIf;
    public final CalcPropertyObjectInstance<?> propertyReadOnly;
    public final CalcPropertyObjectInstance<?> propertyFooter;
    public final CalcPropertyObjectInstance<?> propertyBackground;
    public final CalcPropertyObjectInstance<?> propertyForeground;

    // извращенное множественное наследование
    public CaptionReaderInstance captionReader = new CaptionReaderInstance();
    public ShowIfReaderInstance showIfReader = new ShowIfReaderInstance();
    public FooterReaderInstance footerReader = new FooterReaderInstance();
    public ReadOnlyReaderInstance readOnlyReader = new ReadOnlyReaderInstance();
    public BackgroundReaderInstance backgroundReader = new BackgroundReaderInstance();
    public ForegroundReaderInstance foregroundReader = new ForegroundReaderInstance();

    public HiddenReaderInstance hiddenReader = new HiddenReaderInstance();

    public PropertyDrawInstance(PropertyDrawEntity<P> entity,
                                PropertyObjectInstance<P, ?> propertyObject,
                                GroupObjectInstance toDraw,
                                ImOrderSet<GroupObjectInstance> columnGroupObjects,
                                CalcPropertyObjectInstance<?> propertyCaption,
                                CalcPropertyObjectInstance<?> propertyShowIf,
                                CalcPropertyObjectInstance<?> propertyReadOnly,
                                CalcPropertyObjectInstance<?> propertyFooter,
                                CalcPropertyObjectInstance<?> propertyBackground,
                                CalcPropertyObjectInstance<?> propertyForeground) {
        super(entity);
        this.propertyObject = propertyObject;
        this.toDraw = toDraw;
        this.columnGroupObjects = columnGroupObjects;
        this.propertyCaption = propertyCaption;
        this.propertyShowIf = propertyShowIf;
        this.propertyReadOnly = propertyReadOnly;
        this.propertyFooter = propertyFooter;
        this.propertyBackground = propertyBackground;
        this.propertyForeground = propertyForeground;
    }

    public CalcPropertyObjectInstance getPropertyObjectInstance() {
        return getDrawInstance();
    }

    public CalcPropertyObjectInstance<?> getDrawInstance() {
        return propertyObject.getDrawProperty();
    }

    public byte getTypeID() {
        return PropertyReadType.DRAW;
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

    // заглушка чтобы на сервере ничего не читать
    public class HiddenReaderInstance implements PropertyReaderInstance {

        public CalcPropertyObjectInstance getPropertyObjectInstance() {
            return new CalcPropertyObjectInstance<PropertyInterface>(NullValueProperty.instance, MapFact.<PropertyInterface, ObjectInstance>EMPTY());
        }

        public byte getTypeID() {
            return PropertyDrawInstance.this.getTypeID();
        }

        public int getID() {
            return PropertyDrawInstance.this.getID();
        }
    }

    public class ShowIfReaderInstance implements PropertyReaderInstance {

        public CalcPropertyObjectInstance getPropertyObjectInstance() {
            return propertyShowIf;
        }

        public byte getTypeID() {
            return PropertyReadType.SHOWIF;
        }

        public int getID() {
            return PropertyDrawInstance.this.getID();
        }

        public PropertyDrawInstance<P> getPropertyDraw() {
            return PropertyDrawInstance.this;
        }
    }

    public class CaptionReaderInstance implements PropertyReaderInstance {
        public CalcPropertyObjectInstance getPropertyObjectInstance() {
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
        public CalcPropertyObjectInstance getPropertyObjectInstance() {
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

    public class ReadOnlyReaderInstance implements PropertyReaderInstance {
        public CalcPropertyObjectInstance getPropertyObjectInstance() {
            return propertyReadOnly;
        }

        public byte getTypeID() {
            return PropertyReadType.READONLY;
        }

        public int getID() {
            return PropertyDrawInstance.this.getID();
        }

        @Override
        public String toString() {
            return ServerResourceBundle.getString("logics.property.readonly") + "(" + PropertyDrawInstance.this.toString() + ")";
        }
    }

    public class BackgroundReaderInstance implements PropertyReaderInstance {
        public CalcPropertyObjectInstance getPropertyObjectInstance() {
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
        public CalcPropertyObjectInstance getPropertyObjectInstance() {
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

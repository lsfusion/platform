package platform.server.form.instance;

import platform.base.BaseUtils;
import platform.interop.ClassViewType;
import platform.interop.form.PropertyReadType;
import platform.server.form.entity.PropertyDrawEntity;
import platform.server.logics.property.PropertyInterface;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

// представление св-ва
public class PropertyDrawInstance<P extends PropertyInterface> extends CellInstance<PropertyDrawEntity> implements PropertyReaderInstance {

    public PropertyObjectInstance<P> propertyObject;

    // в какой "класс" рисоваться, ессно один из Object.GroupTo должен быть ToDraw
    public GroupObjectInstance toDraw;
    public List<GroupObjectInstance> columnGroupObjects;

    // предполагается что propertyCaption ссылается на все из propertyObject но без toDraw (хотя опять таки не обязательно)
    public final PropertyObjectInstance<?> propertyCaption;
    public final PropertyObjectInstance<?> propertyHighlight;

    // извращенное множественное наследование
    public CaptionReaderInstance captionReader = new CaptionReaderInstance();
    public HighlightReaderInstance highlightReader = new HighlightReaderInstance();

    public PropertyDrawInstance(PropertyDrawEntity<P> entity, PropertyObjectInstance<P> propertyObject, GroupObjectInstance toDraw, List<GroupObjectInstance> columnGroupObjects, PropertyObjectInstance<?> propertyCaption, PropertyObjectInstance<?> propertyHighlight) {
        super(entity);
        this.propertyObject = propertyObject;
        this.toDraw = toDraw;
        this.columnGroupObjects = columnGroupObjects;
        this.propertyCaption = propertyCaption;
        this.propertyHighlight = propertyHighlight;
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
    }

    public class HighlightReaderInstance implements PropertyReaderInstance {
        public PropertyObjectInstance getPropertyObjectInstance() {
            return propertyHighlight;
        }

        public byte getTypeID() {
            return PropertyReadType.CELL_HIGHLIGHT;
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
    }
}

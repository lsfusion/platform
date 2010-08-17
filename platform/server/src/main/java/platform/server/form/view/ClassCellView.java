package platform.server.form.view;

import platform.server.classes.CustomClass;
import platform.server.classes.StringClass;
import platform.server.data.type.Type;
import platform.server.form.entity.ObjectEntity;

public class ClassCellView extends CellView implements ClientSerialize {

    ObjectEntity entity;
    Type type;

    public ClassCellView(int ID, ObjectEntity entity) {
        super(ID);

        this.entity = entity;
        this.type = new StringClass(30);
        this.show = entity.baseClass instanceof CustomClass && !((CustomClass) entity.baseClass).children.isEmpty();
    }

    Type getType() {
        return type;
    }

    int getID() {
        return entity.getID();
    }

    String getSID() {
        return entity.getSID();
    }

    String getDefaultCaption() {
        return entity.caption;
    }
}
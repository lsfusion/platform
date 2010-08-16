package platform.server.form.view;

import platform.server.data.type.Type;
import platform.server.form.entity.ObjectEntity;

public class ObjectIDCellView extends CellView {

    ObjectEntity entity;
    
    public ObjectIDCellView(int ID, ObjectEntity entity) {
        super(ID);

        this.entity = entity;
    }

    Type getType() {
        return entity.baseClass.getType();
    }

    int getID() {
        return entity.ID;
    }

    String getSID() {
        return entity.getSID();
    }

    String getDefaultCaption() {
        return entity.caption;
    }

}

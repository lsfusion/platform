package platform.server.form.view;

import platform.server.data.type.Type;
import platform.server.form.entity.PropertyDrawEntity;

import java.io.DataOutputStream;
import java.io.IOException;

public class PropertyDrawView extends CellView implements ClientSerialize {

    public PropertyDrawEntity<?> entity;

    public PropertyDrawView(int ID, PropertyDrawEntity entity) {
        super(ID);
        this.entity = entity;
    }

    public Type getType() {
        return entity.propertyObject.property.getType();
    }

    public int getID() {
        return entity.ID;
    }

    public String getSID() {
        return entity.getSID();
    }

    public String getDefaultCaption() {
        return entity.propertyObject.property.caption;
    }

    public void serialize(DataOutputStream outStream) throws IOException {
        super.serialize(outStream);

        outStream.writeInt(entity.ID);
        outStream.writeUTF(entity.propertyObject.property.sID);
        outStream.writeBoolean(entity.toDraw==null);
        if(entity.toDraw!=null)
            outStream.writeInt(entity.toDraw.ID);
    }
}

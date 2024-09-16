package lsfusion.client.form.object.table.grid;

import lsfusion.client.form.controller.remote.serialization.ClientSerializationPool;
import lsfusion.client.form.design.ClientComponent;
import lsfusion.client.form.object.ClientGroupObject;
import lsfusion.client.form.object.ClientGroupObjectValue;
import lsfusion.client.form.object.table.controller.TableController;
import lsfusion.client.form.property.ClientPropertyReader;
import lsfusion.interop.form.property.PropertyReadType;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.Map;

public abstract class ClientGridProperty extends ClientComponent {

    public int captionHeight;
    public int captionCharHeight;

    public Boolean resizeOverflow;

    public int lineWidth;
    public int lineHeight;

    public Boolean boxed;

    public String valueClass;

    public ClientGridProperty() {
    }

    public final ClientPropertyReader valueElementClassReader = new ClientPropertyReader() {
        public ClientGroupObject getGroupObject() {
            return null;
        }

        public void update(Map<ClientGroupObjectValue, Object> values, boolean updateKeys, TableController controller) {
        }

        public int getID() {
            return ClientGridProperty.this.getID();
        }

        public byte getType() {
            return PropertyReadType.GRID_VALUECLASS;
        }
    };

    @Override
    public void customDeserialize(ClientSerializationPool pool, DataInputStream inStream) throws IOException {
        super.customDeserialize(pool, inStream);

        // GridProperty
        boxed = inStream.readBoolean() ? inStream.readBoolean() : null;

        captionHeight = inStream.readInt();
        captionCharHeight = inStream.readInt();

        resizeOverflow = inStream.readBoolean() ? inStream.readBoolean() : null;

        lineWidth = inStream.readInt();
        lineHeight = inStream.readInt();

        valueClass = pool.readString(inStream);
    }
}

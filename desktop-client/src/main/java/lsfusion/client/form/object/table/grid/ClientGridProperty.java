package lsfusion.client.form.object.table.grid;

import lsfusion.client.form.controller.remote.serialization.ClientSerializationPool;
import lsfusion.client.form.design.ClientComponent;
import lsfusion.client.form.property.ClientPropertyReader;
import lsfusion.interop.form.property.PropertyReadType;

import java.io.DataInputStream;
import java.io.IOException;

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

    public final ClientPropertyReader valueElementClassReader = new ExtraReader(PropertyReadType.GRID_VALUECLASS) {
        public int getID() {
            return ClientGridProperty.this.getID();
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

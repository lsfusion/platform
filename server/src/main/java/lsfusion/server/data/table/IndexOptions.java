package lsfusion.server.data.table;

import lsfusion.base.BaseUtils;
import lsfusion.server.physics.admin.Settings;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class IndexOptions {
    public boolean order;
    public IndexType type;
    public String language;

    public static IndexOptions defaultIndexOptions = new IndexOptions(true, IndexType.DEFAULT, Settings.get().getFilterMatchLanguage());

    public IndexOptions(boolean order) {
        this(order, IndexType.DEFAULT, null);
    }

    public IndexOptions(boolean order, IndexType type, String language) {
        this.order = order;
        this.type = type;
        this.language = language;
    }

    public void serialize(DataOutputStream outStream) throws IOException {
        outStream.writeBoolean(order);
        outStream.writeByte(type.serialize());
        outStream.writeUTF(language);
    }

    public static IndexOptions deserialize(DataInputStream inStream) throws IOException {
        boolean order = inStream.readBoolean();
        IndexType indexType = IndexType.deserialize(inStream.readByte());
        String language = inStream.readUTF();
        return new IndexOptions(order, indexType, language);
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof IndexOptions && order == ((IndexOptions) o).order && type.equals(((IndexOptions) o).type)
                && (!type.isMatch() || BaseUtils.nullEquals(language, ((IndexOptions) o).language));
    }
}

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
    public String dbName;

    public static IndexOptions defaultIndexOptions = new IndexOptions(true, IndexType.DEFAULT, Settings.get().getFilterMatchLanguage(), null);

    public IndexOptions(boolean order) {
        this(order, IndexType.DEFAULT, null, null);
    }

    public static IndexOptions deserialize35(DataInputStream inStream) throws IOException {
        boolean order = inStream.readBoolean();
        IndexType indexType = IndexType.deserialize(inStream.readByte());
        String language = inStream.readUTF();
        return new IndexOptions(order, indexType, language, null);
    }

    public IndexOptions(boolean order, IndexType type, String language, String dbName) {
        this.order = order;
        this.type = type;
        this.language = language;
        this.dbName = dbName;
    }

    public void serialize(DataOutputStream outStream) throws IOException {
        outStream.writeBoolean(order);
        outStream.writeByte(type.serialize());
        outStream.writeUTF(language);
        outStream.writeBoolean(dbName != null);
        if(dbName != null)
            outStream.writeUTF(dbName);
    }

    public static IndexOptions deserialize(DataInputStream inStream) throws IOException {
        boolean order = inStream.readBoolean();
        IndexType indexType = IndexType.deserialize(inStream.readByte());
        String language = inStream.readUTF();
        String dbName = inStream.readBoolean() ? inStream.readUTF() : null;
        return new IndexOptions(order, indexType, language, dbName);
    }

    public static IndexOptions deserialize36(DataInputStream inStream) throws IOException {
        boolean order = inStream.readBoolean();
        IndexType indexType = IndexType.deserialize(inStream.readByte());
        String language = inStream.readUTF();
        String dbName = inStream.readBoolean() ? inStream.readUTF() : null;
        return new IndexOptions(order, indexType, language);
    }


    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof IndexOptions && order == ((IndexOptions) o).order && type.equals(((IndexOptions) o).type)
                && (!type.isMatch() || BaseUtils.nullEquals(language, ((IndexOptions) o).language)) && BaseUtils.nullEquals(dbName, ((IndexOptions) o).dbName);
    }
}

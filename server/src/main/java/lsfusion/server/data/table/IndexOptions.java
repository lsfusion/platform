package lsfusion.server.data.table;

import lsfusion.base.BaseUtils;
import lsfusion.server.base.controller.thread.ThreadLocalContext;
import lsfusion.server.physics.admin.Settings;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class IndexOptions {
    public boolean order;
    public IndexType type;
    public String language;
    public String dbName;

    public IndexOptions(boolean order, IndexType type, String dbName) {
        this(order, type, ThreadLocalContext.getBusinessLogics().getSearchLanguage(), dbName);
    }

    private IndexOptions(boolean order, IndexType type, String language, String dbName) {
        this.order = order;
        this.type = type;
        this.language = language;
        this.dbName = dbName;
    }
    public static IndexOptions deserialize35(DataInputStream inStream) throws IOException {
        boolean order = inStream.readBoolean();
        IndexType indexType = IndexType.deserialize(inStream.readByte());
        String language = inStream.readUTF();
        return new IndexOptions(order, indexType, language, null);
    }

    public static IndexOptions deserialize32(DataInputStream inStream) throws IOException {
        return new IndexOptions(inStream.readBoolean(), IndexType.DEFAULT, null, null);
    }

    public IndexOptions changeType(IndexType newType) {
        String newDBName = (dbName == null ? null : dbName + newType.suffix());
        return new IndexOptions(order, newType, language, newDBName);
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

    public boolean equalsWithoutDBName(Object o) {
        return this == o || o instanceof IndexOptions && order == ((IndexOptions) o).order && type.equals(((IndexOptions) o).type)
                && (!type.isMatch() || BaseUtils.nullEquals(language, ((IndexOptions) o).language));
    }
    
    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof IndexOptions && order == ((IndexOptions) o).order && type.equals(((IndexOptions) o).type)
                && (!type.isMatch() || BaseUtils.nullEquals(language, ((IndexOptions) o).language)) && BaseUtils.nullEquals(dbName, ((IndexOptions) o).dbName);
    }
}

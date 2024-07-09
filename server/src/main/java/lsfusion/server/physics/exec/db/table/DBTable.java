package lsfusion.server.physics.exec.db.table;

import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MExclSet;
import lsfusion.base.col.interfaces.mutable.MOrderExclSet;
import lsfusion.base.mutability.TwinImmutableObject;
import lsfusion.server.data.query.compile.CompileSource;
import lsfusion.server.data.sql.syntax.SQLSyntax;
import lsfusion.server.data.table.*;
import lsfusion.server.data.where.classes.ClassWhere;
import lsfusion.server.logics.classes.user.BaseClass;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public abstract class DBTable extends StoredTable {

    public DBTable(String name) {
        super(name);
    }

    public DBTable(DataInputStream inStream, String name, BaseClass baseClass) throws IOException {
        super(name, deserializeKeys(inStream), deserializeProperties(inStream), ClassWhere.FALSE(), MapFact.EMPTY());

        initBaseClasses(baseClass);
    }

    public void setName(String name) {
        this.name = name;
    }

    private static ImSet<PropertyField> deserializeProperties(DataInputStream inStream) throws IOException {
        int propNum = inStream.readInt();
        MExclSet<PropertyField> mProperties = SetFact.mExclSet(propNum);
        for(int i=0;i<propNum;i++)
            mProperties.exclAdd((PropertyField) Field.deserialize(inStream));
        return mProperties.immutable();
    }

    private static ImOrderSet<KeyField> deserializeKeys(DataInputStream inStream) throws IOException {
        int keysNum = inStream.readInt();
        MOrderExclSet<KeyField> mKeys = SetFact.mOrderExclSet(keysNum); // десериализация, поэтому порядок важен
        for(int i=0;i<keysNum;i++)
            mKeys.exclAdd((KeyField) Field.deserialize(inStream));
        return mKeys.immutableOrder();
    }

    public void serialize(DataOutputStream outStream) throws IOException {
        outStream.writeUTF(name);
        outStream.writeBoolean(canonicalName != null);
        if (canonicalName != null) {
            outStream.writeUTF(canonicalName);
        }
        outStream.writeInt(keys.size());
        for(KeyField key : keys)
            key.serialize(outStream);
        outStream.writeInt(properties.size());
        for(PropertyField property : properties)
            property.serialize(outStream);
    }

    protected String canonicalName;

    public String getCanonicalName() {
        return canonicalName;
    }

    public void setCanonicalName(String canonicalName) {
        this.canonicalName = canonicalName;
    }

    public String getName(SQLSyntax syntax) {
        return syntax.getTableName(name);
    }

    public String getQuerySource(CompileSource source) {
        return getName(source.syntax);
    }

    public boolean calcTwins(TwinImmutableObject o) {
        return name.equals(((DBTable) o).name) && super.calcTwins(o);
    }

    public int immutableHashCode() {
        return 31 * super.immutableHashCode() + name.hashCode();
    }

    protected DBTable(String name, ImOrderSet<KeyField> keys, ImSet<PropertyField> properties, ClassWhere<KeyField> classes, ImMap<PropertyField, ClassWhere<Field>> propertyClasses) {
        super(name, keys, properties, classes, propertyClasses);
    }
}

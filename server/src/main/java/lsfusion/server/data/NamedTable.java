package lsfusion.server.data;

import lsfusion.base.mutability.TwinImmutableObject;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MExclSet;
import lsfusion.base.col.interfaces.mutable.MOrderExclSet;
import lsfusion.server.logics.classes.BaseClass;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.base.context.ThreadLocalContext;
import lsfusion.server.data.query.CompileSource;
import lsfusion.server.data.sql.SQLSyntax;
import lsfusion.server.data.where.classes.ClassWhere;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public abstract class NamedTable extends Table {

    public NamedTable(String name) {
        super();
        this.name = name;
    }

    public NamedTable(String name, ImOrderSet<KeyField> keys, ImSet<PropertyField> properties, ClassWhere<KeyField> classes, ImMap<PropertyField, ClassWhere<Field>> propertyClasses) {
        super(keys, properties, classes, propertyClasses);
        this.name = name;
    }

    protected NamedTable(DataInputStream inStream, BaseClass baseClass) throws IOException {
        this(inStream.readUTF(), deserializeKeys(inStream), deserializeProperties(inStream), ClassWhere.<KeyField>FALSE(), MapFact.<PropertyField, ClassWhere<Field>>EMPTY());

        initBaseClasses(baseClass);
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
        outStream.writeInt(keys.size());
        for(KeyField key : keys)
            key.serialize(outStream);
        outStream.writeInt(properties.size());
        for(PropertyField property : properties)
            property.serialize(outStream);
    }

    protected String name;
    public void setName(String name) {
        this.name = name;
    }

    public String toString() {
        return getName();
    }

    public String getName() {
        return name;
    }

    public String outputKeys() {
        return ThreadLocalContext.localize("{data.table} : ") + name + ThreadLocalContext.localize(", {data.keys} : ") + classes.getCommonParent(getTableKeys()).toString();
    }

    public String outputField(PropertyField field, boolean outputTable) {
        ImMap<Field, ValueClass> commonParent = propertyClasses.get(field).getCommonParent(SetFact.addExcl(getTableKeys(), field));
        return (outputTable ? ThreadLocalContext.localize("{data.table}")+" : " + name + ", ":"") + ThreadLocalContext.localize("{data.field}") +" : " + field.getName() + " - " + commonParent.get(field) + ", "+ThreadLocalContext.localize("{data.keys}")+" : " + commonParent.remove(field);
    }

    public boolean calcTwins(TwinImmutableObject o) {
        return name.equals(((NamedTable)o).name) && super.calcTwins(o);
    }

    public int immutableHashCode() {
        return 31 * super.immutableHashCode() + name.hashCode();
    }

    public String getName(SQLSyntax syntax) {
        return syntax.getTableName(name);
    }

    public String getQuerySource(CompileSource source) {
        return getName(source.syntax);
    }
}

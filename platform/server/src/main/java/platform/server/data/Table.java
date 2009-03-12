package platform.server.data;

import platform.server.data.query.Join;
import platform.server.data.query.JoinQuery;
import platform.server.data.query.exprs.ValueExpr;
import platform.server.data.sql.SQLSyntax;
import platform.server.data.types.Type;
import platform.server.logics.session.DataSession;

import java.sql.SQLException;
import java.util.*;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.DataInputStream;

public class Table extends DataSource<KeyField, PropertyField> {
    public String name;
    public Collection<PropertyField> properties = new ArrayList();

    public Table(String iName) {
        name =iName;}

    public String getSource(SQLSyntax syntax, Map<ValueExpr, String> params) {
        return getName(syntax);
    }

    public String getName(SQLSyntax Syntax) {
        return name;
    }

    public void fillJoinQueries(Set<JoinQuery> Queries) {
    }

    public String toString() {
        return name;
    }

    public Collection<PropertyField> getProperties() {
        return properties;
    }

    public Type getType(PropertyField Property) {
        return Property.type;
    }

    public String getKeyName(KeyField Key) {
        return Key.name;
    }

    public String getPropertyName(PropertyField Property) {
        return Property.name;
    }

    public Map<ValueExpr,ValueExpr> getValues() {
        return new HashMap<ValueExpr,ValueExpr>();
    }

    public void outSelect(DataSession Session) throws SQLException {
        JoinQuery<KeyField, PropertyField> OutQuery = new JoinQuery<KeyField, PropertyField>(keys);
        Join<KeyField, PropertyField> OutJoin = new Join<KeyField, PropertyField>(this,OutQuery);
        OutQuery.properties.putAll(OutJoin.exprs);
        OutQuery.where = OutQuery.where.and(OutJoin.inJoin);
        OutQuery.outSelect(Session);
    }

    public int hashProperty(PropertyField Property) {
        return Property.hashCode();
    }

    public DataSource<KeyField, PropertyField> translateValues(Map<ValueExpr, ValueExpr> values) {
        return this;
    }

    public KeyField findKey(String name) {
        for(KeyField key : keys)
            if(key.name.equals(name))
                return key;
        return null;
    }

    public PropertyField findProperty(String name) {
        for(PropertyField property : properties)
            if(property.name.equals(name))
                return property;
        return null;
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


    public Table(DataInputStream inStream) throws IOException {
        name = inStream.readUTF();
        int keysNum = inStream.readInt();
        for(int i=0;i<keysNum;i++)
            keys.add((KeyField) Field.deserialize(inStream));
        int propNum = inStream.readInt();
        for(int i=0;i<propNum;i++)
            properties.add((PropertyField) Field.deserialize(inStream));
    }
}

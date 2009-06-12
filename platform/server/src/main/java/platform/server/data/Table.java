package platform.server.data;

import platform.server.data.classes.where.ClassWhere;
import platform.server.data.query.exprs.ValueExpr;
import platform.server.data.query.exprs.KeyExpr;
import platform.server.data.query.JoinQuery;
import platform.server.data.query.Join;
import platform.server.data.query.MapKeysInterface;
import platform.server.data.sql.SQLSyntax;
import platform.server.data.types.Type;
import platform.server.session.SQLSession;
import platform.base.BaseUtils;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.*;
import java.sql.SQLException;

public class Table extends DataSource<KeyField, PropertyField> implements MapKeysInterface<KeyField> {
    public final String name;
    public final Collection<KeyField> keys = new ArrayList<KeyField>();
    public final Collection<PropertyField> properties = new ArrayList<PropertyField>();

    public Collection<KeyField> getKeys() {
        return keys;
    }

    public Map<KeyField, KeyExpr> getMapKeys() {
        Map<KeyField,KeyExpr> result = new HashMap<KeyField, KeyExpr>();
        for(KeyField key : keys)
            result.put(key,new KeyExpr(key.name));
        return result;
    }

    public Table(String iName,ClassWhere<KeyField> iClasses) {
        name =iName;
        classes = iClasses;
        propertyClasses = new HashMap<PropertyField, ClassWhere<Field>>();
    }

    public Table(String iName) {
        name =iName;
        classes = new ClassWhere<KeyField>();
        propertyClasses = new HashMap<PropertyField, ClassWhere<Field>>();
    }

    public Table(String iName,ClassWhere<KeyField> iClasses,Map<PropertyField, ClassWhere<Field>> iPropertyClasses) {
        name =iName;
        classes = iClasses;
        propertyClasses = iPropertyClasses;
    }

    public String getSource(SQLSyntax syntax, Map<ValueExpr, String> params) {
        return getName(syntax);
    }

    public String getName(SQLSyntax Syntax) {
        return name;
    }

    public String toString() {
        return name;
    }

    public Collection<PropertyField> getProperties() {
        return properties;
    }

    public Type getType(PropertyField property) {
        return property.type;
    }

    public String getKeyName(KeyField Key) {
        return Key.name;
    }

    public String getPropertyName(PropertyField Property) {
        return Property.name;
    }

    public Collection<ValueExpr> getValues() {
        return new ArrayList<ValueExpr>();
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

        classes = new ClassWhere<KeyField>();
        propertyClasses = new HashMap<PropertyField, ClassWhere<Field>>();
    }

    public DataSource<KeyField, PropertyField> packClassWhere(ClassWhere<KeyField> keyClasses) {
        return this;
    }

    public ClassWhere<KeyField> classes; // по сути условия на null'ы в том числе
    public ClassWhere<KeyField> getKeyClassWhere() {
        return classes;
    }
    
    public final Map<PropertyField,ClassWhere<Field>> propertyClasses;
    public ClassWhere<Object> getClassWhere(Collection<PropertyField> notNull) {
        ClassWhere<Field> result = new ClassWhere<Field>();
        for(PropertyField property : notNull)
            result = result.or(propertyClasses.get(property));
        return (ClassWhere<Object>)(ClassWhere<?>)result;
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj || obj instanceof Table && name.equals(((Table)obj).name) && classes.equals(((Table)obj).classes) && propertyClasses.equals(((Table)obj).propertyClasses);

    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    protected int getHash() {
        return hashCode();
    }

    public int hashProperty(PropertyField property) {
        return property.hashCode();
    }

    public <EK, EV> Iterable<MapSource<KeyField, PropertyField, EK, EV>> map(DataSource<EK, EV> source) {
        if(equals(source))
            return Collections.singleton(new MapSource<KeyField,PropertyField,EK,EV>((Map<KeyField, EK>)BaseUtils.toMap(keys),(Map<PropertyField, EV>)BaseUtils.toMap(getProperties()),BaseUtils.toMap(getValues())));
        else
            return new ArrayList<MapSource<KeyField,PropertyField,EK,EV>>();
    }


    public void out(SQLSession session) throws SQLException {
        JoinQuery<KeyField,PropertyField> query = new JoinQuery<KeyField,PropertyField>(this);
        Join<PropertyField> join = join(query.mapKeys);
        query.and(join.getWhere());
        query.properties.putAll(join.getExprs());
        query.outSelect(session);
    }
}

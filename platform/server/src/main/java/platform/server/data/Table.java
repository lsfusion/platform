package platform.server.data;

import platform.server.data.query.Join;
import platform.server.data.query.JoinQuery;
import platform.server.data.query.exprs.ValueExpr;
import platform.server.data.sql.SQLSyntax;
import platform.server.data.types.Type;
import platform.server.logics.session.DataSession;

import java.sql.SQLException;
import java.util.*;

public class Table extends DataSource<KeyField, PropertyField> {
    public String Name;
    public Collection<PropertyField> properties = new ArrayList();

    public Table(String iName) {Name=iName;}

    public String getSource(SQLSyntax syntax, Map<ValueExpr, String> params) {
        return getName(syntax);
    }

    public String getName(SQLSyntax Syntax) {
        return Name;
    }

    public void fillJoinQueries(Set<JoinQuery> Queries) {
    }

    public String toString() {
        return Name;
    }

    public Set<List<PropertyField>> indexes = new HashSet();

    public Collection<PropertyField> getProperties() {
        return properties;
    }

    public Type getType(PropertyField Property) {
        return Property.type;
    }

    public String getKeyName(KeyField Key) {
        return Key.Name;
    }

    public String getPropertyName(PropertyField Property) {
        return Property.Name;
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
}

package lsfusion.server.data.table;
import lsfusion.base.BaseUtils;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.lambda.Processor;
import lsfusion.base.mutability.TwinImmutableObject;
import lsfusion.server.data.OperationOwner;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.query.Query;
import lsfusion.server.data.query.build.QueryBuilder;
import lsfusion.server.data.query.result.ResultHandler;
import lsfusion.server.data.sql.SQLSession;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.sql.syntax.SQLSyntax;
import lsfusion.server.data.stat.PropStat;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.data.where.classes.ClassWhere;
import lsfusion.server.logics.action.session.DataSession;
import lsfusion.server.logics.classes.data.DataClass;
import lsfusion.server.logics.classes.data.file.FileClass;
import lsfusion.server.logics.classes.user.BaseClass;
import lsfusion.server.logics.classes.user.set.AndClassSet;
import lsfusion.server.logics.form.stat.LimitOffset;
import lsfusion.server.physics.exec.db.table.ImplementTable;
import lsfusion.server.physics.exec.db.table.SerializedTable;

import java.sql.SQLException;
import java.util.function.Function;

public abstract class StoredTable extends Table {

    protected String name;

    public String toString() {
        return getName();
    }

    public String getName() {
        return name;
    }

    public ImSet<PropertyField> properties;
    protected ClassWhere<KeyField> classes;
    protected ImMap<PropertyField,ClassWhere<Field>> propertyClasses;

    public StoredTable(String name) {
        this(name, SetFact.EMPTYORDER(), SetFact.EMPTY(), ClassWhere.FALSE(), MapFact.EMPTY());
    }

    public StoredTable(String name, ImOrderSet<KeyField> keys, ImSet<PropertyField> properties, ClassWhere<KeyField> classes, ImMap<PropertyField, ClassWhere<Field>> propertyClasses) {
        super(keys);

        this.name = name;
        this.properties = properties;
        this.classes = classes;
        this.propertyClasses = propertyClasses;

        // assert fitTypes();
        // last || is for debug
        assert (this instanceof SerializedTable || this instanceof ImplementTable.InconsistentTable || classes == null) || (classes.isEqual(keys.getSet()) && propClassesFull() && assertClasses()); // see ClassExprWhere.getKeyType
    }

    public ClassWhere<KeyField> getClasses() {
        return classes;
    }

    private boolean assertClasses() {
        if(classes == null)
            return true;

        for(ClassWhere<Field> pClasses : propertyClasses.valueIt()) {
            assert pClasses.means(BaseUtils.immutableCast(classes), true);
        }
        return true;
    }

    private <K extends Field> ImMap<K, DataClass> getDataFields(ImSet<K> fields) {
        return BaseUtils.immutableCast(fields.mapValues((K value) -> value.type).filterFnValues(element -> element instanceof DataClass));
    }
    private boolean fitTypes() {
        ImMap<KeyField, DataClass> keyDataFields = getDataFields(keys.getSet());
        if(!classes.fitDataClasses(keyDataFields))
            return false;

        for(int i=0,size=propertyClasses.size();i<size;i++)
            if(!propertyClasses.getValue(i).fitDataClasses(MapFact.addExcl(keyDataFields, getDataFields(SetFact.singleton(propertyClasses.getKey(i))))))
                return false;
        return true;
    }
    private boolean propClassesFull() {
        if(!BaseUtils.hashEquals(propertyClasses.keys(), properties))
            return false;

        for(int i=0,size=propertyClasses.size();i<size;i++)
            if(!propertyClasses.getValue(i).isEqual(SetFact.addExcl(keys.getSet(), propertyClasses.getKey(i))))
                return false;
        return true;
    }

    protected static ImMap<PropertyField, PropStat> getStatProps(final StoredTable table) { // для мн-го наследования
        return table.properties.mapValues((PropertyField prop) -> getStatProp(table, prop));
    }

    public abstract String getName(SQLSyntax syntax);

    public ImMap<PropertyField, Type> getPropTypes() {
        return properties.mapValues((PropertyField value) -> value.type);
    }

    public KeyField findKey(String name) {
        for(KeyField key : keys)
            if(key.getName().equals(name))
                return key;
        return null;
    }

    public PropertyField findProperty(String name) {
        for(PropertyField property : properties)
            if(property.getName().equals(name))
                return property;
        return null;
    }

    public abstract ImMap<PropertyField, PropStat> getStatProps();

    @Override
    public PropStat getStatProp(PropertyField property) {
        return getStatProps().get(property);
    }

    public ClassWhere<Field> getClassWhere(PropertyField property) {
        return propertyClasses.get(property);
    }

    protected void initBaseClasses(final BaseClass baseClass) {
        final ImMap<KeyField, AndClassSet> baseClasses = getTableKeys().mapValues((KeyField value) -> value.type.getBaseClassSet(baseClass));
        classes = new ClassWhere<>(baseClasses);

        propertyClasses = properties.mapValues((Function<PropertyField, ClassWhere<Field>>) value -> new ClassWhere<>(MapFact.addExcl(baseClasses, value, value.type.getBaseClassSet(baseClass))));
    }

    public Query<KeyField, PropertyField> getQuery() {
        return getQuery(false);
    }
    public Query<KeyField, PropertyField> getQuery(boolean noFilesAndLogs) {
        QueryBuilder<KeyField, PropertyField> query = new QueryBuilder<>(this);
        lsfusion.server.data.query.build.Join<PropertyField> join = join(query.getMapExprs());
        ImMap<PropertyField, Expr> exprs = properties.mapValues(join::getExpr);
        query.and(join.getWhere());
        if(noFilesAndLogs)
            exprs = exprs.filterFn(element -> !(element.type instanceof FileClass || element.getName().contains("_LG_") || element.getName().contains("_LOG_")));
        query.addProperties(exprs);
        return query.getQuery();
    }

    public void out(SQLSession session) throws SQLException, SQLHandledException {
        getQuery().outSelect(session);
    }
    public void outClasses(SQLSession session, BaseClass baseClass) throws SQLException, SQLHandledException {
        getQuery().outClassesSelect(session, baseClass);
    }
    public void outClasses(SQLSession session, BaseClass baseClass, Processor<String> processor) throws SQLException, SQLHandledException {
        getQuery().outClassesSelect(session, baseClass, processor);
    }

    public ImOrderMap<ImMap<KeyField, DataObject>,ImMap<PropertyField, ObjectValue>> read(SQLSession session, BaseClass baseClass, OperationOwner owner) throws SQLException, SQLHandledException {
        return getQuery().executeClasses(session, baseClass, owner);
    }
    public void readData(SQLSession session, BaseClass baseClass, OperationOwner owner, boolean noFilesAndLogs, ResultHandler<KeyField, PropertyField> result) throws SQLException, SQLHandledException {
        getQuery(noFilesAndLogs).executeSQL(session, MapFact.EMPTYORDER(), LimitOffset.NOLIMIT, false, DataSession.emptyEnv(owner), result);
    }

    public boolean calcTwins(TwinImmutableObject o) {
        return propertyClasses.equals(((StoredTable) o).propertyClasses) && classes.equals(((StoredTable) o).classes);
    }

    public int immutableHashCode() {
        return 31 * propertyClasses.hashCode() + classes.hashCode();
    }

}

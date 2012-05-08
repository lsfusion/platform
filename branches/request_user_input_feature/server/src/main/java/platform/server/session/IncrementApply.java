package platform.server.session;

import org.apache.poi.ss.formula.Formula;
import platform.base.BaseUtils;
import platform.interop.action.ClientAction;
import platform.server.Message;
import platform.server.ParamMessage;
import platform.server.Settings;
import platform.server.caches.IdentityLazy;
import platform.server.caches.MapValuesIterable;
import platform.server.caches.hash.HashValues;
import platform.server.classes.BaseClass;
import platform.server.classes.ConcreteCustomClass;
import platform.server.classes.ConcreteObjectClass;
import platform.server.data.*;
import platform.server.data.expr.KeyExpr;
import platform.server.data.query.Query;
import platform.server.data.translator.MapValuesTranslate;
import platform.server.data.type.Type;
import platform.server.data.where.Where;
import platform.server.data.where.WhereBuilder;
import platform.server.form.instance.FormInstance;
import platform.server.form.instance.PropertyObjectInterfaceInstance;
import platform.server.logics.BusinessLogics;
import platform.server.logics.DataObject;
import platform.server.logics.property.*;
import platform.server.logics.table.ImplementTable;

import java.sql.SQLException;
import java.util.*;

import static platform.base.BaseUtils.crossJoin;
import static platform.base.BaseUtils.objectInstancer;

// вообщем то public потому как иначе aspect не ловит
public class IncrementApply extends OverrideModifier implements ExecutionEnvironmentInterface {

    public final NoUpdate noUpdate = new NoUpdate();
    public final IncrementProps increment = new IncrementProps();
    public final DataSession session;

    public IncrementApply(DataSession session) {
        lateInit(noUpdate, increment, session);
        this.session = session;
    }

    public void cleanIncrementTables() throws SQLException {
        increment.cleanIncrementTables(session.sql);
    }

    public <P extends PropertyInterface> void updateApplyStart(OldProperty<P> property, SinglePropertyTableUsage<P> tableUsage, BaseClass baseClass) throws SQLException { // изврат конечно
        assert !rollbacked;

        SinglePropertyTableUsage<P> prevTable = increment.getTable(property);
        if(prevTable==null) {
            prevTable = property.createChangeTable();
            increment.add(property, prevTable);
        }
        Map<P, KeyExpr> mapKeys = property.getMapKeys();
        prevTable.addRows(session.sql, mapKeys, property.getExpr(mapKeys), tableUsage.join(mapKeys).getWhere(), baseClass, session.env); // если он уже был в базе он не заместится
        increment.addChange(property);
        tableUsage.drop(session.sql);
    }

    public Map<ImplementTable, Collection<Property>> groupPropertiesByTables() {
        return BaseUtils.group(
               new BaseUtils.Group<ImplementTable, Property>() {
                   public ImplementTable group(Property key) {
                       if(key.isStored())
                           return key.mapTable.table;
                       assert key instanceof OldProperty;
                       return null;
                   }
               }, increment.getProperties());
    }

    @Message("message.increment.read.properties")
    public <P extends PropertyInterface> SessionTableUsage<KeyField, Property> readSave(ImplementTable table, @ParamMessage Collection<Property> properties) throws SQLException {
        assert !rollbacked;

        SessionTableUsage<KeyField, Property> changeTable =
                new SessionTableUsage<KeyField, Property>(table.keys, new ArrayList<Property>(properties), Field.<KeyField>typeGetter(),
                                                          new Type.Getter<Property>() {
                                                              public Type getType(Property key) {
                                                                  return key.getType();
                                                              }
                                                          });

        // подготавливаем запрос
        Query<KeyField, Property> changesQuery = new Query<KeyField, Property>(table.keys);
        WhereBuilder changedWhere = new WhereBuilder();
        for (Property<P> property : properties)
            changesQuery.properties.put(property, property.getIncrementExpr(BaseUtils.join(property.mapTable.mapKeys, changesQuery.mapKeys), this, changedWhere));
        changesQuery.and(changedWhere.toWhere());

        // подготовили - теперь надо сохранить в курсор и записать классы
        changeTable.writeRows(session.sql, changesQuery, session.baseClass, session.env);
        return changeTable;
    }

    @Override
    public PropertyChanges getPropertyChanges() {
        assert !rollbacked;
        return super.getPropertyChanges();
    }

    public boolean apply(BusinessLogics<?> BL, List<ClientAction> actions) throws SQLException {
        throw new RuntimeException("apply is not allowed in event");
    }

    boolean rollbacked = false;
    public ExecutionEnvironmentInterface cancel() throws SQLException {
        assert !rollbacked;

        // не надо DROP'ать так как Rollback автоматически drop'ает все temporary таблицы
        cleanIncrementTables();
        session.rollbackTransaction();
        rollbacked = true;

        return session;
    }

    public DataSession getSession() {
        return session;
    }

    public Modifier getModifier() {
        return this;
    }

    public FormInstance getFormInstance() {
        return null;
    }

    public boolean isInTransaction() {
        return true;
    }

    public <P extends PropertyInterface> void fireChange(Property<P> property, PropertyChange<P> change) throws SQLException {
    }

    public DataObject addObject(ConcreteCustomClass cls) throws SQLException {
        return session.addObject(cls);
    }

    public void changeClass(PropertyObjectInterfaceInstance objectInstance, DataObject object, ConcreteObjectClass cls, boolean groupLast) throws SQLException {
        session.changeClass(objectInstance, object, cls, groupLast);
    }
}

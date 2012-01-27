package platform.server.session;

import org.apache.poi.ss.formula.Formula;
import platform.base.BaseUtils;
import platform.server.Message;
import platform.server.ParamMessage;
import platform.server.Settings;
import platform.server.caches.IdentityLazy;
import platform.server.caches.MapValuesIterable;
import platform.server.caches.hash.HashValues;
import platform.server.classes.BaseClass;
import platform.server.data.*;
import platform.server.data.expr.KeyExpr;
import platform.server.data.query.Query;
import platform.server.data.translator.MapValuesTranslate;
import platform.server.data.type.Type;
import platform.server.data.where.Where;
import platform.server.data.where.WhereBuilder;
import platform.server.logics.property.*;
import platform.server.logics.table.ImplementTable;

import java.sql.SQLException;
import java.util.*;

import static platform.base.BaseUtils.crossJoin;
import static platform.base.BaseUtils.objectInstancer;

// вообщем то public потому как иначе aspect не ловит
public class IncrementApply extends OverrideModifier {

    public final NoUpdate noUpdate = new NoUpdate();
    public final IncrementProps increment = new IncrementProps();
    public final DataSession session;

    public IncrementApply(DataSession session) {
        lateInit(noUpdate, increment, session);
        this.session = session;
    }

    public void cleanIncrementTables() throws SQLException {
        increment.cleanIncrementTables(session.sql);
        applyStart.cleanIncrementTables(session.sql);
    }

    public <P extends PropertyInterface> void updateApplyStart(Property<P> property, SinglePropertyTableUsage<P> tableUsage, BaseClass baseClass) throws SQLException { // изврат конечно
        SinglePropertyTableUsage<P> prevTable = applyStart.getTable(property);
        if(prevTable==null) {
            prevTable = property.createChangeTable();
            applyStart.add(property, prevTable);
        }
        Map<P, KeyExpr> mapKeys = property.getMapKeys();
        prevTable.addRows(session.sql, mapKeys, property.getExpr(mapKeys), tableUsage.join(mapKeys).getWhere(), baseClass, session.env); // если он уже был в базе он не заместится
        applyStart.addChange(property);
    }

    public final IncrementProps applyStart = new IncrementProps();

    public Map<ImplementTable, Collection<Property>> groupPropertiesByTables() {
       return BaseUtils.group(
               new BaseUtils.Group<ImplementTable, Property>() {
                   public ImplementTable group(Property key) {
                       return key.mapTable.table;
                   }
               }, increment.getProperties());
    }

    @Message("message.increment.read.properties")
    public <P extends PropertyInterface> SessionTableUsage<KeyField, Property> readSave(ImplementTable table, @ParamMessage Collection<Property> properties) throws SQLException {
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

}

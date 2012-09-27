package platform.server.integration;

import platform.server.Message;
import platform.server.classes.IntegerClass;
import platform.server.data.Modify;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.query.GroupExpr;
import platform.server.data.expr.query.GroupType;
import platform.server.data.expr.where.cases.CaseExpr;
import platform.server.data.query.Query;
import platform.server.data.where.Where;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.logics.property.CalcPropertyImplement;
import platform.server.logics.property.PropertyInterface;
import platform.server.session.*;

import java.sql.SQLException;
import java.util.*;

public class IntegrationService {
    private ImportTable table;
    private Collection<ImportProperty<?>> properties;
    private Collection<? extends ImportKey<?>> keys;
    private DataSession session;
    private Collection<ImportDelete> deletes;

    public IntegrationService(DataSession session, ImportTable table, Collection<? extends ImportKey<?>> keys,
                              Collection<ImportProperty<?>> properties) {
        this(session, table, keys, properties, null);
    }

    public IntegrationService(DataSession session, ImportTable table, Collection<? extends ImportKey<?>> keys,
                              Collection<ImportProperty<?>> properties, Collection<ImportDelete> deletes) {
        this.session = session;
        this.table = table;
        this.properties = properties;
        this.keys = keys;
        this.deletes = deletes;
    }

    public SessionTableUsage<String, ImportField> synchronize() throws SQLException {
        return synchronize(false);
    }

    public SessionTableUsage<String, ImportField> synchronize(boolean replaceNull) throws SQLException {
        return synchronize(replaceNull, true);
    }

    @Message("message.synchronize")
    public SessionTableUsage<String, ImportField> synchronize(boolean replaceNull, boolean replaceEqual) throws SQLException {
        SingleKeyTableUsage<ImportField> importTable = new SingleKeyTableUsage<ImportField>(IntegerClass.instance, table.fields, ImportField.typeGetter);

        int counter = 0;
        for (PlainDataTable.Row row : table) {
            Map<ImportField, ObjectValue> insertRow = new HashMap<ImportField, ObjectValue>();
            for (ImportField field : table.fields)
                insertRow.put(field, ObjectValue.getValue(row.getValue(field), field.getFieldClass()));
            importTable.modifyRecord(session.sql, new DataObject(counter++), insertRow, Modify.ADD);
        }

        if (deletes != null) {
            deleteObjects(importTable);
        }

        // приходится через addKeys, так как synchronize сам не может resolv'ить сессию на добавление
        Map<ImportKey<?>, SinglePropertyTableUsage<?>> addedKeys = new HashMap<ImportKey<?>, SinglePropertyTableUsage<?>>();
        for (ImportKey<?> key : keys)
            if(!key.skipKey)
                addedKeys.put(key, key.synchronize(session, importTable));

        DataChanges propertyChanges = new DataChanges();
        for (ImportProperty<?> property : properties)
            propertyChanges = propertyChanges.add(property.synchronize(session, importTable, addedKeys, replaceNull, replaceEqual));
        
        System.gc();

        session.change(propertyChanges);

        return importTable;
    }

    private <P extends PropertyInterface> void deleteObjects(SingleKeyTableUsage<ImportField> importTable) throws SQLException {
        for (ImportDelete delete : deletes) {
            KeyExpr keyExpr = new KeyExpr("key");

            Where deleteWhere = Where.TRUE;

            // выражения для полей в импортируемой таблице
            Map<ImportField, Expr> importExprs = importTable.join(importTable.getMapKeys()).getExprs();

            // фильтруем только те, которых нету в ImportTable
            if (!delete.deleteAll)
                deleteWhere = deleteWhere.and(GroupExpr.create(Collections.singletonMap("key",
                                           delete.key.getExpr(importExprs, session.getModifier())),
                                           Where.TRUE,
                                           Collections.singletonMap("key", keyExpr)).getWhere().not());

            Map<P, KeyExpr> intraKeyExprs = delete.deleteProperty.property.getMapKeys(); // генерим ключи (использовать будем только те, что не в DataObject
            Map<P, Expr> deleteExprs = new HashMap<P, Expr>();
            KeyExpr groupExpr = null;
            for (Map.Entry<P, ImportDeleteInterface> entry : ((CalcPropertyImplement<P, ImportDeleteInterface>)delete.deleteProperty).mapping.entrySet()) {
                P propInt = entry.getKey();
                KeyExpr intraKeyExpr = intraKeyExprs.get(propInt);
                if (delete.key.equals(entry.getValue())) {
                    groupExpr = intraKeyExpr; // собственно группируем по этому ключу
                    deleteExprs.put(propInt, groupExpr);
                } else
                    deleteExprs.put(propInt, entry.getValue().getDeleteExpr(importTable, intraKeyExpr, session.getModifier()));
            }

            deleteWhere = deleteWhere.and(GroupExpr.create(Collections.singletonMap("key", groupExpr),
                                       delete.deleteProperty.property.getExpr(deleteExprs, session.getModifier()),
                                       GroupType.ANY, Collections.singletonMap("key", keyExpr)).getWhere());

            session.changeClass(new ClassChange(keyExpr, deleteWhere, CaseExpr.NULL));
        }
    }
}

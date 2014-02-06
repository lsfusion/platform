package lsfusion.server.integration;

import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.mutable.MExclMap;
import lsfusion.base.col.interfaces.mutable.add.MAddSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.base.col.interfaces.mutable.mapvalue.ImValueMap;
import lsfusion.server.Message;
import lsfusion.server.classes.ConcreteCustomClass;
import lsfusion.server.classes.IntegerClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.KeyExpr;
import lsfusion.server.data.expr.query.GroupExpr;
import lsfusion.server.data.expr.query.GroupType;
import lsfusion.server.data.expr.where.cases.CaseExpr;
import lsfusion.server.data.where.Where;
import lsfusion.server.logics.DBManager;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.property.CalcPropertyImplement;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.DataProperty;
import lsfusion.server.logics.property.PropertyInterface;
import lsfusion.server.session.*;

import java.sql.SQLException;
import java.util.Collection;

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

    public void synchronize() throws SQLException, SQLHandledException {
        synchronize(false);
    }

    public void synchronize(boolean replaceNull) throws SQLException, SQLHandledException {
        synchronize(replaceNull, true);
    }

    @Message("message.synchronize")
    public void synchronize(boolean replaceNull, boolean replaceEqual) throws SQLException, SQLHandledException {
        SingleKeyTableUsage<ImportField> importTable = new SingleKeyTableUsage<ImportField>(IntegerClass.instance, table.fields, ImportField.typeGetter);
        
        MExclMap<ImMap<String, DataObject>, ImMap<ImportField, ObjectValue>> mRows = MapFact.mExclMap();
        int counter = 0;
        for (final PlainDataTable.Row row : table)
            mRows.exclAdd(MapFact.singleton("key", new DataObject(counter++)), table.fields.getSet().mapValues(new GetValue<ObjectValue, ImportField>() {
                public ObjectValue getMapValue(ImportField value) {
                    return ObjectValue.getValue(row.getValue(value), value.getFieldClass());
                }}));
        importTable.writeRows(session.sql, mRows.immutable());

        if (deletes != null) {
            deleteObjects(importTable);
        }

        // приходится через addKeys, так как synchronize сам не может resolv'ить сессию на добавление
        MExclMap<ImportKey<?>, SinglePropertyTableUsage<?>> mAddedKeys = MapFact.mExclMapMax(keys.size());
        for (ImportKey<?> key : keys)
            if(!key.skipKey && key.keyClass instanceof ConcreteCustomClass)
                mAddedKeys.exclAdd(key, key.synchronize(session, importTable));
        ImMap<ImportKey<?>, SinglePropertyTableUsage<?>> addedKeys = mAddedKeys.immutable();

        MAddSet<SinglePropertyTableUsage<ClassPropertyInterface>> usedPropTables = SetFact.mAddSet();
        DataChanges propertyChanges = DataChanges.EMPTY;
        try {
            for (ImportProperty<?> property : properties) {
                DataChanges synchronize = property.synchronize(session, importTable, addedKeys, replaceNull, replaceEqual);
                for(DataProperty change : synchronize.getProperties()) {
                    SinglePropertyTableUsage<ClassPropertyInterface> materialize = synchronize.get(change).materialize(change, session); // materialize'им чтобы избавится от таблиц из хинтов, которые при change'е могут сброситься
                    usedPropTables.add(materialize);
                    propertyChanges.add(new DataChanges(change, SinglePropertyTableUsage.getChange(materialize)));
                }
            }
            session.change(propertyChanges);
        } finally {
            for(SinglePropertyTableUsage<ClassPropertyInterface> usedPropTable : usedPropTables)
                usedPropTable.drop(session.sql);
            
            for(SinglePropertyTableUsage<?> addedTable : addedKeys.valueIt())
                addedTable.drop(session.sql);
    
            importTable.drop(session.sql);
        }
    }

    private <P extends PropertyInterface> void deleteObjects(SingleKeyTableUsage<ImportField> importTable) throws SQLException, SQLHandledException {
        for (ImportDelete delete : deletes) {
            KeyExpr keyExpr = new KeyExpr("key");

            Where deleteWhere = Where.TRUE;

            // выражения для полей в импортируемой таблице
            ImMap<ImportField, Expr> importExprs = importTable.join(importTable.getMapKeys()).getExprs();

            // фильтруем только те, которых нету в ImportTable
            if (!delete.deleteAll)
                deleteWhere = deleteWhere.and(GroupExpr.create(MapFact.singleton("key",
                                           delete.key.getExpr(importExprs, session.getModifier())),
                                           Where.TRUE,
                                           MapFact.singleton("key", keyExpr)).getWhere().not());

            ImRevMap<P, KeyExpr> intraKeyExprs = delete.deleteProperty.property.getMapKeys(); // генерим ключи (использовать будем только те, что не в DataObject
            KeyExpr groupExpr = null;
            ImMap<P, ImportDeleteInterface> deleteMapping = ((CalcPropertyImplement<P, ImportDeleteInterface>) delete.deleteProperty).mapping;
            ImValueMap<P,Expr> mvDeleteExprs = deleteMapping.mapItValues();
            for (int i=0,size=deleteMapping.size();i<size;i++) {
                KeyExpr intraKeyExpr = intraKeyExprs.get(deleteMapping.getKey(i));
                ImportDeleteInterface deleteInterface = deleteMapping.getValue(i);
                if (delete.key.equals(deleteInterface)) {
                    groupExpr = intraKeyExpr; // собственно группируем по этому ключу
                    mvDeleteExprs.mapValue(i, groupExpr);
                } else
                    mvDeleteExprs.mapValue(i, deleteInterface.getDeleteExpr(importTable, intraKeyExpr, session.getModifier()));
            }

            deleteWhere = deleteWhere.and(GroupExpr.create(MapFact.singleton("key", groupExpr),
                                       delete.deleteProperty.property.getExpr(mvDeleteExprs.immutableValue(), session.getModifier()),
                                       GroupType.ANY, MapFact.singleton("key", keyExpr)).getWhere());

            session.changeClass(new ClassChange(keyExpr, deleteWhere, CaseExpr.NULL));
        }
    }
}

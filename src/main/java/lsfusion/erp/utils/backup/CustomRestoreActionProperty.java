package lsfusion.erp.utils.backup;

import com.google.common.base.Throwables;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.mutable.MExclMap;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.interop.Compare;
import lsfusion.interop.action.MessageClientAction;
import lsfusion.server.classes.ConcreteCustomClass;
import lsfusion.server.classes.IntegerClass;
import lsfusion.server.classes.UnknownClass;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.OperationOwner;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.SQLSession;
import lsfusion.server.data.expr.KeyExpr;
import lsfusion.server.data.query.Join;
import lsfusion.server.data.query.QueryBuilder;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.where.Where;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.NullValue;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.linear.LP;
import lsfusion.server.logics.property.CalcProperty;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.property.StoredDataProperty;
import lsfusion.server.logics.scripted.ScriptingActionProperty;
import lsfusion.server.logics.scripted.ScriptingErrorLog;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;
import lsfusion.server.session.DataSession;
import lsfusion.server.session.PropertyChange;
import lsfusion.server.session.SingleKeyTableUsage;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

import static org.apache.commons.lang.StringUtils.trimToNull;

public class CustomRestoreActionProperty extends ScriptingActionProperty {
    private final ClassPropertyInterface backupInterface;

    public CustomRestoreActionProperty(ScriptingLogicsModule LM, ValueClass... classes) throws ScriptingErrorLog.SemanticErrorException {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        backupInterface = i.next();
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        DataObject backupObject = context.getDataKeyValue(backupInterface);
        String dbName = null;
        try {
            String fileBackup = (String) findProperty("fileBackup").read(context, backupObject);
            Map<String, CustomRestoreTable> tables = getTables(context);
            if (new File(fileBackup).exists() && !tables.isEmpty()) {
                dbName = context.getDbManager().customRestoreDB(fileBackup, tables.keySet());
                importColumns(context, dbName, tables);
            } else {
                context.requestUserInteraction(new MessageClientAction("Backup File not found or no selected tables", "Error"));
            }
        } catch (Exception e) {
            throw Throwables.propagate(e);
        } finally {
            try {
                if (dbName != null)
                    context.getDbManager().dropDB(dbName);
            } catch (IOException ignored) {
            }
        }
    }

    private Map<String, CustomRestoreTable> getTables(ExecutionContext context) throws ScriptingErrorLog.SemanticErrorException, SQLException, SQLHandledException {
        KeyExpr tableExpr = new KeyExpr("Table");
        ImRevMap<Object, KeyExpr> tableKeys = MapFact.<Object, KeyExpr>singletonRev("Table", tableExpr);
        QueryBuilder<Object, Object> tableQuery = new QueryBuilder<>(tableKeys);
        tableQuery.addProperty("sidTable", findProperty("sidTable").getExpr(context.getModifier(), tableExpr));
        tableQuery.and(findProperty("sidTable").getExpr(context.getModifier(), tableExpr).getWhere());
        tableQuery.and(findProperty("inCustomRestoreTable").getExpr(context.getModifier(), tableExpr).getWhere());
        ImOrderMap<ImMap<Object, DataObject>, ImMap<Object, ObjectValue>> tableResult = tableQuery.executeClasses(context.getSession());
        Map<String, CustomRestoreTable> tables = new HashMap<>();
        Map<String, DataObject> tableMap = new HashMap<>();
        for (int i = 0; i < tableResult.size(); i++) {
            DataObject tableObject = tableResult.getKey(i).get("Table");
            String sidTable = trimToNull((String) tableResult.getValue(i).get("sidTable").getValue());
            tableMap.put(sidTable, tableObject);
        }

        for (Map.Entry<String, DataObject> tableEntry : tableMap.entrySet()) {

            //columns
            KeyExpr tableColumnExpr = new KeyExpr("TableColumn");
            ImRevMap<Object, KeyExpr> tableColumnKeys = MapFact.<Object, KeyExpr>singletonRev("TableColumn", tableColumnExpr);
            QueryBuilder<Object, Object> tableColumnQuery = new QueryBuilder<>(tableColumnKeys);
            tableColumnQuery.addProperty("sidTableColumn", findProperty("sidTableColumn").getExpr(context.getModifier(), tableColumnExpr));
            tableColumnQuery.addProperty("canonicalNameTableColumn", findProperty("canonicalNameTableColumn").getExpr(context.getModifier(), tableColumnExpr));
            tableColumnQuery.addProperty("replaceOnlyNullTableColumn", findProperty("replaceOnlyNullTableColumn").getExpr(context.getModifier(), tableColumnExpr));
            tableColumnQuery.and(findProperty("propertyTableColumn").getExpr(context.getModifier(), tableColumnExpr).getWhere());
            tableColumnQuery.and(findProperty("inCustomRestoreTableColumn").getExpr(context.getModifier(), tableColumnExpr).getWhere());
            tableColumnQuery.and(findProperty("canonicalNameTableColumn").getExpr(context.getModifier(), tableColumnExpr).getWhere());
            tableColumnQuery.and(findProperty("tableTableColumn").getExpr(context.getModifier(), tableColumnExpr).compare(tableEntry.getValue().getExpr(), Compare.EQUALS));
            ImOrderMap<ImMap<Object, Object>, ImMap<Object, Object>> tableColumnResult = tableColumnQuery.execute(context.getSession());
            for (ImMap<Object, Object> columnEntry : tableColumnResult.values()) {
                String sidTableColumn = trimToNull((String) columnEntry.get("sidTableColumn"));
                String canonicalNameTableColumn = trimToNull((String) columnEntry.get("canonicalNameTableColumn"));
                CustomRestoreTable table = tables.get(tableEntry.getKey());
                if (table == null)
                    table = new CustomRestoreTable();
                if(columnEntry.get("replaceOnlyNullTableColumn") != null)
                    table.replaceOnlyNullSet.add(canonicalNameTableColumn);
                table.sqlProperties.add(sidTableColumn);
                table.lcpProperties.add(canonicalNameTableColumn.split("\\[")[0]);
                tables.put(tableEntry.getKey(), table);
            }

            //keys
            KeyExpr tableKeyExpr = new KeyExpr("TableKey");
            ImRevMap<Object, KeyExpr> tableKeyKeys = MapFact.<Object, KeyExpr>singletonRev("TableKey", tableKeyExpr);
            QueryBuilder<Object, Object> tableKeyQuery = new QueryBuilder<>(tableKeyKeys);
            tableKeyQuery.addProperty("nameTableKey", findProperty("nameTableKey").getExpr(context.getModifier(), tableKeyExpr));
            tableKeyQuery.addProperty("classSIDTableKey", findProperty("classSIDTableKey").getExpr(context.getModifier(), tableKeyExpr));
            tableKeyQuery.and(findProperty("nameTableKey").getExpr(context.getModifier(), tableKeyExpr).getWhere());
            tableKeyQuery.and(findProperty("tableTableKey").getExpr(context.getModifier(), tableKeyExpr).compare(tableEntry.getValue().getExpr(), Compare.EQUALS));
            ImOrderMap<ImMap<Object, Object>, ImMap<Object, Object>> tableKeyResult = tableKeyQuery.execute(context.getSession(), MapFact.singletonOrder((Object) "nameTableKey", false));
            for (ImMap<Object, Object> keyEntry : tableKeyResult.values()) {
                String nameTableKey = trimToNull((String) keyEntry.get("nameTableKey"));
                String classTableKey = trimToNull((String) keyEntry.get("classSIDTableKey"));
                CustomRestoreTable table = tables.get(tableEntry.getKey());
                if (table == null)
                    table = new CustomRestoreTable();
                table.keys.add(nameTableKey);
                table.classKeys.add(classTableKey);
                tables.put(tableEntry.getKey(), table);
            }
        }
        return tables;
    }

    private void importColumns(final ExecutionContext context, String dbName, Map<String, CustomRestoreTable> tables) {
        try {
            for (final Map.Entry<String, CustomRestoreTable> tableEntry : tables.entrySet()) {
                String tableName = tableEntry.getKey();
                CustomRestoreTable table = tableEntry.getValue();

                List<List<List<Object>>> data = context.getDbManager().readCustomRestoredColumns(dbName, tableName, table.keys, table.sqlProperties);
                List<List<Object>> keys = data.get(0);
                List<List<Object>> columns = data.get(1);

                //step1: props, mRows
                final ImOrderSet<LP> props = getProps(context.getBL().findProperties(table.lcpProperties.toArray(new String[table.lcpProperties.size()])));
                MExclMap<ImMap<String, DataObject>, ImMap<LP, ObjectValue>> mRows = MapFact.mExclMap();

                for (int i = 0; i < columns.size(); i++) {
                    List<Object> keysEntry = keys.get(i);
                    final List<Object> columnsEntry = columns.get(i);

                    //step2: exclAdd
                    ImMap<String, DataObject> keysMap = MapFact.EMPTY();
                    for (int k = 0; k < keysEntry.size(); k++) {
                        ValueClass valueClass = context.getBL().findClass(table.classKeys.get(k).replace("_", "."));
                        DataObject keyObject = context.getSession().getDataObject(valueClass, keysEntry.get(k));
                        if (keyObject.objectClass instanceof UnknownClass) {
                            try (DataSession session = context.getSession().createSession()) {
                                //приходится пока через новую сессию, иначе свойства для новосозданного объекта не проставляются
                                keyObject = session.addObject((ConcreteCustomClass) valueClass, keyObject);
                                keyObject.object = keysEntry.get(k);
                                session.apply(context);
                                keyObject = context.getSession().getDataObject(valueClass, keysEntry.get(k));
                            }
                        }
                        keysMap = keysMap.addExcl("key", keyObject); //что, если несколько ключей?
                    }

                    mRows.exclAdd(keysMap, props.getSet().mapValues(new GetValue<ObjectValue, LP>() {
                        public ObjectValue getMapValue(LP prop) {
                            try {
                                Object object = columnsEntry.get(props.indexOf(prop));
                                if (object == null) return NullValue.instance;
                                ValueClass classValue = ((StoredDataProperty) prop.property).value;
                                if (classValue instanceof ConcreteCustomClass) {
                                    return context.getSession().getDataObject(((StoredDataProperty) prop.property).value, object);
                                } else
                                    return object instanceof String ? new DataObject(((String) object).trim()) : object instanceof Integer ? new DataObject((Integer) object)
                                            : object instanceof Boolean ? new DataObject((Boolean) object) : new DataObject(String.valueOf(object));
                            } catch (SQLException | SQLHandledException e) {
                                return null;
                            }
                        }
                    }));
                }

                //step3: writeRows
                String result = writeRows(context, props, mRows, table.replaceOnlyNullSet);
                if (result != null)
                    context.requestUserInteraction(new MessageClientAction(result, "Error restoring table " + tableName));

                /*try(DataSession session = context.createSession()) {
                    Map<Integer, DataObject> objectMap = new HashMap<>();
                    for (int i = 0; i < columns.size(); i++) {
                        List<Object> columnEntry = columns.get(i);
                        List<Object> keyEntry = keys.get(i);
                        for (int j = 0; j < columnEntry.size(); j++) {
                            String propertySID = table.lcpProperties.get(j);
                            Object value = columnEntry.get(j);
                            List<DataObject> keyObjects = new ArrayList<>();
                            for(int k = 0; k < table.classKeys.size(); k++) {
                                DataObject keyObject;
                                Integer keyValue = (Integer) keyEntry.get(k);
                                keyObject = objectMap.get(keyValue);
                                ValueClass valueClass = context.getBL().findClass(table.classKeys.get(k).replace("_", "."));
                                if(keyObject == null) {
                                    keyObject = session.getDataObject(valueClass, keyValue);
                                    if (keyObject.objectClass instanceof UnknownClass) {
                                        keyObject = session.addObject((ConcreteCustomClass) valueClass);
                                        keyObject.object = keyValue;
                                    }
                                    objectMap.put(keyValue, keyObject);
                                }
                                keyObjects.add(new DataObject(keyEntry.get(k), (ConcreteClass) valueClass));
                            }
                            context.getBL().findProperty(propertySID).change(value, session, keyObjects.toArray(new DataObject[keyObjects.size()]));
                        }
                    }
                    session.apply(context);
                }*/

            }
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    private ImOrderSet<LP> getProps(LP[] properties) {
        return SetFact.fromJavaOrderSet(new ArrayList<>(Arrays.asList(properties)));
    }

    private String writeRows(ExecutionContext context, ImOrderSet<LP> props, MExclMap<ImMap<String, DataObject>, ImMap<LP, ObjectValue>> mRows, Set<String> replaceOnlyNullSet)
            throws SQLException, SQLHandledException, ScriptingErrorLog.SemanticErrorException {
        SingleKeyTableUsage<LP> importTable = new SingleKeyTableUsage<>(IntegerClass.instance, props, new Type.Getter<LP>() {
            @Override
            public Type getType(LP key) {
                return key.property.getType();
            }
        });
        DataSession session = context.getSession();
        OperationOwner owner = session.getOwner();
        SQLSession sql = session.sql;
        importTable.writeRows(sql, mRows.immutable(), owner);

        ImRevMap<String, KeyExpr> mapKeys = importTable.getMapKeys();
        Join<LP> importJoin = importTable.join(mapKeys);
        try {
            for (LP lcp : props) {
                ImMap<Object, Object> values = MapFact.EMPTY();
                for (int i = 0; i < mapKeys.values().size(); i++) {
                    values = values.addExcl(lcp.listInterfaces.get(i), mapKeys.values().get(i));
                }
                Where where = importJoin.getWhere();
                if (replaceOnlyNullSet.contains(lcp.property.getSID())) {
                    where = where.and(((CalcProperty)lcp.property).getExpr(values).getWhere().not());
                }
                PropertyChange propChange = new PropertyChange(values.toRevMap(), importJoin.getExpr(lcp), where);
                context.getEnv().change((CalcProperty) lcp.property, propChange);
            }
        } finally {
            importTable.drop(sql, owner);
        }
        return session.applyMessage(context);
    }
}

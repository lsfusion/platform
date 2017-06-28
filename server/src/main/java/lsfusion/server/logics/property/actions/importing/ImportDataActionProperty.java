package lsfusion.server.logics.property.actions.importing;

import com.google.common.base.Throwables;
import lsfusion.base.BaseUtils;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.mutable.MExclMap;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetIndex;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.interop.action.MessageClientAction;
import lsfusion.server.classes.*;
import lsfusion.server.data.OperationOwner;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.SQLSession;
import lsfusion.server.data.expr.KeyExpr;
import lsfusion.server.data.query.Join;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.where.Where;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.NullValue;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.i18n.LocalizedString;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.*;
import lsfusion.server.logics.property.actions.SystemActionProperty;
import lsfusion.server.logics.property.actions.flow.FlowResult;
import lsfusion.server.logics.property.actions.importing.dbf.ImportDBFDataActionProperty;
import lsfusion.server.logics.property.actions.importing.jdbc.ImportJDBCDataActionProperty;
import lsfusion.server.logics.property.actions.importing.mdb.ImportMDBDataActionProperty;
import lsfusion.server.logics.property.derived.DerivedProperty;
import lsfusion.server.session.PropertyChange;
import lsfusion.server.session.SingleKeyTableUsage;
import org.jdom.JDOMException;
import org.xBaseJ.xBaseJException;

import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class ImportDataActionProperty extends SystemActionProperty {

    protected final List<String> ids;
    protected final List<LCP> properties;
    
    private final LCP<?> importedProperty;

    public final static Map<String, Integer> XLSColumnsMapping = new HashMap<>();
    static {
        fillXLSCollumsMapping();
    }

    private static String XLSColumnByIndex(int index) {
        String columnName = "";
        int resultLen = 1;
        final int LETTERS_CNT = 26;
        int sameLenCnt = LETTERS_CNT;
        while (sameLenCnt <= index) {
            ++resultLen;
            index -= sameLenCnt;
            sameLenCnt *= LETTERS_CNT;
        }
        
        for (int i = 0; i < resultLen; ++i) {
            columnName = (char)('A' + (index % LETTERS_CNT)) + columnName; 
            index /= LETTERS_CNT;
        }
        return columnName;
    }
    
    private static void fillXLSCollumsMapping() {
        final int MAX_COLUMN = 256;
        for (int i = 0; i < MAX_COLUMN; ++i) {
            XLSColumnsMapping.put(XLSColumnByIndex(i), i);
        }
    }

    public static ImportDataActionProperty createDBFProperty(int paramsCount, boolean hasWheres, boolean hasMemo, List<String> ids, List<LCP> properties, String charset, BaseLogicsModule baseLM) {
        for (int i = 0; i < ids.size(); ++i) { // для DBF делаем case insensitive
            String id = ids.get(i);
            if (id != null)
                ids.set(i, id.toLowerCase());
            else
                throw new RuntimeException("Import error: field for property " + properties.get(i) + " not specified");
        }

        return new ImportDBFDataActionProperty(paramsCount, hasWheres, hasMemo, ids, properties, charset, baseLM);
    }

    public static ImportDataActionProperty createProperty(ImportSourceFormat format, List<String> ids, List<LCP> properties, BaseLogicsModule baseLM) {
        if (format == ImportSourceFormat.JDBC) {
            return new ImportJDBCDataActionProperty(ids, properties, baseLM);
        } else if (format == ImportSourceFormat.MDB) {
            return new ImportMDBDataActionProperty(ids, properties, baseLM);
        }
        return null;
    }

    public ImportDataActionProperty(int paramsCount, List<String> ids, List<LCP> properties, BaseLogicsModule baseLM) {
        super(LocalizedString.create("Import"), SetFact.toOrderExclSet(paramsCount, new GetIndex<PropertyInterface>() {
            @Override
            public PropertyInterface getMapValue(int i) {
                return new PropertyInterface();
            }
        }));
        this.ids = ids;
        this.properties = properties;
        this.importedProperty = baseLM.imported;
    }

    @Override
    public CalcPropertyMapImplement<?, PropertyInterface> calcWhereProperty() {
        // Сделано по подобию MessageAction
        // TRUE AND a OR (NOT a), т.е. значение всегда TRUE, но при join'е будет учавствовать в classWhere - FULL
        ImList props = ListFact.EMPTY();
        for (PropertyInterface i : interfaces) {
            props.addList(DerivedProperty.createAnd(DerivedProperty.createTrue(), i));
            props.addList(DerivedProperty.createNot(i));
        }
        return DerivedProperty.createUnion(interfaces, props);
    }

    @Override
    protected FlowResult aspectExecute(ExecutionContext<PropertyInterface> context) throws SQLException, SQLHandledException {
        DataObject value = context.getDataKeys().getValue(0);
        assert value.getType() instanceof FileClass;

        Object file = value.object;
        if (file instanceof byte[]) {
            try {
                if (value.getType() instanceof DynamicFormatFileClass) {
                    file = BaseUtils.getFile((byte[]) file);
                }
                ImportIterator iterator = getIterator((byte[]) file);

                Object row;
                int i = 0;
                ImOrderSet<LCP> props = SetFact.fromJavaOrderSet(properties).addOrderExcl(importedProperty);

                for (LCP prop : props) {
                    if (prop.property instanceof DataProperty)
                        context.getSession().dropChanges((DataProperty) prop.property);
                }

                SingleKeyTableUsage<LCP> importTable = new SingleKeyTableUsage<>("idaatabke", IntegerClass.instance, props, new Type.Getter<LCP>() {
                    @Override
                    public Type getType(LCP key) {
                        return key.property.getType();
                    }
                });
                MExclMap<ImMap<String, DataObject>, ImMap<LCP, ObjectValue>> mRows = MapFact.mExclMap();
                while ((row = iterator.nextRow()) != null) {
                    if(row instanceof List) {
                        DataObject rowKey = new DataObject(i++, IntegerClass.instance);
                        final List<String> finalRow = (List<String>) row;
                        mRows.exclAdd(MapFact.singleton("key", rowKey), props.getSet().mapValues(new GetValue<ObjectValue, LCP>() {
                            public ObjectValue getMapValue(LCP prop) {
                                if (prop == importedProperty) {
                                    return ObjectValue.getValue(true, LogicalClass.instance);
                                } else if (properties.indexOf(prop) < finalRow.size()) {
                                    Type type = prop.property.getType();
                                    Object parsedObject = null;
                                    try {
                                        parsedObject = type.parseString(finalRow.get(properties.indexOf(prop)));
                                    } catch (lsfusion.server.data.type.ParseException ignored) {
                                    }
                                    return ObjectValue.getValue(parsedObject, (ConcreteClass) prop.property.getValueClass(ClassType.editPolicy));
                                }

                                return NullValue.instance;
                            }
                        }));
                    }
                }
                OperationOwner owner = context.getSession().getOwner();
                SQLSession sql = context.getSession().sql;
                importTable.writeRows(sql, mRows.immutable(), owner);

                ImRevMap<String, KeyExpr> mapKeys = importTable.getMapKeys();
                Join<LCP> importJoin = importTable.join(mapKeys);
                Where where = importJoin.getWhere();
                try {
                    for (LCP lcp : props) {
                        PropertyChange propChange = new PropertyChange(MapFact.singletonRev(lcp.listInterfaces.single(), mapKeys.singleValue()), importJoin.getExpr(lcp), where);
                        context.getEnv().change((CalcProperty) lcp.property, propChange);
                    }
                } finally {
                    importTable.drop(sql, owner);
                }
                
                iterator.release();

            } catch (IncorrectFileException e) {
                context.delayUserInterfaction(new MessageClientAction(e.getMessage(), "Import Error"));
            } catch (Exception e) {
                Throwables.propagate(e);
            }
        }
        return FlowResult.FINISH;
    }

    protected List<Integer> getSourceColumns(Map<String, Integer> mapping) {
        List<Integer> columns = new ArrayList<>();
        int previousIndex = columnsNumberBase() - 1;
        for (String id : ids) {

            int currentIndex;
            if (id == null) {
                currentIndex = previousIndex + 1;
            } else {
                Integer desiredColumn = mapping.get(id);
                if (desiredColumn != null) {
                    currentIndex = desiredColumn;
                } else {
                    if(ignoreIncorrectColumns())
                        currentIndex = previousIndex + 1;
                    else
                        throw new RuntimeException("Import error: column " + id + " not found");
                }
            }
            columns.add(currentIndex);
            previousIndex = currentIndex;
        }

        return columns;
    }
    
    protected int columnsNumberBase() {
        return 0;
    }

    public abstract ImportIterator getIterator(byte[] file) throws IOException, ParseException, xBaseJException, JDOMException, ClassNotFoundException, IncorrectFileException;

    protected boolean ignoreIncorrectColumns() {
        return true;
    }
}

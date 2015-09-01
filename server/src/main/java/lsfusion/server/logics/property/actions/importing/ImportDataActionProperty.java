package lsfusion.server.logics.property.actions.importing;

import com.google.common.base.Throwables;
import lsfusion.base.BaseUtils;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.mutable.MExclMap;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.server.classes.*;
import lsfusion.server.data.OperationOwner;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.SQLSession;
import lsfusion.server.data.expr.KeyExpr;
import lsfusion.server.data.query.Join;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.where.Where;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.NullValue;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.*;
import lsfusion.server.logics.property.actions.importing.dbf.ImportDBFDataActionProperty;
import lsfusion.server.logics.property.actions.importing.jdbc.ImportJDBCDataActionProperty;
import lsfusion.server.logics.property.actions.importing.mdb.ImportMDBDataActionProperty;
import lsfusion.server.logics.property.actions.importing.xls.ImportXLSDataActionProperty;
import lsfusion.server.logics.property.actions.importing.xlsx.ImportXLSXDataActionProperty;
import lsfusion.server.logics.scripted.ScriptingActionProperty;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;
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

public abstract class ImportDataActionProperty extends ScriptingActionProperty {

    protected final List<String> ids;
    protected final List<LCP> properties;

    public final static Map<String, Integer> XLSColumnsMapping = new HashMap<String, Integer>() {{
        put("A", 0); put("B", 1); put("C", 2); put("D", 3); put("E", 4); put("F", 5); put("G", 6); put("H", 7); put("I", 8);
        put("J", 9); put("K", 10); put("L", 11); put("M", 12); put("N", 13); put("O", 14); put("P", 15); put("Q", 16);
        put("R", 17); put("S", 18); put("T", 19); put("U", 20); put("V", 21); put("W", 22); put("X", 23); put("Y", 24);
        put("Z", 25); put("AA", 26); put("AB", 27); put("AC", 28); put("AD", 29); put("AE", 30); put("AF", 31); put("AG", 32);
        put("AH", 33); put("AI", 34); put("AJ", 35); put("AK", 36); put("AL", 37); put("AM", 38); put("AN", 39); put("AO", 40);
        put("AP", 41); put("AQ", 42); put("AR", 43); put("AS", 44); put("AT", 45); put("BA", 52); put("BB", 53); put("BC", 54);
    }};

    public static ImportDataActionProperty createExcelProperty(ValueClass valueClass, ImportSourceFormat format, ScriptingLogicsModule LM, List<String> ids, List<LCP> properties, ValueClass sheetIndex) {
        if (format == ImportSourceFormat.XLS) {
            return new ImportXLSDataActionProperty(sheetIndex == null ? new ValueClass[] {valueClass} : new ValueClass[] {valueClass, sheetIndex} , LM, ids, properties);
        } else if (format == ImportSourceFormat.XLSX) {
            return new ImportXLSXDataActionProperty(sheetIndex == null ? new ValueClass[] {valueClass} : new ValueClass[] {valueClass, sheetIndex}, LM, ids, properties);
        } else return null;
    }

    public static ImportDataActionProperty createProperty(ValueClass valueClass, ImportSourceFormat format, ScriptingLogicsModule LM, List<String> ids, List<LCP> properties) {
        if (format == ImportSourceFormat.DBF) {
            return new ImportDBFDataActionProperty(valueClass, LM, ids, properties);
        } else if (format == ImportSourceFormat.JDBC) {
            return new ImportJDBCDataActionProperty(valueClass, LM, ids, properties);
        } else if (format == ImportSourceFormat.MDB) {
            return new ImportMDBDataActionProperty(valueClass, LM, ids, properties);
        }
        return null;
    }

    public ImportDataActionProperty(ValueClass[] valueClasses, ScriptingLogicsModule LM, List<String> ids, List<LCP> properties) {
        super(LM, valueClasses);
        this.ids = ids;
        this.properties = properties;
    }

    @Override
    protected void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        DataObject value = context.getDataKeys().getValue(0);
        assert value.getType() instanceof FileClass;

        Object file = value.object;
        if (file instanceof byte[]) {
            try {
                Integer prevImported = (Integer) findProperty("prevImported").read(context);
                if (value.getType() instanceof DynamicFormatFileClass) {
                    file = BaseUtils.getFile((byte[]) file);
                }
                ImportIterator iterator = getIterator((byte[]) file);

                List<String> row;
                int i = 0;
                ImOrderSet<LCP> props = SetFact.fromJavaOrderSet(properties).addOrderExcl(LM.baseLM.imported);  
                SingleKeyTableUsage<LCP> importTable = new SingleKeyTableUsage<>(IntegerClass.instance, props, new Type.Getter<LCP>() {
                    @Override
                    public Type getType(LCP key) {
                        return key.property.getType();
                    }
                });
                MExclMap<ImMap<String, DataObject>, ImMap<LCP, ObjectValue>> mRows = MapFact.mExclMap();
                while ((row = iterator.nextRow()) != null) {
                    DataObject rowKey = new DataObject(i++, IntegerClass.instance);
                    final List<String> finalRow = row;
                    mRows.exclAdd(MapFact.singleton("key", rowKey), props.getSet().mapValues(new GetValue<ObjectValue, LCP>() {
                        public ObjectValue getMapValue(LCP prop) {
                            if (prop == LM.baseLM.imported) {
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
                        }}));
                }
                findProperty("prevImported").change(i, context);
                if(prevImported != null) {
                    while (i < prevImported) {
                        DataObject rowKey = new DataObject(i++, IntegerClass.instance);
                        mRows.exclAdd(MapFact.singleton("key", rowKey), props.getSet().mapValues(new GetValue<ObjectValue, LCP>() {
                            public ObjectValue getMapValue(LCP prop) {
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
                
            } catch (Exception e) {
                Throwables.propagate(e);
            }
        }
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
                    currentIndex = previousIndex + 1;
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

    public abstract ImportIterator getIterator(byte[] file) throws IOException, ParseException, xBaseJException, JDOMException, ClassNotFoundException;
}

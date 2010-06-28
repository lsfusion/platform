package platform.server.data.type;

import net.sf.jasperreports.engine.JRAlignment;
import platform.base.BaseUtils;
import platform.base.OrderedMap;
import platform.server.classes.BaseClass;
import platform.server.classes.ConcreteClass;
import platform.server.classes.sets.AndClassSet;
import platform.server.data.PropertyField;
import platform.server.data.SQLSession;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.ValueExpr;
import platform.server.data.expr.KeyType;
import platform.server.data.query.Join;
import platform.server.data.query.Query;
import platform.server.data.sql.SQLSyntax;
import platform.server.logics.DataObject;
import platform.server.view.form.client.report.ReportDrawField;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.Format;
import java.text.NumberFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ObjectType implements Type<Integer> {

    public ObjectType() {
        super();
    }

    public boolean isCompatible(Type type) {
        return type instanceof ObjectType;
    }

    public static final ObjectType instance = new ObjectType();

    public String getDB(SQLSyntax syntax) {
        return syntax.getIntegerType();
    }

    public Integer read(Object value) {
        if(value==null) return null;
        return ((Number)value).intValue();
    }

    public void writeParam(PreparedStatement statement, int num, Object value, SQLSyntax syntax) throws SQLException {
        statement.setInt(num, (Integer)value);
    }

    public boolean isSafeString(Object value) {
        return true;
    }

    public String getString(Object value, SQLSyntax syntax) {
        return value.toString();
    }

    public DataObject getEmptyValueExpr() {
        throw new RuntimeException("temporary");
    }

    public int getPreferredWidth() { return 45; }
    public int getMaximumWidth() { return getPreferredWidth(); }
    public int getMinimumWidth() { return getPreferredWidth(); }

    public Format getDefaultFormat() {
        return NumberFormat.getInstance();
    }

    public void fillReportDrawField(ReportDrawField reportField) {
        reportField.valueClass = Integer.class;
        reportField.alignment = JRAlignment.HORIZONTAL_ALIGN_RIGHT;
    }

    public ConcreteClass getDataClass(Object value, SQLSession session, BaseClass baseClass) throws SQLException {
        Query<Object,String> query = new Query<Object,String>(new HashMap<Object, KeyExpr>());
        Join<PropertyField> joinTable = baseClass.table.joinAnd(Collections.singletonMap(baseClass.table.key,new ValueExpr(value,baseClass.getConcrete())));
        query.and(joinTable.getWhere());
        query.properties.put("classid", joinTable.getExpr(baseClass.table.objectClass));
        OrderedMap<Map<Object, Object>, Map<String, Object>> result = query.execute(session);
        if(result.size()==0)
            return baseClass.unknown;
        else {
            assert (result.size()==1);
            return baseClass.findConcreteClassID((Integer) result.singleValue().get("classid"));
        }
    }

    public ConcreteClass getBinaryClass(byte[] value, SQLSession session, BaseClass baseClass) throws SQLException {
        int idobject = 0;
        for(int i=0;i<value.length;i++)
            idobject = idobject * 8 + value[i];
        return getDataClass(idobject, session, baseClass); 
    }

    public void prepareClassesQuery(Expr expr, Query<?, Object> query, BaseClass baseClass) {
        query.properties.put(expr,expr.classExpr(baseClass));
    }

    public ConcreteClass readClass(Expr expr, Map<Object, Object> classes, BaseClass baseClass, KeyType keyType) {
        return baseClass.findConcreteClassID((Integer) classes.get(expr));
    }

    public List<AndClassSet> getUniversal(BaseClass baseClass) {
        return BaseUtils.<AndClassSet>toList(baseClass.getUpSet(),baseClass.unknown);
    }

    public int getBinaryLength(boolean charBinary) {
        return 8;
    }

    public boolean isSafeType(Object value) {
        return true;
    }
}

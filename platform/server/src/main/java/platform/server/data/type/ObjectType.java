package platform.server.data.type;

import net.sf.jasperreports.engine.type.HorizontalAlignEnum;
import platform.base.BaseUtils;
import platform.server.classes.*;
import platform.server.classes.sets.AndClassSet;
import platform.server.data.SQLSession;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyType;
import platform.server.data.expr.query.Stat;
import platform.server.data.query.Query;
import platform.server.data.sql.SQLSyntax;
import platform.server.form.view.report.ReportDrawField;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.Format;
import java.text.NumberFormat;
import java.util.List;
import java.util.Map;

public class ObjectType extends AbstractType<Integer> {

    public ObjectType() {
        super();
    }

    @Override
    public Type getCompatible(Type type) {
        if(type instanceof ObjectType)
            return this;
        return null;
    }

    public static final ObjectType instance = new ObjectType();
    public static final IntegerClass idClass = IntegerClass.instance;

    public String getDB(SQLSyntax syntax) {
        return idClass.getDB(syntax);
    }
    public int getSQL(SQLSyntax syntax) {
        return idClass.getSQL(syntax);
    }

    public Integer read(Object value) {
        if(value==null) return null;
        return ((Number)value).intValue();
    }

    public void writeParam(PreparedStatement statement, int num, Object value, SQLSyntax syntax) throws SQLException {
        idClass.writeParam(statement, num, value, syntax);
    }

    public boolean isSafeString(Object value) {
        return true;
    }

    public String getString(Object value, SQLSyntax syntax) {
        return value.toString();
    }

    public int getPreferredWidth() { return 45; }
    public int getMaximumWidth() { return getPreferredWidth(); }
    public int getMinimumWidth() { return getPreferredWidth(); }

    public Format getReportFormat() {
        return NumberFormat.getInstance();
    }

    public boolean fillReportDrawField(ReportDrawField reportField) {
        reportField.valueClass = Integer.class;
        reportField.alignment = HorizontalAlignEnum.RIGHT.getValue();
        return true;
    }

    public ConcreteClass getDataClass(Object value, SQLSession session, BaseClass baseClass) throws SQLException {
        Integer classID = baseClass.getClassID((Integer) value, session);
        if(classID==null)
            return baseClass.unknown;
        else
            return baseClass.findConcreteClassID(classID);
    }

    public ConcreteClass getBinaryClass(byte[] value, SQLSession session, BaseClass baseClass) throws SQLException {
        int idobject;
        if(session.syntax.isBinaryString()) {
            idobject = Integer.parseInt(new String(value).trim());
        } else {
            idobject = 0;
            for(int i=0;i<value.length;i++)
                idobject = idobject * 8 + value[i];
        }
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

    public Integer parseString(String s) throws ParseException {
        try {
            return Integer.parseInt(s);
        } catch (Exception e) {
            throw new ParseException("error parsing object", e);
        }
    }

    public AndClassSet getBaseClassSet(BaseClass baseClass) {
        return baseClass.getUpSet();
    }

    public Stat getDefaultStat() {
        return Stat.DEFAULT;
    }
}

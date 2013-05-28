package platform.server.data.type;

import net.sf.jasperreports.engine.type.HorizontalAlignEnum;
import platform.base.col.MapFact;
import platform.base.col.SetFact;
import platform.base.col.interfaces.immutable.ImList;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.immutable.ImRevMap;
import platform.base.col.interfaces.mutable.MSet;
import platform.server.classes.BaseClass;
import platform.server.classes.ConcreteClass;
import platform.server.classes.IntegerClass;
import platform.server.classes.ObjectValueClassSet;
import platform.server.classes.sets.AndClassSet;
import platform.server.data.SQLSession;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.KeyType;
import platform.server.data.expr.ValueExpr;
import platform.server.data.expr.where.CaseExprInterface;
import platform.server.data.query.QueryBuilder;
import platform.server.data.query.TypeEnvironment;
import platform.server.data.sql.SQLSyntax;
import platform.server.data.where.Where;
import platform.server.form.view.report.ReportDrawField;
import platform.server.logics.property.ClassField;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.Format;
import java.text.NumberFormat;

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

    public String getDB(SQLSyntax syntax, TypeEnvironment typeEnv) {
        return idClass.getDB(syntax, typeEnv);
    }
    public int getSQL(SQLSyntax syntax) {
        return idClass.getSQL(syntax);
    }

    public Integer read(Object value) {
        if(value==null) return null;
        return ((Number)value).intValue();
    }

    public void writeParam(PreparedStatement statement, int num, Object value, SQLSyntax syntax, TypeEnvironment typeEnv) throws SQLException {
        idClass.writeParam(statement, num, value, syntax, typeEnv);
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

    public ConcreteClass getDataClass(Object value, SQLSession session, AndClassSet classSet, BaseClass baseClass) throws SQLException {
        ObjectValueClassSet objectClassSet = (ObjectValueClassSet)classSet.getValueClassSet(); // unknown не интересуют
        if(objectClassSet.isEmpty())
            return baseClass.unknown;

        QueryBuilder<Object,String> query = new QueryBuilder<Object,String>(MapFact.<Object, KeyExpr>EMPTYREV());
        CaseExprInterface mCases = Expr.newCases(true); // именно так а не через classExpr и т.п. чтобы не соптимизировалось, и не убрало вообще запрос к таблице
        ImRevMap<ClassField,ObjectValueClassSet> readTables = objectClassSet.getTables();
        for(int i=0,size=readTables.size();i<size;i++) {
            Expr expr = readTables.getKey(i).getStoredExpr(new ValueExpr(value, readTables.getValue(i).getSetConcreteChildren().get(0)));
            mCases.add(expr.getWhere(), expr);
        }
        query.addProperty("classid", mCases.getFinal());

        return baseClass.findConcreteClassID((Integer)query.execute(session).singleValue().get("classid")); // тут можно было бы искать только среди ObjectValueClassSet сделать
    }

    public void prepareClassesQuery(Expr expr, Where where, MSet<Expr> exprs, BaseClass baseClass) {
        exprs.add(expr);
    }

    public ConcreteClass readClass(Expr expr, ImMap<Expr, Object> classes, BaseClass baseClass, KeyType keyType) {
        return baseClass.findConcreteClassID((Integer) classes.get(expr));
    }

    public ImList<AndClassSet> getUniversal(BaseClass baseClass) {
        return SetFact.<AndClassSet>toOrderExclSet(baseClass.getUpSet(), baseClass.unknown);
    }

    public int getCharLength() {
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

    public String getSID() {
        return "ObjectType";
    }
}

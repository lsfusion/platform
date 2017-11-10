package lsfusion.server.data.type;

import lsfusion.base.ExtInt;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.server.classes.*;
import lsfusion.server.classes.sets.AndClassSet;
import lsfusion.server.data.OperationOwner;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.SQLSession;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.KeyExpr;
import lsfusion.server.data.expr.KeyType;
import lsfusion.server.data.expr.ValueExpr;
import lsfusion.server.data.expr.query.Stat;
import lsfusion.server.data.expr.where.CaseExprInterface;
import lsfusion.server.data.query.QueryBuilder;
import lsfusion.server.data.query.TypeEnvironment;
import lsfusion.server.data.sql.SQLSyntax;
import lsfusion.server.data.where.Where;
import lsfusion.server.form.view.report.ReportDrawField;
import lsfusion.server.logics.property.ObjectClassField;
import net.sf.jasperreports.engine.type.HorizontalAlignEnum;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ObjectType extends AbstractType<Long> {

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
    public static final LongClass idClass = LongClass.instance;

    public String getDB(SQLSyntax syntax, TypeEnvironment typeEnv) {
        return idClass.getDB(syntax, typeEnv);
    }

    public String getDotNetType(SQLSyntax syntax, TypeEnvironment typeEnv) {
        return idClass.getDotNetType(syntax, typeEnv);
    }

    public String getDotNetRead(String reader) {
        return idClass.getDotNetRead(reader);
    }
    public String getDotNetWrite(String writer, String value) {
        return idClass.getDotNetWrite(writer, value);
    }

    public int getBaseDotNetSize() {
        return idClass.getBaseDotNetSize();
    }

    public int getSQL(SQLSyntax syntax) {
        return idClass.getSQL(syntax);
    }

    public Long read(Object value) {
        return idClass.read(value);
    }

    @Override
    public Long read(ResultSet set, SQLSyntax syntax, String name) throws SQLException {
        return idClass.read(set, syntax, name);
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

    public boolean fillReportDrawField(ReportDrawField reportField) {
        reportField.valueClass = Long.class;
        reportField.alignment = HorizontalAlignEnum.RIGHT.getValue();
        return true;
    }

    public ConcreteClass getDataClass(Object value, SQLSession session, AndClassSet classSet, BaseClass baseClass, OperationOwner owner) throws SQLException, SQLHandledException {
        ObjectValueClassSet objectClassSet = (ObjectValueClassSet)classSet.getValueClassSet(); // unknown не интересуют
        if(objectClassSet.isEmpty())
            return baseClass.unknown;

        QueryBuilder<Object,String> query = new QueryBuilder<>(MapFact.<Object, KeyExpr>EMPTYREV());
        ImRevMap<ObjectClassField,ObjectValueClassSet> readTables = objectClassSet.getObjectClassFields();
        CaseExprInterface mCases = Expr.newCases(true, readTables.size()); // именно так а не через classExpr и т.п. чтобы не соптимизировалось, и не убрало вообще запрос к таблице
        for(int i=0,size=readTables.size();i<size;i++) {
            Expr expr = readTables.getKey(i).getStoredExpr(new ValueExpr((Long)value, readTables.getValue(i).getSetConcreteChildren().get(0)));
            mCases.add(expr.getWhere(), expr);
        }
        query.addProperty("classid", mCases.getFinal());

        return baseClass.findConcreteClassID((Long)query.execute(session, owner).singleValue().get("classid")); // тут можно было бы искать только среди ObjectValueClassSet сделать
    }

    public void prepareClassesQuery(Expr expr, Where where, MSet<Expr> exprs, BaseClass baseClass) {
        exprs.add(expr);
    }

    public ConcreteClass readClass(Expr expr, ImMap<Expr, Object> classes, BaseClass baseClass, KeyType keyType) {
        return baseClass.findConcreteClassID((Long) classes.get(expr));
    }

    public ImList<AndClassSet> getUniversal(BaseClass baseClass) {
        return SetFact.<AndClassSet>toOrderExclSet(baseClass.getUpSet(), baseClass.unknown);
    }

    public ExtInt getCharLength() {
        return new ExtInt(10);
    }

    public Long parseString(String s) throws ParseException {
        return idClass.parseString(s);
    }

    public AndClassSet getBaseClassSet(BaseClass baseClass) {
        return baseClass.getUpSet();
    }

    public String getSID() {
        return "ObjectType";
    }

    @Override
    public Stat getTypeStat(boolean forJoin) {
        return Stat.ALOT;
    }

    public Object getInfiniteValue(boolean min) {
        return idClass.getInfiniteValue(min);
    }
}

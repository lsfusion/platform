package lsfusion.server.data.type;

import com.hexiong.jdbf.JDBFException;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.interop.form.property.ExtInt;
import lsfusion.server.data.OperationOwner;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.join.classes.ObjectClassField;
import lsfusion.server.data.expr.key.KeyExpr;
import lsfusion.server.data.expr.key.KeyType;
import lsfusion.server.data.expr.value.ValueExpr;
import lsfusion.server.data.expr.where.CaseExprInterface;
import lsfusion.server.data.query.build.QueryBuilder;
import lsfusion.server.data.sql.SQLSession;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.sql.syntax.SQLSyntax;
import lsfusion.server.data.stat.Stat;
import lsfusion.server.data.type.exec.TypeEnvironment;
import lsfusion.server.data.where.Where;
import lsfusion.server.logics.classes.ConcreteClass;
import lsfusion.server.logics.classes.data.ParseException;
import lsfusion.server.logics.classes.data.integral.LongClass;
import lsfusion.server.logics.classes.user.BaseClass;
import lsfusion.server.logics.classes.user.ObjectValueClassSet;
import lsfusion.server.logics.classes.user.set.AndClassSet;
import lsfusion.server.logics.form.stat.print.design.ReportDrawField;
import lsfusion.server.logics.form.stat.struct.export.plain.dbf.OverJDBField;
import lsfusion.server.logics.form.stat.struct.imports.plain.dbf.CustomDbfRecord;
import net.sf.jasperreports.engine.type.HorizontalTextAlignEnum;
import org.json.JSONException;

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

    public int getReportPreferredWidth() { return 45; }
    public int getReportMinimumWidth() { return getReportPreferredWidth(); }

    public void fillReportDrawField(ReportDrawField reportField) {
        reportField.valueClass = Long.class;
        reportField.alignment = HorizontalTextAlignEnum.RIGHT;
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

    @Override
    public Long parseDBF(CustomDbfRecord dbfRecord, String fieldName, String charset) throws ParseException, java.text.ParseException {
        return idClass.parseDBF(dbfRecord, fieldName, charset);
    }

    @Override
    public Long parseJSON(Object value) throws JSONException, ParseException {
        return idClass.parseJSON(value);
    }

    @Override
    public OverJDBField formatDBF(String fieldName) throws JDBFException {
        return idClass.formatDBF(fieldName);
    }

    @Override
    public Object formatJSON(Long object) {
        return idClass.formatJSON(object);
    }

    @Override
    public String formatJSONSource(String valueSource, SQLSyntax syntax) {
        return idClass.formatJSONSource(valueSource, syntax);
    }

    @Override
    public String getJSONType() {
        return idClass.getJSONType();
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

    public Long getInfiniteValue(boolean min) {
        return idClass.getInfiniteValue(min);
    }
}

package platform.server.data.type;

import platform.base.ExtInt;
import platform.base.ListCombinations;
import platform.base.col.ListFact;
import platform.base.col.interfaces.immutable.ImList;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.mutable.MExclMap;
import platform.base.col.interfaces.mutable.MList;
import platform.base.col.interfaces.mutable.MSet;
import platform.base.col.interfaces.mutable.mapvalue.GetValue;
import platform.server.classes.BaseClass;
import platform.server.classes.ConcatenateClassSet;
import platform.server.classes.ConcreteClass;
import platform.server.classes.sets.AndClassSet;
import platform.server.data.SQLSession;
import platform.server.data.expr.DeconcatenateExpr;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyType;
import platform.server.data.query.TypeEnvironment;
import platform.server.data.sql.DataAdapter;
import platform.server.data.sql.SQLSyntax;
import platform.server.data.where.Where;
import platform.server.form.view.report.ReportDrawField;

import java.io.DataOutputStream;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.Format;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

public class ConcatenateType extends AbstractType<Object[]> {

    private Type[] types;

    private static Collection<ConcatenateType> instances = new ArrayList<ConcatenateType>();

    public static ConcatenateType get(Type[] types) {
        for (ConcatenateType instance : instances)
            if (Arrays.equals(types, instance.types))
                return instance;

        ConcatenateType instance = new ConcatenateType(types);
        instances.add(instance);
        return instance;
    }

    private ConcatenateType(Type[] types) {
        this.types = types;
    }

    public Type get(int i) {
        return types[i];        
    }

    public int getPartsCount() {
        return types.length;
    }

    public String getDB(SQLSyntax syntax, TypeEnvironment typeEnv) {
        typeEnv.addNeedType(this);
        return DataAdapter.genConcTypeName(this);
    }

    public boolean isSafeString(Object value) {
        return false;
    }

    public String getString(Object value, SQLSyntax syntax) {
        return value.toString();
    }

    public Object[] read(Object value) {
        return (Object[])value;
    }

    public int getSQL(SQLSyntax syntax) {
        assert !syntax.hasDriverCompositeProblem();

        throw new RuntimeException("not supported yet");
//        return syntax.getCompositeSQL();
    }

    public void writeParam(PreparedStatement statement, int num, Object value, SQLSyntax syntax, TypeEnvironment typeEnv) throws SQLException {
        assert !syntax.hasDriverCompositeProblem();

        throw new RuntimeException("not supported yet");
    }

    public void writeNullParam(PreparedStatement statement, SQLSession.ParamNum num, SQLSyntax syntax, TypeEnvironment typeEnv) throws SQLException {
        if(syntax.hasDriverCompositeProblem()) {
            for (Type type : types)
                type.writeNullParam(statement, num, syntax, typeEnv);
            return;
        }

        super.writeNullParam(statement, num, syntax, typeEnv);
    }

    @Override
    public void writeParam(PreparedStatement statement, SQLSession.ParamNum num, Object value, SQLSyntax syntax, TypeEnvironment typeEnv) throws SQLException {
        if(syntax.hasDriverCompositeProblem()) {
            for (int i=0,size=types.length;i<size;i++)
                types[i].writeParam(statement, num, ((Object[])value)[i], syntax, typeEnv);
            return;
        }

        super.writeParam(statement, num, value, syntax, typeEnv);
    }

    private static String getDeconcName(String name, int i) {
        return "p" + i + "_" + name + "_p" + i;
    }

    @Override
    public boolean isSafeType(Object value) { // важно что not safe иначе в parsing'е не будет этого типа
        return false;
    }

    public ImList<Type> getTypes() {
        return ListFact.toList(types);
    }

    @Override
    public String writeDeconc(final SQLSyntax syntax, final TypeEnvironment env) { // дублирование getConcatenateSource, но по идее так тоже можно
        if(syntax.hasDriverCompositeProblem())
            return getNotSafeConcatenateSource(ListFact.toList(types).mapListValues(new GetValue<String, Type>() {
                public String getMapValue(Type value) {
                    return value.writeDeconc(syntax, env);
                }
            }), syntax, env);

        return super.writeDeconc(syntax, env);
    }

    @Override
    public void readDeconc(String source, String name, MExclMap<String, String> mResult, SQLSyntax syntax, TypeEnvironment typeEnv) {
        if(syntax.hasDriverCompositeProblem()) {
            for(int i=0;i<types.length;i++)
                types[i].readDeconc(getDeconcatenateSource(source, i, syntax, typeEnv), getDeconcName(name, i), mResult, syntax, typeEnv);
            return;
        }

        super.readDeconc(source, name, mResult, syntax, typeEnv);
    }

    private boolean allNulls(ResultSet set, SQLSyntax syntax, String name) throws SQLException {
        for(int i=1;i<types.length;i++)
            if(types[i].read(set, syntax, getDeconcName(name, i))!=null)
                return false;
        return true;
    }
    @Override
    public Object[] read(ResultSet set, SQLSyntax syntax, String name) throws SQLException {
        if(syntax.hasDriverCompositeProblem()) {
            Object[] result = new Object[types.length];
            for(int i=0;i<types.length;i++) {
                result[i] = types[i].read(set, syntax, getDeconcName(name, i));
                if(i==0 && result[0]==null) {
                    assert allNulls(set, syntax, name);
                    return null;
                }
            }
            return result;
        }

        return super.read(set, syntax, name);
    }

    public Format getReportFormat() {
        throw new RuntimeException("not supported");
    }

    public int getMinimumWidth() {
        throw new RuntimeException("not supported");
    }

    public int getPreferredWidth() {
        throw new RuntimeException("not supported");
    }

    public int getMaximumWidth() {
        throw new RuntimeException("not supported");
    }

    public boolean fillReportDrawField(ReportDrawField reportField) {
        throw new RuntimeException("not supported");
    }

    @Override
    public Type getCompatible(Type type) {
        if(!(type instanceof ConcatenateType)) return null;
        ConcatenateType concatenate = (ConcatenateType)type;
        if (concatenate.types.length != types.length)
            return null;

        Type[] compatible = new Type[types.length];
        for(int i=0;i<types.length;i++) {
            Type compType = types[i].getCompatible(concatenate.types[i]);
            if(compType == null)
                return null;
            compatible[i] = compType;
        }
        return get(compatible);
    }

    private ConcreteClass createConcrete(ConcreteClass[] classes) {
        return new ConcatenateClassSet(classes);
    }

    public ConcreteClass getDataClass(Object value, SQLSession session, AndClassSet classSet, BaseClass baseClass) throws SQLException {
        Object[] objects = read(value);
        assert objects!=null;

        ConcreteClass[] classes = new ConcreteClass[types.length];
        for(int i=0;i<types.length;i++)
            classes[i] = types[i].getDataClass(objects[i],session, ((ConcatenateClassSet)classSet).get(i), baseClass);

        return createConcrete(classes);
    }

    public String getNotSafeConcatenateSource(ImList<String> exprs, SQLSyntax syntax, TypeEnvironment typeEnv) {
        return "ROW(" + exprs.toString(",") + ")";
    }

    public String getConcatenateSource(ImList<String> exprs, SQLSyntax syntax, TypeEnvironment typeEnv) {
        String source = getCast(getNotSafeConcatenateSource(exprs, syntax, typeEnv), syntax, typeEnv);

        if(exprs.size()>0)
            source =  "CASE WHEN " + exprs.toString(new GetValue<String, String>() {
                public String getMapValue(String value) {
                    return value + " IS NOT NULL";
                }}, " AND ") + " THEN " + source + " ELSE NULL END";
        return source;
    }

    public static String getFieldName(int part) {
        return "f" + part;
    }
    public String getDeconcatenateSource(String expr, int part, SQLSyntax syntax, TypeEnvironment typeEnv) {
        return "(" + expr + ")." + getFieldName(part);
    }

    public void prepareClassesQuery(Expr expr, Where where, MSet<Expr> exprs, BaseClass baseClass) {
        for(int i=0;i<types.length;i++) {
            Expr partExpr = DeconcatenateExpr.create(expr, i, baseClass);
            partExpr.getReader(where).prepareClassesQuery(partExpr, where, exprs,baseClass);
        }
    }

    public ConcreteClass readClass(Expr expr, ImMap<Expr, Object> classes, BaseClass baseClass, KeyType keyType) {
        ConcreteClass[] classSets = new ConcreteClass[types.length];
        for(int i=0;i<types.length;i++) {
            Expr partExpr = DeconcatenateExpr.create(expr, i, baseClass);
            classSets[i] = partExpr.getReader(keyType).readClass(partExpr,classes,baseClass, keyType);
        }
        return new ConcatenateClassSet(classSets);
    }

    public ImList<AndClassSet> getUniversal(BaseClass baseClass) {
        throw new RuntimeException("not supported yet");
    }

    public AndClassSet getBaseClassSet(BaseClass baseClass) {
        AndClassSet[] classSets = new AndClassSet[types.length];
        for(int i=0;i<types.length;i++)
            classSets[i] = types[i].getBaseClassSet(baseClass);
        return new ConcatenateClassSet(classSets);
    }

    public Iterable<ImList<AndClassSet>> getUniversal(BaseClass baseClass, int part, AndClassSet fix) {
        MList<ImList<AndClassSet>> mClassSets = ListFact.mList(types.length);
        for(int i=0;i<types.length;i++)
            mClassSets.add(i==part? ListFact.singleton(fix) : ((Type<?>)types[i]).getUniversal(baseClass));
        return new ListCombinations<AndClassSet>(mClassSets.immutableList());
    }

    public ExtInt getCharLength() {
        ExtInt length = ExtInt.ZERO;
        for(Type type : types)
            length = length.sum(type.getCharLength());
        return length;
    }

    public Object[] parseString(String s) throws ParseException {
        throw new RuntimeException("Parsing values from string is not supported");
    }

    public void serialize(DataOutputStream outStream) throws IOException {
        outStream.writeInt(types.length);
        for (Type type : types) {
            TypeSerializer.serializeType(outStream, type);
        }
    }

    public String getSID() {
        String result = "C";
        for (Type type : types)
            result = result + "_" + type.getSID() + "_C";
        return result;
    }
}

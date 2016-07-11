package lsfusion.server.data.type;

import lsfusion.base.ExtInt;
import lsfusion.base.ListCombinations;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.mutable.MExclMap;
import lsfusion.base.col.interfaces.mutable.MList;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.server.classes.BaseClass;
import lsfusion.server.classes.ConcatenateClassSet;
import lsfusion.server.classes.ConcreteClass;
import lsfusion.server.classes.sets.AndClassSet;
import lsfusion.server.data.OperationOwner;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.SQLSession;
import lsfusion.server.data.expr.DeconcatenateExpr;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.KeyType;
import lsfusion.server.data.query.TypeEnvironment;
import lsfusion.server.data.sql.SQLSyntax;
import lsfusion.server.data.where.Where;
import lsfusion.server.form.view.report.ReportDrawField;

import java.io.DataOutputStream;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

public class ConcatenateType extends AbstractType<Object[]> {

    private Type[] types;
    private boolean[] desc;

    private static Collection<ConcatenateType> instances = new ArrayList<>();

    public static ConcatenateType get(Type[] types) {
        return get(types, new boolean[types.length]);
    }
    
    public synchronized static ConcatenateType get(Type[] types, boolean[] desc) {
        for (ConcatenateType instance : instances)
            if (Arrays.equals(types, instance.types) && Arrays.equals(desc, instance.desc))
                return instance;

        ConcatenateType instance = new ConcatenateType(types, desc);
        instances.add(instance);
        return instance;
    }

    private ConcatenateType(Type[] types, boolean[] desc) {
        this.types = types;
        this.desc = desc;
        assert types.length == desc.length;
    }

    public Type get(int i) {
        return types[i];        
    }

    public int getPartsCount() {
        return types.length;
    }

    public String getDB(SQLSyntax syntax, TypeEnvironment typeEnv) {
        typeEnv.addNeedType(this);
        return syntax.getConcTypeName(this);
    }

    public String getDotNetType(SQLSyntax syntax, TypeEnvironment typeEnv) {
        typeEnv.addNeedType(this);
        return syntax.getConcTypeName(this);
    }

    public String getDotNetRead(String reader) {
        throw new UnsupportedOperationException();
    }

    public String getDotNetWrite(String writer, String value) {
        throw new UnsupportedOperationException();
    }
    public int getBaseDotNetSize() {
        int result = 0;
        for(Type t : types)
            result += t.getDotNetSize();
        return result;
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

    public void writeParam(PreparedStatement statement, int num, Object value, SQLSyntax syntax) throws SQLException {
        assert !syntax.hasDriverCompositeProblem();

        throw new RuntimeException("not supported yet");
    }

    public void writeNullParam(PreparedStatement statement, SQLSession.ParamNum num, SQLSyntax syntax) throws SQLException {
        if(syntax.hasDriverCompositeProblem()) {
            for (Type type : types)
                type.writeNullParam(statement, num, syntax);
            return;
        }

        super.writeNullParam(statement, num, syntax);
    }

    @Override
    public void writeParam(PreparedStatement statement, SQLSession.ParamNum num, Object value, SQLSyntax syntax) throws SQLException {
        if(syntax.hasDriverCompositeProblem()) {
            for (int i=0,size=types.length;i<size;i++)
                types[i].writeParam(statement, num, ((Object[])value)[i], syntax);
            return;
        }

        super.writeParam(statement, num, value, syntax);
    }

    private static String getDeconcName(String name, int i) {
        return "p" + i + "_" + name + "_p" + i;
    }

    @Override
    public boolean isSafeType() { // важно что not safe иначе в parsing'е не будет этого типа
        return false;
    }

    public ImList<Type> getTypes() {
        return ListFact.toList(types);
    }

    public boolean[] getDesc() {
        return desc;
    }

    @Override
    public String writeDeconc(final SQLSyntax syntax, final TypeEnvironment env) { // дублирование getConcatenateSource, но по идее так тоже можно
        if(syntax.hasDriverCompositeProblem())
            return syntax.getNotSafeConcatenateSource(this, ListFact.toList(types).mapListValues(new GetValue<String, Type>() {
                public String getMapValue(Type value) {
                    return value.writeDeconc(syntax, env);
                }
            }), env);

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
            if(compType == null || desc[i] != concatenate.desc[i])
                return null;
            compatible[i] = compType;
        }
        return get(compatible, desc);
    }

    private ConcreteClass createConcrete(ConcreteClass[] classes) {
        return new ConcatenateClassSet(classes);
    }

    public ConcreteClass getDataClass(Object value, SQLSession session, AndClassSet classSet, BaseClass baseClass, OperationOwner owner) throws SQLException, SQLHandledException {
        Object[] objects = read(value);
        assert objects!=null;

        ConcreteClass[] classes = new ConcreteClass[types.length];
        for(int i=0;i<types.length;i++)
            classes[i] = types[i].getDataClass(objects[i],session, ((ConcatenateClassSet)classSet).get(i), baseClass, owner);

        return createConcrete(classes);
    }

    public String getConcatenateSource(ImList<String> exprs, SQLSyntax syntax, TypeEnvironment typeEnv) {
        String source = syntax.getNotSafeConcatenateSource(this, exprs, typeEnv);

        if(exprs.size()>0)
            source =  syntax.getAndExpr(exprs.toString(new GetValue<String, String>() {
                public String getMapValue(String value) {
                    return value + " IS NOT NULL";
                }}, " AND "), source, this, typeEnv);
        return source;
    }

    public static String getFieldName(int part) {
        return "f" + part;
    }
    public static String getDeconcatenateSource(String expr, int part) {
        return "(" + expr + ")." + getFieldName(part);
    }
    public String getDeconcatenateSource(String expr, int part, SQLSyntax syntax, TypeEnvironment typeEnv) {
        return getDeconcatenateSource(expr, part);
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
        return new ListCombinations<>(mClassSets.immutableList());
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
        for (boolean d : desc)
            if (d)
                throw new UnsupportedOperationException();
    }

    public String getSID() {
        String result = "C";
        for (int i = 0; i < types.length; i++) {
            result = result + "_" + types[i].getSID() + (desc[i] ? "_D" : "") + "_C";
        }
        return result;
    }

    public Object getInfiniteValue(boolean min) {
        throw new UnsupportedOperationException();
    }
}

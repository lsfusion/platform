package platform.server.data.type;

import platform.base.ListCombinations;
import platform.base.col.ListFact;
import platform.base.col.interfaces.immutable.ImList;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.mutable.MList;
import platform.base.col.interfaces.mutable.MSet;
import platform.server.classes.BaseClass;
import platform.server.classes.ConcatenateClassSet;
import platform.server.classes.ConcreteClass;
import platform.server.classes.sets.AndClassSet;
import platform.server.data.SQLSession;
import platform.server.data.expr.DeconcatenateExpr;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyType;
import platform.server.data.expr.query.Stat;
import platform.server.data.sql.SQLSyntax;
import platform.server.data.where.Where;
import platform.server.form.view.report.ReportDrawField;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.Format;
import java.util.Arrays;

public class ConcatenateType extends AbstractType<byte[]> {

    private Type[] types;

    public ConcatenateType(Type[] types) {
        this.types = types;
    }

    public Type get(int i) {
        return types[i];        
    }

    public int getPartsCount() {
        return types.length;
    }

    public String getDB(SQLSyntax syntax) {
        return syntax.getBinaryType(getBinaryLength(syntax.isBinaryString()));
    }
    public int getSQL(SQLSyntax syntax) {
        return syntax.getBinarySQL();
    }

    public boolean isSafeString(Object value) {
        return false;
    }

    public String getString(Object value, SQLSyntax syntax) {
        return value.toString();
    }

    public byte[] read(Object value) {
        if(value instanceof String)
            return ((String)value).getBytes();
        else
            return (byte[])value;
    }
    
    public void writeParam(PreparedStatement statement, int num, Object value, SQLSyntax syntax) throws SQLException {
         if(syntax.isBinaryString())
            statement.setString(num,new String((byte[])value));
         else
            statement.setBytes(num,(byte[])value);
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
        assert concatenate.types.length == types.length;

        Type[] compatible = new Type[types.length];
        for(int i=0;i<types.length;i++) {
            Type compType = types[i].getCompatible(concatenate.types[i]);
            if(compType == null)
                return null;
            compatible[i] = compType;
        }
        return new ConcatenateType(compatible);
    }

    private ConcreteClass createConcrete(ConcreteClass[] classes) {
        return new ConcatenateClassSet(classes);
    }

    public ConcreteClass getDataClass(Object value, SQLSession session, BaseClass baseClass) throws SQLException {
        byte[] byteValue = read(value);

        int offset = 0;
        ConcreteClass[] classes = new ConcreteClass[types.length];
        for(int i=0;i<types.length;i++) {
            int blength = types[i].getBinaryLength(session.syntax.isBinaryString());
            byte[] typeValue;
            if(session.syntax.isBinaryString())
                typeValue = new String(byteValue).substring(offset,offset+blength).getBytes();
            else
                typeValue = Arrays.copyOfRange(byteValue,offset,offset+blength);
            classes[i] = types[i].getBinaryClass(typeValue,session,baseClass);
            offset += blength;
        }

        return createConcrete(classes);
    }

    public String getConcatenateSource(ImList<String> exprs,SQLSyntax syntax) {
        // сначала property и extra объединяем в одну строку
        String source = "";
        for(int i=0;i<types.length;i++)
            source = (source.length() == 0 ? "" : source + syntax.getBinaryConcatenate()) + types[i].getBinaryCast(exprs.get(i), syntax, true);
        return "(" + source + ")";
    }

    public String getDeconcatenateSource(String expr, int part, SQLSyntax syntax) {

        int offset = 0;
        for(int i=0;i<part;i++)
            offset += types[i].getBinaryLength(syntax.isBinaryString());
        return types[part].getCast("SUBSTRING(" + expr + "," + (offset + 1) + "," + types[part].getBinaryLength(syntax.isBinaryString()) + ")", syntax, false);
    }

    public void prepareClassesQuery(Expr expr, Where where, MSet<Expr> exprs, BaseClass baseClass) {
        for(int i=0;i<types.length;i++) {
            Expr partExpr = DeconcatenateExpr.create(expr, i, baseClass);
            partExpr.getReader(where).prepareClassesQuery(partExpr, where, exprs,baseClass);
        }
    }

    public ConcreteClass readClass(Expr expr, ImMap<Object, Object> classes, BaseClass baseClass, KeyType keyType) {
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

    public int getBinaryLength(boolean charBinary) {
        int length = 0;
        for(Type type : types)
            length += type.getBinaryLength(charBinary);
        return length;
    }

    public ConcreteClass getBinaryClass(byte[] value, SQLSession session, BaseClass baseClass) throws SQLException {
        return getDataClass(value, session, baseClass);
    }

    public byte[] parseString(String s) throws ParseException {
        throw new RuntimeException("Parsing values from string is not supported");
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof ConcatenateType && Arrays.equals(types, ((ConcatenateType) o).types);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(types);
    }

    public Stat getDefaultStat() {
        Stat result = Stat.ONE;
        for(Type type : types)
            result = result.mult(type.getDefaultStat());
        return result;
    }
}

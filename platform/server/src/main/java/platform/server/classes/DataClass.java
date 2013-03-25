package platform.server.classes;

import net.sf.jasperreports.engine.type.HorizontalAlignEnum;
import platform.base.col.ListFact;
import platform.base.col.MapFact;
import platform.base.col.interfaces.immutable.ImList;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.mutable.MSet;
import platform.base.col.interfaces.mutable.add.MAddExclMap;
import platform.interop.Data;
import platform.server.caches.ManualLazy;
import platform.server.classes.sets.AndClassSet;
import platform.server.classes.sets.OrClassSet;
import platform.server.data.SQLSession;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyType;
import platform.server.data.expr.StaticValueExpr;
import platform.server.data.expr.ValueExpr;
import platform.server.data.expr.query.Stat;
import platform.server.data.type.AbstractType;
import platform.server.data.type.Type;
import platform.server.data.where.Where;
import platform.server.form.entity.ObjectEntity;
import platform.server.form.instance.DataObjectInstance;
import platform.server.form.instance.ObjectInstance;
import platform.server.form.view.report.ReportDrawField;
import platform.server.logics.DataObject;
import platform.server.logics.property.IsClassProperty;
import platform.server.logics.property.group.AbstractGroup;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.text.Format;
import java.util.Random;

public abstract class DataClass<T> extends AbstractType<T> implements StaticClass, AndClassSet, OrClassSet {
    private static MAddExclMap<String, DataClass> sidToClass = MapFact.mBigStrongMap();
    protected String caption;

    public static void storeClass(DataClass... classes) {
        for(DataClass cls : classes)
            sidToClass.exclAdd(cls.getSID(), cls);
    }

    protected DataClass(String caption) {
        this.caption = caption;
    }

    @Override
    public String getCaption() {
        return caption;
    }

    public abstract DataClass getCompatible(DataClass compClass);
    public boolean compatibleEquals(Object object, DataClass compareClass, Object compareObject) {
        DataClass compatible = getCompatible(compareClass);
        return compatible != null && compatible.read(object).equals(compatible.read(compareObject));
    }

    public DataObject getDefaultObjectValue() {
        return new DataObject(getDefaultValue(), this);
    }
    public ValueExpr getDefaultExpr() {
        return getDefaultObjectValue().getExpr();
    }

    public AbstractGroup getParent() {
        return null;
    }

    public boolean isCompatibleParent(ValueClass remoteClass) {
        return remoteClass instanceof DataClass && getCompatible((DataClass) remoteClass) == this;
    }

    public Type getCompatible(Type type) {
        if(!(type instanceof DataClass))
            return null;

        return getCompatible((DataClass) type);
    }

    public DataClass getUpSet() {
        return this;
    }

    public boolean isEmpty() {
        return false;
    }

    public DataClass and(AndClassSet node) {
        if (node.isEmpty()) return this;

        DataClass compatible = getCompatible((DataClass) node);
        assert (compatible != null); // классы должны быть совместимы
        return compatible;
    }

    public OrClassSet and(OrClassSet node) {
        return and((AndClassSet) node);
    }

    public AndClassSet or(AndClassSet node) {
        return and(node);
    }

    public OrClassSet or(OrClassSet node) {
        return and(node);
    }

    public DataClass getRandom(Random randomizer) {
        return this;
    }

    public DataClass getCommonClass() {
        return this;
    }

    public boolean containsAll(AndClassSet node) {
        return node instanceof DataClass && getCompatible((DataClass) node) != null;
    }

    public boolean containsAll(OrClassSet node) {
        return node instanceof DataClass && getCompatible((DataClass) node) != null;
    }

    public OrClassSet getOr() {
        return this;
    }

    public boolean inSet(AndClassSet set) {
        return set.containsAll(this);
    }

    public Type getType() {
        return this;
    }

    public abstract byte getTypeID();

    public void serialize(DataOutputStream outStream) throws IOException {
        outStream.writeByte(getTypeID());
    }

    public static DataClass deserialize(DataInputStream inStream, int version) throws IOException {
        byte type = inStream.readByte();

        if (type == Data.INTEGER) return IntegerClass.instance;
        if (type == Data.LONG) return LongClass.instance;
        if (type == Data.DOUBLE) return DoubleClass.instance;
        if (type == Data.NUMERIC) return NumericClass.get(inStream.readInt(), inStream.readInt());
        if (type == Data.LOGICAL) return LogicalClass.instance;
        if (type == Data.DATE) return DateClass.instance;
        if (type == Data.STRING) return StringClass.get(inStream.readInt());
        if (type == Data.INSENSITIVESTRING) return InsensitiveStringClass.get(inStream.readInt());
        if (type == Data.TEXT) return TextClass.instance;
        if (type == Data.YEAR) return YearClass.instance;
        if (type == Data.DATETIME) return DateTimeClass.instance;
        if (type == Data.TIME) return TimeClass.instance;
        if (type == Data.COLOR) return ColorClass.instance;

        if(version>=2) { // обратная совместимость
            if (type == Data.IMAGE) return new ImageClass(inStream, version);
            if (type == Data.WORD) return new WordClass(inStream, version);
            if (type == Data.EXCEL) return new ExcelClass(inStream, version);
            if (type == Data.CUSTOMSTATICFORMATFILE) return new CustomStaticFormatFileClass(inStream, version);
            if (type == Data.DYNAMICFORMATFILE) return new DynamicFormatFileClass(inStream, version);
            if (type == Data.PDF) return new PDFClass(inStream, version);
        } else {
            if (type == Data.IMAGE) return ImageClass.instance;
            if (type == Data.WORD) return WordClass.instance;
            if (type == Data.EXCEL) return ExcelClass.instance;
            if (type == Data.DYNAMICFORMATFILE) return DynamicFormatFileClass.instance;
            if (type == Data.PDF) return PDFClass.instance;
        }

        throw new IOException();
    }

    public Expr getStaticExpr(Object value) {
        Type type = getType();
        return type instanceof DateClass || type instanceof ColorClass || type.isSafeString(value) // идея в том что, если не Safe String то нужно по любому использовать ValueExpr, очень маловероятно что он пересекется с другим значением
               ? new StaticValueExpr(value, this)
               : new ValueExpr(value, this);
    }

    protected abstract Class getReportJavaClass();

    public abstract Format getReportFormat();

    public int getMinimumWidth() {
        return getPreferredWidth();
    }

    public int getPreferredWidth() {
        return 50;
    }

    public int getMaximumWidth() {
        return Integer.MAX_VALUE;
    }

    public boolean fillReportDrawField(ReportDrawField reportField) {
        reportField.valueClass = getReportJavaClass();
        reportField.alignment = HorizontalAlignEnum.LEFT.getValue();
        return !reportField.valueClass.isArray();
    }

    public ObjectInstance newInstance(ObjectEntity entity) {
        return new DataObjectInstance(entity, this);
    }

    public ConcreteClass getDataClass(Object value, SQLSession session, BaseClass baseClass) {
        return this;
    }

    public ConcreteClass getBinaryClass(byte[] value, SQLSession session, BaseClass baseClass) throws SQLException {
        return this;
    }

    public void prepareClassesQuery(Expr expr, Where where, MSet<Expr> exprs, BaseClass baseClass) {
    }

    public ConcreteClass readClass(Expr expr, ImMap<Object, Object> classes, BaseClass baseClass, KeyType keyType) {
        return this;
    }

    public ImList<AndClassSet> getUniversal(BaseClass baseClass) {
        return ListFact.<AndClassSet>singleton(this);
    }

    public int getBinaryLength(boolean charBinary) {
        return 8;
    }

    public DataClass getKeepClass() {
        return this;
    }

    public DataClass getBaseClass() {
        return this;
    }

    public AndClassSet getBaseClassSet(BaseClass baseClass) {
        return this;
    }

    public static DataClass findDataClass(String sid) {
        return sidToClass.get(sid);        
    }

    public Object getInfiniteValue() {
        throw new RuntimeException("not supported");
    }

    public boolean calculateStat() {
        return true;
    }

    private IsClassProperty property;
    @ManualLazy
    public IsClassProperty getProperty() {
        if(property == null)
            property = CustomClass.getProperty(this);
        return property;
    }

    public AndClassSet[] getAnd() {
        return new AndClassSet[]{this};
    }

    public Stat getTypeStat() {
        return Stat.ALOT;
    }
}

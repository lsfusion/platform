package platform.server.classes;

import net.sf.jasperreports.engine.JRAlignment;
import platform.interop.Data;
import platform.server.classes.sets.AndClassSet;
import platform.server.classes.sets.OrClassSet;
import platform.server.data.SQLSession;
import platform.server.data.expr.*;
import platform.server.data.query.Query;
import platform.server.data.type.Type;
import platform.server.form.entity.ObjectEntity;
import platform.server.form.instance.DataObjectInstance;
import platform.server.form.instance.ObjectInstance;
import platform.server.form.view.report.ReportDrawField;
import platform.server.logics.DataObject;
import platform.server.logics.property.group.AbstractGroup;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.text.Format;
import java.util.*;

public abstract class DataClass<T> implements StaticClass, Type<T>, AndClassSet, OrClassSet {
    private static Map<String, DataClass> sidToClass = new HashMap<String, DataClass>();
    protected String caption;

    static void storeClass(String sid, DataClass cls) {
        sidToClass.put(sid, cls);
    }

    protected DataClass(String caption) {
        this.caption = caption;
    }

    @Override
    public String getCaption() {
        return caption;
    }

    public abstract DataClass getCompatible(DataClass compClass);

    public DataObject getDefaultObjectValue() {
        return new DataObject(getDefaultValue(), this);
    }
    public ValueExpr getDefaultExpr() {
        return getDefaultObjectValue().getExpr();
    }

    public BaseExpr getClassExpr() {
        return getDefaultExpr();
    }

    public AbstractGroup getParent() {
        return null;
    }

    public boolean isCompatibleParent(ValueClass remoteClass) {
        return remoteClass instanceof DataClass && getCompatible((DataClass) remoteClass) == this;
    }

    public boolean isCompatible(Type type) {
        return type instanceof DataClass && getCompatible((DataClass) type) != null;
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

    public static DataClass deserialize(DataInputStream inStream) throws IOException {
        byte type = inStream.readByte();

        if (type == Data.INTEGER) return IntegerClass.instance;
        if (type == Data.LONG) return LongClass.instance;
        if (type == Data.DOUBLE) return DoubleClass.instance;
        if (type == Data.NUMERIC) return NumericClass.get(inStream.readInt(), inStream.readInt());
        if (type == Data.LOGICAL) return LogicalClass.instance;
        if (type == Data.DATE) return DateClass.instance;
        if (type == Data.STRING) return StringClass.get(inStream.readInt());
        if (type == Data.INSENSITIVESTRING) return InsensitiveStringClass.get(inStream.readInt());
        if (type == Data.IMAGE) return ImageClass.instance;
        if (type == Data.WORD) return WordClass.instance;
        if (type == Data.EXCEL) return ExcelClass.instance;
        if (type == Data.TEXT) return TextClass.instance;
        if (type == Data.YEAR) return YearClass.instance;
        if (type == Data.PDF) return PDFClass.instance;
        if (type == Data.DATETIME) return DateTimeClass.instance;
        if (type == Data.CUSTOMFILECLASS) return CustomFileClass.instance;

        throw new IOException();
    }

    public DataObject getEmptyValueExpr() {
        return new DataObject(0, this);
    }

    public Expr getStaticExpr(Object value) {
        return getType().isSafeString(value) // идея в том что, если не Safe String то нужно по любому использовать ValueExpr, очень маловероятно что он пересекется с другим значением
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
        reportField.alignment = JRAlignment.HORIZONTAL_ALIGN_LEFT;
        return !reportField.valueClass.isArray();
    }

    public ObjectInstance newInstance(ObjectEntity entity) {
        assert !entity.addOnTransaction;
        return new DataObjectInstance(entity, this);
    }

    public ConcreteClass getDataClass(Object value, SQLSession session, BaseClass baseClass) {
        return this;
    }

    public ConcreteClass getBinaryClass(byte[] value, SQLSession session, BaseClass baseClass) throws SQLException {
        return this;
    }

    public void prepareClassesQuery(Expr expr, Query<?, Object> query, BaseClass baseClass) {
    }

    public ConcreteClass readClass(Expr expr, Map<Object, Object> classes, BaseClass baseClass, KeyType keyType) {
        return this;
    }

    public List<AndClassSet> getUniversal(BaseClass baseClass) {
        return Collections.<AndClassSet>singletonList(this);
    }

    public int getBinaryLength(boolean charBinary) {
        return 8;
    }

    public DataClass getKeepClass() {
        return this;
    }

    public boolean isSafeType(Object value) {
        return true;
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
}

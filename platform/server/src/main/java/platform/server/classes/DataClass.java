package platform.server.classes;

import net.sf.jasperreports.engine.JRAlignment;
import platform.interop.Data;
import platform.server.classes.sets.AndClassSet;
import platform.server.classes.sets.OrClassSet;
import platform.server.data.SQLSession;
import platform.server.data.expr.BaseExpr;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyType;
import platform.server.data.expr.ValueExpr;
import platform.server.data.query.Query;
import platform.server.data.type.ParseException;
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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;

public abstract class DataClass<T> implements ConcreteValueClass, Type<T>, AndClassSet, OrClassSet {

    public abstract DataClass getCompatible(DataClass compClass);

    public abstract Object getDefaultValue();

    public ValueExpr getActionExpr() {
        return new ValueExpr(getDefaultValue(), this);
    }

    public BaseExpr getClassExpr() {
        return getActionExpr();
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
        if (type == Data.IMAGE) return ImageClass.instance;
        if (type == Data.WORD) return WordClass.instance;
        if (type == Data.EXCEL) return ExcelClass.instance;
        if (type == Data.TEXT) return TextClass.instance;
        if (type == Data.YEAR) return YearClass.instance;

        throw new IOException();
    }

    public DataObject getEmptyValueExpr() {
        return new DataObject(0, this);
    }

    protected abstract Class getJavaClass();

    public abstract Format getDefaultFormat();

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
        reportField.valueClass = getJavaClass();
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

    public Object parseString(String s) throws ParseException {
        throw new RuntimeException("Parsing values from string is not supported");
    }
}

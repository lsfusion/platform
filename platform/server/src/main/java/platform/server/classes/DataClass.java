package platform.server.classes;

import net.sf.jasperreports.engine.JRAlignment;
import platform.interop.Data;
import platform.server.classes.sets.AndClassSet;
import platform.server.classes.sets.OrClassSet;
import platform.server.data.type.Type;
import platform.server.data.SQLSession;
import platform.server.data.where.Where;
import platform.server.data.expr.Expr;
import platform.server.data.query.Query;
import platform.server.logics.DataObject;
import platform.server.logics.property.group.AbstractGroup;
import platform.server.view.form.CustomClassView;
import platform.server.view.form.DataObjectImplement;
import platform.server.view.form.ObjectImplement;
import platform.server.view.form.client.report.ReportDrawField;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.text.Format;
import java.util.Random;
import java.util.Map;
import java.util.List;
import java.util.Collections;
import java.sql.SQLException;

public abstract class DataClass<T> implements ConcreteValueClass, Type<T>, AndClassSet, OrClassSet {

    public abstract DataClass getCompatible(DataClass compClass);
    public abstract Object getDefaultValue();

    public Object getAnyValue() {
        return getDefaultValue();
    }

    public AbstractGroup getParent() {
        return null;
    }

    public boolean isCompatibleParent(ValueClass remoteClass) {
        return remoteClass instanceof DataClass && getCompatible((DataClass)remoteClass)==this;
    }

    public boolean isCompatible(Type type) {
        return type instanceof DataClass && getCompatible((DataClass)type)!=null;
    }

    public DataClass getUpSet() {
        return this;
    }

    public boolean isEmpty() {
        return false;
    }

    public DataClass and(AndClassSet node) {
        if(node.isEmpty()) return this;

        DataClass compatible = getCompatible((DataClass) node);
        assert (compatible!=null); // классы должны быть совместимы
        return compatible;
    }

    public OrClassSet and(OrClassSet node) {
        return and((AndClassSet)node);
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
        return node instanceof DataClass && getCompatible((DataClass)node)!=null;
    }

    public boolean containsAll(OrClassSet node) {
        return node instanceof DataClass && getCompatible((DataClass)node)!=null;
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

        if(type==Data.INTEGER) return IntegerClass.instance;
        if(type==Data.LONG) return LongClass.instance;
        if(type==Data.DOUBLE) return DoubleClass.instance;
        if(type==Data.NUMERIC) return NumericClass.get(inStream.readInt(), inStream.readInt());
        if(type==Data.LOGICAL) return LogicalClass.instance;
        if(type==Data.DATE) return DateClass.instance;
        if(type==Data.STRING) return StringClass.get(inStream.readInt());

        throw new IOException();
    }

    public DataObject getEmptyValueExpr() {
        return new DataObject(0,this);
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
    public void fillReportDrawField(ReportDrawField reportField) {
        reportField.valueClass = getJavaClass();
        reportField.alignment = JRAlignment.HORIZONTAL_ALIGN_LEFT;
    }

    public ObjectImplement newObject(int ID, String SID, String caption, CustomClassView classView, boolean addOnTransaction) {
        assert !addOnTransaction;
        return new DataObjectImplement(ID, SID, this, caption);
    }

    public ConcreteClass getDataClass(Object value, SQLSession session, BaseClass baseClass) {
        return this;
    }

    public ConcreteClass getBinaryClass(byte[] value, SQLSession session, BaseClass baseClass) throws SQLException {
        return this;
    }

    public void prepareClassesQuery(Expr expr, Query<?, Object> query, BaseClass baseClass) {
    }

    public ConcreteClass readClass(Expr expr, Map<Object, Object> classes, BaseClass baseClass, Where where) {
        return this;
    }

    public List<AndClassSet> getUniversal(BaseClass baseClass) {
        return Collections.<AndClassSet>singletonList(this);
    }

    public int getBinaryLength(boolean charBinary) {
        return 8;
    }
}

package platform.server.data.classes;

import net.sf.jasperreports.engine.JRAlignment;
import platform.server.data.classes.where.ClassSet;
import platform.server.data.classes.where.OrClassSet;
import platform.server.data.types.Type;
import platform.server.logics.DataObject;
import platform.server.logics.properties.DataProperty;
import platform.server.logics.properties.groups.AbstractGroup;
import platform.server.view.form.client.report.ReportDrawField;
import platform.interop.Data;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.text.Format;
import java.util.Random;

public abstract class DataClass<T> implements ConcreteValueClass, Type<T>, ClassSet, OrClassSet {

    public abstract DataClass getCompatible(DataClass compClass);
    public abstract Object getDefaultValue();

    public AbstractGroup getParent() {
        return null;
    }

    public boolean isCompatibleParent(ValueClass remoteClass) {
        return remoteClass instanceof DataClass && getCompatible((DataClass)remoteClass)==this;
    }

    public boolean isCompatible(Type type) {
        return type instanceof DataClass && getCompatible((DataClass)type)!=null;
    }

    public DataProperty getExternalID() {
        return null;
    }

    public DataClass getUpSet() {
        return this;
    }

    public boolean isEmpty() {
        return false;
    }

    public DataClass and(ClassSet node) {
        if(node.isEmpty()) return this;

        DataClass compatible = getCompatible((DataClass) node);
        assert (compatible!=null); // классы должны быть совместимы
        return compatible;
    }

    public DataClass getRandom(Random randomizer) {
        return this;
    }

    public DataClass getCommonClass() {
        return this;
    }

    public boolean containsAll(ClassSet node) {
        return inSet(node);
    }

    public boolean containsAll(OrClassSet node) {
        return node instanceof DataClass && inSet((DataClass)node);
    }

    public OrClassSet or(OrClassSet node) {
        return and((DataClass)node);
    }
    public OrClassSet getOr() {
        return this;
    }

    public boolean inSet(ClassSet set) {
        return set instanceof DataClass && getCompatible((DataClass)set)!=null;
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
}

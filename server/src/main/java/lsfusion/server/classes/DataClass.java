package lsfusion.server.classes;

import lsfusion.server.data.expr.formula.FormulaClass;
import net.sf.jasperreports.engine.type.HorizontalAlignEnum;
import lsfusion.base.ExtInt;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.base.col.interfaces.mutable.add.MAddExclMap;
import lsfusion.server.caches.ManualLazy;
import lsfusion.server.classes.sets.AndClassSet;
import lsfusion.server.classes.sets.OrClassSet;
import lsfusion.server.data.SQLSession;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.KeyType;
import lsfusion.server.data.expr.StaticValueExpr;
import lsfusion.server.data.expr.ValueExpr;
import lsfusion.server.data.expr.query.Stat;
import lsfusion.server.data.type.AbstractType;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.where.Where;
import lsfusion.server.form.entity.ObjectEntity;
import lsfusion.server.form.instance.DataObjectInstance;
import lsfusion.server.form.instance.ObjectInstance;
import lsfusion.server.form.view.report.ReportDrawField;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.property.IsClassProperty;
import lsfusion.server.logics.property.group.AbstractGroup;

import java.io.DataOutputStream;
import java.io.IOException;
import java.text.Format;
import java.util.Random;

public abstract class DataClass<T> extends AbstractType<T> implements StaticClass, FormulaClass, ValueClassSet, OrClassSet {
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

    private boolean containsAll(DataClass node, boolean implicitCast) {
        DataClass compatible = getCompatible((DataClass) node);
        if(implicitCast)
            return compatible != null;
        return compatible == this;
    }

    public boolean containsAll(AndClassSet node, boolean implicitCast) {
        return node instanceof DataClass && containsAll((DataClass) node, implicitCast);
    }

    public boolean containsAll(OrClassSet node, boolean implicitCast) {
        return node instanceof DataClass && containsAll((DataClass) node, implicitCast);
    }

    public OrClassSet getOr() {
        return this;
    }

    public boolean inSet(AndClassSet set) {
        return ConcreteCustomClass.inSet(this, set);
    }

    public Type getType() {
        return this;
    }

    public abstract byte getTypeID();

    public void serialize(DataOutputStream outStream) throws IOException {
        outStream.writeByte(getTypeID());
    }

    public Expr getStaticExpr(Object value) {
        return new StaticValueExpr(value, this);
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

    public ConcreteClass getDataClass(Object value, SQLSession session, AndClassSet classSet, BaseClass baseClass) {
        return this;
    }

    public void prepareClassesQuery(Expr expr, Where where, MSet<Expr> exprs, BaseClass baseClass) {
    }

    public ConcreteClass readClass(Expr expr, ImMap<Expr, Object> classes, BaseClass baseClass, KeyType keyType) {
        return this;
    }

    public ImList<AndClassSet> getUniversal(BaseClass baseClass) {
        return ListFact.<AndClassSet>singleton(this);
    }

    public ExtInt getCharLength() {
        return new ExtInt(8);
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

    public Object getInfiniteValue(boolean min) {
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

    public ValueClassSet getValueClassSet() {
        return this;
    }
}

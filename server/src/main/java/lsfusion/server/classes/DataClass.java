package lsfusion.server.classes;

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
import lsfusion.server.classes.sets.ResolveClassSet;
import lsfusion.server.classes.sets.ResolveUpClassSet;
import lsfusion.server.data.OperationOwner;
import lsfusion.server.data.SQLSession;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.KeyType;
import lsfusion.server.data.expr.StaticValueExpr;
import lsfusion.server.data.expr.ValueExpr;
import lsfusion.server.data.expr.formula.FormulaClass;
import lsfusion.server.data.expr.query.Stat;
import lsfusion.server.data.type.AbstractType;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.where.Where;
import lsfusion.server.form.entity.ObjectEntity;
import lsfusion.server.form.instance.DataObjectInstance;
import lsfusion.server.form.instance.ObjectInstance;
import lsfusion.server.form.view.report.ReportDrawField;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.i18n.LocalizedString;
import lsfusion.server.logics.property.IsClassProperty;
import lsfusion.server.logics.property.group.AbstractGroup;
import net.sf.jasperreports.engine.type.HorizontalAlignEnum;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Random;

public abstract class DataClass<T> extends AbstractType<T> implements StaticClass, FormulaClass, ValueClassSet, OrClassSet, ResolveClassSet {
    private static MAddExclMap<String, DataClass> sidToClass = MapFact.mBigStrongMap();
    protected LocalizedString caption;

    public static void storeClass(DataClass... classes) {
        for(DataClass cls : classes)
            sidToClass.exclAdd(cls.getSID(), cls);
    }

    protected DataClass(LocalizedString caption) {
        this.caption = caption;
    }

    @Override
    public LocalizedString getCaption() {
        return caption;
    }

    public abstract DataClass getCompatible(DataClass compClass, boolean or);

    // очень аккуратно / нельзя использовать, так как если рассинхронизируется с equals, то будут неправильные кэши, скажем как раньше в EqualsWhere было, будет возвращать true, а кэши будут подхватываться и для различных valueExpr
    public boolean compatibleEquals(Object object, DataClass compareClass, Object compareObject) {
        DataClass compatible = getCompatible(compareClass, true);
        return compatible != null && compatible.read(object).equals(compatible.read(compareObject));
    }

    public T readCast(Object value, ConcreteClass typeFrom) {
        if(!(typeFrom instanceof DataClass))
            throw new RuntimeException("Cannot cast value : " + value + ", from : " + typeFrom + ", to : " + this);

        return read(value);
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
        return remoteClass instanceof DataClass && containsAll((DataClass) remoteClass, false);
    }

    public Type getCompatible(Type type) {
        if(!(type instanceof DataClass))
            return null;

        return getCompatible((DataClass) type, true);
    }

    public DataClass getUpSet() {
        return this;
    }

    public boolean isEmpty() {
        return false;
    }

    public DataClass and(AndClassSet node) {
        if (node.isEmpty()) return (DataClass) node;

        DataClass compatible = getCompatible((DataClass) node, false);
        assert (compatible != null); // классы должны быть совместимы
        return compatible;
    }

    public OrClassSet and(OrClassSet node) {
        return and((AndClassSet) node);
    }

    public DataClass or(AndClassSet node) {
        if (node.isEmpty()) return this;

        DataClass compatible = getCompatible((DataClass) node, true);
        assert (compatible != null); // классы должны быть совместимы
        return compatible;
    }

    public OrClassSet or(OrClassSet node) {
        return or((AndClassSet)node);
    }

    public DataClass getRandom(Random randomizer) {
        return this;
    }

    public DataClass getCommonClass() {
        return this;
    }

    public AndClassSet getCommonAnd() {
        return this;
    }

    private boolean containsAll(DataClass node, boolean implicitCast) {
        DataClass compatible = getCompatible((DataClass) node, true);
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

    public ConcreteClass getDataClass(Object value, SQLSession session, AndClassSet classSet, BaseClass baseClass, OperationOwner owner) {
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

    public Stat getTypeStat(boolean forJoin) {
        if(forJoin) // см. описание BaseExpr.getTypeStat, единственное использование у остальных в основном и так ALOT
            return Stat.ALOT;
        return getTypeStat();
    }

    public Stat getTypeStat() {
        return Stat.ALOT;
    }
    
    public ValueClassSet getValueClassSet() {
        return this;
    }

    public ResolveClassSet getResolveSet() {
        return this;
    }

    public boolean containsAll(ResolveClassSet set, boolean implicitCast) {
        return set instanceof DataClass && containsAll((DataClass) set, implicitCast);
    }

    public ResolveClassSet and(ResolveClassSet set) {
        if(!(set instanceof DataClass))
            return ResolveUpClassSet.FALSE;

        DataClass compatible = getCompatible((DataClass) set, false);
        if(compatible == null)
            return ResolveUpClassSet.FALSE;
        return compatible;
    }

    public ResolveClassSet or(ResolveClassSet set) {
        return or((AndClassSet)set);
    }

    public AndClassSet toAnd() {
        return this;
    }

    public ResolveClassSet toResolve() {
        return this;
    }

    @Override
    public String getCanonicalName() {
        return getSID();
    }

    public String getParsedName() {
        return getCanonicalName();
    }

    public boolean fixedSize() {
        return true;
    }

    @Override
    public String getShortName() {
        return "";
    }

    @Override
    public boolean isZero(Object object) {
        return isValueZero(read(object));
    }
    
    public boolean isValueZero(T value) {
        return false;
    }
}

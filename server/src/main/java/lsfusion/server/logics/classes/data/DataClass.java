package lsfusion.server.logics.classes.data;

import lsfusion.base.col.ListFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.interop.form.property.ExtInt;
import lsfusion.server.base.caches.ManualLazy;
import lsfusion.server.base.controller.thread.ThreadLocalContext;
import lsfusion.server.data.OperationOwner;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.formula.FormulaClass;
import lsfusion.server.data.expr.key.KeyType;
import lsfusion.server.data.expr.value.StaticValueExpr;
import lsfusion.server.data.expr.value.ValueExpr;
import lsfusion.server.data.sql.SQLSession;
import lsfusion.server.data.sql.syntax.SQLSyntax;
import lsfusion.server.data.stat.Stat;
import lsfusion.server.data.type.AbstractType;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.data.where.Where;
import lsfusion.server.language.action.LA;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.classes.ConcreteClass;
import lsfusion.server.logics.classes.StaticClass;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.classes.ValueClassSet;
import lsfusion.server.logics.classes.user.BaseClass;
import lsfusion.server.logics.classes.user.ConcreteCustomClass;
import lsfusion.server.logics.classes.user.CustomClass;
import lsfusion.server.logics.classes.user.set.AndClassSet;
import lsfusion.server.logics.classes.user.set.OrClassSet;
import lsfusion.server.logics.classes.user.set.ResolveClassSet;
import lsfusion.server.logics.classes.user.set.ResolveUpClassSet;
import lsfusion.server.logics.form.interactive.instance.object.DataObjectInstance;
import lsfusion.server.logics.form.interactive.instance.object.ObjectInstance;
import lsfusion.server.logics.form.stat.print.design.ReportDrawField;
import lsfusion.server.logics.form.struct.group.Group;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.property.classes.IsClassProperty;
import lsfusion.server.physics.dev.i18n.LocalizedString;
import net.sf.jasperreports.engine.type.HorizontalTextAlignEnum;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Random;

public abstract class DataClass<T> extends AbstractType<T> implements StaticClass, FormulaClass, ValueClassSet, OrClassSet, ResolveClassSet {
//    private static MAddExclMap<String, DataClass> sidToClass = MapFact.mBigStrongMap();
    protected LocalizedString caption;

    public static void storeClass(DataClass... classes) {
//        for(DataClass cls : classes)
//            sidToClass.exclAdd(cls.getSID(), cls);
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

    public abstract T getDefaultValue();

    public DataObject getDefaultObjectValue() {
        return new DataObject(getDefaultValue(), this);
    }
    public ValueExpr getDefaultExpr() {
        return getDefaultObjectValue().getExpr();
    }

    public Group getParent() {
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

    @Override
    public boolean equalsCompatible(ResolveClassSet set) {
        if(!(set instanceof DataClass))
            return false;
        
        return getCompatible((DataClass)set, true) != null;
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

    public int getReportMinimumWidth() {
        return getReportPreferredWidth();
    }

    public int getReportPreferredWidth() {
        return 50;
    }

    public void fillReportDrawField(ReportDrawField reportField) {
        reportField.valueClass = getReportJavaClass();
        reportField.alignment = HorizontalTextAlignEnum.LEFT;
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

//    public static DataClass findDataClass(String sid) {
//        return sidToClass.get(sid);
//    }

    public T getInfiniteValue(boolean min) {
        throw new RuntimeException("not supported");
    }

    @Override
    public LA getDefaultOpenAction(BaseLogicsModule baseLM) {
        return null;
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

    public abstract String getString(Object value, SQLSyntax syntax);
    
    @Override
    public String toString() {
        return getCanonicalName() + " '" + ThreadLocalContext.localize(caption) + "'";
    }

}

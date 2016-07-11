package lsfusion.server.logics.property;

import lsfusion.base.BaseUtils;
import lsfusion.base.FullFunctionSet;
import lsfusion.base.FunctionSet;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.lru.LRUUtil;
import lsfusion.base.col.lru.LRUWWVSMap;
import lsfusion.server.Settings;
import lsfusion.server.caches.ManualLazy;
import lsfusion.server.caches.ValuesContext;
import lsfusion.server.classes.BaseClass;
import lsfusion.server.data.OperationOwner;
import lsfusion.server.data.QueryEnvironment;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.SQLSession;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.session.*;

import java.sql.SQLException;

import static lsfusion.base.BaseUtils.merge;

public class OverrideSessionModifier extends SessionModifier {

    private final IncrementProps override;
    private final SessionModifier modifier;

    private final FunctionSet<CalcProperty> forceDisableHintIncrement;
    private final FunctionSet<CalcProperty> forceDisableNoUpdate;
    private final FunctionSet<CalcProperty> forceHintIncrement;
    private final FunctionSet<CalcProperty> forceNoUpdate;

    private Integer limitHintIncrementComplexity = null;
    private Long limitHintIncrementStat = null;

    @Override
    public ImSet<CalcProperty> getHintProps() {
        return super.getHintProps().merge(modifier.getHintProps());
    }

    private FunctionSet<CalcProperty> pushHints() {
        return merge(override.getProperties(), forceHintIncrement, forceNoUpdate);
    }

    public int getMaxCount(CalcProperty property) {
        return BaseUtils.max(override.getMaxCount(property), modifier.getMaxCount(property));
    }

    private static LRUWWVSMap<OverrideSessionModifier, CalcProperty, Boolean> pushHint = new LRUWWVSMap<>(LRUUtil.L2);
    @ManualLazy
    private boolean pushHint(CalcProperty property) { // частый вызов из-за кэширования хинтов, уже нет так как отрезается проверкой на complex
        Boolean result = pushHint.get(this, property);
        if(result==null) {
            result = !CalcProperty.dependsSet(property, override.getProperties(), forceHintIncrement, forceNoUpdate);
            pushHint.put(this, property, result);
        }
        return result;
    }

    @Override
    public boolean allowHintIncrement(CalcProperty property) {
        if(forceDisableHintIncrement.contains(property))
            return false;

        if(pushHint(property))
            return modifier.allowHintIncrement(property);

        return super.allowHintIncrement(property);
    }

    @Override
    public boolean forceHintIncrement(CalcProperty property) {
        return forceHintIncrement.contains(property);
    }

    @Override
    public boolean forceNoUpdate(CalcProperty property) {
        return forceNoUpdate.contains(property);
    }

    @Override
    public boolean forceDisableNoUpdate(CalcProperty property) {
        boolean result = forceDisableNoUpdate.contains(property);
        // если здесь запрещено, то и в modifier'е должно быть запрещено
        assert !result || modifier.forceDisableNoUpdate(property);
        return result;
    }

    @Override
    public <P extends PropertyInterface> ValuesContext cacheAllowPrereadValues(CalcProperty<P> property) {
        if(!allowPropertyPrereadValues(property)) // оптимизация
            return null;

        if(pushHint(property))
            return modifier.cacheAllowPrereadValues(property);

        return super.cacheAllowPrereadValues(property);
    }

    @Override
    public <P extends PropertyInterface> boolean allowPrereadValues(CalcProperty<P> property, ImMap<P, Expr> values) {
        if(!allowPropertyPrereadValues(property)) // оптимизация
            return false;

        if(pushHint(property))
            return modifier.allowPrereadValues(property, values);

        return super.allowPrereadValues(property, values);
    }


    @Override
    public int getLimitHintIncrementComplexity() {
        if(limitHintIncrementComplexity!=null)
            return limitHintIncrementComplexity;

        return super.getLimitHintIncrementComplexity();
    }

    @Override
    public long getLimitHintIncrementStat() {
        if(limitHintIncrementStat!=null)
            return limitHintIncrementStat;

        return super.getLimitHintIncrementStat();
    }

    @Override
    public void addHintIncrement(CalcProperty property) throws SQLException, SQLHandledException {
        assert allowHintIncrement(property);

        // если не зависит от override'а проталкиваем внутрь
        if(pushHint(property)) {
            modifier.addHintIncrement(property);
            return;
        }

        super.addHintIncrement(property);
    }

    @Override
    public <P extends PropertyInterface> void addPrereadValues(CalcProperty<P> property, ImMap<P, Expr> values) throws SQLException, SQLHandledException {
        // если не зависит от override'а проталкиваем внутрь
        if(pushHint(property)) {
            modifier.addPrereadValues(property, values);
            return;
        }

        super.addPrereadValues(property, values);
    }

    @Override
    public void clearPrereads() throws SQLException {
        super.clearPrereads();
        modifier.clearPrereads();
    }

    @Override
    public void addNoUpdate(CalcProperty property) {
        assert allowNoUpdate(property);

        if(pushHint(property) && modifier.allowNoUpdate(property)) {
            modifier.addNoUpdate(property);
            return;
        }

        super.addNoUpdate(property);
    }

    // уведомляет что IncrementProps изменился
    public void eventIncrementChange(CalcProperty property, boolean dataChanged, boolean sourceChanged) {
//        pushHint.remove(property);
        eventChange(property, dataChanged, sourceChanged);

        if(sourceChanged)
            for(CalcProperty incrementProperty : modifier.getHintProps()) // пробежим по increment'ам modifier, и уведомим что они могли изменится
                if(CalcProperty.depends(incrementProperty, property))
                    eventChange(incrementProperty, false, true);
    }

    @Override
    public void clean(SQLSession sql, OperationOwner owner) throws SQLException {
        override.unregisterView(this);
        modifier.unregisterView(this);

        super.clean(sql, owner);
    }

    public OverrideSessionModifier(IncrementProps override, FunctionSet<CalcProperty> forceDisableHintIncrement, FunctionSet<CalcProperty> forceDisableNoUpdate, FunctionSet<CalcProperty> forceHintIncrement, FunctionSet<CalcProperty> forceNoUpdate, SessionModifier modifier) { // нужно clean вызывать после такого modifier'а
        this.override = override;
        this.modifier = modifier;
        this.forceDisableHintIncrement = forceDisableHintIncrement;
        this.forceDisableNoUpdate = forceDisableNoUpdate;
        this.forceHintIncrement = forceHintIncrement;
        this.forceNoUpdate = forceNoUpdate;

        // assert что modifier.forceDisableNoUpdate содержит все this.forceDisableNoUpdate
        // assert что forceDisableIncrement содержит все this.forceDisabeNoUpdate

        override.registerView(this);
        modifier.registerView(this);
    }

    public OverrideSessionModifier(IncrementProps override, FunctionSet<CalcProperty> forceDisableHintIncrement, SessionModifier modifier) { // нужно clean вызывать после такого modifier'а
        this(override, forceDisableHintIncrement, FullFunctionSet.<CalcProperty>instance(), SetFact.<CalcProperty>EMPTY(), SetFact.<CalcProperty>EMPTY(), modifier);
    }

    public OverrideSessionModifier(IncrementProps override, SessionModifier modifier) { // нужно clean вызывать после такого modifier'а
        this(override, Settings.get().isNoApplyIncrement(), modifier);

        limitHintIncrementComplexity = Settings.get().getLimitApplyHintIncrementComplexity();
        limitHintIncrementStat = Settings.get().getLimitApplyHintIncrementStat();
    }

    public OverrideSessionModifier(IncrementProps override, boolean disableHintIncrement, SessionModifier modifier) { // нужно clean вызывать после такого modifier'а
        this(override, disableHintIncrement ? FullFunctionSet.<CalcProperty>instance() : SetFact.<CalcProperty>EMPTY(), modifier);
    }

    @Override
    protected <P extends PropertyInterface> ModifyChange<P> calculateModifyChange(CalcProperty<P> property, PrereadRows<P> preread, FunctionSet<CalcProperty> overrided) {
        PropertyChange<P> overrideChange = override.getPropertyChange(property);
        if(overrideChange!=null)
            return new ModifyChange<>(overrideChange, true);
        return modifier.getModifyChange(property, preread, merge(overrided, CalcProperty.getDependsOnSet(override.getProperties()), forceDisableHintIncrement));
    }

    public ImSet<CalcProperty> calculateProperties() {
        return modifier.getProperties().merge(override.getProperties());
    }

    public SQLSession getSQL() {
        return modifier.getSQL();
    }

    public OperationOwner getOpOwner() {
        return modifier.getOpOwner();
    }

    public BaseClass getBaseClass() {
        return modifier.getBaseClass();
    }

    public QueryEnvironment getQueryEnv() {
        return modifier.getQueryEnv();
    }
}

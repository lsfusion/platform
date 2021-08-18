package lsfusion.server.logics.action.session.change.modifier;

import com.google.common.base.Throwables;
import lsfusion.base.BaseUtils;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.lru.LRUUtil;
import lsfusion.base.col.lru.LRUWWVSMap;
import lsfusion.base.lambda.set.FullFunctionSet;
import lsfusion.base.lambda.set.FunctionSet;
import lsfusion.server.base.caches.ManualLazy;
import lsfusion.server.data.OperationOwner;
import lsfusion.server.data.QueryEnvironment;
import lsfusion.server.data.caches.ValuesContext;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.sql.SQLSession;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.logics.action.data.PrereadRows;
import lsfusion.server.logics.action.session.change.ModifyChange;
import lsfusion.server.logics.action.session.change.PropertyChange;
import lsfusion.server.logics.action.session.change.increment.IncrementProps;
import lsfusion.server.logics.classes.user.BaseClass;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.admin.Settings;

import java.sql.SQLException;

import static lsfusion.base.BaseUtils.merge;

public class OverrideSessionModifier extends SessionModifier {

    private final IncrementProps override;
    private final SessionModifier modifier;

    private final FunctionSet<Property> forceDisableHintIncrement;
    private final FunctionSet<Property> forceDisableNoUpdate;
    private final FunctionSet<Property> forceHintIncrement;
    private final FunctionSet<Property> forceNoUpdate;

    protected Integer limitHintIncrementComplexity = null;
    protected Long limitHintIncrementStat = null;

    @Override
    public ImSet<Property> getHintProps() {
        return super.getHintProps().merge(modifier.getHintProps());
    }

    private FunctionSet<Property> pushHints() {
        return merge(override.getProperties(), forceHintIncrement, forceNoUpdate);
    }

    public long getMaxCount(Property property) {
        return BaseUtils.max(override.getMaxCount(property), modifier.getMaxCount(property));
    }

    private static LRUWWVSMap<OverrideSessionModifier, Property, Boolean> pushHint = new LRUWWVSMap<>(LRUUtil.L2);
    @ManualLazy
    private boolean pushHint(Property property) { // частый вызов из-за кэширования хинтов, уже нет так как отрезается проверкой на complex
        Boolean result = pushHint.get(this, property);
        if(result==null) {
            result = !Property.dependsSet(property, override.getProperties(), forceHintIncrement, forceNoUpdate);
            pushHint.put(this, property, result);
        }
        return result;
    }

    @Override
    public boolean allowHintIncrement(Property property) {
        if(forceDisableHintIncrement.contains(property))
            return false;

        if(pushHint(property))
            return modifier.allowHintIncrement(property);

        return super.allowHintIncrement(property);
    }

    @Override
    public boolean forceHintIncrement(Property property) {
        return forceHintIncrement.contains(property);
    }

    @Override
    public boolean forceNoUpdate(Property property) {
        return forceNoUpdate.contains(property);
    }

    @Override
    public boolean forceDisableNoUpdate(Property property) {
        boolean result = forceDisableNoUpdate.contains(property);
        // если здесь запрещено, то и в modifier'е должно быть запрещено
        assert !result || modifier.forceDisableNoUpdate(property);
        return result;
    }

    @Override
    public <P extends PropertyInterface> ValuesContext cacheAllowPrereadValues(Property<P> property) {
        if(!allowPropertyPrereadValues(property)) // оптимизация
            return null;

        if(pushHint(property))
            return modifier.cacheAllowPrereadValues(property);

        return super.cacheAllowPrereadValues(property);
    }

    @Override
    public <P extends PropertyInterface> boolean allowPrereadValues(Property<P> property, ImMap<P, Expr> values, boolean hasChanges) {
        if(!allowPropertyPrereadValues(property)) // оптимизация
            return false;

        if(pushHint(property))
            return modifier.allowPrereadValues(property, values, hasChanges);

        return super.allowPrereadValues(property, values, hasChanges);
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
    public void addHintIncrement(Property property) throws SQLException, SQLHandledException {
        assert allowHintIncrement(property);

        // если не зависит от override'а проталкиваем внутрь
        if(pushHint(property)) {
            modifier.addHintIncrement(property);
            return;
        }

        super.addHintIncrement(property);
    }

    @Override
    public <P extends PropertyInterface> void addPrereadValues(Property<P> property, ImMap<P, Expr> values, boolean hasChanges) throws SQLException, SQLHandledException {
        // если не зависит от override'а проталкиваем внутрь
        if(pushHint(property)) {
            modifier.addPrereadValues(property, values, hasChanges);
            return;
        }

        super.addPrereadValues(property, values, hasChanges);
    }

    @Override
    public void clearPrereads() throws SQLException, SQLHandledException {
        super.clearPrereads();
        modifier.clearPrereads();
    }

    @Override
    public void addNoUpdate(Property property) throws SQLException, SQLHandledException {
        assert allowNoUpdate(property);

        if(pushHint(property) && modifier.allowNoUpdate(property)) {
            modifier.addNoUpdate(property);
            return;
        }

        super.addNoUpdate(property);
    }

    // уведомляет что IncrementProps изменился
    public void eventIncrementChange(Property property, boolean dataChanged, boolean sourceChanged) throws SQLException, SQLHandledException {
//        pushHint.remove(property);
        eventChange(property, dataChanged, sourceChanged);

        if(sourceChanged)
            for(Property incrementProperty : modifier.getHintProps()) // пробежим по increment'ам modifier, и уведомим что они могли изменится
                if(Property.depends(incrementProperty, property))
                    eventChange(incrementProperty, false, true);
    }

    @Override
    public void clean(SQLSession sql, OperationOwner owner) throws SQLException {
        cleanViews(); // двойной вызов, в случае если cleanViews был раньше, но с текущей реализацией cleanViews это не страшно

        super.clean(sql, owner);
    }

    @Override
    public void cleanViews() {
        override.unregisterView(this); // можно оставить в clean так как проблем синхронизации нет пока (используется только в DataSession.scope), но для чистоты логики закинем и сюда  
        modifier.unregisterView(this);
    }
    
    protected OverrideSessionModifier(String debugInfo, IncrementProps override, FunctionSet<Property> forceDisableHintIncrement, FunctionSet<Property> forceDisableNoUpdate, FunctionSet<Property> forceHintIncrement, FunctionSet<Property> forceNoUpdate, SessionModifier modifier) { // нужно clean вызывать после такого modifier'а
        super(debugInfo);
        this.override = override;
        this.modifier = modifier;
        this.forceDisableHintIncrement = forceDisableHintIncrement;
        this.forceDisableNoUpdate = forceDisableNoUpdate;
        this.forceHintIncrement = forceHintIncrement;
        this.forceNoUpdate = forceNoUpdate;

        // assert что modifier.forceDisableNoUpdate содержит все this.forceDisableNoUpdate
        // assert что forceDisableIncrement содержит все this.forceDisabeNoUpdate

        try {
            override.registerView(this);
            modifier.registerView(this);
        } catch (SQLException | SQLHandledException e) { // по идее updateSource быть не должно
            throw Throwables.propagate(e);
        }
    }

    public OverrideSessionModifier(String debugInfo, IncrementProps override, SessionModifier modifier) { // нужно clean вызывать после такого modifier'а
        this(debugInfo, override, Settings.get().isNoApplyIncrement() ? FullFunctionSet.instance() : SetFact.EMPTY(), FullFunctionSet.instance(), SetFact.EMPTY(), SetFact.EMPTY(), modifier);

        limitHintIncrementComplexity = Settings.get().getLimitApplyHintIncrementComplexity();
        limitHintIncrementStat = Settings.get().getLimitApplyHintIncrementStat();
    }

    @Override
    protected <P extends PropertyInterface> ModifyChange<P> calculateModifyChange(Property<P> property, PrereadRows<P> preread, FunctionSet<Property> overrided) {
        PropertyChange<P> overrideChange = override.getPropertyChange(property);
        if(overrideChange!=null)
            return new ModifyChange<>(overrideChange, preread, true);

        return modifier.getModifyChange(property, preread, merge(overrided, Property.getDependsOnSet(override.getProperties()), forceDisableHintIncrement));
    }

    public ImSet<Property> calculateProperties() {
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

    @Override
    public String out() {
        return super.out() + "\noverride : " + BaseUtils.tab(override.out()) + "\nmodifier : " + modifier.out();
    }
}

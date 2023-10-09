package lsfusion.server.logics.action.session.action;

import com.google.common.base.Throwables;
import lsfusion.base.BaseUtils;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.lambda.set.FunctionSet;
import lsfusion.base.lambda.set.SFunctionSet;
import lsfusion.server.data.sql.SQLSession;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.action.flow.AroundAspectAction;
import lsfusion.server.logics.action.flow.ChangeFlowType;
import lsfusion.server.logics.action.flow.FlowResult;
import lsfusion.server.logics.action.flow.FormChangeFlowType;
import lsfusion.server.logics.action.implement.ActionMapImplement;
import lsfusion.server.logics.action.session.DataSession;
import lsfusion.server.logics.form.interactive.action.async.map.AsyncMapEventExec;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.PropertyFact;
import lsfusion.server.logics.property.classes.IsClassProperty;
import lsfusion.server.logics.property.classes.infer.ClassType;
import lsfusion.server.logics.property.data.SessionDataProperty;
import lsfusion.server.logics.property.implement.PropertyMapImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.sql.SQLException;

public class NewSessionAction extends AroundAspectAction {
    private final FunctionSet<SessionDataProperty> explicitMigrateProps; // актуальны и для nested, так как иначе будет отличаться поведение от NEW SESSION
    private final boolean migrateClasses;
    private final boolean isNested;
    private final boolean singleApply;
    private final boolean newSQL;
    private final ImSet<FormEntity> fixedForms;

    public <I extends PropertyInterface> NewSessionAction(LocalizedString caption, ImOrderSet<I> innerInterfaces,
                                                          ActionMapImplement<?, I> action, boolean singleApply,
                                                          boolean newSQL,
                                                          FunctionSet<SessionDataProperty> explicitMigrateProps,
                                                          boolean isNested) {
        this(caption, innerInterfaces, action, singleApply, newSQL, explicitMigrateProps, false, isNested, null);
    }

    public <I extends PropertyInterface> NewSessionAction(LocalizedString caption, ImOrderSet<I> innerInterfaces,
                                                          ActionMapImplement<?, I> action, boolean singleApply,
                                                          boolean newSQL,
                                                          FunctionSet<SessionDataProperty> explicitMigrateProps,
                                                          boolean migrateClasses,
                                                          boolean isNested, ImSet<FormEntity> fixedForms) {
        super(caption, innerInterfaces, action);

        this.singleApply = singleApply;
        this.newSQL = newSQL;

        this.isNested = isNested;
        this.explicitMigrateProps = explicitMigrateProps;
        this.migrateClasses = migrateClasses;

        this.fixedForms = fixedForms;

        // (nested || explicitly nested) and used in action
        migrateProps = BaseUtils.remove(BaseUtils.merge(DataSession.keepNested(false), explicitMigrateProps),
                                    (SFunctionSet<SessionDataProperty>) element -> !(action.action.uses(element) || action.action.changes(element)));

        finalizeInit();
    }

    private final FunctionSet<SessionDataProperty> migrateProps;

    @Override
    protected ImMap<Property, Boolean> aspectChangeExtProps() {
        return super.aspectChangeExtProps().replaceValues(true);
    }

    @Override
    public ImMap<Property, Boolean> aspectUsedExtProps() {
        return super.aspectUsedExtProps().replaceValues(true);
    }

    @Override
    public PropertyMapImplement<?, PropertyInterface> calcWhereProperty() {
        return IsClassProperty.getMapProperty(
                super.calcWhereProperty().mapInterfaceClasses(ClassType.wherePolicy)); // нет смысла делать mapOld и нарушать , все равно весь механизм во многом эвристичен
    }

    protected ExecutionContext<PropertyInterface> beforeAspect(ExecutionContext<PropertyInterface> context) throws SQLException, SQLHandledException {
        DataSession session = context.getSession();
        if(session.isInTransaction()) { // если в транзацкции
            session.addRecursion(aspectActionImplement.getValueImplement(context.getKeys(), context.getObjectInstances(), context.getFormAspectInstance()), migrateProps, singleApply);
            return null;
        }

        ExecutionContext.NewSession<PropertyInterface> newContext;
        if(newSQL) {
            SQLSession sql;
            try {
                sql = context.getDbManager().createSQL();
            } catch (ClassNotFoundException | IllegalAccessException | InstantiationException e) {
                throw Throwables.propagate(e);
            }
            newContext = context.newSession(sql, fixedForms);
            newContext.getSession().isPrivateSql = true; // not pretty, in theory createSQL and isPrivateSql should be in DataSession constructor but it is a really rare case
        } else {
            newContext = context.newSession(fixedForms);
            DataSession newSession = newContext.getSession();
            if (isNested) {
                context.executeSessionEvents();

                newSession.setParentSession(session);
            } else {
                migrateSessionProperties(session, newSession);
            }
        }

        return newContext;
    }

    @Override
    public boolean hasFlow(ChangeFlowType type) {
        if (type == ChangeFlowType.NEWSESSION) 
            return true;
        if (type == ChangeFlowType.APPLY)
            return false;
        if (type == ChangeFlowType.CANCEL)
            return false;
        if ((type instanceof FormChangeFlowType || type == ChangeFlowType.HASSESSIONUSAGES || type == ChangeFlowType.NEEDMORESESSIONUSAGES) && !isNested)
            return false;
        return super.hasFlow(type);
    }

    protected void afterAspect(FlowResult result, ExecutionContext<PropertyInterface> context, ExecutionContext<PropertyInterface> innerContext) throws SQLException, SQLHandledException {
        if (!context.getSession().isInTransaction() && !newSQL) {
            migrateSessionProperties(innerContext.getSession(), context.getSession());
        }
    }

    private void migrateSessionProperties(DataSession migrateFrom, DataSession migrateTo) throws SQLException, SQLHandledException {
        assert !newSQL;
        migrateFrom.copySessionDataTo(migrateTo, migrateProps, migrateClasses);
    }

    protected void finallyAspect(ExecutionContext<PropertyInterface> context, ExecutionContext<PropertyInterface> innerContext) throws SQLException {
        ((ExecutionContext.NewSession<PropertyInterface>)innerContext).close(); // по сути и есть аналог try with resources ()
    }

    @Override
    protected <T extends PropertyInterface> ActionMapImplement<?, PropertyInterface> createAspectImplement(ImSet<PropertyInterface> interfaces, ActionMapImplement<?, PropertyInterface> action) {
        return PropertyFact.createNewSessionAction(interfaces, action, singleApply, newSQL, explicitMigrateProps, isNested);
    }

    @Override
    public AsyncMapEventExec<PropertyInterface> calculateAsyncEventExec(boolean optimistic, boolean recursive) {
        AsyncMapEventExec<PropertyInterface> simpleInput = aspectActionImplement.mapAsyncEventExec(optimistic, recursive);
        if(simpleInput != null)
            return simpleInput.newSession();
        return null;
    }
}

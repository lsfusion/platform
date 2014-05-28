package lsfusion.server.logics.property.actions.flow;

import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MExclSet;
import lsfusion.server.caches.IdentityLazy;
import lsfusion.server.classes.CustomClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.form.instance.FormInstance;
import lsfusion.server.logics.property.*;
import lsfusion.server.session.DataSession;
import lsfusion.server.session.PropertyChange;
import lsfusion.server.session.SinglePropertyTableUsage;

import java.sql.SQLException;
import java.util.Map;

public class NewSessionActionProperty extends AroundAspectActionProperty {
    private final boolean doApply;
    private final boolean migrateAllSessionProperties;
    private final ImSet<SessionDataProperty> migrateSessionProperties;
    private final boolean singleApply;

    public <I extends PropertyInterface> NewSessionActionProperty(String sID, String caption, ImOrderSet<I> innerInterfaces,
                                                                  ActionPropertyMapImplement<?, I> action, boolean doApply, boolean singleApply,
                                                                  boolean migrateAllSessionProperties, ImSet<SessionDataProperty> migrateSessionProperties) {
        super(sID, caption, innerInterfaces, action);

        this.doApply = doApply;
        this.singleApply = singleApply;
        this.migrateAllSessionProperties = migrateAllSessionProperties;
        this.migrateSessionProperties = migrateSessionProperties;

        finalizeInit();
    }

    @Override
    protected ImMap<CalcProperty, Boolean> aspectChangeExtProps() {
        return super.aspectChangeExtProps().replaceValues(true);
    }

    @Override
    public ImMap<CalcProperty, Boolean> aspectUsedExtProps() {
        return super.aspectUsedExtProps().replaceValues(true);
    }

    @Override
    public CalcPropertyMapImplement<?, PropertyInterface> getWhereProperty() {
        return IsClassProperty.getMapProperty(
                super.getWhereProperty().mapInterfaceClasses(ClassType.FULL)); // нет смысла делать mapOld и нарушать , все равно весь механизм во многом эвристичен
    }

    @IdentityLazy
    private ImSet<SessionDataProperty> getUsed(ExecutionContext<PropertyInterface> context) {
//        return CalcProperty.used(localUsed, aspectActionImplement.property.getUsedProps()).merge(migrateSessionProperties);

        if (migrateAllSessionProperties) {
            DataSession session = context.getSession();

            MExclSet<SessionDataProperty> mMigrateProps = SetFact.mExclSet();
            for (DataProperty changedProp : session.getDataChanges().keySet()) {
                if (changedProp instanceof SessionDataProperty) {
                    mMigrateProps.exclAdd((SessionDataProperty) changedProp);
                }
            }

            return mMigrateProps.immutable();
        } else {
            return migrateSessionProperties;
        }
    }

    protected ExecutionContext<PropertyInterface> beforeAspect(ExecutionContext<PropertyInterface> context) throws SQLException, SQLHandledException {
        DataSession session = context.getSession();
        if(session.isInTransaction()) { // если в транзацкции
            session.addRecursion(aspectActionImplement.getValueImplement(context.getKeys()), getUsed(context), singleApply);
            return null;
        }

        DataSession newSession = session.createSession();

        migrateProperties(session, newSession);

        return context.override(newSession);
    }

    @Override
    public boolean hasFlow(ChangeFlowType type) {
        return type == ChangeFlowType.NEWSESSION || (!(type == ChangeFlowType.APPLY || type == ChangeFlowType.CANCEL) && super.hasFlow(type));
    }

    protected void afterAspect(FlowResult result, ExecutionContext<PropertyInterface> context, ExecutionContext<PropertyInterface> innerContext) throws SQLException, SQLHandledException {
        if (!context.getSession().isInTransaction()) {
            migrateProperties(innerContext.getSession(), context.getSession());
        }

        if (doApply) {
            innerContext.apply();
        }

        FormInstance<?> formInstance = context.getFormInstance();
        if (formInstance != null) {
            formInstance.refreshData();
        }
    }

    private void migrateProperties(DataSession migrateFrom, DataSession migrateTo) throws SQLException, SQLHandledException {
        if (migrateAllSessionProperties) {
            for (Map.Entry<DataProperty, SinglePropertyTableUsage<ClassPropertyInterface>> e : migrateFrom.getDataChanges().entrySet()) {
                DataProperty prop = e.getKey();
                if (prop instanceof SessionDataProperty) {
                    SinglePropertyTableUsage<ClassPropertyInterface> usage = e.getValue();
                    migrateTo.change(prop, SinglePropertyTableUsage.getChange(usage));
                }
            }
        } else {
            int migrateCount = migrateSessionProperties.size();
            for (int i = 0; i < migrateCount; ++i) {
                SessionDataProperty migrateProp = migrateSessionProperties.get(i);

                PropertyChange<ClassPropertyInterface> propChange = migrateFrom.getDataChange(migrateProp);
                if (propChange != null) {
                    migrateTo.change(migrateProp, propChange);
                }
            }
        }
    }

    protected void finallyAspect(ExecutionContext<PropertyInterface> context, ExecutionContext<PropertyInterface> innerContext) throws SQLException, SQLHandledException {
        innerContext.getSession().close();
    }

    @Override
    public CustomClass getSimpleAdd() {
        return aspectActionImplement.property.getSimpleAdd();
    }

    @Override
    public PropertyInterface getSimpleDelete() {
        return aspectActionImplement.property.getSimpleDelete();
    }
}

package lsfusion.server.logics.form.interactive.instance.object;

import lsfusion.base.col.ListFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.col.interfaces.mutable.MOrderSet;
import lsfusion.interop.form.property.PropertyGroupType;
import lsfusion.server.base.caches.ManualLazy;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.formula.CastFormulaImpl;
import lsfusion.server.data.expr.formula.FormulaExpr;
import lsfusion.server.data.expr.query.GroupExpr;
import lsfusion.server.data.expr.query.GroupType;
import lsfusion.server.data.expr.value.ValueExpr;
import lsfusion.server.data.expr.where.cases.MExprCaseList;
import lsfusion.server.data.expr.where.classes.data.CompareWhere;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.sql.lambda.SQLFunction;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.data.where.Where;
import lsfusion.server.logics.classes.data.integral.NumericClass;
import lsfusion.server.logics.form.interactive.controller.init.InstanceFactory;
import lsfusion.server.logics.form.interactive.instance.property.AggrReaderInstance;
import lsfusion.server.logics.form.interactive.instance.property.PropertyDrawInstance;
import lsfusion.server.logics.form.interactive.instance.property.PropertyObjectInstance;
import lsfusion.server.logics.form.struct.property.PropertyDrawEntity;
import lsfusion.server.logics.property.oraction.PropertyInterface;

import java.sql.SQLException;
import java.util.function.Function;

public class GroupMode {
    public final ImSet<GroupColumn> groupProps;
    public final ImMap<PropertyDrawInstance, ImMap<ImMap<ObjectInstance, DataObject>, PropertyGroupType>> aggrProps;

    public GroupMode(ImSet<GroupColumn> groupProps, ImMap<PropertyDrawInstance, ImMap<ImMap<ObjectInstance, DataObject>, PropertyGroupType>> aggrProps) {
        this.groupProps = groupProps;
        this.aggrProps = aggrProps;
    }

    public static GroupMode create(ImSet<GroupColumn> groupProps, ImSet<GroupColumn> aggrProps, PropertyGroupType aggrType, InstanceFactory instanceFactory) {
        assert !groupProps.intersect(aggrProps);

        // look for formulas in aggrProps, and replace them with properties that they use
        // breadth-first search
        MOrderSet<GroupColumn> mAggrGroupProps = SetFact.mOrderSet(aggrProps.toOrderSet());
        for(int i=0;i<mAggrGroupProps.size();i++) {
            GroupColumn groupColumn = mAggrGroupProps.get(i);
            PropertyDrawEntity<?> entity = ((PropertyDrawInstance<?>) groupColumn.property).entity;

            if(entity.formula != null)
                for(PropertyDrawEntity formulaOperand : entity.formulaOperands) {
                    GroupColumn formulaColumn = new GroupColumn(instanceFactory.getInstance(formulaOperand), groupColumn.columnKeys);
                    if(!groupProps.contains(formulaColumn))
                        mAggrGroupProps.add(formulaColumn);
                }
        }
        ImOrderSet<GroupColumn> aggrGroupProps = mAggrGroupProps.immutableOrder();

        return new GroupMode(groupProps, group(aggrGroupProps.getSet(), groupColumn -> {
            PropertyDrawEntity entity = ((PropertyDrawInstance<?>) groupColumn.property).entity;
            if(entity.aggrFunc != null)
                return entity.aggrFunc;
            return aggrType;
        }));
    }

    private ImMap<PropertyDrawInstance, ImMap<ImMap<ObjectInstance, DataObject>, PropertyGroupType>> groupByProps;
    @ManualLazy
    public ImMap<PropertyDrawInstance, ImMap<ImMap<ObjectInstance, DataObject>, PropertyGroupType>> getGroupByProps() {
        if(groupByProps == null)
            groupByProps = calculateGroupByProps();
        return groupByProps;
    }

    private ImMap<PropertyDrawInstance, ImMap<ImMap<ObjectInstance, DataObject>, PropertyGroupType>> calculateGroupByProps() {
        return aggrProps.addExcl(group(groupProps, groupColumn -> PropertyGroupType.GROUP));
    }

    public static ImMap<PropertyDrawInstance, ImMap<ImMap<ObjectInstance, DataObject>, PropertyGroupType>> group(ImSet<GroupColumn> props, Function<GroupColumn, PropertyGroupType> types) {
        return props.group(key -> key.property).mapValues(key -> key.mapKeyValues(keyc -> keyc.columnKeys, types));
    }

    public boolean need(PropertyDrawInstance property) {
        return getGroupByProps().containsKey(property);
    }
    
    // object is ObjectInstance or GroupColumn
    public <K extends PropertyInterface> Expr transformExpr(final SQLFunction<PropertyObjectInstance<?>, Expr> getExpr, AggrReaderInstance aggrReader, Where groupModeWhere, ImMap<Object, Expr> groupModeExprs) throws SQLException, SQLHandledException {

        PropertyDrawInstance<K> property = aggrReader.getProperty();
        ImMap<ImMap<ObjectInstance, DataObject>, PropertyGroupType> groupColumnKeys = getGroupByProps().get(property);
        if(groupColumnKeys != null) {
            MExprCaseList mCases = new MExprCaseList(true);
            for (int j = 0, sizeJ = groupColumnKeys.size(); j < sizeJ; j++) {
                Where columnsWhere = CompareWhere.compareInclValues(groupModeExprs, groupColumnKeys.getKey(j));// in groupModeExprs there are all columnKeys in theory
                PropertyGroupType propertyGroupType = groupColumnKeys.getValue(j);

                Expr groupExpr;
                if(propertyGroupType != PropertyGroupType.GROUP) {
                    GroupType groupType = GroupType.valueOf(propertyGroupType.name());

                    // first find last values
                    ImMap<PropertyDrawInstance<K>.LastReaderInstance, Expr> lastAggrExprs = property.aggrLastReaders.<Expr, SQLException, SQLHandledException>mapOrderValuesEx(lastReaderInstance -> getExpr.apply(lastReaderInstance.getPropertyObjectInstance()));
                    ImOrderMap<Expr, Boolean> orders = property.aggrLastReaders.mapOrderKeyValues(lastAggrExprs::get, lastReaderInstance -> property.entity.lastAggrDesc);
                    ImMap<PropertyDrawInstance<K>.LastReaderInstance, Expr> lastValues = property.aggrLastReaders.getSet().mapValues((PropertyDrawInstance<K>.LastReaderInstance aggrLastReader) ->
                            GroupExpr.create(groupModeExprs, ListFact.toList(ValueExpr.get(groupModeWhere), lastAggrExprs.get(aggrLastReader)), orders, false, GroupType.LAST, groupModeExprs, false));

                    if(aggrReader instanceof PropertyDrawInstance) { // calculate value
                        groupExpr = getExpr.apply(property.getDrawInstance());
                        groupExpr = FormulaExpr.create(new CastFormulaImpl(NumericClass.get(100, 20)), ListFact.singleton(groupExpr));
                        groupExpr = GroupExpr.create(groupModeExprs, groupExpr, groupModeWhere.and(CompareWhere.equalsNull(lastAggrExprs, lastValues)), groupType, groupModeExprs);
                    } else // select last value
                        groupExpr = lastValues.get((PropertyDrawInstance<K>.LastReaderInstance)aggrReader);
                } else
                    groupExpr = getExpr.apply(aggrReader.getPropertyObjectInstance());

                mCases.add(columnsWhere, groupExpr);
            }
            return mCases.getFinal();
        }
        return null;
    }
}

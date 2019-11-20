package lsfusion.server.logics.form.interactive.instance.object;

import lsfusion.base.Pair;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.ThrowingSupplier;
import lsfusion.interop.form.property.PropertyGroupType;
import lsfusion.server.base.caches.IdentityLazy;
import lsfusion.server.base.caches.ManualLazy;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.formula.CastFormulaImpl;
import lsfusion.server.data.expr.formula.FormulaExpr;
import lsfusion.server.data.expr.key.KeyExpr;
import lsfusion.server.data.expr.query.GroupExpr;
import lsfusion.server.data.expr.query.GroupType;
import lsfusion.server.data.expr.value.ValueExpr;
import lsfusion.server.data.expr.where.cases.MExprCaseList;
import lsfusion.server.data.expr.where.classes.data.CompareWhere;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.data.where.Where;
import lsfusion.server.logics.action.session.change.modifier.Modifier;
import lsfusion.server.logics.classes.data.integral.NumericClass;
import lsfusion.server.logics.form.interactive.instance.property.PropertyDrawInstance;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.Objects;
import java.util.function.Function;

public class GroupMode {
    public final ImSet<GroupColumn> groupProps;
    public final ImMap<PropertyDrawInstance, ImMap<ImMap<ObjectInstance, DataObject>, PropertyGroupType>> aggrProps;

    public GroupMode(ImSet<GroupColumn> groupProps, ImMap<PropertyDrawInstance, ImMap<ImMap<ObjectInstance, DataObject>, PropertyGroupType>> aggrProps) {
        this.groupProps = groupProps;
        this.aggrProps = aggrProps;
    }

    private ImMap<PropertyDrawInstance, ImMap<ImMap<ObjectInstance, DataObject>, PropertyGroupType>> groupByProps;
    @ManualLazy
    public ImMap<PropertyDrawInstance, ImMap<ImMap<ObjectInstance, DataObject>, PropertyGroupType>> getGroupByProps() {
        if(groupByProps == null) {
            groupByProps = aggrProps.override(group(groupProps, groupColumn -> PropertyGroupType.GROUP));
        }
        return groupByProps;
    }

    public static ImMap<PropertyDrawInstance, ImMap<ImMap<ObjectInstance, DataObject>, PropertyGroupType>> group(ImSet<GroupColumn> props, Function<GroupColumn, PropertyGroupType> types) {
        return props.group(key -> key.property).mapValues(key -> key.mapKeyValues(keyc -> keyc.columnKeys, types));
    }

    public boolean need(PropertyDrawInstance property) {
        return getGroupByProps().containsKey(property);
    }
    
    // object is ObjectInstance or GroupColumn
    public Expr transformExpr(final Expr expr, PropertyDrawInstance property, Where groupModeWhere, ImMap<Object, Expr> groupModeExprs) {
        
        ImMap<ImMap<ObjectInstance, DataObject>, PropertyGroupType> groupColumnKeys = getGroupByProps().get(property);
        if(groupColumnKeys != null) {
            MExprCaseList mCases = new MExprCaseList(true);
            for (int j = 0, sizeJ = groupColumnKeys.size(); j < sizeJ; j++) {
                Where columnsWhere = CompareWhere.compareInclValues(groupModeExprs, groupColumnKeys.getKey(j));// in groupModeExprs there are all columnKeys in theory
                PropertyGroupType propertyGroupType = groupColumnKeys.getValue(j);

                Expr groupExpr = expr;
                if(propertyGroupType != PropertyGroupType.GROUP) {
                    GroupType groupType;
                    if(propertyGroupType == PropertyGroupType.COUNT) {
                        groupType = GroupType.SUM;
                        groupExpr = ValueExpr.COUNT.and(groupExpr.getWhere());
                    } else
                        groupType = GroupType.valueOf(propertyGroupType.name());

                    groupExpr = FormulaExpr.create(new CastFormulaImpl(NumericClass.get(100, 20)), ListFact.singleton(groupExpr));
                    groupExpr = GroupExpr.create(groupModeExprs, groupExpr, groupModeWhere, groupType, groupModeExprs);
                }
                
                mCases.add(columnsWhere, groupExpr);
            }
            return mCases.getFinal();
        }
        return null;
    }
}

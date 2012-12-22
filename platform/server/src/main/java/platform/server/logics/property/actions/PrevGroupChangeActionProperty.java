package platform.server.logics.property.actions;

import platform.base.col.MapFact;
import platform.base.col.SetFact;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.immutable.ImOrderSet;
import platform.base.col.interfaces.immutable.ImRevMap;
import platform.base.col.interfaces.immutable.ImSet;
import platform.base.col.interfaces.mutable.MRevMap;
import platform.base.col.interfaces.mutable.MSet;
import platform.base.col.interfaces.mutable.mapvalue.GetValue;
import platform.server.classes.ValueClass;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.where.extra.CompareWhere;
import platform.server.data.where.Where;
import platform.server.form.entity.GroupObjectEntity;
import platform.server.form.instance.GroupObjectInstance;
import platform.server.form.instance.PropertyObjectInterfaceInstance;
import platform.server.logics.PropertyUtils;
import platform.server.logics.linear.LAP;
import platform.server.logics.property.ActionProperty;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;
import platform.server.logics.property.PropertyInterface;
import platform.server.session.PropertySet;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.TreeSet;

import static platform.base.BaseUtils.toListFromArray;

public class PrevGroupChangeActionProperty<P extends PropertyInterface> extends SystemActionProperty {

    private final GroupObjectEntity filterGroupObject;
    private final ActionProperty<P> mainProperty;

    private ImSet<P> grouping;

    private ImRevMap<P, ClassPropertyInterface> mapMainToThis;
    private boolean removeGroupingInterfaces;

    public static ValueClass[] getValueClasses(LAP<?> mainLP, int[] mainInts, int[] groupInts, boolean removeGroupingInterfaces) {
        ValueClass[] fullClasses = PropertyUtils.getValueClasses(new LAP<?>[]{mainLP}, new int[][]{mainInts});

        if (!removeGroupingInterfaces) {
            return fullClasses;
        }

        HashSet<Integer> groupIntsSet = new HashSet<Integer>(toListFromArray(groupInts));
        ValueClass[] result = new ValueClass[fullClasses.length - groupIntsSet.size()];

        int ri = 0;
        for (int i = 0; i < fullClasses.length; ++i) {
            if (!groupIntsSet.contains(i)) {
                result[ri++] = fullClasses[i];
            }
        }

        assert ri == result.length;

        return result;
    }

    /**
     * @param mainLP   - свойство, куда будем писать
     */
    public PrevGroupChangeActionProperty(String sID, String caption, GroupObjectEntity filterGroupObject, LAP<P> mainLP, int[] mainInts, int[] groupInts) {
        super(sID, caption, getValueClasses(mainLP, mainInts, groupInts, filterGroupObject == null));

        TreeSet<Integer> groupIntsSet = new TreeSet<Integer>(toListFromArray(groupInts));

        this.removeGroupingInterfaces = filterGroupObject == null;

        this.filterGroupObject = filterGroupObject;
        this.mainProperty = mainLP.property;

        ImOrderSet<ClassPropertyInterface> listInterfaces = getOrderInterfaces();

        this.mapMainToThis = getInterfacesMapping(mainLP, mainInts, groupIntsSet);

        if (!removeGroupingInterfaces) {
            MSet<P> mGrouping = SetFact.mSet();
            for (int gi : groupInts) {
                //Добавляем все интерфейсы главного свойства, которые указывают на группирующие интерфейсы
                mGrouping.addAll(mapMainToThis.filterValues(SetFact.singleton(listInterfaces.get(gi))).keys());
            }
            grouping = mGrouping.immutable();
        }
    }

    /**
     * получает мэппинг интерфейсов property на интерфейсы этого результирующего свойтсва
     */
    private <P extends PropertyInterface> ImRevMap<P, ClassPropertyInterface> getInterfacesMapping(LAP<P> property, int[] ifacesMapping, TreeSet<Integer> groupIntsSet) {
        ImOrderSet<ClassPropertyInterface> listInterfaces = getOrderInterfaces();

        MRevMap<P, ClassPropertyInterface> mResult = MapFact.mRevMap(ifacesMapping.length); // лень разбираться в это бреде, потом все равно уйдет
        for (int i = 0; i < ifacesMapping.length; ++i) {
            int pi = ifacesMapping[i];

            if (removeGroupingInterfaces) {
                if (groupIntsSet.contains(pi)) {
                    //пропускаем группирующие интерфейсы
                    continue;
                }
                // реальный номер интерфейса меньше на количество группирующих интерфейсов меньше его по номеру
                // т.к. они пропускаются при создании
                pi -= groupIntsSet.headSet(pi).size();
            }

            mResult.revAdd(property.listInterfaces.get(i), listInterfaces.get(pi));
        }

        return mResult.immutableRev();
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {
        ImRevMap<P, KeyExpr> mainKeys = mainProperty.getMapKeys();

        ImRevMap<P, PropertyObjectInterfaceInstance> mainMapObjects = getMapObjectsForMainProperty(context.getObjectInstances());

        //включаем в сравнение на конкретные значения те интерфейсы главноего свойства, которые мэпятся на интерфейсы текущего свойства
        Where changeWhere = CompareWhere.compareValues(
                removeGroupingInterfaces ? mainKeys.filter(mapMainToThis.keys()) : mainKeys.removeRev(grouping),
                mapMainToThis.join(context.getKeys())
        );

        if (filterGroupObject != null) {
            context.emitExceptionIfNotInFormSession();

            GroupObjectInstance groupInstance = context.getFormInstance().instanceFactory.getInstance(filterGroupObject);
            changeWhere = changeWhere.and(
                    groupInstance.getWhere(
                            mainMapObjects.crossJoin(mainKeys).filterRev(groupInstance.objects),
                            context.getModifier()));
        }

        executeAction(context, mainProperty, mainKeys, mainMapObjects, changeWhere);
    }

    private static <P extends PropertyInterface> void executeAction(ExecutionContext<ClassPropertyInterface> context, ActionProperty<P> property, ImRevMap<P, KeyExpr> keys, ImMap<P, PropertyObjectInterfaceInstance> objects, Where where) throws SQLException {
        context.getEnv().execute(property, new PropertySet<P>(keys, where, MapFact.<Expr, Boolean>EMPTYORDER(), false),
                                    new FormEnvironment<P>(objects, context.getForm().getDrawInstance()));
    }
    
    private ImRevMap<P, PropertyObjectInterfaceInstance> getMapObjectsForMainProperty(final ImMap<ClassPropertyInterface, PropertyObjectInterfaceInstance> mapObjects) {
        if (mapObjects == null) {
            return null;
        }

        return mainProperty.interfaces.mapRevValues(new GetValue<PropertyObjectInterfaceInstance, P>() {
            public PropertyObjectInterfaceInstance getMapValue(P value) {
                return mapObjects.get(mapMainToThis.get(value));
            }});
//        return join(mapMainToThis, mapObjects);
    }

    @Override
    protected boolean isVolatile() { // все равно потом уйдет это действие
        return true;
    }
}

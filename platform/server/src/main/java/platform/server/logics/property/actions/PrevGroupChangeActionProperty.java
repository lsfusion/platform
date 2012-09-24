package platform.server.logics.property.actions;

import platform.base.BaseUtils;
import platform.base.OrderedMap;
import platform.server.classes.ValueClass;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.where.extra.CompareWhere;
import platform.server.data.where.Where;
import platform.server.form.entity.GroupObjectEntity;
import platform.server.form.instance.GroupObjectInstance;
import platform.server.form.instance.PropertyObjectInterfaceInstance;
import platform.server.logics.DataObject;
import platform.server.logics.PropertyUtils;
import platform.server.logics.linear.LAP;
import platform.server.logics.property.*;
import platform.server.session.PropertySet;

import java.sql.SQLException;
import java.util.*;

import static platform.base.BaseUtils.*;

public class PrevGroupChangeActionProperty<P extends PropertyInterface> extends SystemActionProperty {

    private final GroupObjectEntity filterGroupObject;
    private final ActionProperty<P> mainProperty;

    private HashSet<P> grouping;

    private Map<P, ClassPropertyInterface> mapMainToThis;
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

        List<ClassPropertyInterface> listInterfaces = (List<ClassPropertyInterface>) interfaces;

        this.mapMainToThis = getInterfacesMapping(mainLP, mainInts, groupIntsSet);

        if (!removeGroupingInterfaces) {
            grouping = new HashSet<P>();
            for (int gi : groupInts) {
                //Добавляем все интерфейсы главного свойства, которые указывают на группирующие интерфейсы
                grouping.addAll(filterValues(mapMainToThis, listInterfaces.get(gi)));
            }
        }
    }

    /**
     * получает мэппинг интерфейсов property на интерфейсы этого результирующего свойтсва
     */
    private <P extends PropertyInterface> Map<P, ClassPropertyInterface> getInterfacesMapping(LAP<P> property, int[] ifacesMapping, TreeSet<Integer> groupIntsSet) {
        List<ClassPropertyInterface> listInterfaces = (List<ClassPropertyInterface>) interfaces;

        HashMap<P, ClassPropertyInterface> result = new HashMap<P, ClassPropertyInterface>();
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

            result.put(property.listInterfaces.get(i), listInterfaces.get(pi));
        }

        return result;
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {
        Map<P, KeyExpr> mainKeys = mainProperty.getMapKeys();

        Map<P, PropertyObjectInterfaceInstance> mainMapObjects = getMapObjectsForMainProperty(context.getObjectInstances());

        //включаем в сравнение на конкретные значения те интерфейсы главноего свойства, которые мэпятся на интерфейсы текущего свойства
        Where changeWhere = CompareWhere.compareValues(
                removeGroupingInterfaces ? filterKeys(mainKeys, mapMainToThis.keySet()) : filterNotKeys(mainKeys, grouping),
                join(mapMainToThis, context.getKeys())
        );

        if (filterGroupObject != null) {
            context.emitExceptionIfNotInFormSession();

            GroupObjectInstance groupInstance = context.getFormInstance().instanceFactory.getInstance(filterGroupObject);
            changeWhere = changeWhere.and(
                    groupInstance.getWhere(
                            filterKeys(crossJoin(mainMapObjects, mainKeys), groupInstance.objects),
                            context.getModifier()));
        }

        executeAction(context, mainProperty, mainKeys, mainMapObjects, changeWhere);
    }

    private static <P extends PropertyInterface> void executeAction(ExecutionContext<ClassPropertyInterface> context, ActionProperty<P> property, Map<P, KeyExpr> keys, Map<P, PropertyObjectInterfaceInstance> objects, Where where) throws SQLException {
        context.getEnv().execute(property, new PropertySet<P>(keys, where, new OrderedMap<Expr, Boolean>(), false),
                                    new FormEnvironment<P>(objects, context.getForm().getDrawInstance()));
    }
    
    private Map<P, PropertyObjectInterfaceInstance> getMapObjectsForMainProperty(Map<ClassPropertyInterface, PropertyObjectInterfaceInstance> mapObjects) {
        if (mapObjects == null) {
            return null;
        }

        Map<P, PropertyObjectInterfaceInstance> execMapObjects = new HashMap<P, PropertyObjectInterfaceInstance>();
        for (P mainIFace : (List<P>) mainProperty.interfaces) {
            execMapObjects.put(mainIFace, mapMainToThis.containsKey(mainIFace) ? mapObjects.get(mapMainToThis.get(mainIFace)) : null);
        }

        return execMapObjects;
//        return join(mapMainToThis, mapObjects);
    }

    @Override
    protected boolean isVolatile() { // все равно потом уйдет это действие
        return true;
    }
}

package platform.server.logics.property.actions;

import platform.base.BaseUtils;
import platform.server.classes.ActionClass;
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
import platform.server.logics.linear.LP;
import platform.server.logics.property.*;
import platform.server.session.PropertyChange;
import platform.server.session.PropertySet;

import java.sql.SQLException;
import java.util.*;

import static platform.base.BaseUtils.*;

public class PrevGroupChangeActionProperty extends CustomActionProperty {

    private final GroupObjectEntity filterGroupObject;
    private final ActionProperty mainProperty;

    private HashSet<ClassPropertyInterface> grouping;

    private Map<ClassPropertyInterface, ClassPropertyInterface> mapMainToThis;
    private boolean removeGroupingInterfaces;

    public static ValueClass[] getValueClasses(LP mainLP, int[] mainInts, int[] groupInts, boolean removeGroupingInterfaces) {
        ValueClass[] fullClasses = PropertyUtils.getValueClasses(false, new LP[]{mainLP}, new int[][]{mainInts});

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
    public PrevGroupChangeActionProperty(String sID, String caption, GroupObjectEntity filterGroupObject, LAP mainLP, int[] mainInts, int[] groupInts) {
        super(sID, caption, getValueClasses(mainLP, mainInts, groupInts, filterGroupObject == null));

        TreeSet<Integer> groupIntsSet = new TreeSet<Integer>(toListFromArray(groupInts));

        this.removeGroupingInterfaces = filterGroupObject == null;

        this.filterGroupObject = filterGroupObject;
        this.mainProperty = mainLP.property;

        List<ClassPropertyInterface> listInterfaces = (List<ClassPropertyInterface>) interfaces;

        this.mapMainToThis = getInterfacesMapping(mainLP, mainInts, groupIntsSet);

        if (!removeGroupingInterfaces) {
            grouping = new HashSet<ClassPropertyInterface>();
            for (int gi : groupInts) {
                //Добавляем все интерфейсы главного свойства, которые указывают на группирующие интерфейсы
                grouping.addAll(filterValues(mapMainToThis, listInterfaces.get(gi)));
            }
        }
    }

    /**
     * получает мэппинг интерфейсов property на интерфейсы этого результирующего свойтсва
     */
    private Map<ClassPropertyInterface, ClassPropertyInterface> getInterfacesMapping(LAP property, int[] ifacesMapping, TreeSet<Integer> groupIntsSet) {
        List<ClassPropertyInterface> listInterfaces = (List<ClassPropertyInterface>) interfaces;

        HashMap<ClassPropertyInterface, ClassPropertyInterface> result = new HashMap<ClassPropertyInterface, ClassPropertyInterface>();
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

    public void execute(ExecutionContext context) throws SQLException {
        Map<ClassPropertyInterface, KeyExpr> mainKeys = mainProperty.getMapKeys();

        Map<ClassPropertyInterface, PropertyObjectInterfaceInstance> mainMapObjects = getMapObjectsForMainProperty(context.getObjectInstances());

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

        executeAction(context, mainProperty, BaseUtils.<Map<ClassPropertyInterface, KeyExpr>>immutableCast(mainKeys),
                BaseUtils.<Map<ClassPropertyInterface, PropertyObjectInterfaceInstance>>immutableCast(mainMapObjects), changeWhere);
    }

    private static void executeAction(ExecutionContext context, ActionProperty property, Map<ClassPropertyInterface, KeyExpr> keys, Map<ClassPropertyInterface, PropertyObjectInterfaceInstance> objects, Where where) throws SQLException {
        context.getEnv().execute(property, new PropertySet<ClassPropertyInterface>(new HashMap<ClassPropertyInterface, DataObject>(), keys, where),
                                    new FormEnvironment<ClassPropertyInterface>(objects, context.getForm().getDrawInstance()));
    }
    
    private Map<ClassPropertyInterface, PropertyObjectInterfaceInstance> getMapObjectsForMainProperty(Map<ClassPropertyInterface, PropertyObjectInterfaceInstance> mapObjects) {
        if (mapObjects == null) {
            return null;
        }

        Map<ClassPropertyInterface, PropertyObjectInterfaceInstance> execMapObjects = new HashMap<ClassPropertyInterface, PropertyObjectInterfaceInstance>();
        for (ClassPropertyInterface mainIFace : (List<ClassPropertyInterface>) mainProperty.interfaces) {
            execMapObjects.put(mainIFace, mapMainToThis.containsKey(mainIFace) ? mapObjects.get(mapMainToThis.get(mainIFace)) : null);
        }

        return execMapObjects;
//        return join(mapMainToThis, mapObjects);
    }
}

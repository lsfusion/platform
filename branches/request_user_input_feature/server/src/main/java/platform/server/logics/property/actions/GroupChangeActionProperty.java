package platform.server.logics.property.actions;

import platform.server.classes.ValueClass;
import platform.server.data.expr.Expr;
import platform.server.data.expr.where.extra.CompareWhere;
import platform.server.data.where.Where;
import platform.server.form.entity.GroupObjectEntity;
import platform.server.form.instance.GroupObjectInstance;
import platform.server.form.instance.PropertyObjectInterfaceInstance;
import platform.server.logics.PropertyUtils;
import platform.server.logics.linear.LP;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;
import platform.server.logics.property.Property;
import platform.server.logics.property.PropertyInterface;
import platform.server.session.PropertyChange;

import java.sql.SQLException;
import java.util.*;

import static platform.base.BaseUtils.*;

public class GroupChangeActionProperty extends CustomActionProperty {

    private final GroupObjectEntity filterGroupObject;
    private final Property getterProperty;
    private final Property mainProperty;

    private HashSet<PropertyInterface> grouping;

    private Map<PropertyInterface, PropertyInterface> mapGetToMain;
    private Map<PropertyInterface, ClassPropertyInterface> mapGetToThis;

    private Map<PropertyInterface, ClassPropertyInterface> mapMainToThis;
    private boolean removeGroupingInterfaces;

    public static ValueClass[] getValueClasses(LP mainLP, int[] mainInts, LP getterLP, int[] getterInts, int[] groupInts, boolean removeGroupingInterfaces) {
        ValueClass[] fullClasses = PropertyUtils.getValueClasses(false, new LP[]{mainLP, getterLP}, new int[][]{mainInts, getterInts});

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
     * @param getterLP - свойство, из которого будем читать значение
     */
    public GroupChangeActionProperty(String sID, String caption, GroupObjectEntity filterGroupObject, LP<?> mainLP, int[] mainInts, LP<?> getterLP, int[] getterInts, int[] groupInts) {
        super(sID, caption, getValueClasses(mainLP, mainInts, getterLP, getterInts, groupInts, filterGroupObject == null));

        TreeSet<Integer> groupIntsSet = new TreeSet<Integer>(toListFromArray(groupInts));
        Map<PropertyInterface, Integer> mapMainToInd = getNumMapping(mainLP, mainInts);
        Map<PropertyInterface, Integer> mapGetToInd = getNumMapping(getterLP, getterInts);

        this.removeGroupingInterfaces = filterGroupObject == null;

        this.filterGroupObject = filterGroupObject;
        this.mainProperty = mainLP.property;
        this.getterProperty = getterLP.property;

        this.mapGetToMain = new HashMap<PropertyInterface, PropertyInterface>();

        List<ClassPropertyInterface> listInterfaces = (List<ClassPropertyInterface>) interfaces;

        this.mapGetToThis = getInterfacesMapping(getterLP, getterInts, groupIntsSet);
        this.mapMainToThis = getInterfacesMapping(mainLP, mainInts, groupIntsSet);
        this.mapGetToMain = crossValues(mapGetToInd, mapMainToInd, true);

        if (!removeGroupingInterfaces) {
            grouping = new HashSet<PropertyInterface>();
            for (int gi : groupInts) {
                //Добавляем все интерфейсы главного свойства, которые указывают на группирующие интерфейсы
                grouping.addAll(filterValues(mapMainToThis, listInterfaces.get(gi)));
            }
        }
    }

    private Map<PropertyInterface, Integer> getNumMapping(LP<?> property, int[] intNums) {
        assert property.listInterfaces.size() == intNums.length;

        HashMap<PropertyInterface, Integer> result = new HashMap<PropertyInterface, Integer>();
        for (int i = 0; i < intNums.length; ++i) {
            result.put(property.listInterfaces.get(i), intNums[i]);
        }

        return result;
    }

    /**
     * получает мэппинг интерфейсов property на интерфейсы этого результирующего свойтсва
     */
    private Map<PropertyInterface, ClassPropertyInterface> getInterfacesMapping(LP<?> property, int[] ifacesMapping, TreeSet<Integer> groupIntsSet) {
        List<ClassPropertyInterface> listInterfaces = (List<ClassPropertyInterface>) interfaces;

        HashMap<PropertyInterface, ClassPropertyInterface> result = new HashMap<PropertyInterface, ClassPropertyInterface>();
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
        Map<PropertyInterface, Expr> mainKeys = mainProperty.getMapKeys();

        Map<PropertyInterface, PropertyObjectInterfaceInstance> mainMapObjects = getMapObjectsForMainProperty(context.getObjectInstances());

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

        Map<PropertyInterface, Expr> getterKeys = new HashMap<PropertyInterface, Expr>();
        for (PropertyInterface getIFace : (List<PropertyInterface>) getterProperty.interfaces) {
            if (mapGetToMain.containsKey(getIFace)) {
                getterKeys.put(getIFace, mainKeys.get(mapGetToMain.get(getIFace)));
            } else {
                getterKeys.put(getIFace, context.getKeyValue(mapGetToThis.get(getIFace)).getExpr());
            }
        }

        Expr setExpr = getterProperty.getExpr(getterKeys, context.getModifier());

        PropertyChange mainPropertyChange = new PropertyChange(mainKeys, setExpr, changeWhere);

        context.addActions(context.getEnv().execute(mainProperty, mainPropertyChange, mainMapObjects));
    }

    private Map<PropertyInterface, PropertyObjectInterfaceInstance> getMapObjectsForMainProperty(Map<ClassPropertyInterface, PropertyObjectInterfaceInstance> mapObjects) {
        if (mapObjects == null) {
            return null;
        }

        Map<PropertyInterface, PropertyObjectInterfaceInstance> execMapObjects = new HashMap<PropertyInterface, PropertyObjectInterfaceInstance>();
        for (PropertyInterface mainIFace : (List<PropertyInterface>) mainProperty.interfaces) {
            execMapObjects.put(mainIFace, mapMainToThis.containsKey(mainIFace) ? mapObjects.get(mapMainToThis.get(mainIFace)) : null);
        }

        return execMapObjects;
//        return join(mapMainToThis, mapObjects);
    }
}

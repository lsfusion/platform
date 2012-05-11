package platform.server.logics.property.actions.edit;

import platform.base.BaseUtils;
import platform.interop.Compare;
import platform.interop.form.UserInputResult;
import platform.server.classes.ValueClass;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.where.extra.CompareWhere;
import platform.server.data.query.Query;
import platform.server.data.where.Where;
import platform.server.form.instance.FormInstance;
import platform.server.form.instance.GroupObjectInstance;
import platform.server.form.instance.ObjectInstance;
import platform.server.form.instance.PropertyObjectInterfaceInstance;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;
import platform.server.logics.property.PropertyMapImplement;
import platform.server.logics.property.actions.CustomActionProperty;
import platform.server.logics.property.actions.flow.FlowActionProperty;
import platform.server.logics.property.actions.flow.FlowResult;
import platform.server.session.Modifier;
import platform.server.session.PropertyChange;

import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static platform.base.BaseUtils.filterValues;

// групповые изменения (групповая корректировка, paste таблицы, multi cell paste)
public class DefaultGroupChangeActionProperty extends CustomActionProperty {

    private final PropertyMapImplement<ClassPropertyInterface, ClassPropertyInterface> changeAction;

    public DefaultGroupChangeActionProperty(String sID, String caption, ValueClass[] classes, PropertyMapImplement<ClassPropertyInterface, ClassPropertyInterface> changeAction) {
        super(sID, caption, classes);
        this.changeAction = changeAction;
    }

    private Collection<Map<ClassPropertyInterface, DataObject>> getGroupKeys(ExecutionContext context) throws SQLException {
        Map<ClassPropertyInterface,DataObject> keys = context.getKeys();
        Modifier modifier = context.getModifier();
        GroupObjectInstance groupObject = context.getGroupObjectInstance();
        Map<ClassPropertyInterface,PropertyObjectInterfaceInstance> objectInstances = context.getObjectInstances();

        Map<ClassPropertyInterface, KeyExpr> mapKeys = getMapKeys();
        Map<ClassPropertyInterface, ObjectInstance> usedGroupObjects = filterValues(objectInstances, groupObject.objects);

        Where changeWhere = Where.TRUE;
        // закидываем ключи из группы
        Map<ObjectInstance, KeyExpr> groupKeys = new HashMap<ObjectInstance, KeyExpr>();
        for(Map.Entry<ClassPropertyInterface, KeyExpr> mapKey : BaseUtils.filterKeys(mapKeys, usedGroupObjects.keySet()).entrySet()) {
            ObjectInstance usedGroupObject = usedGroupObjects.get(mapKey.getKey());
            KeyExpr groupKey = groupKeys.get(usedGroupObject);
            if(groupKey==null)
                groupKeys.put(usedGroupObject, mapKey.getValue());
            else
                changeWhere = changeWhere.and(mapKey.getValue().compare(groupKey, Compare.EQUALS));
        }
        return new Query<ClassPropertyInterface, Object>(mapKeys, changeWhere.and(groupObject.getWhere(groupKeys, modifier)).and( // закидываем фильтр группы
                CompareWhere.compareValues(BaseUtils.filterNotKeys(mapKeys, usedGroupObjects.keySet()), keys))). // закидываем оставшиеся ключи
                    executeClasses(context.getSession()).keySet();
    }

    @Override
    public void execute(ExecutionContext context) throws SQLException {
        
        FlowActionProperty.execute(context, changeAction); // вызываем CHANGE (для текущего)

        ObjectValue lastObject = context.getLastUserInput(); // запоминаем его значение, если не cancel
        if(lastObject==null)
            return;

        for(Map<ClassPropertyInterface, DataObject> row : getGroupKeys(context)) // бежим по всем
            if(!BaseUtils.hashEquals(row, context.getKeys())) { // кроме текущего
                context.pushUserInput(lastObject);
                FlowActionProperty.execute(context, changeAction, row, BaseUtils.toMap(interfaces));
                context.popUserInput(lastObject);
            }
    }
}

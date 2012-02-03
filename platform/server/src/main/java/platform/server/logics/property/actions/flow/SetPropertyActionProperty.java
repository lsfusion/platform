package platform.server.logics.property.actions.flow;

import platform.server.classes.ValueClass;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.where.extra.CompareWhere;
import platform.server.data.where.Where;
import platform.server.form.instance.PropertyObjectInterfaceInstance;
import platform.server.logics.DataObject;
import platform.server.logics.linear.LP;
import platform.server.logics.property.*;
import platform.server.session.PropertyChange;

import java.sql.SQLException;
import java.util.*;

import static platform.base.BaseUtils.*;
import static platform.server.logics.PropertyUtils.getValueClasses;

public class SetPropertyActionProperty<P extends PropertyInterface, T extends PropertyInterface> extends ActionProperty {
    public static class Interface extends PropertyInterface<Interface> {
        public Interface(int ID) {
            super(ID);
        }
    }

    private Collection<Interface> allInterfaces;

    private Map<ClassPropertyInterface, Interface> mapThisToAll;
    private PropertyMapImplement<P, Interface> mapWriteToAll;
    private PropertyInterfaceImplement<Interface> writeFrom;

    private SetPropertyActionProperty(String sID,
                                      String caption,
                                      ValueClass[] classes,
                                      Collection<Interface> allInterfaces,
                                      PropertyMapImplement<P, Interface> mapWriteToAll,
                                      Map<Integer, Interface> mapThisToAll,
                                      PropertyInterfaceImplement<Interface> writeFrom) {
        super(sID, caption, classes);

        this.mapThisToAll = new HashMap<ClassPropertyInterface, Interface>();
        List<ClassPropertyInterface> listInterfaces = (List<ClassPropertyInterface>) interfaces;
        for (Map.Entry<Integer, Interface> entry : mapThisToAll.entrySet()) {
            this.mapThisToAll.put(listInterfaces.get(entry.getKey()), entry.getValue());
        }

        this.allInterfaces = allInterfaces;
        this.mapWriteToAll = mapWriteToAll;
        this.writeFrom = writeFrom;
    }

    @Override
    public void execute(ExecutionContext context) throws SQLException {
        Map<Interface, KeyExpr> allKeys = KeyExpr.getMapKeys(allInterfaces);

        Map<P, KeyExpr> toKeys = join(mapWriteToAll.mapping, allKeys);
        Map<P, DataObject> toValues = innerJoin(mapWriteToAll.mapping, crossJoin(mapThisToAll, context.getKeys()));
        Map<P, PropertyObjectInterfaceInstance> toObjects = innerJoin(mapWriteToAll.mapping, crossJoin(mapThisToAll, context.getObjectInstances()));

        Map<Interface, Expr> fromKeys = new HashMap<Interface, Expr>(allKeys);
        for (Map.Entry<ClassPropertyInterface, Interface> entry : mapThisToAll.entrySet()) {
            fromKeys.put(entry.getValue(), context.getKeyValue(entry.getKey()).getExpr());
        }

        Where changeWhere = CompareWhere.compareValues(filterKeys(toKeys, toValues.keySet()), toValues);

        PropertyChange<P> change = new PropertyChange<P>(toKeys, writeFrom.mapExpr(fromKeys, context.getModifier()), changeWhere);
        context.addActions(
                context.getSession().execute(mapWriteToAll.property, change, context.getModifier(), context.getRemoteForm(), toObjects)
        );
    }

    private static List<Interface> getInterfaces(int count) {
        List<Interface> interfaces = new ArrayList<Interface>();
        for (int i = 0; i < count; ++i) {
            interfaces.add(new Interface(i));
        }
        return interfaces;
    }

    private static Map<PropertyInterface, Interface> mapPropertyInterfaces(List<Interface> allInterfaces, LP<?> property, int[] mapInterfaces) {
        Map<PropertyInterface, Interface> mapWriteToAll = new HashMap<PropertyInterface, Interface>();
        for (int i = 0; i < mapInterfaces.length; i++) {
            mapWriteToAll.put(property.listInterfaces.get(i), allInterfaces.get(mapInterfaces[i]));
        }
        return mapWriteToAll;
    }

    public static <P extends PropertyInterface, T extends PropertyInterface> SetPropertyActionProperty<P, T> create(String sID,
                                                                                                                    String caption,
                                                                                                                    LP toProperty,
                                                                                                                    int[] mapToInterfaces,
                                                                                                                    int mapResultInterface) {
        return create(sID, caption, toProperty, mapToInterfaces, mapResultInterface, null);
    }

    public static <P extends PropertyInterface, T extends PropertyInterface> SetPropertyActionProperty<P, T> create(String sID,
                                                                                                                    String caption,
                                                                                                                    LP toProperty,
                                                                                                                    int[] mapToInterfaces,
                                                                                                                    LP fromProperty,
                                                                                                                    int[] mapFromInterfaces) {
        return create(sID, caption, toProperty, mapToInterfaces, fromProperty, mapFromInterfaces, null);
    }

    public static <P extends PropertyInterface, T extends PropertyInterface> SetPropertyActionProperty<P, T> create(String sID,
                                                                                                                    String caption,
                                                                                                                    LP toProperty,
                                                                                                                    int[] mapToInterfaces,
                                                                                                                    int mapResultInterface,
                                                                                                                    int[] mapThisInterfaces) {
        //создаём списки всех классов, участвующих в записи...
        ValueClass[] allClasses = getValueClasses(true, new LP[]{toProperty}, new int[][]{addInt(mapToInterfaces, mapResultInterface)});
        //.. и всех интерфейсов (контекст)
        List<Interface> allInterfaces = getInterfaces(allClasses.length);

        //мэппинг входов изменяемого свойства на интерфейсы контекста
        Map<PropertyInterface, Interface> mapWriteToAll = mapPropertyInterfaces(allInterfaces, toProperty, mapToInterfaces);

        return create(sID, caption, allClasses, allInterfaces, toProperty,
                      allInterfaces.get(mapResultInterface), mapWriteToAll, mapThisInterfaces, new int[]{mapResultInterface});
    }

    public static <P extends PropertyInterface, T extends PropertyInterface> SetPropertyActionProperty<P, T> create(String sID,
                                                                                                                    String caption,
                                                                                                                    LP toProperty,
                                                                                                                    int[] mapToInterfaces,
                                                                                                                    LP fromProperty,
                                                                                                                    int[] mapFromInterfaces,
                                                                                                                    int[] mapThisInterfaces) {
        //создаём списки всех классов, участвующих в записи...
        ValueClass[] allClasses = getValueClasses(false, new LP[]{toProperty, fromProperty}, new int[][]{mapToInterfaces, mapFromInterfaces});
        //.. и всех интерфейсов (контекст)
        List<Interface> allInterfaces = getInterfaces(allClasses.length);

        //мэппинг входов изменяемого свойства на интерфейсы контекста
        Map<PropertyInterface, Interface> mapWriteToAll = mapPropertyInterfaces(allInterfaces, toProperty, mapToInterfaces);
        //мэппинг входов изменяющего свойства на интерфейсы контекста
        Map<PropertyInterface, Interface> mapFromToAll = mapPropertyInterfaces(allInterfaces, fromProperty, mapFromInterfaces);

        return create(sID, caption, allClasses, allInterfaces, toProperty,
                      new PropertyMapImplement(fromProperty.property, mapFromToAll), mapWriteToAll, mapThisInterfaces, mapFromInterfaces);
    }

    private static <P extends PropertyInterface, T extends PropertyInterface> SetPropertyActionProperty<P, T> create(String sID,
                                                                                                                     String caption,
                                                                                                                     ValueClass[] allClasses,
                                                                                                                     List<Interface> allInterfaces,
                                                                                                                     LP toProperty,
                                                                                                                     PropertyInterfaceImplement<Interface> writeFrom,
                                                                                                                     Map<PropertyInterface, Interface> mapWriteToAll,
                                                                                                                     int[] mapThisInterfaces,
                                                                                                                     int[] rightUsedInterfaces) {
        if (mapThisInterfaces == null) {
            // если не задан мэппинг для интерфейсов результируюего свойства, то они мэпятся на все интерфейсы котекста по пордяку
            mapThisInterfaces = consecutiveInts(allClasses.length);
        }

        //мэппинг входов результирующего свойства на интерфейсы контекста
        Map<Integer, Interface> mapThisToAll = new HashMap<Integer, Interface>();
        List<ValueClass> interfaceClasses = new ArrayList<ValueClass>();
        for (int i = 0; i < mapThisInterfaces.length; i++) {
            int mapThis = mapThisInterfaces[i];

            interfaceClasses.add(allClasses[mapThis]);
            mapThisToAll.put(i, allInterfaces.get(mapThis));
        }

        //все интерфейсы справа должны быть либо в списке использованных слева интерфейсов, либо в списке интерфейсов результирующего свойства
        for (int usedFromInterface : rightUsedInterfaces) {
            Interface fromInterface = allInterfaces.get(usedFromInterface);
            if (!mapThisToAll.containsValue(fromInterface) && !mapWriteToAll.containsValue(fromInterface)) {
                throw new IllegalArgumentException("right side of set property action ( X <- Y ) " +
                                                   "should map all interfaces to either left side, or result action property");
            }
        }

        return new SetPropertyActionProperty<P, T>(sID,
                                                   caption,
                                                   interfaceClasses.toArray(new ValueClass[0]),
                                                   allInterfaces,
                                                   new PropertyMapImplement(toProperty.property, mapWriteToAll),
                                                   mapThisToAll,
                                                   writeFrom);
    }
}

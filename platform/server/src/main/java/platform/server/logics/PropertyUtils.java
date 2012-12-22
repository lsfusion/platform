package platform.server.logics;

import platform.base.BaseUtils;
import platform.base.col.ListFact;
import platform.base.col.interfaces.immutable.ImList;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.immutable.ImOrderSet;
import platform.base.col.interfaces.immutable.ImRevMap;
import platform.base.col.interfaces.mutable.MList;
import platform.base.col.interfaces.mutable.mapvalue.GetIndex;
import platform.base.col.interfaces.mutable.mapvalue.GetValue;
import platform.server.classes.ValueClass;
import platform.server.logics.linear.LAP;
import platform.server.logics.linear.LCP;
import platform.server.logics.linear.LP;
import platform.server.logics.property.*;

import java.util.HashMap;
import java.util.Map;

public class PropertyUtils {
    public static ValueClass[] getValueClasses(LAP<?>[] dataProperties, int[][] mapInterfaces) {
        return getValueClasses(dataProperties, mapInterfaces, true);
    }

    public static ValueClass[] getValueClasses(LAP<?>[] dataProperties, int[][] mapInterfaces, boolean allowMissingInterfaces) {
        Map<Integer, ValueClass> mapClasses = new HashMap<Integer, ValueClass>(); // deprecated этот метод скоро уйдет
        for (int i = 0; i < dataProperties.length; ++i) {
            LAP<?> dataProperty = dataProperties[i];

            if (dataProperty.listInterfaces.size() == 0) // специально для vnull сделано
                continue;

            int[] mapPropInterfaces = mapInterfaces[i];
            if (mapPropInterfaces == null) {
                mapPropInterfaces = BaseUtils.consecutiveInts(dataProperty.listInterfaces.size());
            }

            ValueClass[] propClasses = dataProperty.getInterfaceClasses();

            assert propClasses.length == mapPropInterfaces.length;

            for (int j = 0; j < mapPropInterfaces.length; ++j) {
                ValueClass valueClass = propClasses[j];

                int thisIndex = mapPropInterfaces[j];

                ValueClass definedValueClass = mapClasses.get(thisIndex);
                if (definedValueClass != null) {
                    if (valueClass.isCompatibleParent(definedValueClass)) {
                        valueClass = definedValueClass;
                    } else {
                        assert definedValueClass.isCompatibleParent(valueClass);
                    }
                }

                mapClasses.put(thisIndex, valueClass);
            }
        }

        ValueClass classes[] = new ValueClass[mapClasses.size()];
        for (int i = 0; i < mapClasses.size(); ++i) {
            classes[i] = mapClasses.get(i);
            assert allowMissingInterfaces || classes[i] != null;
        }

        return classes;
    }

    public static Object[] getParams(LP prop) {
        Object[] params  = new Object[prop.listInterfaces.size()];
        for(int i=0;i<prop.listInterfaces.size();i++)
            params[i] = (i+1);
        return params;
    }

    public static Integer[] getIntParams(LP prop) {
        Integer[] params  = new Integer[prop.listInterfaces.size()];
        for(int i=0;i<prop.listInterfaces.size();i++)
            params[i] = (i+1);
        return params;
    }

    public static Object[] getUParams(LP[] props) {
        Object[] result = new Object[0];
        for (LP prop : props)
            result = BaseUtils.add(result, directLI(prop));
        return result;
    }

    public static Object[] getUParams(int intNum) {
        Object[] result = new Object[intNum];
        for (int i = 1; i <= intNum; i++)
            result[i-1] = i;
        return result;
    }

    public static Object[] directLI(LP prop) {
        return BaseUtils.add(prop, getParams(prop));
    }

    // считывает "линейные" имплементации
    private static ImList<LI> readLI(Object[] params) {
        MList<LI> mResult = ListFact.mList();
        for (int i = 0; i < params.length; i++)
            if (params[i] instanceof Integer)
                mResult.add(new LII((Integer) params[i]));
            else {
                LMI impl = new LMI((LP) params[i]);
                for (int j = 0; j < impl.mapInt.length; j++)
                    impl.mapInt[j] = (Integer) params[i + j + 1];
                i += impl.mapInt.length;
                mResult.add(impl);
            }
        return mResult.immutableList();
    }

    private static <T extends PropertyInterface> ImList<PropertyInterfaceImplement<T>> mapLI(ImList<LI> linearImpl, final ImOrderSet<T> interfaces) {
        return linearImpl.mapListValues(new GetValue<PropertyInterfaceImplement<T>, LI>() {
            public PropertyInterfaceImplement<T> getMapValue(LI value) {
                return value.map(interfaces);
            }});
    }

    public static <T extends PropertyInterface> ImList<PropertyInterfaceImplement<T>> readImplements(ImOrderSet<T> listInterfaces, Object... params) {
        return mapLI(readLI(params), listInterfaces);
    }

    public static <T extends PropertyInterface> ImList<CalcPropertyInterfaceImplement<T>> readCalcImplements(ImOrderSet<T> listInterfaces, Object... params) {
        return BaseUtils.immutableCast(readImplements(listInterfaces, params));
    }

    public static <T extends PropertyInterface> ImList<ActionPropertyMapImplement<?, T>> readActionImplements(ImOrderSet<T> listInterfaces, Object... params) {
        return BaseUtils.immutableCast(readImplements(listInterfaces, params));
    }

    public static int getIntNum(Object[] params) {
        int intNum = 0;
        for (Object param : params)
            if (param instanceof Integer)
                intNum = Math.max(intNum, (Integer) param);
        return intNum;
    }

    public static <P extends PropertyInterface> ActionPropertyImplement<P, CalcPropertyInterfaceImplement<P>> mapActionImplement(LAP<P> property, ImList<CalcPropertyInterfaceImplement<P>> propImpl) {
        return new ActionPropertyImplement<P, CalcPropertyInterfaceImplement<P>>(property.property, getMapping(property, propImpl));
    }

    public static <T extends PropertyInterface, P extends PropertyInterface> CalcPropertyImplement<T, CalcPropertyInterfaceImplement<P>> mapCalcImplement(LCP<T> property, ImList<CalcPropertyInterfaceImplement<P>> propImpl) {
        return new CalcPropertyImplement<T, CalcPropertyInterfaceImplement<P>>(property.property, getMapping(property, propImpl));
    }

    private static <T extends PropertyInterface, P extends PropertyInterface> ImMap<T, CalcPropertyInterfaceImplement<P>> getMapping(LP<T, ?> property, ImList<CalcPropertyInterfaceImplement<P>> propImpl) {
        return property.listInterfaces.mapList(propImpl);
    }

    static ValueClass[] overrideClasses(ValueClass[] commonClasses, ValueClass[] overrideClasses) {
        ValueClass[] classes = new ValueClass[commonClasses.length];
        int ic = 0;
        for (ValueClass common : commonClasses) {
            ValueClass overrideClass;
            if (ic < overrideClasses.length && ((overrideClass = overrideClasses[ic]) != null)) {
                classes[ic++] = overrideClass;
                assert !overrideClass.isCompatibleParent(common);
            } else
                classes[ic++] = common;
        }
        return classes;
    }

    // Linear Implement
    static abstract class LI {
        abstract <T extends PropertyInterface<T>> PropertyInterfaceImplement<T> map(ImOrderSet<T> interfaces);

        abstract Object[] write();

    }

    static class LII extends LI {
        int intNum;

        LII(int intNum) {
            this.intNum = intNum;
        }

        <T extends PropertyInterface<T>> CalcPropertyInterfaceImplement<T> map(ImOrderSet<T> interfaces) {
            return interfaces.get(intNum - 1);
        }

        Object[] write() {
            return new Object[]{intNum};
        }

    }

    static class LMI<P extends PropertyInterface> extends LI {
        LP<P, ?> lp;
        int[] mapInt;

        LMI(LP<P, ?> lp) {
            this.lp = lp;
            this.mapInt = new int[lp.listInterfaces.size()];
        }

        <T extends PropertyInterface<T>> PropertyInterfaceImplement<T> map(final ImOrderSet<T> interfaces) {
            ImRevMap<P, T> mapping = lp.listInterfaces.mapOrderRevValues(new GetIndex<T>() {
                public T getMapValue(int i) {
                    return interfaces.get(mapInt[i] - 1);
                }});

            if(lp.property instanceof ActionProperty)
                return new ActionPropertyMapImplement<P, T>((ActionProperty<P>) lp.property, mapping);
            else
                return new CalcPropertyMapImplement<P, T>((CalcProperty<P>) lp.property, mapping);
        }

        Object[] write() {
            Object[] result = new Object[mapInt.length + 1];
            result[0] = lp;
            for (int i = 0; i < mapInt.length; i++)
                result[i + 1] = mapInt[i];
            return result;
        }

    }
}

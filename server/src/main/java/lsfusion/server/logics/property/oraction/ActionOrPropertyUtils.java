package lsfusion.server.logics.property.oraction;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.mutable.MList;
import lsfusion.interop.form.property.Compare;
import lsfusion.server.language.action.LA;
import lsfusion.server.language.property.LP;
import lsfusion.server.language.property.oraction.LAP;
import lsfusion.server.logics.action.Action;
import lsfusion.server.logics.action.implement.ActionImplement;
import lsfusion.server.logics.action.implement.ActionMapImplement;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.implement.*;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class ActionOrPropertyUtils {
    public static ValueClass[] getValueClasses(LA<?>[] dataProperties, int[][] mapInterfaces) {
        return getValueClasses(dataProperties, mapInterfaces, true);
    }

    public static ValueClass[] getValueClasses(LA<?>[] dataProperties, int[][] mapInterfaces, boolean allowMissingInterfaces) {
        Map<Integer, ValueClass> mapClasses = new HashMap<>(); // deprecated этот метод скоро уйдет
        for (int i = 0; i < dataProperties.length; ++i) {
            LA<?> dataProperty = dataProperties[i];

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

    public static Object[] getParams(LAP prop) {
        Object[] params  = new Object[prop.listInterfaces.size()];
        for(int i=0;i<prop.listInterfaces.size();i++)
            params[i] = (i+1);
        return params;
    }

    public static Integer[] getIntParams(LAP prop, int from, int to) {
        Integer[] params  = new Integer[prop.listInterfaces.size()];
        for(int i=0;i<prop.listInterfaces.size();i++) {
            params[i] = (from == i + 1 ? to : i + 1);
        }
        return params;
    }

    public static Object[] getUParams(LAP[] props) {
        Object[] result = new Object[0];
        for (LAP prop : props)
            result = BaseUtils.add(result, directLI(prop));
        return result;
    }

    public static Object[] getUParams(int intNum) {
        Object[] result = new Object[intNum];
        for (int i = 1; i <= intNum; i++)
            result[i-1] = i;
        return result;
    }

    public static Object[] directLI(LAP prop) {
        return BaseUtils.add(prop, getParams(prop));
    }

    // считывает "линейные" имплементации
    private static ImList<LI> readLI(Object[] params) {
        MList<LI> mResult = ListFact.mList();
        for (int i = 0; i < params.length; i++)
            if (params[i] instanceof Integer)
                mResult.add(new LII((Integer) params[i]));
            else {
                LMI impl = new LMI((LAP) params[i]);
                for (int j = 0; j < impl.mapInt.length; j++)
                    impl.mapInt[j] = (Integer) params[i + j + 1];
                i += impl.mapInt.length;
                mResult.add(impl);
            }
        return mResult.immutableList();
    }

    private static <T extends PropertyInterface> ImList<ActionOrPropertyInterfaceImplement> mapLI(ImList<LI> linearImpl, final ImOrderSet<T> interfaces) {
        return linearImpl.mapListValues((LI value) -> value.map(interfaces));
    }

    private static <T> ImList<PropertyObjectInterfaceImplement<T>> mapObjectLI(ImList<LI> linearImpl, final ImOrderSet<T> interfaces) {
        return linearImpl.mapListValues((Function<LI, PropertyObjectInterfaceImplement<T>>) value -> value.mapObject(interfaces));
    }

    public static <T extends PropertyInterface> ImList<ActionOrPropertyInterfaceImplement> readImplements(ImOrderSet<T> listInterfaces, Object... params) {
        return mapLI(readLI(params), listInterfaces);
    }

    public static <T> ImList<PropertyObjectInterfaceImplement<T>> readObjectImplements(ImOrderSet<T> listInterfaces, Object... params) {
        return mapObjectLI(readLI(params), listInterfaces);
    }

    public static <T extends PropertyInterface> ImList<lsfusion.server.logics.property.implement.PropertyInterfaceImplement<T>> readCalcImplements(ImOrderSet<T> listInterfaces, Object... params) {
        return BaseUtils.immutableCast(readImplements(listInterfaces, params));
    }

    public static <T extends PropertyInterface> ImList<ActionMapImplement<?, T>> readActionImplements(ImOrderSet<T> listInterfaces, Object... params) {
        return BaseUtils.immutableCast(readImplements(listInterfaces, params));
    }

    public static int getIntNum(Object[] params) {
        int intNum = 0;
        for (Object param : params)
            if (param instanceof Integer)
                intNum = Math.max(intNum, (Integer) param);
        return intNum;
    }

    public static Compare stringToCompare(String compare) {
        switch (compare) {
            // Left words for backward compatibility. Symbols should be used 
            case "EQUALS":
            case "=":
                return Compare.EQUALS;
            case "GREATER":
            case ">":
                return Compare.GREATER;
            case "LESS":
            case "<":
                return Compare.LESS;
            case "GREATER_EQUALS":
            case ">=":
                return Compare.GREATER_EQUALS;
            case "LESS_EQUALS":
            case "<=":
                return Compare.LESS_EQUALS;
            case "NOT_EQUALS":
            case "!=":
                return Compare.NOT_EQUALS;
            case "LIKE":
            case "=*":
                return Compare.LIKE;
            case "CONTAINS": // CONTAINS is gone. Should be removed soon
            case "=@":
                return Compare.MATCH;
            case "INARRAY":
                return Compare.INARRAY;
            default:
                return null;
        }
    }

    public static <P extends PropertyInterface> ActionImplement<P, lsfusion.server.logics.property.implement.PropertyInterfaceImplement<P>> mapActionImplement(LA<P> property, ImList<lsfusion.server.logics.property.implement.PropertyInterfaceImplement<P>> propImpl) {
        return new ActionImplement<>(property.action, getMapping(property, propImpl));
    }

    public static <T extends PropertyInterface, P extends PropertyInterface> PropertyImplement<T, lsfusion.server.logics.property.implement.PropertyInterfaceImplement<P>> mapCalcImplement(LP<T> property, ImList<lsfusion.server.logics.property.implement.PropertyInterfaceImplement<P>> propImpl) {
        return new PropertyImplement<>(property.property, getMapping(property, propImpl));
    }

    private static <T extends PropertyInterface, P extends PropertyInterface> ImMap<T, lsfusion.server.logics.property.implement.PropertyInterfaceImplement<P>> getMapping(LAP<T, ?> property, ImList<lsfusion.server.logics.property.implement.PropertyInterfaceImplement<P>> propImpl) {
        return property.listInterfaces.mapList(propImpl);
    }

    public static ValueClass[] overrideClasses(ValueClass[] commonClasses, ValueClass[] overrideClasses) {
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
        abstract <T extends PropertyInterface<T>> ActionOrPropertyInterfaceImplement map(ImOrderSet<T> interfaces);

        abstract <T> PropertyObjectInterfaceImplement<T> mapObject(ImOrderSet<T> interfaces);

        abstract Object[] write();

    }

    static class LII extends LI {
        int intNum;

        LII(int intNum) {
            this.intNum = intNum;
        }

        <T extends PropertyInterface<T>> lsfusion.server.logics.property.implement.PropertyInterfaceImplement<T> map(ImOrderSet<T> interfaces) {
            return interfaces.get(intNum - 1);
        }

        <T> PropertyObjectInterfaceImplement<T> mapObject(ImOrderSet<T> interfaces) {
            return new PropertyObjectImplement<>(interfaces.get(intNum - 1));
        }

        Object[] write() {
            return new Object[]{intNum};
        }

    }

    static class LMI<P extends PropertyInterface> extends LI {
        LAP<P, ?> lp;
        int[] mapInt;

        LMI(LAP<P, ?> lp) {
            this.lp = lp;
            this.mapInt = new int[lp.listInterfaces.size()];
        }

        <T extends PropertyInterface<T>> ActionOrPropertyInterfaceImplement map(final ImOrderSet<T> interfaces) {
            ImRevMap<P, T> mapping = lp.listInterfaces.mapOrderRevValues(i -> interfaces.get(mapInt[i] - 1));

            if(lp.getActionOrProperty() instanceof Action)
                return new ActionMapImplement<>((Action<P>) lp.getActionOrProperty(), mapping);
            else
                return new PropertyMapImplement<>((Property<P>) lp.getActionOrProperty(), mapping);
        }

        <T> PropertyObjectInterfaceImplement<T> mapObject(final ImOrderSet<T> interfaces) {
            ImRevMap<P, T> mapping = lp.listInterfaces.mapOrderRevValues(i -> interfaces.get(mapInt[i] - 1));

            return new PropertyRevImplement<>((Property<P>) lp.getActionOrProperty(), mapping);
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

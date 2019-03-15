package lsfusion.server.logics.property.classes.infer;

import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.server.data.where.classes.ClassWhere;
import lsfusion.server.logics.action.ActionWhereType;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.classes.user.set.AndClassSet;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.implement.PropertyInterfaceImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;

// нужны сами классы - в Info не нужны
public interface AlgType {

    <P extends PropertyInterface> ClassWhere<Object> getClassValueWhere(Property<P> property);

    <P extends PropertyInterface> ImMap<P, ValueClass> getInterfaceClasses(Property<P> property, ExClassSet valueClasses);

    <P extends PropertyInterface> ValueClass getValueClass(Property<P> property);

    <P extends PropertyInterface> boolean isInInterface(Property<P> property, ImMap<P, ? extends AndClassSet> interfaceClasses, boolean isAny);

    <T extends PropertyInterface, P extends PropertyInterface> void checkExclusiveness(Property<T> property, String caseInfo, Property<P> intersect, String intersectInfo, ImRevMap<P, T> map, String abstractInfo);

    <T extends PropertyInterface, P extends PropertyInterface> void checkContainsAll(Property<T> property, Property<P> intersect, String caseInfo, ImRevMap<P, T> map, PropertyInterfaceImplement<T> value, String abstractInfo);

    <T extends PropertyInterface, P extends PropertyInterface> void checkAllImplementations(Property<T> property, ImList<Property<P>> intersects, ImList<ImRevMap<P, T>> maps);

    AlgInfoType getAlgInfo();
    
    ActionWhereType actionWhere = ActionWhereType.CLASSCALC; // нельзя оборачивать на каждом шаге, так как IF (a IS A) { MESSAGE; g[B](a) } начнет выводить B 

    boolean useInfer = true; // после разделения на infer / resolve и calculate ветки, использовать старую схему в основном из-за проблем с abstract'ами проблематично 
    boolean useInferForInfo = true;
    boolean useClassInfer = useInfer;
    AlgInfoType defaultType = useInfer ? InferType.PREVBASE : CalcClassType.PREVBASE;
    AlgType caseCheckType = useInfer ? InferType.PREVSAME : CalcClassType.PREVSAME; // вопрос, так как возможно нужна сильнее логика разгребать
    AlgInfoType checkType = defaultType;
    AlgInfoType statAlotType = defaultType;
    AlgInfoType hintType = CalcType.EXPR.getAlgInfo();
    AlgInfoType drillType = defaultType;
    AlgInfoType syncType = defaultType; // тоже желательно совпадать с настройкой для classValueWhere
    AlgInfoType actionType = defaultType; // компиляция действий assign и for

    boolean checkExplicitInfer = false;
    boolean checkInferCalc = false;

}

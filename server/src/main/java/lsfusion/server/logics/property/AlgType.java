package lsfusion.server.logics.property;

import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.classes.sets.AndClassSet;
import lsfusion.server.data.where.classes.ClassWhere;
import lsfusion.server.logics.property.infer.ExClassSet;
import lsfusion.server.logics.property.infer.InferType;

// нужны сами классы - в Info не нужны
public interface AlgType {

    <P extends PropertyInterface> ClassWhere<Object> getClassValueWhere(CalcProperty<P> property);

    <P extends PropertyInterface> ImMap<P, ValueClass> getInterfaceClasses(CalcProperty<P> property, ExClassSet valueClasses);

    <P extends PropertyInterface> ValueClass getValueClass(CalcProperty<P> property);

    <P extends PropertyInterface> boolean isInInterface(CalcProperty<P> property, ImMap<P, ? extends AndClassSet> interfaceClasses, boolean isAny);

    <T extends PropertyInterface, P extends PropertyInterface> void checkExclusiveness(CalcProperty<T> property, String caption, CalcProperty<P> intersect, String intersectCaption, ImRevMap<P, T> map);

    <T extends PropertyInterface, P extends PropertyInterface> void checkContainsAll(CalcProperty<T> property, CalcProperty<P> intersect, String caption, ImRevMap<P, T> map, CalcPropertyInterfaceImplement<T> value);

    <T extends PropertyInterface, P extends PropertyInterface> void checkAllImplementations(CalcProperty<T> property, ImList<CalcProperty<P>> intersects, ImList<ImRevMap<P, T>> maps);

    AlgInfoType getAlgInfo();
    
    ActionWhereType actionWhere = ActionWhereType.CLASSCALC; // нельзя оборачивать на каждом шаге, так как IF (a IS A) { MESSAGE; g[B](a) } начнет выводить B 

    boolean useInfer = true; // после разделения на infer / resolve и calculate ветки, использовать старую схему в основном из-за проблем с abstract'ами проблематично 
    boolean useInferForInfo = true;
    boolean useClassInfer = useInfer;
    AlgInfoType defaultType = useInfer ? InferType.PREVBASE : CalcClassType.PREVBASE;
    AlgType caseCheckType = useInfer ? InferType.PREVSAME : CalcClassType.PREVSAME; // вопрос, так как возможно нужна сильнее логика разгребать
    AlgInfoType checkType = defaultType;
    AlgInfoType hintType = CalcType.EXPR.getAlgInfo();
    AlgInfoType drillType = defaultType;
    AlgInfoType syncType = defaultType; // тоже желательно совпадать с настройкой для classValueWhere
    AlgInfoType actionType = defaultType; // компиляция действий assign и for

    boolean checkExplicitInfer = false;
    boolean checkInferCalc = false;

}

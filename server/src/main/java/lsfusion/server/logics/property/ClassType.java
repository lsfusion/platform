package lsfusion.server.logics.property;

import lsfusion.server.logics.property.infer.InferType;

public enum ClassType {
    ASIS_BASE, // когда висячие - orAny (например b из f(a,b) OR g(a)) можно / нужно отключать, и PREV'ы должны быть BASE
    FULL_SAME, // для интерфейсов, нужно добавлять в том числе и висячие (orAny), PREV'ы считаются SAME, IS'ы оборачиваются table при использовании логики выражений 
    ASSERTFULL_NOPREV; // предполагается что orAny и PREV'ов нет 
    public static final ClassType useInsteadOfAssert = FULL_SAME;

    public AlgType getAlg() {
        assert this != ClassType.ASSERTFULL_NOPREV;
        if(AlgType.useClassInfer)
            return getInfer();
        else
            return getCalc();
    }

    public InferType getInfer() {
        switch (this) {
            case ASIS_BASE:
                return InferType.PREVBASE;
            case FULL_SAME:
                return InferType.PREVSAME;
        }
        throw new UnsupportedOperationException();
    }

    public CalcClassType getCalc() {
        switch (this) {
            case ASIS_BASE:
                return CalcClassType.PREVBASE;
            case FULL_SAME:
                return CalcClassType.PREVSAME;
        }
        throw new UnsupportedOperationException();
    }

    public static final ClassType obsolete = ASSERTFULL_NOPREV;
    
    public static final ClassType strictPolicy = ASIS_BASE; // когда не важны классы, или наоборот нельзя ошибиться

    public static final ClassType signaturePolicy = FULL_SAME; // явный вывод сигнатуры не в рамках формы
    public static final ClassType formPolicy = signaturePolicy;
    public static final ClassType syncPolicy = signaturePolicy;
    
    public static final ClassType wherePolicy = signaturePolicy; // ??? не уверен насчет FULL_SAME
    public static final ClassType forPolicy = strictPolicy; // вообще ASSERTFULL, но только по внутренним интерфейсам + PREV'ы как BASE должны идти
    
    public static final ClassType iteratePolicy = ASSERTFULL_NOPREV; 
    public static final ClassType logPolicy = iteratePolicy; // PREV'ов нет
    public static final ClassType materializeChangePolicy = iteratePolicy; // вообще как бы PREV'ов тоже не должно быть (так как получается CHANGED для PREV'а), но явной проверки нет, поэтому и assert на PREV вставлять не будем
    public static final ClassType storedPolicy = iteratePolicy; // PREV'ов нет
    
    public static final ClassType editPolicy = iteratePolicy; // редактируемые свойства, соотвественно без prev'ов 
    public static final ClassType filePolicy = editPolicy;
    public static final ClassType autoSetPolicy = editPolicy;
    
    public static final ClassType tryEditPolicy = signaturePolicy; // попытка редактирования свойства 

    // остальные
    public static final ClassType casePolicy = signaturePolicy; // вообще тут конечно, либо ASSERT из FULL убрать или NOPREV добавить, но такой вариант долгое время существовал
    public static final ClassType drillDownPolicy = signaturePolicy; // в явную есть проверки что только для isFull работать (хотя формально необязательно)
    public static final ClassType aroundPolicy = signaturePolicy; // наследование ннтерфейса, как правило PREV'ов нет, но в общем-то никто не запрещает

    public static final ClassType valuePolicy = signaturePolicy; 
    public static final ClassType typePolicy = valuePolicy;
    public static final ClassType resolvePolicy = valuePolicy;
}

package lsfusion.server.logics.property;

public enum CalcType {
    EXPR, // вычисления 
    CLASS, // определение классов 
    CLASS_PREVSAME, // определение классов, где prev'ы имеют те же классы (а не OBJECT)
    CLASS_INVERTSAME, // определение классов, где prev'ы имеют те же классы (а не OBJECT), а IS оборачиваются в таблицы
    STAT; // определение статистики, пока нигде не используется, кроме того чтобы предотвратить кэширования, когда статистика еще не обновлена
    
    public boolean isClass() {
        return this == CLASS || this == CLASS_PREVSAME || this == CLASS_INVERTSAME;
    }
    
    public boolean isClassInvert() {
        return this == CLASS_INVERTSAME;
    }

    public PrevClasses getPrevClasses() {
        assert isClass();
        switch (this) {
            case CLASS_PREVSAME:
                return PrevClasses.SAME;
            case CLASS_INVERTSAME:
                return PrevClasses.INVERTSAME;
            case CLASS:
                return PrevClasses.BASE;
        }
        throw new UnsupportedOperationException();
    }

    public boolean isStat() {
        return this == STAT;
    }

    public boolean isExpr() {
        return this == EXPR; 
    }
}

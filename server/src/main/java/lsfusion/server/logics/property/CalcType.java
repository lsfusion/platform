package lsfusion.server.logics.property;

public enum CalcType {
    EXPR, // вычисления 
    CLASS, // определение классов 
    STAT; // определение статистики, пока нигде не используется, кроме того чтобы предотвратить кэширования, когда статистика еще не обновлена
    
    public boolean isClass() {
        return this == CLASS;
    }

    public boolean isStat() {
        return this == STAT;
    }

    public boolean isExpr() {
        return this == EXPR; 
    }
}

package lsfusion.server.logics.property;

public enum PrevClasses {
    SAME, BASE, INVERTSAME;
    
    public CalcType getCalc() {
        switch(this) {
            case SAME:
                return CalcType.CLASS_PREVSAME;
            case BASE: 
                return CalcType.CLASS;
            case INVERTSAME:
                return CalcType.CLASS_INVERTSAME;
        }
        throw new UnsupportedOperationException();
    }
}

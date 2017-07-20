package lsfusion.server.data.expr.query;

public enum StatType {

    ALL, // COST и STAT
    COST; // только COST, not supported

    // COMPILE

    // GLOBAL

    public static final StatType PACK = ALL; // для выполнения

    private static StatType PUSH() {
        return ALL;
    }
    public static StatType PUSH_INNER() { // внутрь чего проталкиваем
        return ALL;
    }
    public static StatType PUSH_OUTER() { // откуда проталкиваем
        return ALL;
    }

    public static final StatType GROUP_SPLIT = ALL;

    // LOCAL

    public static final StatType COMPILE = ALL; // COST для TIMEOUT + NOT NULL PROBLEM

    public static final StatType ADJUST_RECURSION = ALL;

    public static final StatType ANTIJOIN = ALL;

    // PROPS

    // GLOBAL

    public static final StatType HINTCHANGE = ALL;

    public static final StatType PROP_STATS = ALL; // для ALOT ограничений на логирование

    // LOCAL

    public static final StatType UPDATE = ALL;

    public static final StatType DEFAULT = ALL; // статистика чисто в общем интересует

}

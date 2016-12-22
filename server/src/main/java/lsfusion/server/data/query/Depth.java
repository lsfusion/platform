package lsfusion.server.data.query;

public enum Depth {
    NORMAL, // обычный
    
    LARGE, // см. subQueryLargeDepth
    //    1       1
    // 6      -       6  - при cost + stat будет проталкиваться бесконечно (из-за того что вместе статистика 5 а по отдельности 6)
    //    5       6
    
    INFINITE // см. subQueryInfiniteDepth
}

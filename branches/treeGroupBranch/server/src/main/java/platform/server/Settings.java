package platform.server;

public class Settings {

    // обозначает что при проверке условия на TRUE не будет преобразовывать A cmp B в 3 противоположных NOT'а как правильно, а будет использовать эвристику
    public final static boolean SIMPLE_CHECK_COMPARE = true;

    // обозначает что на следствия (и отрицания) условия будет проверять когда остались только термы, не делая этого на промежуточных уровнях
    public final static boolean CHECK_FOLLOWS_WHEN_OBJECTS = false;

    // будет ли оптимизатор пытаться перестраивать условия по правилу X OR (Y AND Z) и X=>Y, то Y AND (X OR Z)
    public final static boolean RESTRUCT_WHERE_ON_MEANS = false;

    // будет ли оптимизатор разбивать группирующие выражения, чтобы не было FULL JOIN и UNION ALL 
    public static boolean SPLIT_GROUP_INNER_JOINS(boolean max) {
        return max;
    }

    // будет ли оптимизатор разбивать группирующие выражения на максимум, так чтобы в группируемом выражении не было бы Case'ов 
    public final static boolean SPLIT_GROUP_MAX_EXPRCASES = true;

    // будет ли высчитываться что именно изменилось в группирующих свойствах или же будет считаться что изменилось все
    public final static boolean CALCULATE_GROUP_DATA_CHANGED = false;

    // не использовать инкрементную логику в группирующем свойстве на максимум
    public final static boolean NO_INCREMENT_MAX_GROUP_PROPERTY = true;

}

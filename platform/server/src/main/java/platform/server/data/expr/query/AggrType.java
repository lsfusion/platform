package platform.server.data.expr.query;

public interface AggrType {

    boolean isSelect(); // оператор выбирает из множества значений (а не суммирует, объединяет и т.п.)

    boolean canBeNull(); // может возвращать null если само выражение не null
}

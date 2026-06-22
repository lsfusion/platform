package lsfusion.server.data.expr.join.select;

import lsfusion.interop.form.property.Compare;
import lsfusion.server.data.expr.BaseExpr;
import lsfusion.server.data.expr.join.inner.InnerJoins;

// реализуется WhereJoin'ами (ExprIndexedJoin, KeyExprCompareJoin); не наследует WhereJoin, так как тот self-типизирован (WhereJoin<K, T extends WhereJoin<K, T>>)
// сравнение baseExpr <compare> value, которое при подсчёте статистики сворачивается в интервал (см. ExprIndexedJoin.fillIntervals):
// несколько границ по одному baseExpr схлопываются в ExprIntervalJoin (полный интервал) или ExprStatJoin (иначе)
public interface IntervalCompareJoin {

    BaseExpr getIntervalBaseExpr();

    Compare getIntervalCompare();

    InnerJoins getIntervalValueJoins();

    boolean isFoldableInterval(); // ExprIndexedJoin при not оставляем как есть (not не => notNull)
}

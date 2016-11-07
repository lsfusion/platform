package lsfusion.server.data.expr.query;

import lsfusion.server.Settings;

public class Stat {

    public final static Stat MAX = new Stat(1000000, true);
    public final static Stat ONE = new Stat(1);
    public final static Stat ONESTAT = new Stat(1, true);
    public final static Stat MIN = new Stat(-1000, true);
    private final int deg;

    public final static Stat ALOT = new Stat(10000, true);
    public final static Stat AGGR = new Stat(100, true); // должно быть меньше ALOT, чтобы в [Group BY K](I) (где K бесконечное количество ключей (ALOT)) или I+5  = value, не уменьшало общую статистика до 0 и не считала что запрос построить можно (то есть там нет висячих ключей)
    public final static Stat DEFAULT = new Stat(5, true);

    public String toString() {
        return "(" + deg + ")";
    }

    public Stat(int count) {
        this((long)count);
    }

    public Stat(long count) {
        this(count, 1);
    }
    
    public static double getDegree(double count, int countDeg) {
        return (double)countDeg * (Math.log10(count) / Math.log10(Settings.get().getStatDegree()));
    }

    public Stat(double count, int countDeg) { // ceil чтобы инвариант у суммы stat <=
        this(count == 0 ? 0 : (int) Math.round(getDegree(count, countDeg)), true);
    }

    public Stat(int count, boolean isDeg) {
        assert isDeg;
        deg = count;
    }

    public boolean less(Stat stat) {
        return deg < stat.deg;
    }

    public boolean lessEquals(Stat stat) {
        return deg <= stat.deg;
    }

    public boolean isMin() {
        return deg == 0;
    }

    public Stat min(Stat stat) {
        if(less(stat))
            return this;
        else
            return stat;
    }

    public Stat max(Stat stat) {
        if(less(stat))
            return stat;
        else
            return this;
    }

    public Stat or(Stat stat) {
        return max(stat);
    }

    public Stat mult(Stat stat) {
        return new Stat(deg + stat.deg, true);
    }

    public Stat deg(int mdeg) {
        return new Stat(deg * mdeg, true);
    }

    public Stat div(Stat stat) {
        int divDeg = deg - stat.deg;
        if(divDeg<0) // по сути учитываем дисперсию
            divDeg = 0;
        return new Stat(divDeg, true);
    }

    public boolean equals(Object o) {
        return this == o || o instanceof Stat && deg == ((Stat) o).deg;
    }

    public int hashCode() {
        return deg;
    }
    
    public int getWeight() {
        return deg;
    }

    public int getCount() {
        int statDegree = Settings.get().getStatDegree();
        long result = 1;
        for(int i=0,size=getWeight();i<size;i++) {
            result = result * statDegree;
            if(result > Integer.MAX_VALUE)
                break;
        }
        return (int) Math.min(result, Integer.MAX_VALUE);
    }
    
    public Stat avg(Stat add) {
        return new Stat((deg + add.deg) / 2, true);
    }
}

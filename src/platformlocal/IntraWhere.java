package platformlocal;
import java.util.*;

interface IntraWhere extends SourceJoin {

    IntraWhere in(IntraWhere where);
    IntraWhere not();

    IntraWhere followFalse(OuterWhere where);
    IntraWhere translate(Translator translator);

    boolean isFalse();
    boolean isTrue();

    String getSource(Map<QueryData, String> joinAlias, SQLSyntax syntax);
    OuterWhere getOr();

    boolean getValue(List<DataWhere> trueWheres);

    IntraWhere inNot(IntraWhere where);
    boolean means(IntraWhere where);

    void fillData(Collection<DataWhere> wheres);

    IntraWhere getJoinWhere();

    IntraWhere followTrue(InnerWhere where);

    IntraWhere reverseNot();

    IntraWhere copy();

    // для кэша
    boolean equals(IntraWhere where, Map<ObjectExpr, ObjectExpr> mapExprs, Map<JoinWhere, JoinWhere> mapWheres);

    int hash();
}

class OuterWhere extends ArrayList<InnerWhere> implements IntraWhere {

    // ------------------- конструкторы ------------------------ //

    OuterWhere() {
    }

    OuterWhere(ObjectWhere Where) {
        this(new InnerWhere(Where));
    }

    OuterWhere(InnerWhere where) {
        super(Collections.singleton(where));
    }

    OuterWhere(ArrayList<InnerWhere> iNodes) {
        super(iNodes);
    }

    OuterWhere(Set<InnerWhere> iNodes) {
        super(iNodes);
    }

    // ------------------- Реализация or ------------------------ //

    void out(IntraWhere where) {

        int firstSize = size();

        if(where instanceof InnerWhere) {
            if (((InnerWhere)where).size() == 0) { clear(); add((InnerWhere)where); return; }
            out((InnerWhere)where);
        } else
        if(where instanceof OuterWhere) {
            if (((OuterWhere)where).size() == 0) return;
            out((OuterWhere)where);
        } else
            out(new InnerWhere((ObjectWhere) where));

        simplify(firstSize, size(), false);
    }

    private void out(InnerWhere where) {
        add(where);
    }

    private void out(OuterWhere where) {
        addAll(where);
    }

    // ------------------- Реализация in ------------------------ //

    public IntraWhere in(IntraWhere where) {

        OuterWhere result;
        if(where instanceof InnerWhere) {
            if (((InnerWhere)where).size() == 0) return new OuterWhere(this);
            result = in((InnerWhere)where);
        } else
        if(where instanceof OuterWhere) {
            if (((OuterWhere)where).size() == 0) return new OuterWhere();
            result = in((OuterWhere)where);
        } else
            result = in(new InnerWhere((ObjectWhere)where));

        result.simplify();

        return result;
    }

    private OuterWhere in(OuterWhere where) {

        OuterWhere result = new OuterWhere();
        for (InnerWhere innerWhere : where)
            result.out(in(innerWhere) );

        return result;
    }

    private OuterWhere in(InnerWhere where) {

        OuterWhere result = new OuterWhere();
        for (InnerWhere innerWhere : this) {
            IntraWhere inResult = innerWhere.in(where);
            if (inResult instanceof InnerWhere)
                result.out((InnerWhere)inResult);
        }

        return result;
    }

    // ------------------- Реализация not ------------------------ //

    public IntraWhere not() {

        OuterWhere result = new OuterWhere(new InnerWhere());
        for(InnerWhere innerWhere : this)
            result = result.in(innerWhere.not());

        result.simplify();
        return result;
    }

    public IntraWhere followFalse(OuterWhere where) {

        int firstSize = size();
        OuterWhere outerWhere = new OuterWhere(this);
        outerWhere.out(where);
        outerWhere.simplify(firstSize, outerWhere.size(), true);

        for (InnerWhere innerWhere : where)
            outerWhere.remove(innerWhere);

//        outerWhere.simplify();
        return outerWhere;
    }

    void simplifyFull() {
        simplifyBuildDNF();
        simplifyConcatenate();
        simplifyRemoveRedundant();
        simplifyInWheres();
        simplifyMatrix();
    }

    void simplify() {
        simplify(0, 0, false);
    }

    void simplify(int firstIndex, int secondIndex, boolean keepSecond) {
        simplifySimpleConcatenate(firstIndex, secondIndex);
        simplifyRemoveRedundant(firstIndex, secondIndex, keepSecond);
    }

    void simplifyRemoveRedundant() {
        simplifyRemoveRedundant(0, 0, false);
    }

    void simplifyRemoveRedundant(int firstIndex, int secondIndex, boolean keepSecond) {

//        System.out.println("redund started " + size());

        boolean[] redundant = new boolean[size()];
        int redCount = 0;

        for (int i1 = 0; i1 < size(); i1++)
            if (!redundant[i1]) {
                InnerWhere where1 = get(i1);
                if (keepSecond && i1 >= firstIndex && i1 < secondIndex) continue;
                for (int i2 = 0; i2 < size(); i2++)
                    if (i1 != i2 && !redundant[i2]) {
                        if (i1 < firstIndex) {
                            if (i2 < firstIndex) continue;
                        } else {
                            if (i1 < secondIndex && i2 >= firstIndex && i2 < secondIndex) continue;
                        }
                        InnerWhere where2 = get(i2);
                        if (where1.follow(where2)) {
                            redundant[i1] = true;
                            redCount++;
                            break;
                        }
                    }
            }

        if (redCount == 0) return;

        InnerWhere[] newWheres = new InnerWhere[size() - redCount];
        int j = 0;
        for (int i = 0; i < size(); i++)
            if (!redundant[i]) newWheres[j++] = get(i);

        clear();
        Collections.addAll(this, newWheres);

//        System.out.println("redund ended " + size());
    }

    void simplifySimpleConcatenate(int firstIndex, int secondIndex) {

        List<Boolean> redundant = new ArrayList(size());
        for (int i = 0; i < size(); i++)
            redundant.add(false);
        int redCount = 0;

        for (int i1 = 0; i1 < size(); i1++) {
            if (!redundant.get(i1)) {
                InnerWhere where1 = get(i1);
                for (int i2 = i1+1; i2 < size(); i2++) {
                    if (!redundant.get(i2)) {
                        if (i1 < firstIndex) {
                            if (i2 < firstIndex) continue;
                        } else {
                            if (i1 < secondIndex && i2 >= firstIndex && i2 < secondIndex) continue;
                        }
                        InnerWhere where2 = get(i2);
                        InnerWhere concat = where1.concatenate(where2);
                        if (concat != null) {
                            if (concat.size() == 0) { clear(); add(concat); return; }
                            add(concat);
                            redundant.add(false);
                            redundant.set(i1, true);
                            redundant.set(i2, true);
                        }
                    }
                }
            }
        }

        if (redCount == 0) return;

        InnerWhere[] newWheres = new InnerWhere[size() - redCount];
        int j = 0;
        for (int i = 0; i < size(); i++)
            if (!redundant.get(i)) newWheres[j++] = get(i);

        clear();
        Collections.addAll(this, newWheres);
    }

    void simplifyConcatenate() {
        simplifyConcatenate(0, 0);
    }

    void simplifyConcatenate(int firstIndex, int secondIndex) {

        if (size() == 0) return;

        int maxSize = 0;
        for (InnerWhere where : this) {
            if (where.size() > maxSize) maxSize = where.size();
            // проверка, что .T.
            if (where.size() == 0) { clear(); add(where); return; }
        }

//        System.out.println("concat started " + size());

        ArrayList<InnerWhere>[] wheres = new ArrayList[maxSize];
        int[] firstIndexes = new int[maxSize];
        int[] secondIndexes = new int[maxSize];

        for (int i = 0; i < maxSize; i++) {
            wheres[i] = new ArrayList();
        }

        for (int i = 0; i < size(); i++) {
            InnerWhere where = get(i);
            int rowNum = where.size() - 1;

            Collection<InnerWhere> rowWhere = wheres[rowNum];
            rowWhere.add(where);

            if (i < firstIndex) {
                firstIndexes[rowNum]++;
                secondIndexes[rowNum]++;
            } else {
                if (i < secondIndex)
                    secondIndexes[rowNum]++;
            }

        }

        Set<InnerWhere> wasWheres = new HashSet(this);

        for (int whereSize = maxSize-1; whereSize >= 0; whereSize--) {

            ArrayList<InnerWhere> rowWhere = wheres[whereSize];
            for (int i1 = 0; i1 < rowWhere.size(); i1++) {
                InnerWhere where1 = rowWhere.get(i1);
                for (int i2 = i1+1; i2 < rowWhere.size(); i2++) {

                    if (i1 < firstIndexes[whereSize]) {
                        if (i2 < firstIndexes[whereSize]) continue;
                    } else {
                        if (i1 < secondIndexes[whereSize] && i2 >= firstIndexes[whereSize] && i2 < secondIndexes[whereSize]) continue;
                    }

                    InnerWhere where2 = rowWhere.get(i2);
                    InnerWhere concat = where1.concatenate(where2);
                    if (concat != null && !wasWheres.contains(concat)) {
                        add(concat);
                        wasWheres.add(concat);
                        // проверка, что .T.
                        if (whereSize == 0) { clear(); add(concat); return; }
                        wheres[whereSize-1].add(concat);
                    }
                }
            }
        }

//        System.out.println("concat ended " + size());
    }

    boolean simplifyConglutinate(Collection<InnerWhere> secondaryWheres) {

        int startSize = size();
//        System.out.println("conglut started " + size());

        List<Boolean> redundant = new ArrayList(size());
        for (int i = 0; i < size(); i++)
            redundant.add(false);
        
        int redCount = 0;

        for (int i1 = 0; i1 < size(); i1++) {
            if (!redundant.get(i1)) {
                InnerWhere where1 = get(i1);
                if (where1.size() == 1 || where1.size() == 2) {
                    for (int i2 = 0; i2 < size(); i2++) {
                        if (!redundant.get(i2)) {
                            InnerWhere where2 = get(i2);
                            if (where1 != where2 && !BaseUtils.findByReference(secondaryWheres, where2)) {

                                if (where1.size() == 1) {
                                    InnerWhere congl = where1.conglutinate(where2);
                                    if (congl != null) {
                                        redundant.set(i2, true);
                                        redCount++;
                                        if (!contains(congl)) { add(congl); redundant.add(false); }
                                        continue;
                                    }
                                }

                                if (where1.size() == 2) {
                                    for (int i3 = 0; i3 < size(); i3++) {
                                        InnerWhere where3 = get(i3);
                                        if (!redundant.get(i3) && where3 != where1 && where3 != where2) {
                                            InnerWhere congl2 = where1.conglutinate(where2, where3);
                                            if (congl2 != null) {
                                                redundant.set(i2, true);
                                                redundant.set(i3, true);
                                                redCount += 2;
                                                if (!contains(congl2)) { add(congl2); redundant.add(false); }
                                                break;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (redCount == 0) return false;

        InnerWhere[] newWheres = new InnerWhere[size() - redCount];
        int j = 0;
        for (int i = 0; i < size(); i++)
            if (!redundant.get(i)) newWheres[j++] = get(i);

        clear();
        Collections.addAll(this, newWheres);

        return true;
//        System.out.println("conglut ended " + size());
    }

    void simplifyBuildDNF() {

        Set<DataWhere> allDataWheres = new HashSet();
        fillData(allDataWheres);

//        System.out.println("bdnf started " + size() + " : " + allDataWheres.size());

        Set<Set<ObjectWhere>> resultWhere = new HashSet();

        for (InnerWhere where : this) {

            Set<DataWhere> dataWheres = new HashSet(allDataWheres);
            for (ObjectWhere objWhere : where)
                dataWheres.remove(objWhere.getDataWhere());

            Collection<InnerWhere> curWheres = Collections.singleton(new InnerWhere(where)); // надо именно так делать, иначе цикл будет менять where

            for (ObjectWhere objWhere : dataWheres) {

                Collection<InnerWhere> newWheres = new ArrayList();

                for (InnerWhere curWhere : curWheres) {

                    boolean follows = false, followed = false;
                    for (ObjectWhere objCurWhere : curWhere) {
                        if (objCurWhere instanceof DataWhere && objCurWhere.getDataWhere().follow(objWhere.getDataWhere()))
                            follows = true;
                        if (objCurWhere instanceof NotDataWhere && objWhere.getDataWhere().follow(objCurWhere.getDataWhere()))
                            followed = true;
                        if (follows && followed) break;
                    }

                    if (!follows) {
                        InnerWhere newWhere = new InnerWhere(curWhere);
                        newWhere.add((ObjectWhere)objWhere.not());
                        newWheres.add(newWhere);
                    }

                    if (!followed) {
                        curWhere.add(objWhere);
                        newWheres.add(curWhere);
                    }                    
                }

                curWheres = newWheres;
            }

            for (InnerWhere curWhere : curWheres) {
                resultWhere.add(new HashSet(curWhere));
            }
        }

        clear();

        for (Set<ObjectWhere> inWhere : resultWhere) {
            add(new InnerWhere(inWhere));
        }

//        System.out.println("bdnf ended " + size());
    }

    void simplifyInWheres() {

        for (InnerWhere where : this)
            where.simplify();
    }

    void simplifyMatrix() {

        Set<DataWhere> allDataWheres = new HashSet();
        fillData(allDataWheres);

//        System.out.println("matrix started " + size() + " : " + allDataWheres.size());
        
        Collection<List<DataWhere>> values = buildAllDataWheres(allDataWheres);

        Collection<List<DataWhere>> trueValues = new ArrayList();
        for (List<DataWhere> value : values)
            if (getValue(value))
                trueValues.add(value);

        Collection<InnerWhere> bestWheres = calculateMinimumInWheres(trueValues, 0, new ArrayList(), new HashSet());

        if (bestWheres.size() < size()) {
            clear();
            addAll(bestWheres);
        }

//        System.out.println("matrix ended " + size());
    }

    private Collection<InnerWhere> calculateMinimumInWheres(Collection<List<DataWhere>> values, int index, Collection<InnerWhere> currentWheres, Set<List<DataWhere>> currentValues) {

        if (currentValues.size() >= values.size()) return new ArrayList(currentWheres);
        if (index == size()) return null;

        Collection<InnerWhere> bestWhere = calculateMinimumInWheres(values, index+1, currentWheres, currentValues);

        InnerWhere currentWhere = get(index);
        Set<List<DataWhere>> newValues = new HashSet(currentValues);
        for (List<DataWhere> value : values) {
            if (currentWhere.getValue(value))
                newValues.add(value);
        }

        currentWheres.add(currentWhere);
        Collection<InnerWhere> resultWhere = calculateMinimumInWheres(values, index+1, currentWheres, newValues);
        currentWheres.remove(currentWhere);

        if (bestWhere == null || resultWhere.size() < bestWhere.size()) bestWhere = resultWhere;
        return bestWhere;
    }

    public static Collection<List<DataWhere>> buildAllDataWheres(Collection<DataWhere> dataWheres) {

        Collection<List<DataWhere>> curValues = new ArrayList();
        curValues.add(new ArrayList());

        List<DataWhere> wasWheres = new ArrayList();

        for (DataWhere dataWhere : dataWheres) {

            Collection<List<DataWhere>> newValues = new ArrayList();
            for (List<DataWhere> value : curValues) {

                boolean follows = false;
                for (DataWhere trueWhere : value)
                    if (trueWhere.follow(dataWhere)) { follows = true; break; }

                if (!follows)
                    newValues.add(value);

                boolean followed = false;
                for (DataWhere checkWhere : wasWheres)
                    if (!value.contains(checkWhere) && dataWhere.follow(checkWhere)) { followed = true; break; }

                if (!followed) {
                    List<DataWhere> newValue = new ArrayList(value);
                    newValue.add(dataWhere);
                    newValues.add(newValue);
                }
            }

//            newValues.add(Collections.singletonList(dataWhere));
            curValues = newValues;

            wasWheres.add(dataWhere);
        }

        return curValues;
    }

    // ------------------- Реализация translate ------------------------ //

    public IntraWhere translate(Translator translator) {
        // сначала транслируем InnerWhere
        Collection<IntraWhere> transWheres = new ArrayList<IntraWhere>();
        boolean changedWheres = false;
        for(InnerWhere where : this) {
            IntraWhere transWhere = translator.translate(where);
            transWheres.add(transWhere);
            changedWheres = changedWheres || (transWhere!=where);
        }

        if(!changedWheres)
            return this;

        OuterWhere transOuter = new OuterWhere();
        for(IntraWhere where : transWheres)
            transOuter.out(where);
        return transOuter;
    }

    // ------------------- Реализация вспомогательных  интерфейсов ------------------------ //

    public boolean isFalse() {
        return isEmpty();
    }

    public boolean isTrue() {
        return (size()==1 && iterator().next().isTrue());
    }

    // -------------------- Выходной интерфейс ------------------------- //

    public String getSource(Map<QueryData, String> QueryData, SQLSyntax syntax) {
        if(isEmpty())
            return "1<>1";

        if(size()==1)
            return iterator().next().getSource(QueryData, syntax);

        String source = "";
        for(InnerWhere where : this)
            source = (source.length()==0?"":source+" OR ") + "(" + where.getSource(QueryData, syntax) + ")";
        return source;
    }

    public String toString() {
        if(isEmpty())
            return "FALSE";

        if(size()==1)
            return iterator().next().toString();

        String source = "";
        for(InnerWhere where : this)
            source = (source.length()==0?"":source+" OR ") + "(" + where.toString() + ")";
        return source;
    }

    public OuterWhere getOr() {
        return this;
    }

    // -------------------- Всякие другие методы ------------------------- //

    public <J extends Join> void fillJoins(List<J> joins) {
        for(InnerWhere where : this)
            where.fillJoins(joins);
    }

    public void fillJoinWheres(MapWhere<JoinData> joins, IntraWhere inWhere) {
        for(InnerWhere where : this)
            where.fillJoinWheres(joins, inWhere);
    }

    public boolean getValue(List<DataWhere> trueWheres) {

        boolean result = false;
        for (InnerWhere where : this) {
            result |= where.getValue(trueWheres);
        }

        return result;
    }

    public IntraWhere inNot(IntraWhere where) {
        if(where instanceof InnerWhere)
            return inNot(new OuterWhere((InnerWhere)where));
        else
        if(where instanceof OuterWhere)
            return inNot((OuterWhere)where);
        else
            return inNot(new InnerWhere((ObjectWhere)where));
    }

    public IntraWhere inNot(OuterWhere where) {

        if (where.size() == 0) return new OuterWhere(this);

        Set<InnerWhere> resultSet = new HashSet();
        for (InnerWhere whereFrom : this) {
            inNot(new InnerWhere(whereFrom), where, 0, resultSet);
        }

        OuterWhere resultWhere = new OuterWhere(resultSet);
        // если не делать removeRedundant до их будет вообще дофига
        resultWhere.simplifyRemoveRedundant();
        resultWhere.simplify();
        return resultWhere;
    }

    private void inNot(InnerWhere whereFrom, OuterWhere where, int index, Set<InnerWhere> resultWhere) {

        if (index >= where.size()) {
            resultWhere.add(whereFrom);
            return;
        }

        InnerWhere whereTo = where.get(index);
        for (ObjectWhere objWhere : whereTo) {
            IntraWhere newWhere = whereFrom.in(objWhere.not());
            if (newWhere.isFalse()) continue;
            inNot((InnerWhere)newWhere, where, index+1, resultWhere);
        }
    }

    public boolean means(IntraWhere where) {
        if(where instanceof InnerWhere)
            return means(new OuterWhere((InnerWhere)where));
        else
        if(where instanceof OuterWhere)
            return means((OuterWhere)where);
        else
            return means(new InnerWhere((ObjectWhere)where));
    }

    public boolean means(OuterWhere where) {

        if(where.size() == 0) return isFalse();

        for (InnerWhere whereFrom : this) {
            if (meansNot(new InnerWhere(whereFrom), where, 0)) return false;
        }

        return true;
    }

    private boolean meansNot(InnerWhere whereFrom, OuterWhere where, int index) {

        if (index >= where.size()) return !whereFrom.isFalse();

        InnerWhere whereTo = where.get(index);
        for (ObjectWhere objWhere : whereTo) {
            IntraWhere newWhere = whereFrom.in(objWhere.not());
            if (newWhere.isFalse()) continue;
            if (meansNot((InnerWhere)newWhere, where, index+1)) return true;
        }

        return false;
    }

    // преобразует ДНФ not'ом в КНФ
    public IntraWhere reverseNot() {
        OuterWhere result = new OuterWhere();
        for(InnerWhere where : this)
            result.add((InnerWhere) where.reverseNot());
        return result;
    }

    // для оптимизации
    int getComplexity() {
        int complexity = 0;
        for(InnerWhere where : this)
            complexity += where.size();
        return complexity;
    }
    int getDataCount() {
        Collection<DataWhere> wheres = new HashSet<DataWhere>();
        fillData(wheres);
        return wheres.size();
    }

    public void fillData(Collection<DataWhere> wheres) {
        for(InnerWhere where : this)
            where.fillData(wheres);
    }

    public IntraWhere getJoinWhere() {
        OuterWhere result = new OuterWhere();
        for(InnerWhere where : this)
            result.out(where.getJoinWhere());
        return result;
    }

    public IntraWhere followTrue(InnerWhere where) {
        return this;
    }

    public IntraWhere copy() {
        OuterWhere Result = new OuterWhere();
        for(InnerWhere where : this)
            Result.add((InnerWhere) where.copy());
        return Result;
    }

    public boolean equals(Object o) {
        return this==o;
    }

    public int hashCode() {
        return System.identityHashCode(this);
    }

    // для кэша
    public boolean equals(IntraWhere where, Map<ObjectExpr, ObjectExpr> mapExprs, Map<JoinWhere, JoinWhere> mapWheres) {
        return where instanceof OuterWhere && equals(this,(OuterWhere)where, mapExprs, mapWheres);
    }

    public int hash() {
        return hash(this,5);
    }

    static <T extends IntraWhere> boolean equals(Collection<T> w1, Collection<T> w2, Map<ObjectExpr, ObjectExpr> mapExprs, Map<JoinWhere, JoinWhere> mapWheres) {

        if(w1.size()!=w2.size()) return false;

        Collection<T> mapped = new ArrayList<T>();
        for(T inWhere : w1) {
            T mappedWhere = null;
            for(T inEqualWhere : w2)
                if(!mapped.contains(inEqualWhere) && inEqualWhere.equals(inWhere,mapExprs, mapWheres)) {
                    mappedWhere = inEqualWhere;}
            if(mappedWhere==null) return false;
            mapped.add(mappedWhere);
        }

        return true;
    }

    static <T extends IntraWhere> int hash(Collection<T> wheres, int Hash) {
        for(T where : wheres)
            Hash += where.hash();
        return Hash;
    }

}

class InnerWhere extends ArrayList<ObjectWhere> implements IntraWhere {

    // ------------------- конструкторы ------------------------ //

    InnerWhere() {
    }

    InnerWhere(ObjectWhere where) {
        super(Collections.singleton(where));
    }

    InnerWhere(InnerWhere where) {
        super(where);
    }

    InnerWhere(Set<ObjectWhere> where) {
        super(where);
    }

    boolean follow(InnerWhere where) {

        for (ObjectWhere objWhereTo : where) {

            boolean follows = false;
            for (ObjectWhere objWhereFrom : this) {
                if ((objWhereTo instanceof DataWhere && objWhereFrom instanceof DataWhere && objWhereFrom.getDataWhere().follow(objWhereTo.getDataWhere())) ||
                    (objWhereTo instanceof NotDataWhere && objWhereFrom instanceof NotDataWhere && objWhereTo.getDataWhere().follow(objWhereFrom.getDataWhere()))) {
                    follows = true;
                    break;
                }
            }

            if (!follows) {
                return false;
            }
        }

        return true;
    }

    InnerWhere concatenate(InnerWhere where) {

        if (where.size() != size()) return null;

        ObjectWhere objWhereFrom = getDifferentObjectWhere(this, where);
        if (objWhereFrom == null) return null;

        ObjectWhere objWhereTo = getDifferentObjectWhere(where, this);
        if (objWhereTo == null) return null;

        if (objWhereFrom instanceof NotDataWhere && objWhereTo instanceof DataWhere) {
            if (!objWhereFrom.getDataWhere().follow(objWhereTo.getDataWhere())) return null;
        } else {
            if (objWhereFrom instanceof DataWhere && objWhereTo instanceof NotDataWhere) {
                if (!objWhereTo.getDataWhere().follow(objWhereFrom.getDataWhere())) return null;
            } else return null;
        }

        InnerWhere concat = new InnerWhere();
        for (ObjectWhere objWhere : this)
            if (objWhere != objWhereFrom)
                concat.add(objWhere);

        return concat;

//        for (ObjectWhere objWhere)

/*        for (ObjectWhere objWhereTo : where) {
            if (objWhereTo instanceof DataWhere)
                for (ObjectWhere objWhereFrom : this) {
                    if (objWhereFrom instanceof NotWhere && objWhereFrom.getDataWhere().follow(objWhereTo.getDataWhere())) { */
//        if (calculateHashCode(this, objWhereFrom) != calculateHashCode(where, objWhereTo)) return null;
    }

    private ObjectWhere getDifferentObjectWhere(InnerWhere whereFrom, InnerWhere whereTo) {

        ObjectWhere result = null;
        for (ObjectWhere objWhereFrom : whereFrom)
            if (!whereTo.contains(objWhereFrom)) {
                if (result != null) return null;
                result = objWhereFrom;
            }

        return result;
    }

/*    private int calculateHashCode(InnerWhere where, ObjectWhere objWhereExcept) {

        int hash1 = 0, hash2 = 1;
        for (ObjectWhere objWhere : where)
            if (objWhere != objWhereExcept) {
                hash1 += objWhere.hashCode();
                hash2 *= objWhere.hashCode() + 16237; // чтобы нулей меньше было
            }

        return hash1 + hash2;
    } */

    InnerWhere conglutinate(InnerWhere where) {

        if (size() != 1) return null;

        ObjectWhere objWhereC = get(0);

        for (ObjectWhere objWhere : where) {
            if (( objWhere instanceof NotDataWhere && objWhereC instanceof DataWhere && objWhere.getDataWhere().follow(objWhereC.getDataWhere()) ) ||
                ( objWhere instanceof DataWhere && objWhereC instanceof NotDataWhere && objWhereC.getDataWhere().follow(objWhere.getDataWhere()) )) {
                InnerWhere result = new InnerWhere(where);
                result.remove(objWhere);
                return result;
            }
        }

        return null;
    }

    InnerWhere conglutinate(InnerWhere where1, InnerWhere where2) {

        if (size() != 2) return null;
        if (where1.size() != where2.size()) return null;

        ObjectWhere objWhereC1 = get(0);
        ObjectWhere objWhereC2 = get(1);

        for (ObjectWhere objWhere1 : where1) {
            if (( objWhere1 instanceof NotDataWhere && objWhereC1 instanceof DataWhere && objWhere1.getDataWhere().follow(objWhereC1.getDataWhere()) ) ||
                ( objWhere1 instanceof DataWhere && objWhereC1 instanceof NotDataWhere && objWhereC1.getDataWhere().follow(objWhere1.getDataWhere()) )) {

                InnerWhere result = new InnerWhere(where1);
                result.remove(objWhere1);

                for (ObjectWhere objWhere2 : where2) {
                    if (( objWhere2 instanceof NotDataWhere && objWhereC2 instanceof DataWhere && objWhere2.getDataWhere().follow(objWhereC2.getDataWhere()) ) ||
                        ( objWhere2 instanceof DataWhere && objWhereC2 instanceof NotDataWhere && objWhereC2.getDataWhere().follow(objWhere2.getDataWhere()) )) {

                        if (where2.containsAll(result) && !result.contains(objWhere2))
                            return result;
                    }
                }

            }
        }

        return null;
    }

    // ------------------- Реализация in ------------------------ //

    public IntraWhere in(IntraWhere where) {
        if(where instanceof InnerWhere)
            return in((InnerWhere)where);
        else
        if(where instanceof OuterWhere)
            return in((OuterWhere) where);
        else
            return in(new InnerWhere((ObjectWhere)where));
    }

    public IntraWhere in(InnerWhere where) {

        InnerWhere result = new InnerWhere(this);

        for (ObjectWhere whereNew : where) {

            boolean redundant = false;
            for (ObjectWhere whereOld : this) {
                if (whereNew.getDataWhere().follow(whereOld.getDataWhere())) {
                    if (whereNew instanceof DataWhere && whereOld instanceof NotDataWhere)
                        return new OuterWhere(); // невыполнимость
                    if (whereNew instanceof DataWhere && whereOld instanceof DataWhere) {
                        result.remove(whereOld);
                        continue;
                    }
                    if (whereNew instanceof NotDataWhere && whereOld instanceof NotDataWhere) {
                        redundant = true;
                        continue;
                    }
                }
                if (whereOld.getDataWhere().follow(whereNew.getDataWhere())) {
                    if (whereOld instanceof DataWhere && whereNew instanceof NotDataWhere)
                        return new OuterWhere(); // невыполнимость
                    if (whereOld instanceof DataWhere && whereNew instanceof DataWhere) {
                        redundant = true;
                        continue;
                    }
                    if (whereOld instanceof NotDataWhere && whereNew instanceof NotDataWhere) {
                        result.remove(whereOld);
                        continue;
                    }
                }
            }

            if (!redundant)
                result.add(whereNew);
        }

        return result;
    }

    public IntraWhere in(OuterWhere where) {
        return where.in(this);
    }

    // ------------------- Реализация not ------------------------ //

    public OuterWhere not() {

        OuterWhere result = new OuterWhere();
        for(ObjectWhere where : this)
            result.out(where.not());
        return result;
    }

    // преобразует ДНФ not'ом в КНФ
    public IntraWhere reverseNot() {
        InnerWhere result = new InnerWhere();
        for(ObjectWhere where : this)
            result.add((ObjectWhere) where.not());
        return result;
    }

    public IntraWhere followFalse(OuterWhere where) {
        return new OuterWhere(this).followFalse(where);
    }

    public void simplify() {

        Set<ObjectWhere> redundant = new HashSet();
        for (ObjectWhere whereFrom : this)
            if (!redundant.contains(whereFrom))
                for (ObjectWhere whereTo : this)
                    if (whereFrom != whereTo && !redundant.contains(whereTo) && whereFrom.getDataWhere().follow(whereTo.getDataWhere())) {
                        if (whereFrom instanceof DataWhere && whereTo instanceof DataWhere) {
                            redundant.add(whereTo);
                            break;
                        }
                        if (whereFrom instanceof NotDataWhere && whereTo instanceof NotDataWhere)
                            redundant.add(whereFrom);
                    }

        for (ObjectWhere where : redundant)
            remove(where);
    }

    // ------------------- Реализация translate ------------------------ //

    public IntraWhere translate(Translator translator) {
        // сначала транслируем InWhere
        Collection<IntraWhere> transWheres = new ArrayList<IntraWhere>();
        boolean changedWheres = false;
        for(ObjectWhere where : this) {
            IntraWhere transWhere = translator.translate(where);
            transWheres.add(transWhere);
            changedWheres = changedWheres || (transWhere!=where);
        }

        if(!changedWheres)
            return this;

        int Complexity = 1;
        for(IntraWhere where : transWheres) {
            Complexity = Complexity*where.getOr().size();
        }
        if(Complexity>50)
            Complexity = Complexity;

        IntraWhere trans = new InnerWhere();
        for(IntraWhere where : transWheres)
            trans = trans.in(where);
        return trans;
    }

    // ------------------- Реализация вспомогательных  интерфейсов ------------------------ //

    public boolean isFalse() {
        return false;
    }

    public boolean isTrue() {
        return isEmpty();
    }

    // -------------------- Выходной интерфейс ------------------------- //

    public String getSource(Map<QueryData, String> QueryData, SQLSyntax syntax) {
        if(isEmpty())
            return "1=1";

        if(size()==1)
            return iterator().next().getSource(QueryData, syntax);

        String source = "";
        for(ObjectWhere where : this)
            source = (source.length()==0?"":source+" AND ") + where.getSource(QueryData, syntax);
        return source;
    }

    public String toString() {
        if(isEmpty())
            return "TRUE";

        if(size()==1)
            return iterator().next().toString();

        String source = "";
        for(ObjectWhere where : this)
            source = (source.length()==0?"":source+" AND ") + where.toString();
        return source;
    }

    public OuterWhere getOr() {
        return new OuterWhere(this);
    }

    // -------------------- Всякие другие методы ------------------------- //

    public <J extends Join> void fillJoins(List<J> joins) {
        for(ObjectWhere where : this)
            where.fillJoins(joins);
    }

    public void fillJoinWheres(MapWhere<JoinData> joins, IntraWhere inWhere) {
        // бежим по всем элементам добавляя остальные In'ы
        inWhere = inWhere.in(this);
        for(ObjectWhere where : this)
            where.fillJoinWheres(joins,inWhere);
    }

    public boolean getValue(List<DataWhere> trueWheres) {

        boolean result = true;
        for (ObjectWhere where : this) {
            result &= where.getValue(trueWheres);
        }

        return result;
    }

    public IntraWhere inNot(IntraWhere where) {
        return new OuterWhere(this).inNot(where);
    }

    public boolean means(IntraWhere where) {
        return new OuterWhere(this).means(where);
    }

    public void fillData(Collection<DataWhere> wheres) {
        for(ObjectWhere where : this)
            where.fillData(wheres);
    }

    public IntraWhere getJoinWhere() {
        IntraWhere result = new InnerWhere();
        for(ObjectWhere where : this)
           result = result.in(where.getJoinWhere());
        return result;
    }

    public IntraWhere followTrue(InnerWhere where) {
        return this;
    }

    public IntraWhere copy() {
        InnerWhere result = new InnerWhere();
        for(ObjectWhere where : this)
            result.add((ObjectWhere) where.copy());
        return result;
    }

    boolean markReadOnly = false;

    public boolean add(ObjectWhere objectWhere) {
        if(markReadOnly)
            throw new RuntimeException("Modified");
        return super.add(objectWhere);    //To change body of overridden methods use File | Settings | File Templates.
    }

    public boolean remove(Object o) {
        if(markReadOnly)
            throw new RuntimeException("Modified");
        return super.remove(o);    //To change body of overridden methods use File | Settings | File Templates.
    }

    public ObjectWhere remove(int index) {
        if(markReadOnly)
            throw new RuntimeException("Modified");
        return super.remove(index);    //To change body of overridden methods use File | Settings | File Templates.
    }

    public boolean equals(Object o) {
        return this==o;
    }

    public int hashCode() {
        return System.identityHashCode(this);
    }

    // для кэша
    public boolean equals(IntraWhere where, Map<ObjectExpr, ObjectExpr> mapExprs, Map<JoinWhere, JoinWhere> mapWheres) {
        return where instanceof InnerWhere && OuterWhere.equals(this,(InnerWhere)where, mapExprs, mapWheres);
    }

    public int hash() {
        return OuterWhere.hash(this,7);
    }
}

abstract class ObjectWhere implements IntraWhere {

    static String TRUE = "1=1";
    static String FALSE = "1<>1";

    abstract DataWhere getDataWhere();

    public IntraWhere followFalse(OuterWhere where) {
        return new InnerWhere(this).followFalse(where);
    }

    // ------------------- Реализация in ------------------------ //

    public IntraWhere in(IntraWhere where) {
        if(where instanceof ObjectWhere) {
            return new InnerWhere(this).in(new InnerWhere((ObjectWhere) where));
        } else
            return where.in(this);
    }

    // ------------------- Реализация вспомогательных  интерфейсов ------------------------ //

    public boolean isFalse() {
        return false;
    }

    public boolean isTrue() {
        return false;
    }

    // -------------------- Выходной интерфейс ------------------------- //

    public OuterWhere getOr() {
        return new OuterWhere(this);
    }

    public IntraWhere inNot(IntraWhere where) {
        return new InnerWhere(this).inNot(where);
    }

    public boolean means(IntraWhere where) {
        return new InnerWhere(this).means(where);
    }

    // -------------------- Всякие другие методы ------------------------- //

    public void fillJoinWheres(MapWhere<JoinData> joins, IntraWhere inWhere) {
        fillDataJoinWheres(joins,inWhere.in(this));
    }

    abstract protected void fillDataJoinWheres(MapWhere<JoinData> joins, IntraWhere inWhere);

    public IntraWhere followTrue(InnerWhere where) {
        return this;
    }

    public IntraWhere reverseNot() {
        return not();
    }
}

class NotDataWhere extends ObjectWhere {

    private DataWhere where;

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || !(o instanceof NotDataWhere)) return false;

        return where.equals(((NotDataWhere) o).where);
    }

    public int hashCode() {
        return where.hashCode();
    }

    NotDataWhere(DataWhere iwhere) {
        where = iwhere;
    }

    final static String PREFIX = "NOT ";

    // ------------------- Реализация not ------------------------ //

    public IntraWhere not() {
        return getDataWhere();
    }

    DataWhere getDataWhere() {
        return where;
    }

    // ------------------- Реализация translate ------------------------ //

    public IntraWhere translate(Translator translator) {
        IntraWhere translatedWhere = translator.translate(where);
        if(translatedWhere == where)
            return this;

        return translatedWhere.not();
    }

    // -------------------- Выходной интерфейс ------------------------- //

    public String getSource(Map<QueryData, String> queryData, SQLSyntax syntax) {
        return where.getNotSource(queryData, syntax);
    }

    public String toString() {
        return PREFIX + where.toString();
    }

    // -------------------- Всякие другие методы ------------------------- //

    public <J extends Join> void fillJoins(List<J> joins) {
        where.fillJoins(joins);
    }

    protected void fillDataJoinWheres(MapWhere<JoinData> joins, IntraWhere inWhere) {
        where.fillDataJoinWheres(joins, inWhere);
    }

    public boolean getValue(List<DataWhere> trueWheres) {
        return !where.getValue(trueWheres);
    }

    public void fillData(Collection<DataWhere> wheres) {
        where.fillData(wheres);
    }

    public IntraWhere getJoinWhere() {
        return where.getNotJoinWhere();
    }

    public IntraWhere copy() {
        return where.not();
    }

    // для кэша
    public boolean equals(IntraWhere equalWhere, Map<ObjectExpr, ObjectExpr> mapExprs, Map<JoinWhere, JoinWhere> mapWheres) {
        return equalWhere instanceof NotDataWhere && where.equals(((NotDataWhere)equalWhere).where, mapExprs, mapWheres) ;
    }

    public int hash() {
        return where.hash()*3;
    }
}

abstract class DataWhere extends ObjectWhere {

    DataWhere getDataWhere() {
        return this;
    }

    // ------------------- Реализация not ------------------------ //

    public IntraWhere not() {
        return new NotDataWhere(this);
    }

    String getNotSource(Map<QueryData, String> queryData, SQLSyntax syntax) {
        return NotDataWhere.PREFIX + getSource(queryData, syntax);
    }

    Map<DataWhere,Boolean> cacheFollow = new IdentityHashMap<DataWhere, Boolean>();
    boolean follow(DataWhere dataWhere) {
        if(!Main.ActivateCaches) return calculateFollow(dataWhere);
        Boolean Result = cacheFollow.get(dataWhere);
        if(Result==null) {
            Result = calculateFollow(dataWhere);
            cacheFollow.put(dataWhere,Result);
        }

        return Result;
    }
    abstract boolean calculateFollow(DataWhere dataWhere);

    public boolean getValue(List<DataWhere> trueWheres) {
        return trueWheres.contains(this);
    }

    public void fillData(Collection<DataWhere> wheres) {
        wheres.add(this);
    }

    public IntraWhere getJoinWhere() {
        return this;
    }

    public IntraWhere getNotJoinWhere() {
        return not();
    }
}
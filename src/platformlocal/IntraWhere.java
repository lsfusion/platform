package platformlocal;

import java.util.*;

interface AbstractWhere extends SourceJoin {

    String getSource(Map<QueryData, String> joinAlias, SQLSyntax syntax);
    void fillData(Collection<DataWhere> wheres);

    boolean getValue(List<DataWhere> trueWheres);
}

interface IntraWhere extends AbstractWhere {

    OuterWhere getOr();
    boolean isFalse();
    boolean isTrue();

    IntraWhere getJoinWhere();
}

abstract class OuterWhere<O extends OuterWhere<O,I,OR,IR>, I extends InnerWhere<O,I,OR,IR>, OR extends OuterWhere<OR,IR,O,I>, IR extends InnerWhere<OR,IR,O,I>> extends ArrayList<I> implements IntraWhere {

    abstract O getThis();

    public OuterWhere() {
    }

    public OuterWhere(I where) {
        super(Collections.singleton(where));
    }

    public OuterWhere(Set<? extends I> where) {
        super(where);
    }

    public OuterWhere(O where) {
        super(where);
    }

    abstract O createOuterWhere();
    abstract O createOuterWhere(I where);
    abstract O createOuterWhere(Set<I> where);
    abstract O createOuterWhere(O where);

    abstract OR createOuterReverseWhere();

    abstract I createInnerWhere();
    abstract I createInnerWhere(ObjectWhere where);
    abstract I createInnerWhere(Set<ObjectWhere> where);
    abstract I createInnerWhere(I where);

    abstract I[] createInnerWhereArray(int size);

/*    OuterWhere(ObjectWhere Where) {
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
    } */

    // ------------------- Интерфейс для and и or ---------------- //

    abstract O or (O where);
    abstract O and (O where);

    abstract O andNot (O where);

    // ------------------- Реализация out ------------------------ //

    void out(ObjectWhere where) {
        out(createInnerWhere(where));
    }

    void out(I where) {
        if (where.size() == 0) { clear(); add(where); return; }
        add(where);
    }

    void out(O where) {
        if (where.size() == 0) return;
        addAll(where);
    }

    // ------------------- Реализация in ------------------------ //

    O in(I where) {

        if (where.size() == 0) return createOuterWhere(getThis());

        O result = createOuterWhere();
        for (I innerWhere : this) {
            O inResult = innerWhere.in(where);
            result.out(inResult);
        }

        return result;
    }

    O in(O where) {

        if (where.size() == 0) return createOuterWhere();

        O result = createOuterWhere();
        for (I innerWhere : where)
            result.out(in(innerWhere) );

        return result;
    }

    public O in(ObjectWhere where) {
        return in(createInnerWhere(where));
    }

    // ------------------- Реализация not ------------------------ //

    public O not() {

        O result = createOuterWhere(createInnerWhere());
        for(I innerWhere : this)
            result = result.in(innerWhere.not());

        result.simplify();
        return result;
    }

    public OR reverseNot() {

        OR result = createOuterReverseWhere();
        for (I innerWhere : this) {
            result.out(innerWhere.reverseNot());
        }
        return result;
    }

    abstract O followFalse(O where);

    void simplifyFull() {
//        simplifyBuildDNF();
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
                I where1 = get(i1);
                if (keepSecond && i1 >= firstIndex && i1 < secondIndex) continue;
                for (int i2 = 0; i2 < size(); i2++)
                    if (i1 != i2 && !redundant[i2]) {
                        if (i1 < firstIndex) {
                            if (i2 < firstIndex) continue;
                        } else {
                            if (i1 < secondIndex && i2 >= firstIndex && i2 < secondIndex) continue;
                        }
                        I where2 = get(i2);
                        if (where1.follow(where2)) {
                            redundant[i1] = true;
                            redCount++;
                            break;
                        }
                    }
            }

        if (redCount == 0) return;

        I[] newWheres = createInnerWhereArray(size() - redCount);
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
                I where1 = get(i1);
                for (int i2 = i1+1; i2 < size(); i2++) {
                    if (!redundant.get(i2)) {
                        if (i1 < firstIndex) {
                            if (i2 < firstIndex) continue;
                        } else {
                            if (i1 < secondIndex && i2 >= firstIndex && i2 < secondIndex) continue;
                        }
                        I where2 = get(i2);
                        I concat = where1.concatenate(where2);
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

        I[] newWheres = createInnerWhereArray(size() - redCount);
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
        for (I where : this) {
            if (where.size() > maxSize) maxSize = where.size();
            // проверка, что .T.
            if (where.size() == 0) { clear(); add(where); return; }
        }

//        System.out.println("concat started " + size());

        ArrayList<I>[] wheres = new ArrayList[maxSize];
        int[] firstIndexes = new int[maxSize];
        int[] secondIndexes = new int[maxSize];

        for (int i = 0; i < maxSize; i++) {
            wheres[i] = new ArrayList();
        }

        for (int i = 0; i < size(); i++) {
            I where = get(i);
            int rowNum = where.size() - 1;

            Collection<I> rowWhere = wheres[rowNum];
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

            ArrayList<I> rowWhere = wheres[whereSize];
            for (int i1 = 0; i1 < rowWhere.size(); i1++) {
                I where1 = rowWhere.get(i1);
                for (int i2 = i1+1; i2 < rowWhere.size(); i2++) {

                    if (i1 < firstIndexes[whereSize]) {
                        if (i2 < firstIndexes[whereSize]) continue;
                    } else {
                        if (i1 < secondIndexes[whereSize] && i2 >= firstIndexes[whereSize] && i2 < secondIndexes[whereSize]) continue;
                    }

                    I where2 = rowWhere.get(i2);
                    I concat = where1.concatenate(where2);
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
                I where1 = get(i1);
                if (where1.size() == 1 || where1.size() == 2) {
                    for (int i2 = 0; i2 < size(); i2++) {
                        if (!redundant.get(i2)) {
                            I where2 = get(i2);
                            if (where1 != where2 && !BaseUtils.findByReference(secondaryWheres, where2)) {

                                if (where1.size() == 1) {
                                    I congl = where1.conglutinate(where2);
                                    if (congl != null) {
                                        redundant.set(i2, true);
                                        redCount++;
                                        if (!contains(congl)) { add(congl); redundant.add(false); }
                                        continue;
                                    }
                                }

                                if (where1.size() == 2) {
                                    for (int i3 = 0; i3 < size(); i3++) {
                                        I where3 = get(i3);
                                        if (!redundant.get(i3) && where3 != where1 && where3 != where2) {
                                            I congl2 = where1.conglutinate(where2, where3);
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

        I[] newWheres = createInnerWhereArray(size() - redCount);
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

        for (I where : this) {

            Set<DataWhere> dataWheres = new HashSet(allDataWheres);
            for (ObjectWhere objWhere : where)
                dataWheres.remove(objWhere.getDataWhere());

            Collection<I> curWheres = Collections.singleton(createInnerWhere(where)); // надо именно так делать, иначе цикл будет менять where

            for (ObjectWhere objWhere : dataWheres) {

                Collection<I> newWheres = new ArrayList();

                for (I curWhere : curWheres) {

                    boolean follows = false, followed = false;
                    for (ObjectWhere objCurWhere : curWhere) {
                        if (objCurWhere instanceof DataWhere && curWhere.follow(objCurWhere, objWhere))
                            follows = true;
                        if (objCurWhere instanceof NotDataWhere && curWhere.follow(objWhere, objCurWhere))
                            followed = true;
                        if (follows && followed) break;
                    }

                    if (!follows) {
                        I newWhere = createInnerWhere(curWhere);
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
            add(createInnerWhere(inWhere));
        }

//        System.out.println("bdnf ended " + size());
    }

    void simplifyInWheres() {

        for (InnerWhere where : this)
            where.simplify();
    }

    abstract boolean isMainValue(boolean value);

    void simplifyMatrix() {

        Set<DataWhere> allDataWheres = new HashSet();
        fillData(allDataWheres);

//        System.out.println("matrix started " + size() + " : " + allDataWheres.size());

        Collection<List<DataWhere>> values = buildAllDataWheres(allDataWheres);

        Collection<List<DataWhere>> trueValues = new ArrayList();
        for (List<DataWhere> value : values)
            if (isMainValue(getValue(value)))
                trueValues.add(value);

        Collection<I> bestWheres = calculateMinimumInWheres(trueValues, 0, new ArrayList(), new HashSet());

        if (bestWheres.size() < size()) {
            clear();
            addAll(bestWheres);
        }

//        System.out.println("matrix ended " + size());
    }

    private Collection<I> calculateMinimumInWheres(Collection<List<DataWhere>> values, int index, Collection<InnerWhere> currentWheres, Set<List<DataWhere>> currentValues) {

        if (currentValues.size() >= values.size()) return new ArrayList(currentWheres);
        if (index == size()) return null;

        Collection<I> bestWhere = calculateMinimumInWheres(values, index+1, currentWheres, currentValues);

        InnerWhere currentWhere = get(index);
        Set<List<DataWhere>> newValues = new HashSet(currentValues);
        for (List<DataWhere> value : values) {
            if (isMainValue(currentWhere.getValue(value)))
                newValues.add(value);
        }

        currentWheres.add(currentWhere);
        Collection<I> resultWhere = calculateMinimumInWheres(values, index+1, currentWheres, newValues);
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

    // -------------------- Выходной интерфейс ------------------------- //

    abstract String getEmptySource();
    abstract String getOperandSource();

    public String getSource(Map<QueryData, String> QueryData, SQLSyntax syntax) {

        if(isEmpty())
            return getEmptySource();

        if(size()==1)
            return iterator().next().getSource(QueryData, syntax);

        String source = "";
        for(InnerWhere where : this)
            source = (source.length()==0 ? "" : source + " " + getOperandSource() + " ") + "(" + where.getSource(QueryData, syntax) + ")";
        return source;
    }

    abstract String getEmptyString();

    public String toString() {
        if(isEmpty())
            return getEmptySource();

        if(size()==1)
            return iterator().next().toString();

        String source = "";
        for(InnerWhere where : this)
            source = (source.length()==0 ? "" : source + " " + getOperandSource() + " ") + "(" + where.toString() + ")";
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

    public void fillJoinWheres(MapWhere<JoinData> joins, OuterWhere inWhere) {
        for(InnerWhere where : this)
            where.fillJoinWheres(joins, inWhere);
    }

    public void fillData(Collection<DataWhere> wheres) {
        for(InnerWhere where : this)
            where.fillData(wheres);
    }

    public IntraWhere getJoinWhere() {
        O result = createOuterWhere();
        for(I where : this)
            result.out(where.getJoinWhere());
        return result;
    }

}

abstract class InnerWhere<O extends OuterWhere<O,I,OR,IR>, I extends InnerWhere<O,I,OR,IR>, OR extends OuterWhere<OR,IR,O,I>, IR extends InnerWhere<OR,IR,O,I>> extends ArrayList<ObjectWhere> implements IntraWhere {

    abstract I getThis();

    // ------------------- конструкторы ------------------------ //

    InnerWhere() {
    }

    InnerWhere(ObjectWhere where) {
        super(Collections.singleton(where));
    }

    InnerWhere(InnerWhere where) {
        super(where);
    }

    public InnerWhere(Set<ObjectWhere> where) {
        super(where);
    }

    abstract O createOuterWhere();
    abstract O createOuterWhere(I where);

    abstract I createInnerWhere();
    abstract I createInnerWhere(ObjectWhere where);
    abstract I createInnerWhere(I where);

    abstract IR createInnerReverseWhere();

    abstract boolean follow(ObjectWhere whereFrom, ObjectWhere whereTo);

    boolean follow(I where) {

        for (ObjectWhere objWhereTo : where) {

            boolean follows = false;
            for (ObjectWhere objWhereFrom : this) {
                if ((objWhereTo instanceof DataWhere && objWhereFrom instanceof DataWhere && follow(objWhereFrom, objWhereTo)) ||
                    (objWhereTo instanceof NotDataWhere && objWhereFrom instanceof NotDataWhere && follow(objWhereTo, objWhereFrom))) {
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

    I concatenate(I where) {

        if (where.size() != size()) return null;

        ObjectWhere objWhereFrom = getDifferentObjectWhere(getThis(), where);
        if (objWhereFrom == null) return null;

        ObjectWhere objWhereTo = getDifferentObjectWhere(where, getThis());
        if (objWhereTo == null) return null;

        if (objWhereFrom instanceof NotDataWhere && objWhereTo instanceof DataWhere) {
            if (!follow(objWhereFrom, objWhereTo)) return null;
        } else {
            if (objWhereFrom instanceof DataWhere && objWhereTo instanceof NotDataWhere) {
                if (!follow(objWhereTo, objWhereFrom)) return null;
            } else return null;
        }

        I concat = createInnerWhere();
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

    private ObjectWhere getDifferentObjectWhere(I whereFrom, I whereTo) {

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

    I conglutinate(I where) {

        if (size() != 1) return null;

        ObjectWhere objWhereC = get(0);

        for (ObjectWhere objWhere : where) {
            if (( objWhere instanceof NotDataWhere && objWhereC instanceof DataWhere && follow(objWhere, objWhereC) ) ||
                ( objWhere instanceof DataWhere && objWhereC instanceof NotDataWhere && follow(objWhereC, objWhere) )) {
                I result = createInnerWhere(where);
                result.remove(objWhere);
                return result;
            }
        }

        return null;
    }

    I conglutinate(I where1, I where2) {

        if (size() != 2) return null;
        if (where1.size() != where2.size()) return null;

        ObjectWhere objWhereC1 = get(0);
        ObjectWhere objWhereC2 = get(1);

        for (ObjectWhere objWhere1 : where1) {
            if (( objWhere1 instanceof NotDataWhere && objWhereC1 instanceof DataWhere && follow(objWhere1, objWhereC1) ) ||
                ( objWhere1 instanceof DataWhere && objWhereC1 instanceof NotDataWhere && follow(objWhereC1 , objWhere1) )) {

                I result = createInnerWhere(where1);
                result.remove(objWhere1);

                for (ObjectWhere objWhere2 : where2) {
                    if (( objWhere2 instanceof NotDataWhere && objWhereC2 instanceof DataWhere && follow(objWhere2, objWhereC2) ) ||
                        ( objWhere2 instanceof DataWhere && objWhereC2 instanceof NotDataWhere && follow(objWhereC2, objWhere2) )) {

                        if (where2.containsAll(result) && !result.contains(objWhere2))
                            return result;
                    }
                }

            }
        }

        return null;
    }

    // ------------------- Реализация in ------------------------ //

    public I inI(ObjectWhere where) {
        return inI(createInnerWhere(where));
    }

    public I inI(I where) {

        I result = createInnerWhere(getThis());

        for (ObjectWhere whereNew : where) {

            boolean redundant = false;
            for (ObjectWhere whereOld : this) {
                if (follow(whereNew, whereOld)) {
                    if (whereNew instanceof DataWhere && whereOld instanceof NotDataWhere)
                        return null; // невыполнимость
                    if (whereNew instanceof DataWhere && whereOld instanceof DataWhere) {
                        result.remove(whereOld);
                        continue;
                    }
                    if (whereNew instanceof NotDataWhere && whereOld instanceof NotDataWhere) {
                        redundant = true;
                        continue;
                    }
                }
                if (follow(whereOld, whereNew)) {
                    if (whereOld instanceof DataWhere && whereNew instanceof NotDataWhere)
                        return null; // невыполнимость
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

    public O in(I where) {

        I result = inI(where);
        if (result == null) return createOuterWhere();

        return createOuterWhere(result);
    }

    public O in(O where) {
        return where.in(getThis());
    }

    public O in(ObjectWhere where) {
        return in(createInnerWhere(where));
    }

    // ------------------- Реализация not ------------------------ //

    public O not() {

        O result = createOuterWhere();
        for(ObjectWhere where : this)
            result.out(where.not());
        return result;
    }

    public IR reverseNot() {

        IR result = createInnerReverseWhere();
        for(ObjectWhere where : this)
            result.in(where.not());
        return result;
    }

    public void simplify() {

        Set<ObjectWhere> redundant = new HashSet();
        for (ObjectWhere whereFrom : this)
            if (!redundant.contains(whereFrom))
                for (ObjectWhere whereTo : this)
                    if (whereFrom != whereTo && !redundant.contains(whereTo) && follow(whereFrom, whereTo)) {
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

    // -------------------- Выходной интерфейс ------------------------- //

    abstract String getEmptySource();
    abstract String getOperandSource();

    public String getSource(Map<QueryData, String> QueryData, SQLSyntax syntax) {

        if(isEmpty())
            return getEmptySource();

        if(size()==1)
            return iterator().next().getSource(QueryData, syntax);

        String source = "";
        for(ObjectWhere where : this)
            source = (source.length() == 0 ? "" : source + " " + getOperandSource() + " ") + where.getSource(QueryData, syntax);
        return source;
    }

    abstract String getEmptyString();

    public String toString() {

        if(isEmpty())
            return getEmptySource();

        if(size()==1)
            return iterator().next().toString();

        String source = "";
        for(ObjectWhere where : this)
            source = (source.length() == 0 ? "" : source + " " + getOperandSource() + " ") + where.toString();
        return source;
    }

    public OuterWhere getOr() {
        return createOuterWhere(getThis());
    }

    // -------------------- Всякие другие методы ------------------------- //

    public <J extends Join> void fillJoins(List<J> joins) {
        for(ObjectWhere where : this)
            where.fillJoins(joins);
    }

    public void fillJoinWheres(MapWhere<JoinData> joins, OuterWhere inWhere) {
        // бежим по всем элементам добавляя остальные In'ы
        inWhere = inWhere.in(getThis());
        for(ObjectWhere where : this)
            where.fillJoinWheres(joins,inWhere);
    }

    public void fillData(Collection<DataWhere> wheres) {
        for(ObjectWhere where : this)
            where.fillData(wheres);
    }

    public O getJoinWhere() {
        O result = createOuterWhere(createInnerWhere());
        for(ObjectWhere where : this)
           result = result.in(where.getDataWhere());
        return result;
    }

}

abstract class ObjectWhere implements AbstractWhere {

    static String TRUE = "1=1";
    static String FALSE = "1<>1";

    abstract DataWhere getDataWhere();

    abstract ObjectWhere not();

/*    public IntraWhere followFalse(OuterWhere where) {
        return new InnerWhere(this).followFalse(where);
    }

    public IntraWhere followTrue(InnerWhere where) {
        return this;
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

    public IntraWhere reverseNot() {
        return not();
    }
    */

    // -------------------- Всякие другие методы ------------------------- //

    public void fillJoinWheres(MapWhere<JoinData> joins, OuterWhere inWhere) {
        fillDataJoinWheres(joins,inWhere.in(this));
    }

    abstract protected void fillDataJoinWheres(MapWhere<JoinData> joins, IntraWhere inWhere);

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

    public ObjectWhere not() {
        return getDataWhere();
    }

    DataWhere getDataWhere() {
        return where;
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

}

abstract class DataWhere extends ObjectWhere
                         implements Where {

    public DNFWhere getDNFWhere() {
        return new DNFWhere(new ConjunctWhere(this));
    }

    public CNFWhere getCNFWhere() {
        return new CNFWhere(new DisjunctWhere(this));
    }

    public boolean means(Where where) {
        return getDNFWhere().means(where.getDNFWhere());
    }

    DataWhere getDataWhere() {
        return this;
    }

    // ------------------- Реализация not ------------------------ //

    public ObjectWhere not() {
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

}

class ConjunctWhere extends InnerWhere<DNFWhere, ConjunctWhere, CNFWhere, DisjunctWhere> {

    ConjunctWhere getThis() {
        return this;
    }

    ConjunctWhere() {
        super();
    }

    ConjunctWhere(ObjectWhere where) {
        super(where);
    }

    ConjunctWhere(ConjunctWhere where) {
        super(where);
    }

    ConjunctWhere(Set<ObjectWhere> where) {
        super(where);
    }

    DNFWhere createOuterWhere() {
        return new DNFWhere();
    }

    DNFWhere createOuterWhere(ConjunctWhere where) {
        return new DNFWhere(where);
    }

    ConjunctWhere createInnerWhere() {
        return new ConjunctWhere();
    }

    ConjunctWhere createInnerWhere(ObjectWhere where) {
        return new ConjunctWhere(where);
    }

    ConjunctWhere createInnerWhere(ConjunctWhere where) {
        return new ConjunctWhere(where);
    }

    DisjunctWhere createInnerReverseWhere() {
        return new DisjunctWhere();
    }

    // ------------------- Реализация вспомогательных  интерфейсов ------------------------ //

    public boolean isFalse() {
        return false;
    }

    public boolean isTrue() {
        return isEmpty();
    }

    public boolean getValue(List<DataWhere> trueWheres) {

        boolean result = true;
        for (ObjectWhere where : this) {
            result &= where.getValue(trueWheres);
        }

        return result;
    }

    boolean follow(ObjectWhere whereFrom, ObjectWhere whereTo) {
        return whereFrom.getDataWhere().follow(whereTo.getDataWhere());
    }

    String getEmptySource() {
        return "1=1";
    }

    String getOperandSource() {
        return "AND";
    }

    String getEmptyString() {
        return "TRUE";
    }
}

class DisjunctWhere extends InnerWhere<CNFWhere, DisjunctWhere, DNFWhere, ConjunctWhere> {

    DisjunctWhere getThis() {
        return this;
    }

    public DisjunctWhere() {
        super();
    }

    public DisjunctWhere(ObjectWhere where) {
        super(where);
    }

    public DisjunctWhere(DisjunctWhere where) {
        super(where);
    }

    public DisjunctWhere(Set<ObjectWhere> where) {
        super(where);
    }

    CNFWhere createOuterWhere() {
        return new CNFWhere();
    }

    CNFWhere createOuterWhere(DisjunctWhere where) {
        return new CNFWhere(where);
    }

    DisjunctWhere createInnerWhere() {
        return new DisjunctWhere();
    }

    DisjunctWhere createInnerWhere(ObjectWhere where) {
        return new DisjunctWhere(where);
    }

    DisjunctWhere createInnerWhere(DisjunctWhere where) {
        return new DisjunctWhere(where);
    }

    ConjunctWhere createInnerReverseWhere() {
        return new ConjunctWhere();
    }

    public boolean isFalse() {
        return isEmpty();
    }

    public boolean isTrue() {
        return false;
    }

    public boolean getValue(List<DataWhere> trueWheres) {

        boolean result = false;
        for (ObjectWhere where : this) {
            result |= where.getValue(trueWheres);
        }

        return result;
    }

    boolean follow(ObjectWhere whereFrom, ObjectWhere whereTo) {
        return whereTo.getDataWhere().follow(whereFrom.getDataWhere());
    }

    String getEmptySource() {
        return "1<>1";
    }

    String getOperandSource() {
        return "OR";
    }

    String getEmptyString() {
        return "FALSE";
    }
}

class DNFWhere extends OuterWhere<DNFWhere, ConjunctWhere, CNFWhere, DisjunctWhere> {

    DNFWhere getThis() {
        return this;
    }

    DNFWhere() {
        super();
    }

    DNFWhere(ConjunctWhere where) {
        super(where);
    }

    DNFWhere(Set<ConjunctWhere> where) {
        super(where);
    }

    DNFWhere(DNFWhere where) {
        super(where);
    }

    DNFWhere createOuterWhere() {
        return new DNFWhere();
    }

    DNFWhere createOuterWhere(ConjunctWhere where) {
        return new DNFWhere(where);
    }

    DNFWhere createOuterWhere(Set<ConjunctWhere> where) {
        return new DNFWhere(where);
    }

    DNFWhere createOuterWhere(DNFWhere where) {
        return new DNFWhere(where);
    }

    CNFWhere createOuterReverseWhere() {
        return new CNFWhere();
    }

    ConjunctWhere createInnerWhere() {
        return new ConjunctWhere();
    }

    ConjunctWhere createInnerWhere(ObjectWhere where) {
        return new ConjunctWhere(where);
    }

    ConjunctWhere createInnerWhere(Set<ObjectWhere> where) {
        return new ConjunctWhere(where);
    }

    ConjunctWhere createInnerWhere(ConjunctWhere where) {
        return new ConjunctWhere(where);
    }

    ConjunctWhere[] createInnerWhereArray(int size) {
        return new ConjunctWhere[size];
    }

    public boolean isFalse() {
        return isEmpty();
    }

    public boolean isTrue() {
        return (size()==1 && iterator().next().isTrue());
    }

    public boolean getValue(List<DataWhere> trueWheres) {

        boolean result = false;
        for (InnerWhere where : this) {
            result |= where.getValue(trueWheres);
        }

        return result;
    }

    boolean isMainValue(boolean value) {
        return value;
    }

    String getEmptySource() {
        return "1<>1";
    }

    String getOperandSource() {
        return "OR";
    }

    String getEmptyString() {
        return "FALSE";
    }

    DNFWhere or(DNFWhere where) {

        int firstSize = size();

        DNFWhere result = new DNFWhere(this);
        result.out(where);

        result.simplify(firstSize, size(), false);

        return result;
    }

    DNFWhere and(DNFWhere where) {

        DNFWhere result = in(where);
        result.simplify();
        return result;
    }

    public DNFWhere andNot(DNFWhere where) {

        if (where.size() == 0) return createOuterWhere(getThis());

        Set<ConjunctWhere> resultSet = new HashSet();
        for (ConjunctWhere whereFrom : this) {
            andNot(createInnerWhere(whereFrom), where, 0, resultSet);
        }

        DNFWhere resultWhere = createOuterWhere(resultSet);
        // если не делать removeRedundant до их будет вообще дофига
        resultWhere.simplifyRemoveRedundant();
        resultWhere.simplify();
        return resultWhere;
    }

    private void andNot(ConjunctWhere whereFrom, DNFWhere where, int index, Set<ConjunctWhere> resultWhere) {

        if (index >= where.size()) {
            resultWhere.add(whereFrom);
            return;
        }

        ConjunctWhere whereTo = where.get(index);
        for (ObjectWhere objWhere : whereTo) {
            ConjunctWhere newWhere = whereFrom.inI(objWhere.not());
            if (newWhere == null) continue;
            andNot(newWhere, where, index+1, resultWhere);
        }
    }

    public boolean means(DNFWhere where) {

        if(where.size() == 0) return isFalse();

        for (ConjunctWhere whereFrom : this) {
            if (meansNot(createInnerWhere(whereFrom), where, 0)) return false;
        }

        return true;
    }

    private boolean meansNot(ConjunctWhere whereFrom, DNFWhere where, int index) {

        if (index >= where.size()) return !whereFrom.isFalse();

        ConjunctWhere whereTo = where.get(index);
        for (ObjectWhere objWhere : whereTo) {
            ConjunctWhere newWhere = whereFrom.inI(objWhere.not());
            if (newWhere == null) continue;
            if (meansNot(newWhere, where, index+1)) return true;
        }

        return false;
    }

    public DNFWhere followFalse(DNFWhere where) {

        int firstSize = size();

        DNFWhere dnfWhere = createOuterWhere(getThis());
        dnfWhere.out(where);
        dnfWhere.simplify(firstSize, dnfWhere.size(), true);

        for (ConjunctWhere conjWhere : where)
            dnfWhere.remove(conjWhere);

        return dnfWhere;
    }

    public DNFWhere followTrue(DNFWhere where) {
        return this;
    }

}

class CNFWhere extends OuterWhere<CNFWhere, DisjunctWhere, DNFWhere, ConjunctWhere> {

    CNFWhere getThis() {
        return this;
    }

    public CNFWhere() {
    }

    public CNFWhere(DisjunctWhere where) {
        super(where);
    }

    public CNFWhere(Set<DisjunctWhere> where) {
        super(where);
    }

    public CNFWhere(CNFWhere where) {
        super(where);
    }

    CNFWhere createOuterWhere() {
        return new CNFWhere();
    }

    CNFWhere createOuterWhere(DisjunctWhere where) {
        return new CNFWhere(where);
    }

    CNFWhere createOuterWhere(Set<DisjunctWhere> where) {
        return new CNFWhere(where);
    }

    CNFWhere createOuterWhere(CNFWhere where) {
        return new CNFWhere(where);
    }

    DNFWhere createOuterReverseWhere() {
        return new DNFWhere();
    }

    DisjunctWhere createInnerWhere() {
        return new DisjunctWhere();
    }

    DisjunctWhere createInnerWhere(ObjectWhere where) {
        return new DisjunctWhere(where);
    }

    DisjunctWhere createInnerWhere(Set<ObjectWhere> where) {
        return new DisjunctWhere(where);
    }

    DisjunctWhere createInnerWhere(DisjunctWhere where) {
        return new DisjunctWhere(where);
    }

    DisjunctWhere[] createInnerWhereArray(int size) {
        return new DisjunctWhere[size];
    }

    public boolean isFalse() {
        return (size()==1 && iterator().next().isTrue());
    }

    public boolean isTrue() {
        return isEmpty();
    }

    public boolean getValue(List<DataWhere> trueWheres) {

        boolean result = true;
        for (InnerWhere where : this) {
            result &= where.getValue(trueWheres);
        }

        return result;
    }

    boolean isMainValue(boolean value) {
        return !value;
    }

    String getEmptySource() {
        return "1=1";
    }

    String getOperandSource() {
        return "AND";
    }

    String getEmptyString() {
        return "TRUE";
    }

    CNFWhere or(CNFWhere where) {

        CNFWhere result = in(where);
        result.simplify();
        return result;
    }

    CNFWhere and(CNFWhere where) {

        int firstSize = size();

        CNFWhere result = new CNFWhere(this);
        result.out(where);

        result.simplify(firstSize, size(), false);

        return result;
    }

    CNFWhere andNot(CNFWhere where) {
        return and(where.not());
    }

    boolean means(CNFWhere where) {
        return not().or(where).isTrue();
    }

    public CNFWhere followFalse(CNFWhere where) {
        return this;
    }

    public CNFWhere followTrue(CNFWhere where) {

        int firstSize = size();
        CNFWhere cnfWhere = createOuterWhere(getThis());
        cnfWhere.out(where);
        cnfWhere.simplify(firstSize, cnfWhere.size(), true);

        for (DisjunctWhere conjWhere : where)
            cnfWhere.remove(conjWhere);

        return cnfWhere;
    }
}
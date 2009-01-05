package platformlocal;
import java.util.*;

interface Where extends SourceJoin {

    Where and(Where where);
    Where not();

    Where followFalse(OrWhere where);
    Where translate(Translator translator);

    boolean isFalse();
    boolean isTrue();

    String getSource(Map<QueryData, String> joinAlias, SQLSyntax syntax);
    OrWhere getOr();

    boolean getValue(List<DataWhere> trueWheres);

    Where andNot(Where where);
    boolean means(Where where);

    void fillData(Collection<DataWhere> wheres);

    Where getJoinWhere();

    Where followTrue(AndWhere where);

    Where reverseNot();

    Where copy();

    // для кэша
    boolean equals(Where where, Map<ObjectExpr, ObjectExpr> mapExprs, Map<JoinWhere, JoinWhere> mapWheres);

    int hash();
}

class OrWhere extends ArrayList<AndWhere> implements Where {

    // ------------------- конструкторы ------------------------ //

    OrWhere() {
    }

    OrWhere(ObjectWhere Where) {
        this(new AndWhere(Where));
    }

    OrWhere(AndWhere where) {
        super(Collections.singleton(where));
    }

    OrWhere(ArrayList<AndWhere> iNodes) {
        super(iNodes);
    }

    OrWhere(Set<AndWhere> iNodes) {
        super(iNodes);
    }

    // ------------------- Реализация or ------------------------ //

    void or(Where where) {

        int firstSize = size();

        if(where instanceof AndWhere) {
            if (((AndWhere)where).size() == 0) { clear(); add((AndWhere)where); return; }
            or((AndWhere)where);
        } else
        if(where instanceof OrWhere) {
            if (((OrWhere)where).size() == 0) return;
            or((OrWhere)where);
        } else
            or(new AndWhere((ObjectWhere) where));

        simplify(firstSize, size(), false);
    }

    private void or(AndWhere where) {
        add(where);
    }

    private void or(OrWhere where) {
        addAll(where);
    }

    // ------------------- Реализация and ------------------------ //

    public Where and(Where where) {

        OrWhere result;
        if(where instanceof AndWhere) {
            if (((AndWhere)where).size() == 0) return new OrWhere(this);
            result = and((AndWhere)where);
        } else
        if(where instanceof OrWhere) {
            if (((OrWhere)where).size() == 0) return new OrWhere(); 
            result = and((OrWhere)where);
        } else
            result = and(new AndWhere((ObjectWhere)where));

        result.simplify();

        return result;
    }

    private OrWhere and(OrWhere where) {

        OrWhere result = new OrWhere();
        for (AndWhere andWhere : where)
            result.or(and(andWhere) );

        return result;
    }

    private OrWhere and(AndWhere where) {

        OrWhere result = new OrWhere();
        for (AndWhere andWhere : this) {
            Where andResult = andWhere.and(where);
            if (andResult instanceof AndWhere)
                result.or((AndWhere)andResult);
        }

        return result;
    }

    // ------------------- Реализация not ------------------------ //

    public Where not() {

        OrWhere result = new OrWhere(new AndWhere());
        for(AndWhere andWhere : this)
            result = result.and(andWhere.not());

        result.simplify();
        return result;
    }

    public Where followFalse(OrWhere where) {

        int firstSize = size();
        OrWhere orWhere = new OrWhere(this);
        orWhere.or(where);
        orWhere.simplify(firstSize, orWhere.size(), true);

        for (AndWhere andWhere : where)
            orWhere.remove(andWhere);

//        orWhere.simplify();
        return orWhere;
    }

    void simplifyFull() {
        simplifyBuildDNF();
        simplifyConcatenate();
        simplifyRemoveRedundant();
        simplifyAndWheres();
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
                AndWhere where1 = get(i1);
                if (keepSecond && i1 >= firstIndex && i1 < secondIndex) continue;
                for (int i2 = 0; i2 < size(); i2++)
                    if (i1 != i2 && !redundant[i2]) {
                        if (i1 < firstIndex) {
                            if (i2 < firstIndex) continue;
                        } else {
                            if (i1 < secondIndex && i2 >= firstIndex && i2 < secondIndex) continue;
                        }
                        AndWhere where2 = get(i2);
                        if (where1.follow(where2)) {
                            redundant[i1] = true;
                            redCount++;
                            break;
                        }
                    }
            }

        if (redCount == 0) return;

        AndWhere[] newWheres = new AndWhere[size() - redCount];
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
                AndWhere where1 = get(i1);
                for (int i2 = i1+1; i2 < size(); i2++) {
                    if (!redundant.get(i2)) {
                        if (i1 < firstIndex) {
                            if (i2 < firstIndex) continue;
                        } else {
                            if (i1 < secondIndex && i2 >= firstIndex && i2 < secondIndex) continue;
                        }
                        AndWhere where2 = get(i2);
                        AndWhere concat = where1.concatenate(where2);
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

        AndWhere[] newWheres = new AndWhere[size() - redCount];
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
        for (AndWhere where : this) {
            if (where.size() > maxSize) maxSize = where.size();
            // проверка, что .T.
            if (where.size() == 0) { clear(); add(where); return; }
        }

//        System.out.println("concat started " + size());

        ArrayList<AndWhere>[] wheres = new ArrayList[maxSize];
        int[] firstIndexes = new int[maxSize];
        int[] secondIndexes = new int[maxSize];

        for (int i = 0; i < maxSize; i++) {
            wheres[i] = new ArrayList();
        }

        for (int i = 0; i < size(); i++) {
            AndWhere where = get(i);
            int rowNum = where.size() - 1;

            Collection<AndWhere> rowWhere = wheres[rowNum];
            rowWhere.add(where);

            if (i < firstIndex) {
                firstIndexes[rowNum]++;
                secondIndexes[rowNum]++;
            } else {
                if (i < secondIndex)
                    secondIndexes[rowNum]++;
            }

        }

        Set<AndWhere> wasWheres = new HashSet(this);

        for (int whereSize = maxSize-1; whereSize >= 0; whereSize--) {

            ArrayList<AndWhere> rowWhere = wheres[whereSize];
            for (int i1 = 0; i1 < rowWhere.size(); i1++) {
                AndWhere where1 = rowWhere.get(i1);
                for (int i2 = i1+1; i2 < rowWhere.size(); i2++) {

                    if (i1 < firstIndexes[whereSize]) {
                        if (i2 < firstIndexes[whereSize]) continue;
                    } else {
                        if (i1 < secondIndexes[whereSize] && i2 >= firstIndexes[whereSize] && i2 < secondIndexes[whereSize]) continue;
                    }

                    AndWhere where2 = rowWhere.get(i2);
                    AndWhere concat = where1.concatenate(where2);
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

    boolean simplifyConglutinate(Collection<AndWhere> secondaryWheres) {

        int startSize = size();
//        System.out.println("conglut started " + size());

        List<Boolean> redundant = new ArrayList(size());
        for (int i = 0; i < size(); i++)
            redundant.add(false);
        
        int redCount = 0;

        for (int i1 = 0; i1 < size(); i1++) {
            if (!redundant.get(i1)) {
                AndWhere where1 = get(i1);
                if (where1.size() == 1 || where1.size() == 2) {
                    for (int i2 = 0; i2 < size(); i2++) {
                        if (!redundant.get(i2)) {
                            AndWhere where2 = get(i2);
                            if (where1 != where2 && !BaseUtils.findByReference(secondaryWheres, where2)) {

                                if (where1.size() == 1) {
                                    AndWhere congl = where1.conglutinate(where2);
                                    if (congl != null) {
                                        redundant.set(i2, true);
                                        redCount++;
                                        if (!contains(congl)) { add(congl); redundant.add(false); }
                                        continue;
                                    }
                                }

                                if (where1.size() == 2) {
                                    for (int i3 = 0; i3 < size(); i3++) {
                                        AndWhere where3 = get(i3);
                                        if (!redundant.get(i3) && where3 != where1 && where3 != where2) {
                                            AndWhere congl2 = where1.conglutinate(where2, where3);
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

        AndWhere[] newWheres = new AndWhere[size() - redCount];
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

        for (AndWhere where : this) {

            Set<DataWhere> dataWheres = new HashSet(allDataWheres);
            for (ObjectWhere objWhere : where)
                dataWheres.remove(objWhere.getDataWhere());

            Collection<AndWhere> curWheres = Collections.singleton(new AndWhere(where)); // надо именно так делать, иначе цикл будет менять where

            for (ObjectWhere objWhere : dataWheres) {

                Collection<AndWhere> newWheres = new ArrayList();

                for (AndWhere curWhere : curWheres) {

                    boolean follows = false, followed = false;
                    for (ObjectWhere objCurWhere : curWhere) {
                        if (objCurWhere instanceof DataWhere && objCurWhere.getDataWhere().follow(objWhere.getDataWhere()))
                            follows = true;
                        if (objCurWhere instanceof NotWhere && objWhere.getDataWhere().follow(objCurWhere.getDataWhere()))
                            followed = true;
                        if (follows && followed) break;
                    }

                    if (!follows) {
                        AndWhere newWhere = new AndWhere(curWhere);
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

            for (AndWhere curWhere : curWheres) {
                resultWhere.add(new HashSet(curWhere));
            }
        }

        clear();

        for (Set<ObjectWhere> andWhere : resultWhere) {
            add(new AndWhere(andWhere));
        }

//        System.out.println("bdnf ended " + size());
    }

    void simplifyAndWheres() {

        for (AndWhere where : this)
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

        Collection<AndWhere> bestWheres = calculateMinimumAndWheres(trueValues, 0, new ArrayList(), new HashSet());

        if (bestWheres.size() < size()) {
            clear();
            addAll(bestWheres);
        }

//        System.out.println("matrix ended " + size());
    }

    private Collection<AndWhere> calculateMinimumAndWheres(Collection<List<DataWhere>> values, int index, Collection<AndWhere> currentWheres, Set<List<DataWhere>> currentValues) {

        if (currentValues.size() >= values.size()) return new ArrayList(currentWheres);
        if (index == size()) return null;

        Collection<AndWhere> bestWhere = calculateMinimumAndWheres(values, index+1, currentWheres, currentValues);

        AndWhere currentWhere = get(index);
        Set<List<DataWhere>> newValues = new HashSet(currentValues);
        for (List<DataWhere> value : values) {
            if (currentWhere.getValue(value))
                newValues.add(value);
        }

        currentWheres.add(currentWhere);
        Collection<AndWhere> resultWhere = calculateMinimumAndWheres(values, index+1, currentWheres, newValues);
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

    public Where translate(Translator translator) {
        // сначала транслируем AndWhere
        Collection<Where> transWheres = new ArrayList<Where>();
        boolean changedWheres = false;
        for(AndWhere where : this) {
            Where transWhere = translator.translate(where);
            transWheres.add(transWhere);
            changedWheres = changedWheres || (transWhere!=where);
        }

        if(!changedWheres)
            return this;

        OrWhere transOr = new OrWhere();
        for(Where where : transWheres)
            transOr.or(where);
        return transOr;
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
        for(AndWhere where : this)
            source = (source.length()==0?"":source+" OR ") + "(" + where.getSource(QueryData, syntax) + ")";
        return source;
    }

    public String toString() {
        if(isEmpty())
            return "FALSE";

        if(size()==1)
            return iterator().next().toString();

        String source = "";
        for(AndWhere where : this)
            source = (source.length()==0?"":source+" OR ") + "(" + where.toString() + ")";
        return source;
    }

    public OrWhere getOr() {
        return this;
    }

    // -------------------- Всякие другие методы ------------------------- //

    public <J extends Join> void fillJoins(List<J> joins) {
        for(AndWhere where : this)
            where.fillJoins(joins);
    }

    public void fillJoinWheres(MapWhere<JoinData> joins, Where andWhere) {
        for(AndWhere where : this)
            where.fillJoinWheres(joins,andWhere);
    }

    public boolean getValue(List<DataWhere> trueWheres) {

        boolean result = false;
        for (AndWhere where : this) {
            result |= where.getValue(trueWheres);
        }

        return result;
    }

    public Where andNot(Where where) {
        if(where instanceof AndWhere)
            return andNot(new OrWhere((AndWhere)where));
        else
        if(where instanceof OrWhere)
            return andNot((OrWhere)where);
        else
            return andNot(new AndWhere((ObjectWhere)where));
    }

    public Where andNot(OrWhere where) {

        if (where.size() == 0) return new OrWhere(this);

        Set<AndWhere> resultSet = new HashSet();
        for (AndWhere whereFrom : this) {
            andNot(new AndWhere(whereFrom), where, 0, resultSet);
        }

        OrWhere resultWhere = new OrWhere(resultSet);
        // если не делать removeRedundant до их будет вообще дофига
        resultWhere.simplifyRemoveRedundant();
        resultWhere.simplify();
        return resultWhere;
    }

    private void andNot(AndWhere whereFrom, OrWhere where, int index, Set<AndWhere> resultWhere) {

        if (index >= where.size()) {
            resultWhere.add(whereFrom);
            return;
        }

        AndWhere whereTo = where.get(index);
        for (ObjectWhere objWhere : whereTo) {
            Where newWhere = whereFrom.and(objWhere.not());
            if (newWhere.isFalse()) continue;
            andNot((AndWhere)newWhere, where, index+1, resultWhere);
        }
    }

    public boolean means(Where where) {
        if(where instanceof AndWhere)
            return means(new OrWhere((AndWhere)where));
        else
        if(where instanceof OrWhere)
            return means((OrWhere)where);
        else
            return means(new AndWhere((ObjectWhere)where));
    }

    public boolean means(OrWhere where) {

        if(where.size() == 0) return isFalse();

        for (AndWhere whereFrom : this) {
            if (meansNot(new AndWhere(whereFrom), where, 0)) return false;
        }

        return true;
    }

    private boolean meansNot(AndWhere whereFrom, OrWhere where, int index) {

        if (index >= where.size()) return !whereFrom.isFalse();

        AndWhere whereTo = where.get(index);
        for (ObjectWhere objWhere : whereTo) {
            Where newWhere = whereFrom.and(objWhere.not());
            if (newWhere.isFalse()) continue;
            if (meansNot((AndWhere)newWhere, where, index+1)) return true;
        }

        return false;
    }

    // преобразует ДНФ not'ом в КНФ
    public Where reverseNot() {
        OrWhere result = new OrWhere();
        for(AndWhere where : this)
            result.add((AndWhere) where.reverseNot());
        return result;
    }

    // для оптимизации
    int getComplexity() {
        int complexity = 0;
        for(AndWhere where : this)
            complexity += where.size();
        return complexity;
    }
    int getDataCount() {
        Collection<DataWhere> wheres = new HashSet<DataWhere>();
        fillData(wheres);
        return wheres.size();
    }

    public void fillData(Collection<DataWhere> wheres) {
        for(AndWhere where : this)
            where.fillData(wheres);
    }

    public Where getJoinWhere() {
        OrWhere result = new OrWhere();
        for(AndWhere where : this)
            result.or(where.getJoinWhere());
        return result;
    }

    public Where followTrue(AndWhere where) {
        return this;
    }

    public Where copy() {
        OrWhere Result = new OrWhere();
        for(AndWhere where : this)
            Result.add((AndWhere) where.copy());
        return Result;
    }

    public boolean equals(Object o) {
        return this==o;
    }

    public int hashCode() {
        return System.identityHashCode(this);
    }

    // для кэша
    public boolean equals(Where where, Map<ObjectExpr, ObjectExpr> mapExprs, Map<JoinWhere, JoinWhere> mapWheres) {
        return where instanceof OrWhere && equals(this,(OrWhere)where, mapExprs, mapWheres);
    }

    public int hash() {
        return hash(this,5);
    }

    static <T extends Where> boolean equals(Collection<T> w1, Collection<T> w2, Map<ObjectExpr, ObjectExpr> mapExprs, Map<JoinWhere, JoinWhere> mapWheres) {

        if(w1.size()!=w2.size()) return false;

        Collection<T> mapped = new ArrayList<T>();
        for(T andWhere : w1) {
            T mappedWhere = null;
            for(T andEqualWhere : w2)
                if(!mapped.contains(andEqualWhere) && andEqualWhere.equals(andWhere,mapExprs, mapWheres)) {
                    mappedWhere = andEqualWhere;}
            if(mappedWhere==null) return false;
            mapped.add(mappedWhere);
        }

        return true;
    }

    static <T extends Where> int hash(Collection<T> wheres, int Hash) {
        for(T where : wheres)
            Hash += where.hash();
        return Hash;
    }

}

class AndWhere extends ArrayList<ObjectWhere> implements Where {

    // ------------------- конструкторы ------------------------ //

    AndWhere() {
    }

    AndWhere(ObjectWhere where) {
        super(Collections.singleton(where));
    }

    AndWhere(AndWhere where) {
        super(where);
    }

    AndWhere(Set<ObjectWhere> where) {
        super(where);
    }

    boolean follow(AndWhere where) {

        for (ObjectWhere objWhereTo : where) {

            boolean follows = false;
            for (ObjectWhere objWhereFrom : this) {
                if ((objWhereTo instanceof DataWhere && objWhereFrom instanceof DataWhere && objWhereFrom.getDataWhere().follow(objWhereTo.getDataWhere())) ||
                    (objWhereTo instanceof NotWhere && objWhereFrom instanceof NotWhere && objWhereTo.getDataWhere().follow(objWhereFrom.getDataWhere()))) {
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

    AndWhere concatenate(AndWhere where) {

        if (where.size() != size()) return null;

        ObjectWhere objWhereFrom = getDifferentObjectWhere(this, where);
        if (objWhereFrom == null) return null;

        ObjectWhere objWhereTo = getDifferentObjectWhere(where, this);
        if (objWhereTo == null) return null;

        if (objWhereFrom instanceof NotWhere && objWhereTo instanceof DataWhere) {
            if (!objWhereFrom.getDataWhere().follow(objWhereTo.getDataWhere())) return null;
        } else {
            if (objWhereFrom instanceof DataWhere && objWhereTo instanceof NotWhere) {
                if (!objWhereTo.getDataWhere().follow(objWhereFrom.getDataWhere())) return null;
            } else return null;
        }

        AndWhere concat = new AndWhere();
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

    private ObjectWhere getDifferentObjectWhere(AndWhere whereFrom, AndWhere whereTo) {

        ObjectWhere result = null;
        for (ObjectWhere objWhereFrom : whereFrom)
            if (!whereTo.contains(objWhereFrom)) {
                if (result != null) return null;
                result = objWhereFrom;
            }

        return result;
    }

/*    private int calculateHashCode(AndWhere where, ObjectWhere objWhereExcept) {

        int hash1 = 0, hash2 = 1;
        for (ObjectWhere objWhere : where)
            if (objWhere != objWhereExcept) {
                hash1 += objWhere.hashCode();
                hash2 *= objWhere.hashCode() + 16237; // чтобы нулей меньше было
            }

        return hash1 + hash2;
    } */

    AndWhere conglutinate(AndWhere where) {

        if (size() != 1) return null;

        ObjectWhere objWhereC = get(0);

        for (ObjectWhere objWhere : where) {
            if (( objWhere instanceof NotWhere && objWhereC instanceof DataWhere && objWhere.getDataWhere().follow(objWhereC.getDataWhere()) ) ||
                ( objWhere instanceof DataWhere && objWhereC instanceof NotWhere && objWhereC.getDataWhere().follow(objWhere.getDataWhere()) )) {
                AndWhere result = new AndWhere(where);
                result.remove(objWhere);
                return result;
            }
        }

        return null;
    }

    AndWhere conglutinate(AndWhere where1, AndWhere where2) {

        if (size() != 2) return null;
        if (where1.size() != where2.size()) return null;

        ObjectWhere objWhereC1 = get(0);
        ObjectWhere objWhereC2 = get(1);

        for (ObjectWhere objWhere1 : where1) {
            if (( objWhere1 instanceof NotWhere && objWhereC1 instanceof DataWhere && objWhere1.getDataWhere().follow(objWhereC1.getDataWhere()) ) ||
                ( objWhere1 instanceof DataWhere && objWhereC1 instanceof NotWhere && objWhereC1.getDataWhere().follow(objWhere1.getDataWhere()) )) {

                AndWhere result = new AndWhere(where1);
                result.remove(objWhere1);

                for (ObjectWhere objWhere2 : where2) {
                    if (( objWhere2 instanceof NotWhere && objWhereC2 instanceof DataWhere && objWhere2.getDataWhere().follow(objWhereC2.getDataWhere()) ) ||
                        ( objWhere2 instanceof DataWhere && objWhereC2 instanceof NotWhere && objWhereC2.getDataWhere().follow(objWhere2.getDataWhere()) )) {

                        if (where2.containsAll(result) && !result.contains(objWhere2))
                            return result;
                    }
                }

            }
        }

        return null;
    }

    // ------------------- Реализация and ------------------------ //

    public Where and(Where where) {
        if(where instanceof AndWhere)
            return and((AndWhere)where);
        else
        if(where instanceof OrWhere)
            return and((OrWhere) where);
        else // тогда уже OR
            return and(new AndWhere((ObjectWhere)where));
    }

    public Where and(AndWhere where) {

        AndWhere result = new AndWhere(this);

        for (ObjectWhere whereNew : where) {

            boolean redundant = false;
            for (ObjectWhere whereOld : this) {
                if (whereNew.getDataWhere().follow(whereOld.getDataWhere())) {
                    if (whereNew instanceof DataWhere && whereOld instanceof NotWhere)
                        return new OrWhere(); // невыполнимость
                    if (whereNew instanceof DataWhere && whereOld instanceof DataWhere) {
                        result.remove(whereOld);
                        continue;
                    }
                    if (whereNew instanceof NotWhere && whereOld instanceof NotWhere) {
                        redundant = true;
                        continue;
                    }
                }
                if (whereOld.getDataWhere().follow(whereNew.getDataWhere())) {
                    if (whereOld instanceof DataWhere && whereNew instanceof NotWhere)
                        return new OrWhere(); // невыполнимость
                    if (whereOld instanceof DataWhere && whereNew instanceof DataWhere) {
                        redundant = true;
                        continue;
                    }
                    if (whereOld instanceof NotWhere && whereNew instanceof NotWhere) {
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

    public Where and(OrWhere where) {
        return where.and(this);
    }

    // ------------------- Реализация not ------------------------ //

    public OrWhere not() {

        OrWhere result = new OrWhere();
        for(ObjectWhere where : this)
            result.or(where.not());
        return result;
    }

    // преобразует ДНФ not'ом в КНФ
    public Where reverseNot() {
        AndWhere result = new AndWhere();
        for(ObjectWhere where : this)
            result.add((ObjectWhere) where.not());
        return result;
    }

    public Where followFalse(OrWhere where) {
        return new OrWhere(this).followFalse(where);
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
                        if (whereFrom instanceof NotWhere && whereTo instanceof NotWhere)
                            redundant.add(whereFrom);
                    }

        for (ObjectWhere where : redundant)
            remove(where);
    }

    // ------------------- Реализация translate ------------------------ //

    public Where translate(Translator translator) {
        // сначала транслируем AndWhere
        Collection<Where> transWheres = new ArrayList<Where>();
        boolean changedWheres = false;
        for(ObjectWhere where : this) {
            Where transWhere = translator.translate(where);
            transWheres.add(transWhere);
            changedWheres = changedWheres || (transWhere!=where);
        }

        if(!changedWheres)
            return this;

        int Complexity = 1;
        for(Where where : transWheres) {
            Complexity = Complexity*where.getOr().size();
        }
        if(Complexity>50)
            Complexity = Complexity;

        Where trans = new AndWhere();
        for(Where where : transWheres)
            trans = trans.and(where);
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

    public OrWhere getOr() {
        return new OrWhere(this);
    }

    // -------------------- Всякие другие методы ------------------------- //

    public <J extends Join> void fillJoins(List<J> joins) {
        for(ObjectWhere where : this)
            where.fillJoins(joins);
    }

    public void fillJoinWheres(MapWhere<JoinData> joins,Where andWhere) {
        // бежим по всем элементам добавляя остальные And'ы
        andWhere = andWhere.and(this);
        for(ObjectWhere where : this)
            where.fillJoinWheres(joins,andWhere);
    }

    public boolean getValue(List<DataWhere> trueWheres) {

        boolean result = true;
        for (ObjectWhere where : this) {
            result &= where.getValue(trueWheres);
        }

        return result;
    }

    public Where andNot(Where where) {
        return new OrWhere(this).andNot(where);
    }

    public boolean means(Where where) {
        return new OrWhere(this).means(where);
    }

    public void fillData(Collection<DataWhere> wheres) {
        for(ObjectWhere where : this)
            where.fillData(wheres);
    }

    public Where getJoinWhere() {
        Where result = new AndWhere();
        for(ObjectWhere where : this)
           result = result.and(where.getJoinWhere());
        return result;
    }

    public Where followTrue(AndWhere where) {
        return this;
    }

    public Where copy() {
        AndWhere Result = new AndWhere();
        for(ObjectWhere where : this)
            Result.add((ObjectWhere) where.copy());
        return Result;
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
    public boolean equals(Where where, Map<ObjectExpr, ObjectExpr> mapExprs, Map<JoinWhere, JoinWhere> mapWheres) {
        return where instanceof AndWhere && OrWhere.equals(this,(AndWhere)where, mapExprs, mapWheres);
    }

    public int hash() {
        return OrWhere.hash(this,7);
    }
}

abstract class ObjectWhere implements Where {

    static String TRUE = "1=1";
    static String FALSE = "1<>1";

    abstract DataWhere getDataWhere();

    public Where followFalse(OrWhere where) {
        return new AndWhere(this).followFalse(where);
    }

    // ------------------- Реализация and ------------------------ //

    public Where and(Where where) {
        if(where instanceof ObjectWhere) {
            return new AndWhere(this).and(new AndWhere((ObjectWhere) where));
        } else
            return where.and(this);
    }

    // ------------------- Реализация вспомогательных  интерфейсов ------------------------ //

    public boolean isFalse() {
        return false;
    }

    public boolean isTrue() {
        return false;
    }

    // -------------------- Выходной интерфейс ------------------------- //

    public OrWhere getOr() {
        return new OrWhere(this);
    }

    public Where andNot(Where where) {
        return new AndWhere(this).andNot(where);
    }

    public boolean means(Where where) {
        return new AndWhere(this).means(where);
    }

    // -------------------- Всякие другие методы ------------------------- //

    public void fillJoinWheres(MapWhere<JoinData> joins, Where andWhere) {
        fillDataJoinWheres(joins,andWhere.and(this));
    }

    abstract protected void fillDataJoinWheres(MapWhere<JoinData> joins, Where andWhere);

    public Where followTrue(AndWhere where) {
        return this;
    }

    public Where reverseNot() {
        return not();
    }
}

class NotWhere extends ObjectWhere {

    private DataWhere where;

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || !(o instanceof NotWhere)) return false;

        return where.equals(((NotWhere) o).where);
    }

    public int hashCode() {
        return where.hashCode();
    }

    NotWhere(DataWhere iwhere) {
        where = iwhere;
    }

    final static String PREFIX = "NOT ";

    // ------------------- Реализация not ------------------------ //

    public Where not() {
        return getDataWhere();
    }

    DataWhere getDataWhere() {
        return where;
    }

    // ------------------- Реализация translate ------------------------ //

    public Where translate(Translator translator) {
        Where translatedWhere = translator.translate(where);
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

    protected void fillDataJoinWheres(MapWhere<JoinData> joins, Where andWhere) {
        where.fillDataJoinWheres(joins, andWhere);
    }

    public boolean getValue(List<DataWhere> trueWheres) {
        return !where.getValue(trueWheres);
    }

    public void fillData(Collection<DataWhere> wheres) {
        where.fillData(wheres);
    }

    public Where getJoinWhere() {
        return where.getNotJoinWhere();
    }

    public Where copy() {
        return where.not();
    }

    // для кэша
    public boolean equals(Where equalWhere, Map<ObjectExpr, ObjectExpr> mapExprs, Map<JoinWhere, JoinWhere> mapWheres) {
        return equalWhere instanceof NotWhere && where.equals(((NotWhere)equalWhere).where, mapExprs, mapWheres) ;
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

    public Where not() {
        return new NotWhere(this);
    }

    String getNotSource(Map<QueryData, String> queryData, SQLSyntax syntax) {
        return NotWhere.PREFIX + getSource(queryData, syntax);
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

    public Where getJoinWhere() {
        return this;
    }

    public Where getNotJoinWhere() {
        return not();
    }
}
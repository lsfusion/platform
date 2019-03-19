package lsfusion.server.logics.form.stat.print.design;

import java.util.*;

public class AbstractRowLayout {

    public static <T extends AbstractRowLayoutElement> int doLayout(List<T> elements, int rowWidth) {
        int rowCount = minimalNumberOfRows(elements, rowWidth);
        Collection<Set<T>> combinations = findCombinations(rowCount, elements, rowWidth);
        List<List<T>> bestRows = findBestRows(elements, combinations, rowWidth);

        if (bestRows != null) {
            setElementsCoords(bestRows, rowWidth);
        }

        return rowCount;
    }

    private static <T extends AbstractRowLayoutElement> List<List<T>> findBestRows(List<T> elements, Collection<Set<T>> combinations, int rowWidth) {
        double bestCoeff = Double.MAX_VALUE;
        List<List<T>> bestRows = null;  
        for (Set<T> combination : combinations) {
            List<List<T>> rows = buildRows(elements, combination);
            double dltCoeff = findDeltaCoeff(rows, rowWidth);
            if (dltCoeff < bestCoeff) {
                bestCoeff = dltCoeff;
                bestRows = rows;
            }
        }
        return bestRows;
    }

    private static <T extends AbstractRowLayoutElement> Collection<Set<T>> findCombinations(int rowCount, List<T> elements, int rowWidth) {
        final int BRUTEFORCE_LIMIT = 20;
        Collection<Set<T>> combinations;
        if (elements.size() <= BRUTEFORCE_LIMIT) {
            // перебираем все возможные точки разбиения
            combinations = buildCombinations(rowCount-1, elements);
        } else {
            // heuristic binary search, selects single combination
            combinations = binarySearchHeuristic(rowCount, elements, rowWidth); 
        }
        return combinations;
    }

    private static <T extends AbstractRowLayoutElement> List<List<T>> buildRows(List<T> elements, Set<T> combination) {
        List<List<T>> rows = new ArrayList<>();
        List<T> currentList = new ArrayList<>();

        for (T element : elements) {
            if (combination.contains(element)) {
                rows.add(currentList);
                currentList = new ArrayList<>();
            }
            currentList.add(element);
        }
        rows.add(currentList);

        return rows;
    }

    private static <T extends AbstractRowLayoutElement> double findDeltaCoeff(List<List<T>> rows, int rowWidth) {
        assert !rows.isEmpty();
        double minCoeff = Double.MAX_VALUE;
        double maxCoeff = Double.MIN_VALUE;

        // тут есть избыточность, поскольку по одному и тому же ряду пробегаем несколько раз
        for (List<T> row : rows) {
            double coeff = countRowCoeff(rowWidth, row);
            if (coeff < 0 || row.size() == 0) { return Double.MAX_VALUE; }

            minCoeff = Math.min(minCoeff, coeff);
            maxCoeff = Math.max(maxCoeff, coeff);
        }

        return maxCoeff - minCoeff;
    }

    private static <T extends AbstractRowLayoutElement> Collection<Set<T>> binarySearchHeuristic(int rowCount, List<T> elements, int rowWidth) {
        final int MAX_ITER = 100;
        double minCoeff = 0, maxCoeff = 100;
        for (int iteration = 0; iteration < MAX_ITER; ++iteration) {
            double coeff = (minCoeff + maxCoeff) / 2;
            if (canLayout(rowCount, rowWidth, elements, coeff)) {
                minCoeff = coeff;
            } else {
                maxCoeff = coeff;
            }
        }
        return buildSolution(rowWidth, elements, minCoeff);
    }

    private static <T extends AbstractRowLayoutElement> boolean canLayout(int rowCount, int rowWidth, List<T> elements, double coeff) {
        int curRow = 0;
        int left = 0;
        for (T element : elements) {
            int scaledWidth = calculateScaledWidth(element, coeff);
            if (left + scaledWidth > rowWidth) {
                if (curRow + 1 == rowCount) return false;
                ++curRow;
                left = 0;
            }
            left += scaledWidth;
        }
        return true;
    }

    private static <T extends AbstractRowLayoutElement> Collection<Set<T>> buildSolution(int rowWidth, List<T> elements, double coeff) {
        Set<T> set = new HashSet<>();
        int left = 0;
        for (T element : elements) {
            int scaledWidth = calculateScaledWidth(element, coeff);
            if (left + scaledWidth > rowWidth) {
                set.add(element);
                left = 0;
            }
            left += scaledWidth;
        }
        return Collections.singletonList(set);
    }

    private static <T extends AbstractRowLayoutElement> void setElementsCoords(List<List<T>> bestRows, int rowWidth) {
        for (List<T> row : bestRows) {
            double coeff = countRowCoeff(rowWidth, row);

            int left = 0;
            for (T element : row) {
                int width = calculateScaledWidth(element, coeff);
                if (row.indexOf(element)+1 == row.size()) width = rowWidth - left;

                element.setLeft(left);
                assert width >= 0;
                element.setWidth(width);
                element.setRow(bestRows.indexOf(row));

                left += width;
            }
        }
    }
    
    private static <T extends AbstractRowLayoutElement> int minimalNumberOfRows(List<T> elements, int rowWidth) {
        int curWidth = 0;
        int rows = 1;
        for (T element : elements) {
            if (curWidth + element.getMinimumWidth() > rowWidth) {
                ++rows;
                curWidth = 0;
            }
            curWidth += element.getMinimumWidth();
        }
        return rows;
    }
    
    private static <T extends AbstractRowLayoutElement> double countRowCoeff(int rowWidth, List<T> row) {
        int minimumWidth = 0;
        int preferredWidth = 0;

        for (T element : row) {
            minimumWidth += element.getMinimumWidth();
            preferredWidth += element.getPreferredWidth();
        }

        return countRowCoeff(rowWidth, minimumWidth, preferredWidth);
    }

    private static double countRowCoeff(int rowWidth, int minWidth, int prefWidth) {
        if (minWidth > rowWidth) {
            return -1.0;
        }
        return (double)(rowWidth - minWidth) / Math.max(prefWidth - minWidth, 1);
    }
    
    private static <T extends AbstractRowLayoutElement> int calculateScaledWidth(T element, double coeff) {
        return ((Double)(element.getMinimumWidth() + (element.getPreferredWidth() - element.getMinimumWidth()) * coeff)).intValue();
    }

    private static <T> Collection<Set<T>> buildCombinations(int count, List<T> listElements) {
        Collection<Set<T>> result = new ArrayList<>();
        buildCombinations(count, listElements, 0, new ArrayList<T>(), result);
        return result;
    }
    
    private static <T> void buildCombinations(int count, List<T> listElements, int current, ArrayList<T> currentList, Collection<Set<T>> result) {
        if (currentList.size() == count) {
            result.add((new HashSet<>(currentList)));
            return;
        }

        if (current >= listElements.size()) return;

        buildCombinations(count, listElements, current+1, currentList, result);
        currentList.add(listElements.get(current));
        buildCombinations(count, listElements, current+1, currentList, result);
        currentList.remove(currentList.size() - 1);
    }
}


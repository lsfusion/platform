package lsfusion.server.form.view.report;

import lsfusion.base.SetBuilder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class AbstractRowLayout {

    private static <T extends AbstractRowLayoutElement> double countRowCoeff(int rowWidth, List<T> row, boolean allowNegative) {

        int minimumWidth = 0;
        int preferredWidth = 0;

        for (T element : row) {
            minimumWidth += element.getMinimumWidth();
            preferredWidth += element.getPreferredWidth();
        }

        if (!allowNegative && minimumWidth > rowWidth) return -1.0;

        double coeff;
        if (preferredWidth == minimumWidth)
            coeff = row.size() * 100;
        else
            coeff = (double)(rowWidth - minimumWidth) / (preferredWidth - minimumWidth);

        return coeff;
    }
    public static <T extends AbstractRowLayoutElement> int doLayout(List<T> elements, int rowWidth, boolean singleRow) {

        int rowNumber;
        // пробуем сначала отрисовать все в 1 ряд, затем в 2 и т.д.
        for (rowNumber = 1; rowNumber <= elements.size(); rowNumber++) {

            // перебираем все возможные точки разбиения
            Collection<List<T>> combinations = SetBuilder.buildSetCombinations(rowNumber-1, elements);

            double bestCoeff = Double.MAX_VALUE;
            List<List<T>> bestRows = new ArrayList<>();

            for (List<T> combination : combinations) {

                List<List<T>> rows = new ArrayList<>();

                List<T> currentList = new ArrayList<>();
                for (T element : elements) {

                    if (combination.contains(element)) {
                        rows.add(currentList);
                        currentList = new ArrayList<>();
                    }

                    currentList.add(element);
                }

                // в rows складываем элементы, на которые разбили
                rows.add(currentList);

                boolean correctCombination = true;

                double minCoeff = Double.MAX_VALUE;
                double maxCoeff = Double.MIN_VALUE;

                // тут есть избыточность, поскольку по одному и тому же ряду пробегаем несколько раз
                for (List<T> row : rows) {

                    double coeff = countRowCoeff(rowWidth, row, singleRow);
                    if (!singleRow && (coeff < 0 || row.size() == 0)) { correctCombination = false; break; }

                    minCoeff = Math.min(minCoeff, coeff);
                    maxCoeff = Math.max(maxCoeff, coeff);
                }

                if (!correctCombination) continue;

                // выбираем комбинацию с наилучшей разницей коэффициентов
                double dltCoeff = maxCoeff - minCoeff;
                if (dltCoeff < bestCoeff) {
                    bestCoeff = dltCoeff;
                    bestRows = rows;
                }
            }

            if (bestCoeff < Double.MAX_VALUE) {

                for (List<T> row : bestRows) {

                    double coeff = countRowCoeff(rowWidth, row, singleRow);

                    int left = 0;
                    for (T element : row) {

                        int width = ((Double)(element.getMinimumWidth() + (element.getPreferredWidth() - element.getMinimumWidth()) * coeff)).intValue();
//                        if (width < 0) width = 0;
                        if (row.indexOf(element)+1 == row.size()) width = rowWidth - left;

                        element.setLeft(left);
                        assert width >= 0;
                        element.setWidth(width);
                        element.setRow(bestRows.indexOf(row));

                        left += width;
                    }
                }

                break;
            }
        }

        return rowNumber;
    }
}


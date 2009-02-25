package platform.server.view.form.report;

import platform.base.SetBuilder;

import java.util.*;
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
        for (rowNumber = 1; rowNumber <= elements.size(); rowNumber++) {

            Collection<List<T>> combinations = SetBuilder.buildSetCombinations(rowNumber-1, elements);

            double bestCoeff = Double.MAX_VALUE;
            List<List<T>> bestRows = new ArrayList<List<T>>();

            for (List<T> combination : combinations) {

                List<List<T>> rows = new ArrayList<List<T>>();

                List<T> currentList = new ArrayList<T>();
                for (T element : elements) {

                    if (combination.contains(element)) {
                        rows.add(currentList);
                        currentList = new ArrayList<T>();
                    }

                    currentList.add(element);
                }

                rows.add(currentList);

                boolean correctCombination = true;

                double minCoeff = Double.MAX_VALUE;
                double maxCoeff = Double.MIN_VALUE;

                for (List<T> row : rows) {

                    double coeff = countRowCoeff(rowWidth, row, singleRow);
                    if (!singleRow && (coeff < 0 || row.size() == 0)) { correctCombination = false; break; }

                    minCoeff = Math.min(minCoeff, coeff);
                    maxCoeff = Math.max(maxCoeff, coeff);
                }

                if (!correctCombination) continue;

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
                        if (row.indexOf(element)+1 == row.size()) width = rowWidth - left;

                        element.setLeft(left);
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


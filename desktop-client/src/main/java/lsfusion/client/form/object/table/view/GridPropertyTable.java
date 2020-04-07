package lsfusion.client.form.object.table.view;

import lsfusion.base.BaseUtils;
import lsfusion.client.base.SwingUtils;
import lsfusion.client.form.property.ClientPropertyDraw;

import javax.swing.*;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static lsfusion.client.form.object.table.grid.view.GridTable.DEFAULT_MAX_PREFERRED_SIZE;
import static lsfusion.client.form.object.table.grid.view.GridTable.DEFAULT_PREFERRED_SIZE;

// наследование не получается (из-за JXTreeTable), поэтому делаем агрегацию
public abstract class GridPropertyTable {

    public abstract void setUserWidth(ClientPropertyDraw property, Integer value);
    public abstract Integer getUserWidth(ClientPropertyDraw property);

    public abstract int getColumnsCount();
    public abstract ClientPropertyDraw getColumnPropertyDraw(int i);
    public abstract TableColumn getColumnDraw(int i);

    private static void setColumnWidth(TableColumn column, int width) {
        column.setWidth(width);
        column.setPreferredWidth(width); // если не выставить grid начинает в какие-то моменты ужиматься в preferred, после чего delta при resize'е становится огромной
    }

    // size чтобы учесть header отступы и т.п.
    public Dimension getMaxPreferredSize(Dimension preferredSize) { // ради этого вся ветка maxPreferredSize и делалась
        JTable gridTable = getTable(); 
        Dimension preferredTableSize = gridTable.getPreferredScrollableViewportSize();
        Dimension preferredAutoTableSize = getTableMaxPreferredSize();
        return new Dimension(BaseUtils.max(preferredSize.width - preferredTableSize.width + preferredAutoTableSize.width, preferredSize.width), // max, 130 px
                BaseUtils.max(preferredSize.height - preferredTableSize.height + preferredAutoTableSize.height, preferredSize.height)); // max, 130 px
    }

    public Dimension getTableMaxPreferredSize() {
        Dimension preferredSize = getTable().getPreferredSize();
        Dimension maxPreferredSize = new Dimension(preferredSize);
        if (preferredSize.height < DEFAULT_PREFERRED_SIZE.height) {
            maxPreferredSize.height = DEFAULT_MAX_PREFERRED_SIZE.height;
        }
        return maxPreferredSize;
    }

    public abstract JTable getTable();

    public void doLayout() {
        JTable table = getTable();
        JTableHeader tableHeader = table.getTableHeader();
        if (tableHeader == null || tableHeader.getResizingColumn() == null) {
            updateLayoutWidthColumns();
        } else {
            TableColumn resizingColumn = tableHeader.getResizingColumn();

            TableColumnModel columnModel = table.getColumnModel();
            int delta = table.getWidth() - columnModel.getTotalColumnWidth(); // текущая модель resizing'а исходит из предположения что ширина колонки после resize'га не меняется (весь смысл resize'га изменить ПРАВЫЕ колонки(
            int leftColumnIndex = resizingColumn.getModelIndex();
            resizeColumn(leftColumnIndex, -delta);
        }
    }


    // в общем то для "групп в колонки" разделено (чтобы когда были группы в колонки - все не расширялись(
    private void updateLayoutWidthColumns() {
        List<TableColumn> flexColumns = new ArrayList<>();
        List<Double> flexValues = new ArrayList<>();
        double totalPref = 0.0;
        double totalFlexValues = 0;

        for (int i = 0; i < columns.length; ++i) {
            TableColumn column = columns[i];

            double pref = prefs[i];
            if(flexes[i]) {
                flexColumns.add(column);
                flexValues.add(pref);
                totalFlexValues += pref;
            } else {
                int intPref = (int) Math.round(prefs[i]);
                assert intPref == basePrefs[i];
                setColumnWidth(column, intPref);
            }
            totalPref += pref;
        }

        // поправка для округлений (чтобы не дрожало)
        int flexSize = flexValues.size();
        if(flexSize % 2 != 0)
            flexSize--;
        for(int i=0;i<flexSize;i++)
            flexValues.set(i, flexValues.get(i) + (i % 2 == 0 ? 0.1 : -0.1));

        double flexWidth = BaseUtils.max(getViewportWidth() - totalPref, 0);

        int precision = 10000; // копия с веба, так то здесь можно double'ы использовать, но чтобы одинаково выглядело работало - сделаем так
        int restPercent = 100 * precision;
        for(int i=0,size=flexColumns.size();i<size;i++) {
            TableColumn flexColumn = flexColumns.get(i);
            double flexValue = flexValues.get(i);
            int flexPercent = (int) Math.round(flexValue * restPercent / totalFlexValues);
            restPercent -= flexPercent;
            totalFlexValues -= flexValue;

            setColumnWidth(flexColumn, ((int)Math.round(flexValue + flexWidth * (double)flexPercent / (double)(100 * precision))));
        }
//        preferredWidth = (int) Math.round(totalPref);
        setMinimumTableWidth(Math.round(totalPref));
    }

    public boolean getScrollableTracksViewportWidth() {
        return minimumTableWidth <= 0 || minimumTableWidth <= getViewportWidth();
    }

    private double minimumTableWidth = -1;

    private void setMinimumTableWidth(double width) {
        minimumTableWidth = width;
    }

    public void resizeColumn(int column, int delta) {
//        int body = ;
        int viewWidth = getViewportWidth(); // непонятно откуда этот один пиксель берется (судя по всему padding)
        SwingUtils.calculateNewFlexesForFixedTableLayout(column, delta, viewWidth, prefs, basePrefs, flexes);
        for (int i = 0; i < prefs.length; i++)
            setUserWidth(i, (int) Math.round(prefs[i]));
        updateLayoutWidthColumns();
    }

    private int getViewportWidth() {
        return getTable().getParent().getWidth() - 1;
//        return getTableDataScroller().getClientWidth() - 1;
    }

    private TableColumn[] columns;
    private double[] prefs;  // mutable
    private int[] basePrefs;
    private boolean[] flexes;
    public void updateLayoutWidth() {
        int columnsCount = getColumnsCount();
        columns = new TableColumn[columnsCount];
        prefs = new double[columnsCount];
        basePrefs = new int[columnsCount];
        flexes = new boolean[columnsCount];
        for (int i = 0; i < columnsCount; ++i) {
            TableColumn columnDraw = getColumnDraw(i);
            columns[i] = columnDraw;

            boolean flex = isColumnFlex(i);
            flexes[i] = flex;

            int basePref = getColumnBaseWidth(i);
            basePrefs[i] = basePref;

            Integer userWidth = getUserWidth(i);
            int pref = flex && userWidth != null ? BaseUtils.max(userWidth, basePref) : basePref;
            prefs[i] = pref;

            // тут хитро, дело в том что базовый механизм resizing'а подразумевает что колонка ВСЕГДА получит запрашиваемую ширину (так как дельта mouseOffsetX - mouseX записывается в ширину, и если колонка не получила ее на прошлом шаге то delta вызовется еще раз и еще раз)
            columnDraw.setMaxWidth(flex ? Integer.MAX_VALUE: basePref); // поэтому выставляем max по сути запрещая расширение таких колонок (для старых колонок наоборот выставляем максимум, потому как refreshColumnModel может из не flex сделать flex колонку)
        }
//        updateLayoutWidthColumns(); // тут не надо, так как в отличие от веб, есть doLayout который и выполняет расположение
    }

    protected boolean isColumnFlex(int i) {
        return getColumnPropertyDraw(i).getFlex() > 0;
    }

    protected void setUserWidth(int i, int width) {
        setUserWidth(getColumnPropertyDraw(i), width);
    }

    protected Integer getUserWidth(int i) {
        return getUserWidth(getColumnPropertyDraw(i));
    }

    protected int getColumnBaseWidth(int i) {
        return getColumnPropertyDraw(i).getValueWidth(getTable()) + getTable().getColumnModel().getColumnMargin();
    }
}

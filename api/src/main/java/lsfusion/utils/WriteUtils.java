package lsfusion.utils;

import java.util.*;

import org.apache.poi.hssf.usermodel.*;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.*;

/**
 * @author jk
 * getted from http://jxls.cvs.sourceforge.net/jxls/jxls/src/java/org/jxls/util/Util.java?revision=1.8&view=markup
 * by Leonid Vysochyn
 * and modified (adding styles copying)
 * modified by Philipp Löpmeier (replacing deprecated classes and methods, using generic types)
 * modified by a.filipchik (xlsx support)
 */
public final class WriteUtils {

    /**
     * DEFAULT CONSTRUCTOR.
     */
    private WriteUtils() {
    }

    public static void copyHSSFSheets(HSSFWorkbook sourceWB, HSSFWorkbook destinationWB) {
        for (Iterator<Sheet> it = sourceWB.sheetIterator(); it.hasNext(); ) {
            HSSFSheet sheet = (HSSFSheet) it.next();
            String sheetName = sheet.getSheetName();
            if (destinationWB.getSheetIndex(sheetName) != -1) {
                int index = 1;
                while (destinationWB.getSheetIndex(sheetName + "(" + index + ")") != -1) {
                    index++;
                }
                sheetName += "(" + index + ")";
            }
            copyHSSFSheet(destinationWB.createSheet(sheetName), sheet);
        }
    }

    /**
     * @param newSheet the sheet to create from the copy.
     * @param sheet    the sheet to copy.
     */
    public static void copyHSSFSheet(HSSFSheet newSheet, HSSFSheet sheet) {
        copyHSSFSheet(newSheet, sheet, true);
    }

    /**
     * @param newSheet  the sheet to create from the copy.
     * @param sheet     the sheet to copy.
     * @param copyStyle true copy the style.
     */
    public static void copyHSSFSheet(HSSFSheet newSheet, HSSFSheet sheet, boolean copyStyle) {
        int maxColumnNum = 0;
        Map<Integer, HSSFCellStyle> styleMap = (copyStyle) ? new HashMap<Integer, HSSFCellStyle>() : null;
        for (int i = sheet.getFirstRowNum(); i <= sheet.getLastRowNum(); i++) {
            HSSFRow srcRow = sheet.getRow(i);
            HSSFRow destRow = newSheet.createRow(i);
            if (srcRow != null) {
                WriteUtils.copyHSSFRow(sheet, newSheet, srcRow, destRow, styleMap);
                if (srcRow.getLastCellNum() > maxColumnNum) {
                    maxColumnNum = srcRow.getLastCellNum();
                }
            }
        }
        for (int i = 0; i <= maxColumnNum; i++) {
            newSheet.setColumnWidth(i, sheet.getColumnWidth(i));
        }
    }

    /**
     * @param srcSheet  the sheet to copy.
     * @param destSheet the sheet to create.
     * @param srcRow    the row to copy.
     * @param destRow   the row to create.
     * @param styleMap  -
     */
    public static void copyHSSFRow(HSSFSheet srcSheet, HSSFSheet destSheet, HSSFRow srcRow, HSSFRow destRow, Map<Integer, HSSFCellStyle> styleMap) {
        // manage a list of merged zone in order to not insert two times a merged zone
        Set<CellRangeAddressWrapper> mergedRegions = new TreeSet<CellRangeAddressWrapper>();
        destRow.setHeight(srcRow.getHeight());
        // pour chaque row
        for (int j = srcRow.getFirstCellNum(); j <= srcRow.getLastCellNum(); j++) {
            HSSFCell oldCell = srcRow.getCell(j);   // ancienne cell
            HSSFCell newCell = destRow.getCell(j);  // new cell
            if (oldCell != null) {
                if (newCell == null) {
                    newCell = destRow.createCell(j);
                }
                // copy chaque cell
                copyHSSFCell(oldCell, newCell, styleMap);
                // copy les informations de fusion entre les cellules
                //System.out.println("row num: " + srcRow.getRowNum() + " , col: " + (short)oldCell.getColumnIndex());
                CellRangeAddress mergedRegion = getMergedRegion(srcSheet, srcRow.getRowNum(), (short) oldCell.getColumnIndex());

                if (mergedRegion != null) {
                    //System.out.println("Selected merged region: " + mergedRegion.toString());
                    CellRangeAddress newMergedRegion = new CellRangeAddress(mergedRegion.getFirstRow(), mergedRegion.getLastRow(), mergedRegion.getFirstColumn(), mergedRegion.getLastColumn());
                    //System.out.println("New merged region: " + newMergedRegion.toString());
                    CellRangeAddressWrapper wrapper = new CellRangeAddressWrapper(newMergedRegion);
                    if (isNewMergedRegion(wrapper, mergedRegions)) {
                        mergedRegions.add(wrapper);
                        destSheet.addMergedRegion(wrapper.range);
                    }
                }
            }
        }

    }

    public static void copyHSSFCell(HSSFCell oldCell, HSSFCell newCell, Map<Integer, HSSFCellStyle> styleMap) {
        if (styleMap != null) {
            if (oldCell.getSheet().getWorkbook() == newCell.getSheet().getWorkbook()) {
                newCell.setCellStyle(oldCell.getCellStyle());
            } else {
                int stHashCode = oldCell.getCellStyle().hashCode();
                HSSFCellStyle newCellStyle = styleMap.get(stHashCode);
                if (newCellStyle == null) {
                    newCellStyle = newCell.getSheet().getWorkbook().createCellStyle();
                    newCellStyle.cloneStyleFrom(oldCell.getCellStyle());
                    styleMap.put(stHashCode, newCellStyle);
                }
                newCell.setCellStyle(newCellStyle);
            }
        }
        switch (oldCell.getCellType()) {
            case HSSFCell.CELL_TYPE_STRING:
                newCell.setCellValue(oldCell.getStringCellValue());
                break;
            case HSSFCell.CELL_TYPE_NUMERIC:
                newCell.setCellValue(oldCell.getNumericCellValue());
                break;
            case HSSFCell.CELL_TYPE_BLANK:
                newCell.setCellType(HSSFCell.CELL_TYPE_BLANK);
                break;
            case HSSFCell.CELL_TYPE_BOOLEAN:
                newCell.setCellValue(oldCell.getBooleanCellValue());
                break;
            case HSSFCell.CELL_TYPE_ERROR:
                newCell.setCellErrorValue(oldCell.getErrorCellValue());
                break;
            case HSSFCell.CELL_TYPE_FORMULA:
                newCell.setCellFormula(oldCell.getCellFormula());
                break;
            default:
                break;
        }

    }

    public static void copyXSSFSheets(XSSFWorkbook sourceWB, XSSFWorkbook destinationWB) {
        for (Iterator<Sheet> it = sourceWB.sheetIterator(); it.hasNext(); ) {
            XSSFSheet sheet = (XSSFSheet) it.next();
            String sheetName = sheet.getSheetName();
            if (destinationWB.getSheetIndex(sheetName) != -1) {
                int index = 1;
                while (destinationWB.getSheetIndex(sheetName + "(" + index + ")") != -1) {
                    index++;
                }
                sheetName += "(" + index + ")";
            }
            copyXSSFSheet(destinationWB.createSheet(sheetName), sheet);
        }
    }

    /**
     * @param newSheet the sheet to create from the copy.
     * @param sheet    the sheet to copy.
     */
    public static void copyXSSFSheet(XSSFSheet newSheet, XSSFSheet sheet) {
        copyXSSFSheet(newSheet, sheet, true);
    }

    /**
     * @param newSheet  the sheet to create from the copy.
     * @param sheet     the sheet to copy.
     * @param copyStyle true copy the style.
     */
    public static void copyXSSFSheet(XSSFSheet newSheet, XSSFSheet sheet, boolean copyStyle) {
        int maxColumnNum = 0;
        Map<Integer, XSSFCellStyle> styleMap = (copyStyle) ? new HashMap<Integer, XSSFCellStyle>() : null;
        for (int i = sheet.getFirstRowNum(); i <= sheet.getLastRowNum(); i++) {
            XSSFRow srcRow = sheet.getRow(i);
            XSSFRow destRow = newSheet.createRow(i);
            if (srcRow != null) {
                WriteUtils.copyXSSFRow(sheet, newSheet, srcRow, destRow, styleMap);
                if (srcRow.getLastCellNum() > maxColumnNum) {
                    maxColumnNum = srcRow.getLastCellNum();
                }
            }
        }
        for (int i = 0; i <= maxColumnNum; i++) {
            newSheet.setColumnWidth(i, sheet.getColumnWidth(i));
        }
    }

    /**
     * @param srcSheet  the sheet to copy.
     * @param destSheet the sheet to create.
     * @param srcRow    the row to copy.
     * @param destRow   the row to create.
     * @param styleMap  -
     */
    public static void copyXSSFRow(XSSFSheet srcSheet, XSSFSheet destSheet, XSSFRow srcRow, XSSFRow destRow, Map<Integer, XSSFCellStyle> styleMap) {
        // manage a list of merged zone in order to not insert two times a merged zone
        Set<CellRangeAddressWrapper> mergedRegions = new TreeSet<CellRangeAddressWrapper>();
        destRow.setHeight(srcRow.getHeight());
        // pour chaque row
        for (int j = srcRow.getFirstCellNum(); j <= srcRow.getLastCellNum(); j++) {
            XSSFCell oldCell = srcRow.getCell(j);   // ancienne cell
            XSSFCell newCell = destRow.getCell(j);  // new cell
            if (oldCell != null) {
                if (newCell == null) {
                    newCell = destRow.createCell(j);
                }
                // copy chaque cell
                copyXSSFCell(oldCell, newCell, styleMap);
                // copy les informations de fusion entre les cellules
                //System.out.println("row num: " + srcRow.getRowNum() + " , col: " + (short)oldCell.getColumnIndex());
                CellRangeAddress mergedRegion = getMergedRegion(srcSheet, srcRow.getRowNum(), (short) oldCell.getColumnIndex());

                if (mergedRegion != null) {
                    //System.out.println("Selected merged region: " + mergedRegion.toString());
                    CellRangeAddress newMergedRegion = new CellRangeAddress(mergedRegion.getFirstRow(), mergedRegion.getLastRow(), mergedRegion.getFirstColumn(), mergedRegion.getLastColumn());
                    //System.out.println("New merged region: " + newMergedRegion.toString());
                    CellRangeAddressWrapper wrapper = new CellRangeAddressWrapper(newMergedRegion);
                    if (isNewMergedRegion(wrapper, mergedRegions)) {
                        mergedRegions.add(wrapper);
                        destSheet.addMergedRegion(wrapper.range);
                    }
                }
            }
        }

    }

    public static void copyXSSFCell(XSSFCell oldCell, XSSFCell newCell, Map<Integer, XSSFCellStyle> styleMap) {
        if (styleMap != null) {
            if (oldCell.getSheet().getWorkbook() == newCell.getSheet().getWorkbook()) {
                newCell.setCellStyle(oldCell.getCellStyle());
            } else {
                int stHashCode = oldCell.getCellStyle().hashCode();
                XSSFCellStyle newCellStyle = styleMap.get(stHashCode);
                if (newCellStyle == null) {
                    newCellStyle = newCell.getSheet().getWorkbook().createCellStyle();
                    newCellStyle.cloneStyleFrom(oldCell.getCellStyle());
                    //по какой-то причине заливка не клонируется
                    newCellStyle.setFillBackgroundColor(oldCell.getCellStyle().getFillBackgroundColor());
                    styleMap.put(stHashCode, newCellStyle);
                }
                newCell.setCellStyle(newCellStyle);
            }
        }
        switch (oldCell.getCellType()) {
            case XSSFCell.CELL_TYPE_STRING:
                newCell.setCellValue(oldCell.getStringCellValue());
                break;
            case XSSFCell.CELL_TYPE_NUMERIC:
                newCell.setCellValue(oldCell.getNumericCellValue());
                break;
            case XSSFCell.CELL_TYPE_BLANK:
                newCell.setCellType(XSSFCell.CELL_TYPE_BLANK);
                break;
            case XSSFCell.CELL_TYPE_BOOLEAN:
                newCell.setCellValue(oldCell.getBooleanCellValue());
                break;
            case XSSFCell.CELL_TYPE_ERROR:
                newCell.setCellErrorValue(oldCell.getErrorCellValue());
                break;
            case XSSFCell.CELL_TYPE_FORMULA:
                newCell.setCellFormula(oldCell.getCellFormula());
                break;
            default:
                break;
        }

    }

    /**
     * Récupère les informations de fusion des cellules dans la sheet source pour les appliquer
     * à la sheet destination...
     * Récupère toutes les zones merged dans la sheet source et regarde pour chacune d'elle si
     * elle se trouve dans la current row que nous traitons.
     * Si oui, retourne l'objet CellRangeAddress.
     *
     * @param sheet   the sheet containing the data.
     * @param rowNum  the num of the row to copy.
     * @param cellNum the num of the cell to copy.
     * @return the CellRangeAddress created.
     */
    public static CellRangeAddress getMergedRegion(HSSFSheet sheet, int rowNum, short cellNum) {
        for (int i = 0; i < sheet.getNumMergedRegions(); i++) {
            CellRangeAddress merged = sheet.getMergedRegion(i);
            if (merged.isInRange(rowNum, cellNum)) {
                return merged;
            }
        }
        return null;
    }

    /**
     * Récupère les informations de fusion des cellules dans la sheet source pour les appliquer
     * à la sheet destination...
     * Récupère toutes les zones merged dans la sheet source et regarde pour chacune d'elle si
     * elle se trouve dans la current row que nous traitons.
     * Si oui, retourne l'objet CellRangeAddress.
     *
     * @param sheet   the sheet containing the data.
     * @param rowNum  the num of the row to copy.
     * @param cellNum the num of the cell to copy.
     * @return the CellRangeAddress created.
     */
    public static CellRangeAddress getMergedRegion(XSSFSheet sheet, int rowNum, short cellNum) {
        for (int i = 0; i < sheet.getNumMergedRegions(); i++) {
            CellRangeAddress merged = sheet.getMergedRegion(i);
            if (merged.isInRange(rowNum, cellNum)) {
                return merged;
            }
        }
        return null;
    }

    /**
     * Check that the merged region has been created in the destination sheet.
     *
     * @param newMergedRegion the merged region to copy or not in the destination sheet.
     * @param mergedRegions   the list containing all the merged region.
     * @return true if the merged region is already in the list or not.
     */
    private static boolean isNewMergedRegion(CellRangeAddressWrapper newMergedRegion, Set<CellRangeAddressWrapper> mergedRegions) {
        return !mergedRegions.contains(newMergedRegion);
    }

    public static class CellRangeAddressWrapper implements Comparable<CellRangeAddressWrapper> {

        public CellRangeAddress range;

        /**
         * @param theRange the CellRangeAddress object to wrap.
         */
        public CellRangeAddressWrapper(CellRangeAddress theRange) {
            this.range = theRange;
        }

        /**
         * @param o the object to compare.
         * @return -1 the current instance is prior to the object in parameter, 0: equal, 1: after...
         */
        public int compareTo(CellRangeAddressWrapper o) {

            if (range.getFirstColumn() < o.range.getFirstColumn() && range.getFirstRow() < o.range.getFirstRow()) {
                return -1;
            } else if (range.getFirstColumn() == o.range.getFirstColumn() && range.getFirstRow() == o.range.getFirstRow()) {
                return 0;
            } else {
                return 1;
            }

        }

    }


}


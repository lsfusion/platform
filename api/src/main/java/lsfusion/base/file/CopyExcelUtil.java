package lsfusion.base.file;

import org.apache.poi.hssf.usermodel.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.PaneInformation;
import org.apache.poi.xssf.usermodel.*;

import java.util.*;

/**
 * source: https://coderanch.com/t/420958/open-source/Copying-sheet-excel-file-excel
 * @author jk
 * getted from http://jxls.cvs.sourceforge.net/jxls/jxls/src/java/org/jxls/util/Util.java?revision=1.8&view=markup
 * by Leonid Vysochyn
 * and modified (adding styles copying)
 * modified by Philipp Löpmeier (replacing deprecated classes and methods, using generic types)
 * modified by a.filipchik (xlsx support & optimizations)
 */
public final class CopyExcelUtil {

    public static void copyHSSFSheets(HSSFWorkbook sourceWB, HSSFWorkbook destinationWB) {
        for (Iterator<Sheet> it = sourceWB.sheetIterator(); it.hasNext(); ) {
            HSSFSheet sheet = (HSSFSheet) it.next();
            String sheetName = getSheetName(sheet, destinationWB);
            HSSFSheet newSheet = destinationWB.createSheet(sheetName);
            copySheetSettings(newSheet, sheet);
            copyHSSFSheet(newSheet, sheet);
            copyPictures(newSheet, sheet);
        }
    }

    public static void copyHSSFSheet(HSSFSheet newSheet, HSSFSheet sheet) {
        int maxColumnNum = 0;
        Map<Integer, HSSFCellStyle> styleMap = new HashMap<>();
        // manage a list of merged zone in order to not insert two times a merged zone
        Set<String> mergedRegions = new TreeSet<>();
        List<CellRangeAddress> sheetMergedRegions = sheet.getMergedRegions();
        int firstRowNum = sheet.getFirstRowNum();
        if(firstRowNum >= 0) {
            for (int i = firstRowNum; i <= sheet.getLastRowNum(); i++) {
                HSSFRow srcRow = sheet.getRow(i);
                HSSFRow destRow = newSheet.createRow(i);
                if (srcRow != null) {
                    copyHSSFRow(newSheet, srcRow, destRow, styleMap, sheetMergedRegions, mergedRegions);
                    if (srcRow.getLastCellNum() > maxColumnNum) {
                        maxColumnNum = srcRow.getLastCellNum();
                    }
                }
            }
            for (int i = 0; i <= maxColumnNum; i++) {
                newSheet.setColumnWidth(i, sheet.getColumnWidth(i));
            }
        }
        copyFreezePane(newSheet, sheet);
    }

    public static void copyHSSFRow(HSSFSheet destSheet, HSSFRow srcRow, HSSFRow destRow, Map<Integer, HSSFCellStyle> styleMap, List<CellRangeAddress> sheetMergedRegions, Set<String> mergedRegions) {
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
                CellRangeAddress mergedRegion = getMergedRegion(sheetMergedRegions, srcRow.getRowNum(), (short) oldCell.getColumnIndex());

                if (mergedRegion != null) {
                    CellRangeAddress newMergedRegion = new CellRangeAddress(mergedRegion.getFirstRow(), mergedRegion.getLastRow(), mergedRegion.getFirstColumn(), mergedRegion.getLastColumn());
                    if (isNewMergedRegion(newMergedRegion, mergedRegions)) {
                        mergedRegions.add(newMergedRegion.formatAsString());
                        destSheet.addMergedRegion(newMergedRegion);
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
            case STRING:
                newCell.setCellValue(oldCell.getStringCellValue());
                break;
            case NUMERIC:
                newCell.setCellValue(oldCell.getNumericCellValue());
                break;
            case BLANK:
                newCell.setCellType(CellType.BLANK);
                break;
            case BOOLEAN:
                newCell.setCellValue(oldCell.getBooleanCellValue());
                break;
            case ERROR:
                newCell.setCellErrorValue(FormulaError.forInt(oldCell.getErrorCellValue()));
                break;
            case FORMULA:
                newCell.setCellFormula(oldCell.getCellFormula());
                break;
            default:
                break;
        }

    }

    public static void copyXSSFSheets(XSSFWorkbook sourceWB, XSSFWorkbook destinationWB) {
        for (Iterator<Sheet> it = sourceWB.sheetIterator(); it.hasNext(); ) {
            XSSFSheet sheet = (XSSFSheet) it.next();
            String sheetName = getSheetName(sheet, destinationWB);
            XSSFSheet newSheet = destinationWB.createSheet(sheetName);
            copySheetSettings(newSheet, sheet);
            copyXSSFSheet(newSheet, sheet);
            copyPictures(newSheet, sheet);
        }
    }

    private static String getSheetName(Sheet sourceSheet, Workbook destinationWB) {
        String sourceSheetName = sourceSheet.getSheetName();
        String destinationSheetName = sourceSheetName;
        if (destinationWB.getSheetIndex(destinationSheetName) != -1) {
            int index = 1;
            while ((destinationSheetName = checkSheetName(sourceSheetName, index, destinationWB)) == null) {
                index++;
            }

        }
        return destinationSheetName;
    }

    private static String checkSheetName(String sourceSheetName, int index, Workbook destinationWB) {
        String postfix = "(" + index + ")"; //max name length = 31
        String destinationSheetName = sourceSheetName.substring(0, Math.min(sourceSheetName.length(), 31 - postfix.length())) + postfix;
        return destinationWB.getSheetIndex(destinationSheetName) != -1 ? null : destinationSheetName;
    }

    public static void copySheetSettings(Sheet newSheet, Sheet sheetToCopy) {

        newSheet.setAutobreaks(sheetToCopy.getAutobreaks());
        newSheet.setDefaultColumnWidth(sheetToCopy.getDefaultColumnWidth());
        newSheet.setDefaultRowHeight(sheetToCopy.getDefaultRowHeight());
        newSheet.setDefaultRowHeightInPoints(sheetToCopy.getDefaultRowHeightInPoints());
        newSheet.setDisplayGuts(sheetToCopy.getDisplayGuts());
        newSheet.setFitToPage(sheetToCopy.getFitToPage());

        newSheet.setForceFormulaRecalculation(sheetToCopy.getForceFormulaRecalculation());

        PrintSetup sheetToCopyPrintSetup = sheetToCopy.getPrintSetup();
        PrintSetup newSheetPrintSetup = newSheet.getPrintSetup();

        newSheetPrintSetup.setPaperSize(sheetToCopyPrintSetup.getPaperSize());
        newSheetPrintSetup.setScale(sheetToCopyPrintSetup.getScale());
        newSheetPrintSetup.setPageStart(sheetToCopyPrintSetup.getPageStart());
        newSheetPrintSetup.setFitWidth(sheetToCopyPrintSetup.getFitWidth());
        newSheetPrintSetup.setFitHeight(sheetToCopyPrintSetup.getFitHeight());
        newSheetPrintSetup.setLeftToRight(sheetToCopyPrintSetup.getLeftToRight());
        newSheetPrintSetup.setLandscape(sheetToCopyPrintSetup.getLandscape());
        newSheetPrintSetup.setValidSettings(sheetToCopyPrintSetup.getValidSettings());
        newSheetPrintSetup.setNoColor(sheetToCopyPrintSetup.getNoColor());
        newSheetPrintSetup.setDraft(sheetToCopyPrintSetup.getDraft());
        newSheetPrintSetup.setNotes(sheetToCopyPrintSetup.getNotes());
        newSheetPrintSetup.setNoOrientation(sheetToCopyPrintSetup.getNoOrientation());
        newSheetPrintSetup.setUsePage(sheetToCopyPrintSetup.getUsePage());
        newSheetPrintSetup.setHResolution(sheetToCopyPrintSetup.getHResolution());
        newSheetPrintSetup.setVResolution(sheetToCopyPrintSetup.getVResolution());
        newSheetPrintSetup.setHeaderMargin(sheetToCopyPrintSetup.getHeaderMargin());
        newSheetPrintSetup.setFooterMargin(sheetToCopyPrintSetup.getFooterMargin());
        newSheetPrintSetup.setCopies(sheetToCopyPrintSetup.getCopies());

        Header sheetToCopyHeader = sheetToCopy.getHeader();
        Header newSheetHeader = newSheet.getHeader();
        newSheetHeader.setCenter(sheetToCopyHeader.getCenter());
        newSheetHeader.setLeft(sheetToCopyHeader.getLeft());
        newSheetHeader.setRight(sheetToCopyHeader.getRight());

        Footer sheetToCopyFooter = sheetToCopy.getFooter();
        Footer newSheetFooter = newSheet.getFooter();
        newSheetFooter.setCenter(sheetToCopyFooter.getCenter());
        newSheetFooter.setLeft(sheetToCopyFooter.getLeft());
        newSheetFooter.setRight(sheetToCopyFooter.getRight());

        newSheet.setHorizontallyCenter(sheetToCopy.getHorizontallyCenter());
        newSheet.setMargin(Sheet.LeftMargin, sheetToCopy.getMargin(Sheet.LeftMargin));
        newSheet.setMargin(Sheet.RightMargin, sheetToCopy.getMargin(Sheet.RightMargin));
        newSheet.setMargin(Sheet.TopMargin, sheetToCopy.getMargin(Sheet.TopMargin));
        newSheet.setMargin(Sheet.BottomMargin, sheetToCopy.getMargin(Sheet.BottomMargin));

        newSheet.setPrintGridlines(sheetToCopy.isPrintGridlines());
        newSheet.setRowSumsBelow(sheetToCopy.getRowSumsBelow());
        newSheet.setRowSumsRight(sheetToCopy.getRowSumsRight());
        newSheet.setVerticallyCenter(sheetToCopy.getVerticallyCenter());
        newSheet.setDisplayFormulas(sheetToCopy.isDisplayFormulas());
        newSheet.setDisplayGridlines(sheetToCopy.isDisplayGridlines());
        newSheet.setDisplayRowColHeadings(sheetToCopy.isDisplayRowColHeadings());
        newSheet.setDisplayZeros(sheetToCopy.isDisplayZeros());
        newSheet.setPrintGridlines(sheetToCopy.isPrintGridlines());
        newSheet.setRightToLeft(sheetToCopy.isRightToLeft());
        newSheet.setZoom(100);
    }

    public static void copyXSSFSheet(XSSFSheet newSheet, XSSFSheet sheet) {
        int maxColumnNum = 0;
        Map<Integer, XSSFCellStyle> styleMap = new HashMap<>();
        // manage a list of merged zone in order to not insert two times a merged zone
        Set<String> mergedRegions = new TreeSet<>();
        List<CellRangeAddress> sheetMergedRegions = sheet.getMergedRegions();
        int firstRowNum = sheet.getFirstRowNum();
        if(firstRowNum >= 0) {
            for (int i = firstRowNum; i <= sheet.getLastRowNum(); i++) {
                XSSFRow srcRow = sheet.getRow(i);
                XSSFRow destRow = newSheet.createRow(i);
                if (srcRow != null) {
                    //BaseUtils.systemLogger.info("copy row " + i);
                    CopyExcelUtil.copyXSSFRow(newSheet, srcRow, destRow, styleMap, sheetMergedRegions, mergedRegions);
                    if (srcRow.getLastCellNum() > maxColumnNum) {
                        maxColumnNum = srcRow.getLastCellNum();
                    }
                }
            }
            for (int i = 0; i <= maxColumnNum; i++) {
                if(newSheet.getColumnWidth(i) != sheet.getColumnWidth(i)) {
                    newSheet.setColumnWidth(i, sheet.getColumnWidth(i));
                }
            }
        }

        copyFreezePane(newSheet, sheet);
    }

    public static void copyXSSFRow(XSSFSheet destSheet, XSSFRow srcRow, XSSFRow destRow, Map<Integer, XSSFCellStyle> styleMap, List<CellRangeAddress> sheetMergedRegions, Set<String> mergedRegions) {
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
                CellRangeAddress mergedRegion = getMergedRegion(sheetMergedRegions, srcRow.getRowNum(), (short) oldCell.getColumnIndex());
                if (mergedRegion != null) {
                    CellRangeAddress newMergedRegion = new CellRangeAddress(mergedRegion.getFirstRow(), mergedRegion.getLastRow(), mergedRegion.getFirstColumn(), mergedRegion.getLastColumn());
                    if (isNewMergedRegion(newMergedRegion, mergedRegions)) {
                        mergedRegions.add(newMergedRegion.formatAsString());
                        destSheet.addMergedRegion(newMergedRegion);
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
            case STRING:
                newCell.setCellValue(oldCell.getStringCellValue());
                break;
            case NUMERIC:
                newCell.setCellValue(oldCell.getNumericCellValue());
                break;
            case BLANK:
                newCell.setCellType(CellType.BLANK);
                break;
            case BOOLEAN:
                Boolean value = getBooleanCellValue(oldCell);
                if(value != null) {
                    newCell.setCellValue(value);
                }
                break;
            case ERROR:
                newCell.setCellErrorValue(oldCell.getErrorCellValue());
                break;
            case FORMULA:
                newCell.setCellFormula(oldCell.getCellFormula());
                break;
            default:
                break;
        }

    }

    private static Boolean getBooleanCellValue(XSSFCell cell) {
        //XSSFCell.getBooleanCellValue compares only with "1"
        String rawValue = cell.getRawValue();
        return rawValue != null ? (cell.getBooleanCellValue() || rawValue.equals("true")) : null;
    }

    private static void copyFreezePane(Sheet newSheet, Sheet sheet) {
        //copy freeze pane
        PaneInformation panelInformation = sheet.getPaneInformation();
        if(panelInformation != null) {
            boolean isFreezePane = panelInformation.isFreezePane();
            if(isFreezePane) {
                int horizontalSplitTopRow = panelInformation.getHorizontalSplitTopRow();
                int verticalSplitLeftColumn = panelInformation.getVerticalSplitLeftColumn();
                if(horizontalSplitTopRow > 0 || verticalSplitLeftColumn > 0) {
                    newSheet.createFreezePane(verticalSplitLeftColumn, horizontalSplitTopRow);
                }
            }
        }
    }

    private static void copyPictures(HSSFSheet newSheet, HSSFSheet sheet) {
        Drawing drawingOld = sheet.createDrawingPatriarch();
        Drawing drawingNew = newSheet.createDrawingPatriarch();
        CreationHelper helper = newSheet.getWorkbook().getCreationHelper();

        List<HSSFShape> shapes = ((HSSFPatriarch) drawingOld).getChildren();
        for (HSSFShape shape : shapes) {
            if (shape instanceof HSSFPicture) {
                HSSFPicture pic = (HSSFPicture) shape;
                HSSFPictureData picdata = pic.getPictureData();
                int pictureIndex = newSheet.getWorkbook().addPicture(picdata.getData(), picdata.getFormat());
                ClientAnchor anchor = null;
                if (pic.getAnchor() != null) {
                    anchor = helper.createClientAnchor();
                    anchor.setDx1(pic.getAnchor().getDx1());
                    anchor.setDx2(pic.getAnchor().getDx2());
                    anchor.setDy1(pic.getAnchor().getDy1());
                    anchor.setDy2(pic.getAnchor().getDy2());
                    anchor.setCol1(((HSSFClientAnchor) pic.getAnchor()).getCol1());
                    anchor.setCol2(((HSSFClientAnchor) pic.getAnchor()).getCol2());
                    anchor.setRow1(((HSSFClientAnchor) pic.getAnchor()).getRow1());
                    anchor.setRow2(((HSSFClientAnchor) pic.getAnchor()).getRow2());
                    anchor.setAnchorType(((HSSFClientAnchor) pic.getAnchor()).getAnchorType());
                }
                drawingNew.createPicture(anchor, pictureIndex);
            }
        }
    }


    private static void copyPictures(XSSFSheet newSheet, XSSFSheet sheet) {
        Drawing drawingOld = sheet.createDrawingPatriarch();
        Drawing drawingNew = newSheet.createDrawingPatriarch();
        CreationHelper helper = newSheet.getWorkbook().getCreationHelper();

        List<XSSFShape> shapes = ((XSSFDrawing) drawingOld).getShapes();
        for (XSSFShape shape : shapes) {
            if (shape instanceof XSSFPicture) {
                XSSFPicture pic = (XSSFPicture) shape;
                XSSFPictureData picdata = pic.getPictureData();
                int pictureIndex = newSheet.getWorkbook().addPicture(picdata.getData(), picdata.getPictureType());
                ClientAnchor anchor = null;
                if (pic.getAnchor() != null) {
                    anchor = helper.createClientAnchor();
                    anchor.setDx1(pic.getAnchor().getDx1());
                    anchor.setDx2(pic.getAnchor().getDx2());
                    anchor.setDy1(pic.getAnchor().getDy1());
                    anchor.setDy2(pic.getAnchor().getDy2());
                    anchor.setCol1(((XSSFClientAnchor) pic.getAnchor()).getCol1());
                    anchor.setCol2(((XSSFClientAnchor) pic.getAnchor()).getCol2());
                    anchor.setRow1(((XSSFClientAnchor) pic.getAnchor()).getRow1());
                    anchor.setRow2(((XSSFClientAnchor) pic.getAnchor()).getRow2());
                    anchor.setAnchorType(((XSSFClientAnchor) pic.getAnchor()).getAnchorType());
                }
                drawingNew.createPicture(anchor, pictureIndex);
            }
        }
    }

    public static CellRangeAddress getMergedRegion(List<CellRangeAddress> sheetMergedRegions, int rowNum, short cellNum) {
        for (CellRangeAddress merged : sheetMergedRegions) {
            if (merged.isInRange(rowNum, cellNum)) {
                return merged;
            }
        }
        return null;
    }

    private static boolean isNewMergedRegion(CellRangeAddress newMergedRegion, Set<String> mergedRegions) {
        return !mergedRegions.contains(newMergedRegion.formatAsString());
    }
}
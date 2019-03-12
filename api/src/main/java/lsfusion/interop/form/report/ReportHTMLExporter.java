package lsfusion.interop.form.report;

import net.sf.jasperreports.engine.export.HtmlExporter;

import java.io.IOException;

public class ReportHTMLExporter extends HtmlExporter {
//public class ReportHTMLExporter extends JRHtmlExporter {
        // copy-paste from JRHTMLExporter
    // todo: При обновлении версии JasperReports возможно понадобится merge c изменениями в JRHTMLExporter.
    // начиная с версии ~6.4.3, класс JRHTMLExporter вырезан. вместе с тем похоже, что большинство наших фиксов внесено и в новые методы класса HtmlExporter.
    // необходимо проверить работоспособность нового класса и в случае успеха почистить здесь код
    
    @Override
    protected void writeEmptyCell(int colSpan, int rowSpan) throws IOException
    {
        startCell(colSpan, rowSpan);
        finishStartCell();
        writer.write("&nbsp;");
        endCell();
    }
    
//    protected void writeEmptyCell(JRExporterGridCell cell, int rowHeight) throws IOException
//    {
//        boolean isUsingImagesToAlign = this.getCurrentConfiguration().isUsingImagesToAlign();
//        if (isUsingImagesToAlign) {
//            super.writeEmptyCell(cell, rowHeight);
//        } else {
//            writer.write("  <td");
//            if (cell.getColSpan() > 1)
//            {
//                writer.write(" colspan=\"" + cell.getColSpan() + "\"");
//            }
//
//            StringBuilder styleBuilder = new StringBuilder();
//            appendBackcolorStyle(cell, styleBuilder);
//            appendBorderStyle(cell.getBox(), styleBuilder);
//
//            if (styleBuilder.length() > 0)
//            {
//                writer.write(" style=\"");
//                writer.write(styleBuilder.toString());
//                writer.write("\"");
//            }
//
//            writer.write(emptyCellStringProvider.getStringForCollapsedTD(cell.getWidth(), rowHeight));
//            writer.write("&nbsp;");
//            writer.write("</td>\n");
//        }
//    }
//
//    protected void exportText(JRPrintText text, JRExporterGridCell gridCell) throws IOException
//    {
//        JRStyledText styledText = getStyledText(text);
//
//        int textLength = 0;
//
//        if (styledText != null)
//        {
//            textLength = styledText.length();
//        }
//
//        writeCellStart(gridCell);//FIXME why dealing with cell style if no text to print (textLength == 0)?
//
//        if (text.getRunDirectionValue() == RunDirectionEnum.RTL)
//        {
//            writer.write(" dir=\"rtl\"");
//        }
//
//        StringBuilder styleBuilder = new StringBuilder();
//
//        String verticalAlignment = HTML_VERTICAL_ALIGN_TOP;
//
//        switch (text.getVerticalTextAlign())
//        {
//            case BOTTOM :
//            {
//                verticalAlignment = HTML_VERTICAL_ALIGN_BOTTOM;
//                break;
//            }
//            case MIDDLE :
//            {
//                verticalAlignment = HTML_VERTICAL_ALIGN_MIDDLE;
//                break;
//            }
//            case TOP :
//            case JUSTIFIED :
//            default :
//            {
//                verticalAlignment = HTML_VERTICAL_ALIGN_TOP;
//            }
//        }
//
//        if (!verticalAlignment.equals(HTML_VERTICAL_ALIGN_TOP))
//        {
//            writer.write(" valign=\"");
//            writer.write(verticalAlignment);
//            writer.write("\"");
//        }
//
//        appendBackcolorStyle(gridCell, styleBuilder);
//        appendBorderStyle(gridCell.getBox(), styleBuilder);
//        appendPaddingStyle(text.getLineBox(), styleBuilder);
//
//        String horizontalAlignment = CSS_TEXT_ALIGN_LEFT;
//
//        if (textLength > 0)
//        {
//            switch (text.getHorizontalTextAlign())
//            {
//                case RIGHT :
//                {
//                    horizontalAlignment = CSS_TEXT_ALIGN_RIGHT;
//                    break;
//                }
//                case CENTER :
//                {
//                    horizontalAlignment = CSS_TEXT_ALIGN_CENTER;
//                    break;
//                }
//                case JUSTIFIED :
//                {
//                    horizontalAlignment = CSS_TEXT_ALIGN_JUSTIFY;
//                    break;
//                }
//                case LEFT :
//                default :
//                {
//                    horizontalAlignment = CSS_TEXT_ALIGN_LEFT;
//                }
//            }
//
////            if (
////                (text.getRunDirectionValue() == RunDirectionEnum.LTR
////                 && !horizontalAlignment.equals(CSS_TEXT_ALIGN_LEFT))
////                || (text.getRunDirectionValue() == RunDirectionEnum.RTL
////                    && !horizontalAlignment.equals(CSS_TEXT_ALIGN_RIGHT))
////                )
////            {
//            styleBuilder.append("text-align: ");
//            styleBuilder.append(horizontalAlignment);
//            styleBuilder.append(";");
////            }
//        }
//
//        boolean isWrapBreakWord = this.getCurrentItemConfiguration().isWrapBreakWord();
//        if (isWrapBreakWord)
//        {
//            styleBuilder.append("width: " + toSizeUnit(gridCell.getWidth()) + "; ");
//            styleBuilder.append("word-wrap: break-word; ");
//        }
//
//        if (text.getLineBreakOffsets() != null)
//        {
//            //if we have line breaks saved in the text, set nowrap so that
//            //the text only wraps at the explicit positions
//            styleBuilder.append("white-space: nowrap; ");
//        }
//
//        if (styleBuilder.length() > 0)
//        {
//            writer.write(" style=\"");
//            writer.write(styleBuilder.toString());
//            writer.write("\"");
//        }
//
//        writer.write(">");
//		writer.write("<p style=\"overflow: hidden; ");
//
//		writer.write("text-indent: " + text.getParagraph().getFirstLineIndent() + "px; ");
////		writer.write("margin-left: " + text.getParagraph().getLeftIndent().intValue() + "px; ");
////		writer.write("margin-right: " + text.getParagraph().getRightIndent().intValue() + "px; ");
////		writer.write("margin-top: " + text.getParagraph().getSpacingBefore().intValue() + "px; ");
////		writer.write("margin-bottom: " + text.getParagraph().getSpacingAfter().intValue() + "px; ");
//		writer.write("\">");
//
//        if (text.getAnchorName() != null)
//        {
//            writer.write("<a name=\"");
//            writer.write(text.getAnchorName());
//            writer.write("\"/>");
//        }
//
//        startHyperlink(text);
//
//        if (textLength > 0)
//        {
//            //only use text tooltip when no hyperlink present
////            String textTooltip = hyperlinkStarted ? null : text.getHyperlinkTooltip();
//            exportStyledText(text, styledText, text.getHyperlinkTooltip());
//        }
//        else
//        {
//            writer.write(emptyCellStringProvider.getStringForEmptyTD());
//        }
//
//        endHyperlink();
//
//		writer.write("</p>");
//
//        writeCellEnd(gridCell);
//    }
}

package jasperapi;

import net.sf.jasperreports.engine.JRPrintText;
import net.sf.jasperreports.engine.export.JRExporterGridCell;
import net.sf.jasperreports.engine.export.JRHtmlExporter;
import net.sf.jasperreports.engine.type.RunDirectionEnum;
import net.sf.jasperreports.engine.util.JRStyledText;

import java.io.IOException;

/**
 * User: DAle
 * Date: 12.01.11
 * Time: 16:34
 */

public class ReportHTMLExporter extends JRHtmlExporter {
    // copy-paste from JRHTMLExporter
    // todo [dale]: При обновлении версии JasperReports возможно понадобится merge c изменениями в JRHTMLExporter.

    protected void writeEmptyCell(JRExporterGridCell cell, int rowHeight) throws IOException
    {
        boolean isUsingImagesToAlign = this.getCurrentConfiguration().isUsingImagesToAlign();
        if (isUsingImagesToAlign) {
            super.writeEmptyCell(cell, rowHeight);
        } else {
            writer.write("  <td");
            if (cell.getColSpan() > 1)
            {
                writer.write(" colspan=\"" + cell.getColSpan() + "\"");
            }

            StringBuffer styleBuffer = new StringBuffer();
            appendBackcolorStyle(cell, styleBuffer);
            appendBorderStyle(cell.getBox(), styleBuffer);

            if (styleBuffer.length() > 0)
            {
                writer.write(" style=\"");
                writer.write(styleBuffer.toString());
                writer.write("\"");
            }

            writer.write(emptyCellStringProvider.getStringForCollapsedTD(cell.getWidth(), rowHeight));
            writer.write("&nbsp;");
            writer.write("</td>\n");
        }
    }

    protected void exportText(JRPrintText text, JRExporterGridCell gridCell) throws IOException
    {
        JRStyledText styledText = getStyledText(text);

        int textLength = 0;

        if (styledText != null)
        {
            textLength = styledText.length();
        }

        writeCellStart(gridCell);//FIXME why dealing with cell style if no text to print (textLength == 0)?

        if (text.getRunDirectionValue() == RunDirectionEnum.RTL)
        {
            writer.write(" dir=\"rtl\"");
        }

        StringBuffer styleBuffer = new StringBuffer();

        String verticalAlignment = HTML_VERTICAL_ALIGN_TOP;

        switch (text.getVerticalTextAlign())
        {
            case BOTTOM :
            {
                verticalAlignment = HTML_VERTICAL_ALIGN_BOTTOM;
                break;
            }
            case MIDDLE :
            {
                verticalAlignment = HTML_VERTICAL_ALIGN_MIDDLE;
                break;
            }
            case TOP :
            default :
            {
                verticalAlignment = HTML_VERTICAL_ALIGN_TOP;
            }
        }

        if (!verticalAlignment.equals(HTML_VERTICAL_ALIGN_TOP))
        {
            writer.write(" valign=\"");
            writer.write(verticalAlignment);
            writer.write("\"");
        }

        appendBackcolorStyle(gridCell, styleBuffer);
        appendBorderStyle(gridCell.getBox(), styleBuffer);
        appendPaddingStyle(text.getLineBox(), styleBuffer);

        String horizontalAlignment = CSS_TEXT_ALIGN_LEFT;

        if (textLength > 0)
        {
            switch (text.getHorizontalTextAlign())
            {
                case RIGHT :
                {
                    horizontalAlignment = CSS_TEXT_ALIGN_RIGHT;
                    break;
                }
                case CENTER :
                {
                    horizontalAlignment = CSS_TEXT_ALIGN_CENTER;
                    break;
                }
                case JUSTIFIED :
                {
                    horizontalAlignment = CSS_TEXT_ALIGN_JUSTIFY;
                    break;
                }
                case LEFT :
                default :
                {
                    horizontalAlignment = CSS_TEXT_ALIGN_LEFT;
                }
            }

//            if (
//                (text.getRunDirectionValue() == RunDirectionEnum.LTR
//                 && !horizontalAlignment.equals(CSS_TEXT_ALIGN_LEFT))
//                || (text.getRunDirectionValue() == RunDirectionEnum.RTL
//                    && !horizontalAlignment.equals(CSS_TEXT_ALIGN_RIGHT))
//                )
//            {
                styleBuffer.append("text-align: ");
                styleBuffer.append(horizontalAlignment);
                styleBuffer.append(";");
//            }
        }

        boolean isWrapBreakWord = this.getCurrentItemConfiguration().isWrapBreakWord();
        if (isWrapBreakWord)
        {
            styleBuffer.append("width: " + toSizeUnit(gridCell.getWidth()) + "; ");
            styleBuffer.append("word-wrap: break-word; ");
        }

        if (text.getLineBreakOffsets() != null)
        {
            //if we have line breaks saved in the text, set nowrap so that
            //the text only wraps at the explicit positions
            styleBuffer.append("white-space: nowrap; ");
        }

        if (styleBuffer.length() > 0)
        {
            writer.write(" style=\"");
            writer.write(styleBuffer.toString());
            writer.write("\"");
        }

        writer.write(">");
		writer.write("<p style=\"overflow: hidden; ");

		writer.write("text-indent: " + text.getParagraph().getFirstLineIndent().intValue() + "px; ");
//		writer.write("margin-left: " + text.getParagraph().getLeftIndent().intValue() + "px; ");
//		writer.write("margin-right: " + text.getParagraph().getRightIndent().intValue() + "px; ");
//		writer.write("margin-top: " + text.getParagraph().getSpacingBefore().intValue() + "px; ");
//		writer.write("margin-bottom: " + text.getParagraph().getSpacingAfter().intValue() + "px; ");
		writer.write("\">");

        if (text.getAnchorName() != null)
        {
            writer.write("<a name=\"");
            writer.write(text.getAnchorName());
            writer.write("\"/>");
        }

        startHyperlink(text);

        if (textLength > 0)
        {
            //only use text tooltip when no hyperlink present
//            String textTooltip = hyperlinkStarted ? null : text.getHyperlinkTooltip();
            exportStyledText(text, styledText, text.getHyperlinkTooltip());
        }
        else
        {
            writer.write(emptyCellStringProvider.getStringForEmptyTD());
        }

        endHyperlink();

		writer.write("</p>");

        writeCellEnd(gridCell);
    }
}

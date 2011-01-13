package platform.server.form.reportstmp;

import net.sf.jasperreports.engine.JRAlignment;
import net.sf.jasperreports.engine.JRPrintText;
import net.sf.jasperreports.engine.JRTextElement;
import net.sf.jasperreports.engine.export.JRExporterGridCell;
import net.sf.jasperreports.engine.export.JRHtmlExporter;
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

            writer.write(emptyCellStringProvider.getStringForCollapsedTD(imagesURI, cell.getWidth(), rowHeight));
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

        writeCellTDStart(gridCell);//FIXME why dealing with cell style if no text to print (textLength == 0)?

        String verticalAlignment = HTML_VERTICAL_ALIGN_TOP;

        switch (text.getVerticalAlignment())
        {
            case JRAlignment.VERTICAL_ALIGN_BOTTOM :
            {
                verticalAlignment = HTML_VERTICAL_ALIGN_BOTTOM;
                break;
            }
            case JRAlignment.VERTICAL_ALIGN_MIDDLE :
            {
                verticalAlignment = HTML_VERTICAL_ALIGN_MIDDLE;
                break;
            }
            case JRAlignment.VERTICAL_ALIGN_TOP :
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

        if (text.getRunDirection() == JRPrintText.RUN_DIRECTION_RTL)
        {
            writer.write(" dir=\"rtl\"");
        }

        StringBuffer styleBuffer = new StringBuffer();
        appendBackcolorStyle(gridCell, styleBuffer);
        appendBorderStyle(gridCell.getBox(), styleBuffer);

        String horizontalAlignment = CSS_TEXT_ALIGN_LEFT;

        if (textLength > 0)
        {
            switch (text.getHorizontalAlignment())
            {
                case JRAlignment.HORIZONTAL_ALIGN_RIGHT :
                {
                    horizontalAlignment = CSS_TEXT_ALIGN_RIGHT;
                    break;
                }
                case JRAlignment.HORIZONTAL_ALIGN_CENTER :
                {
                    horizontalAlignment = CSS_TEXT_ALIGN_CENTER;
                    break;
                }
                case JRAlignment.HORIZONTAL_ALIGN_JUSTIFIED :
                {
                    horizontalAlignment = CSS_TEXT_ALIGN_JUSTIFY;
                    break;
                }
                case JRAlignment.HORIZONTAL_ALIGN_LEFT :
                default :
                {
                    horizontalAlignment = CSS_TEXT_ALIGN_LEFT;
                }
            }

//            if (
//                (text.getRunDirection() == JRPrintText.RUN_DIRECTION_LTR
//                 && !horizontalAlignment.equals(CSS_TEXT_ALIGN_LEFT))
//                || (text.getRunDirection() == JRPrintText.RUN_DIRECTION_RTL
//                    && !horizontalAlignment.equals(CSS_TEXT_ALIGN_RIGHT))
//                )
//            {
                styleBuffer.append("text-align: ");
                styleBuffer.append(horizontalAlignment);
                styleBuffer.append(";");
//            }
        }

        if (isWrapBreakWord)
        {
            styleBuffer.append("width: " + toSizeUnit(gridCell.getWidth()) + "; ");
            styleBuffer.append("word-wrap: break-word; ");
        }

        if (text.getLineSpacing() != JRTextElement.LINE_SPACING_SINGLE)
        {
            styleBuffer.append("line-height: " + text.getLineSpacingFactor() + "; ");
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
            String textTooltip = hyperlinkStarted ? null : text.getHyperlinkTooltip();
            exportStyledText(styledText, textTooltip, getTextLocale(text));
        }
        else
        {
            writer.write(emptyCellStringProvider.getStringForEmptyTD(imagesURI));
        }

        endHyperlink();

        writer.write("</td>\n");
    }
}

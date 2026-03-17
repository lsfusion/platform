package lsfusion.server.logics.form.stat.print.design;

import net.sf.jasperreports.engine.JRBand;
import net.sf.jasperreports.engine.design.*;
import net.sf.jasperreports.engine.type.SplitTypeEnum;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class ReportLayout {
    public static class ReportFieldJasperElement { 
        public JRDesignTextField caption;
        public JRDesignElement designField;
        public JRDesignTextField footer;
        
        public ReportFieldJasperElement(JRDesignTextField caption, JRDesignElement designField, JRDesignTextField footer) {
            this.caption = caption;
            this.designField = designField;
            this.footer = footer;
        }
    }
    
    protected int rowHeight;
    
    public ReportLayout(int rowHeight) {
        this.rowHeight = rowHeight;            
    }

    protected List<ReportDrawField> reportFields = new ArrayList<>();
    protected Map<ReportDrawField, ReportFieldJasperElement> designFields = new HashMap<>();

    public void add(ReportDrawField reportField, JRDesignTextField caption, JRDesignElement text, JRDesignTextField footer) {
        reportFields.add(reportField);
        designFields.put(reportField, new ReportFieldJasperElement(caption, text, footer));
    }

    public int doLayout(int pageWidth) {
        int rowCount = AbstractRowLayout.doLayout(reportFields, pageWidth);

        for (ReportDrawField reportField : reportFields) {
            designFields.get(reportField).caption.setX(reportField.left);
            designFields.get(reportField).caption.setY(reportField.row * rowHeight);
            designFields.get(reportField).caption.setWidth(reportField.width);
            designFields.get(reportField).caption.setHeight(rowHeight);

            designFields.get(reportField).designField.setX(reportField.left);
            designFields.get(reportField).designField.setY(reportField.row * rowHeight);
            designFields.get(reportField).designField.setWidth(reportField.width);
            designFields.get(reportField).designField.setHeight(rowHeight);

            if (designFields.get(reportField).footer != null) {
                designFields.get(reportField).footer.setX(reportField.left);
                designFields.get(reportField).footer.setY(reportField.row * rowHeight);
                designFields.get(reportField).footer.setWidth(reportField.width);
                designFields.get(reportField).footer.setHeight(rowHeight);
            }
        }

        return rowCount;
    }

    public static abstract class BandSection {
        public abstract void setBand(JRBand band);
    }

    public static abstract class MultipleBandSection extends BandSection {
        public void setBand(JRBand band) {
            addBand(band);
        }

        public abstract void addBand(JRBand band);
    }
}

class ReportDetailLayout extends ReportLayout {

    private JRDesignBand pageHeadBand;
    private JRDesignBand detailBand;
    private JRDesignBand columnFooterBand;
    private boolean hasFooter;

    public ReportDetailLayout(JasperDesign design, int rowHeight) {
        super(rowHeight);
        
        // убрать из заголовка страницы заголовок detail?
        pageHeadBand = new JRDesignBand();
        design.setPageHeader(pageHeadBand);

        detailBand = new JRDesignBand();

        ((JRDesignSection)design.getDetailSection()).addBand(detailBand);

        columnFooterBand = new JRDesignBand();
        design.setColumnFooter(columnFooterBand);
    }

    @Override
    public void add(ReportDrawField reportField, JRDesignTextField caption, JRDesignElement text, JRDesignTextField footer) {
        super.add(reportField, caption, text, footer);

        pageHeadBand.addElement(caption);
        detailBand.addElement(text);
        if (footer != null) {
            hasFooter = true;
            columnFooterBand.addElement(footer);
        }
    }

    @Override
    public int doLayout(int pageWidth) {
        int rowCount = super.doLayout(pageWidth);

        pageHeadBand.setHeight(rowCount * rowHeight);
        detailBand.setHeight(rowCount * rowHeight);
        columnFooterBand.setHeight(hasFooter ? rowCount * rowHeight : 0);

        return rowCount;
    }
}

abstract class ReportGroupLayout extends ReportLayout {
    public ReportGroupLayout(int rowHeight) {
        super(rowHeight);
    }
}

class ReportGroupRowLayout extends ReportGroupLayout {

    protected JRDesignBand groupBand;

    public ReportGroupRowLayout(BandSection section, int rowHeight) {
        super(rowHeight);

        groupBand = new JRDesignBand();
        groupBand.setSplitType(SplitTypeEnum.PREVENT);
        section.setBand(groupBand);
    }

    @Override
    public void add(ReportDrawField reportField, JRDesignTextField caption, JRDesignElement text, JRDesignTextField footer) {
        super.add(reportField, caption, text, footer);

        groupBand.addElement(caption);
        groupBand.addElement(text);
    }

    private static class ReportDrawFieldWithCaption implements AbstractRowLayoutElement {
        private ReportDrawField field;
        
        public ReportDrawFieldWithCaption(ReportDrawField field) {
            this.field = field;    
        }
        
        @Override
        public int getMinimumWidth() {
            return field.getMinimumWidth() + field.getCaptionWidth();    
        }

        @Override
        public int getPreferredWidth() {
            return field.getPreferredWidth() + field.getCaptionWidth();
        }

        @Override
        public void setLeft(int left) {
            field.setLeft(left);
        }

        @Override
        public void setWidth(int width) {
            field.setWidth(width);
        }

        @Override
        public void setRow(int row) {
            field.setRow(row);
        }
    }
    
    @Override
    public int doLayout(int pageWidth) {
        List<ReportDrawFieldWithCaption> fields = new ArrayList<>();
        for (ReportDrawField reportField : reportFields) {
            fields.add(new ReportDrawFieldWithCaption(reportField));
        }

        int rowCount = AbstractRowLayout.doLayout(fields, pageWidth);

        for (ReportDrawFieldWithCaption reportFieldWithCaption : fields) {
            ReportDrawField reportField = reportFieldWithCaption.field;
            designFields.get(reportField).caption.setX(reportField.left);
            designFields.get(reportField).caption.setY(reportField.row * rowHeight);
            designFields.get(reportField).caption.setWidth(reportField.getCaptionWidth());
            designFields.get(reportField).caption.setHeight(rowHeight);

            designFields.get(reportField).designField.setX(reportField.getCaptionWidth() + reportField.left);
            designFields.get(reportField).designField.setY(reportField.row * rowHeight);
            designFields.get(reportField).designField.setWidth(reportField.width - reportField.getCaptionWidth());
            designFields.get(reportField).designField.setHeight(rowHeight);
        }

        groupBand.setHeight(rowCount * rowHeight);
        return rowCount;
    }
}

class ReportGroupColumnLayout extends ReportGroupLayout {

    protected JRDesignBand captionGroupBand;
    protected JRDesignBand textGroupBand;

    public ReportGroupColumnLayout(MultipleBandSection designSection, int rowHeight) {
        super(rowHeight);

        captionGroupBand = new JRDesignBand();
        captionGroupBand.setSplitType(SplitTypeEnum.PREVENT);
        designSection.addBand(captionGroupBand);

        textGroupBand = new JRDesignBand();
        textGroupBand.setSplitType(SplitTypeEnum.PREVENT);
        designSection.addBand(textGroupBand);
    }

    @Override
    public void add(ReportDrawField reportField, JRDesignTextField caption, JRDesignElement text, JRDesignTextField footer) {
        super.add(reportField, caption, text, footer);

        captionGroupBand.addElement(caption);
        textGroupBand.addElement(text);
    }

    @Override
    public int doLayout(int pageWidth) {
        int rowCount = super.doLayout(pageWidth);

        captionGroupBand.setHeight(rowCount * rowHeight);
        textGroupBand.setHeight(rowCount * rowHeight);

        return rowCount;
    }
}



package lsfusion.server.logics.form.stat.print.design;

import net.sf.jasperreports.engine.JRBand;
import net.sf.jasperreports.engine.design.JRDesignBand;
import net.sf.jasperreports.engine.design.JRDesignSection;
import net.sf.jasperreports.engine.design.JRDesignTextField;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.type.SplitTypeEnum;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class ReportLayout {
    public static class ReportFieldJasperElement { 
        public JRDesignTextField caption;
        public JRDesignTextField textField;
        
        public ReportFieldJasperElement(JRDesignTextField caption, JRDesignTextField textField) {
            this.caption = caption;
            this.textField = textField;  
        }
    }
    
    protected int rowHeight;
    
    public ReportLayout(int rowHeight) {
        this.rowHeight = rowHeight;            
    }

    protected List<ReportDrawField> reportFields = new ArrayList<>();
    protected Map<ReportDrawField, ReportFieldJasperElement> textFields = new HashMap<>();

    public void add(ReportDrawField reportField, JRDesignTextField caption, JRDesignTextField text) {
        reportFields.add(reportField);
        textFields.put(reportField, new ReportFieldJasperElement(caption, text));
    }

    public int doLayout(int pageWidth) {
        int rowCount = AbstractRowLayout.doLayout(reportFields, pageWidth);

        for (ReportDrawField reportField : reportFields) {
            textFields.get(reportField).caption.setX(reportField.left);
            textFields.get(reportField).caption.setY(reportField.row * rowHeight);
            textFields.get(reportField).caption.setWidth(reportField.width);
            textFields.get(reportField).caption.setHeight(rowHeight);

            textFields.get(reportField).textField.setX(reportField.left);
            textFields.get(reportField).textField.setY(reportField.row * rowHeight);
            textFields.get(reportField).textField.setWidth(reportField.width);
            textFields.get(reportField).textField.setHeight(rowHeight);
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

    public ReportDetailLayout(JasperDesign design, int rowHeight) {
        super(rowHeight);
        
        // убрать из заголовка страницы заголовок detail?
        pageHeadBand = new JRDesignBand();
        design.setPageHeader(pageHeadBand);

        detailBand = new JRDesignBand();

        ((JRDesignSection)design.getDetailSection()).addBand(detailBand);
    }

    public void add(ReportDrawField reportField, JRDesignTextField caption, JRDesignTextField text) {
        super.add(reportField, caption, text);

        pageHeadBand.addElement(caption);
        detailBand.addElement(text);
    }

    @Override
    public int doLayout(int pageWidth) {
        int rowCount = super.doLayout(pageWidth);

        pageHeadBand.setHeight(rowCount * rowHeight);
        detailBand.setHeight(rowCount * rowHeight);

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

    public void add(ReportDrawField reportField, JRDesignTextField caption, JRDesignTextField text) {
        super.add(reportField, caption, text);

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
            textFields.get(reportField).caption.setX(reportField.left);
            textFields.get(reportField).caption.setY(reportField.row * rowHeight);
            textFields.get(reportField).caption.setWidth(reportField.getCaptionWidth());
            textFields.get(reportField).caption.setHeight(rowHeight);

            textFields.get(reportField).textField.setX(reportField.getCaptionWidth() + reportField.left);
            textFields.get(reportField).textField.setY(reportField.row * rowHeight);
            textFields.get(reportField).textField.setWidth(reportField.width - reportField.getCaptionWidth());
            textFields.get(reportField).textField.setHeight(rowHeight);
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

    public void add(ReportDrawField reportField, JRDesignTextField caption, JRDesignTextField text) {
        super.add(reportField, caption, text);

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



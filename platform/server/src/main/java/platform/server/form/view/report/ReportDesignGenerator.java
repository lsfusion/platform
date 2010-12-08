package platform.server.form.view.report;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.design.*;
import net.sf.jasperreports.engine.type.HorizontalAlignEnum;
import net.sf.jasperreports.engine.type.OrientationEnum;
import net.sf.jasperreports.engine.type.PositionTypeEnum;
import net.sf.jasperreports.engine.type.StretchTypeEnum;
import platform.interop.form.ReportConstants;
import platform.server.form.entity.GroupObjectEntity;
import platform.server.form.entity.GroupObjectHierarchy;
import platform.server.form.entity.PropertyObjectEntity;
import platform.server.form.view.FormView;
import platform.server.form.view.GroupObjectView;
import platform.server.form.view.ObjectView;
import platform.server.form.view.PropertyDrawView;

import java.awt.*;
import java.util.*;
import java.util.List;

import static platform.server.form.entity.GroupObjectHierarchy.ReportNode;

/**
 * User: DAle
 * Date: 09.08.2010
 * Time: 18:07:13
 */

public class ReportDesignGenerator {
    private GroupObjectHierarchy.ReportHierarchy hierarchy;
    private FormView formView;
    private Set<Integer> hiddenGroupsId;
    private boolean toExcel;

    private int defaultPageWidth = 842;   
    private final static int defaultPageHeight = 595; // эти константы есть в JasperReports Ultimate Guide

    private int pageWidth;
    private final static int neighboursGap = 5;

    private Map<String, JasperDesign> designs = new HashMap<String, JasperDesign>();

    public ReportDesignGenerator(FormView formView, GroupObjectHierarchy.ReportHierarchy hierarchy, Set<Integer> hiddenGroupsId, boolean toExcel) {
        this.formView = formView;
        this.hierarchy = hierarchy;
        this.hiddenGroupsId = hiddenGroupsId;
        this.toExcel = toExcel;

        if (formView.overridePageWidth != null) {
            defaultPageWidth = formView.overridePageWidth;
        }
        pageWidth = defaultPageWidth - 40;
    }

    public Map<String, JasperDesign> generate() throws JRException {
        JasperDesign rootDesign = createJasperDesignObject(GroupObjectHierarchy.rootNodeName, true, false);

        iterateChildReports(rootDesign, null, 0);

        if (toExcel) {
            for (JasperDesign design : designs.values()) {
                design.setIgnorePagination(true);    
            }
        }

        return designs;
    }

    private void iterateChildReports(JasperDesign design, ReportNode node, int treeGroupLevel) throws JRException {
        List<ReportNode> children = (node == null ? hierarchy.getRootNodes() : hierarchy.getChildNodes(node));
        for (ReportNode childNode : children) {
            JRDesignBand detail = new JRDesignBand();

            if (node == null) {
                treeGroupLevel = childNode.getGroupLevel();
            }
            String sid = childNode.getID();

            boolean hasTopMargin = (childNode != children.get(0));
            JasperDesign childDesign = createJasperDesignObject(sid, false, hasTopMargin);
            createDesignGroups(childDesign, childNode, node, treeGroupLevel);
            iterateChildReports(childDesign, childNode, treeGroupLevel);

            addParametersToDesign(design, sid);

            JRDesignSubreport subreportElement = new JRDesignSubreport(designs.get(sid));
            setExpressionsToSubreportElement(subreportElement, sid);
            subreportElement.setStretchType(StretchTypeEnum.RELATIVE_TO_BAND_HEIGHT);
            detail.addElement(subreportElement);

            ((JRDesignSection)design.getDetailSection()).addBand(detail);
        }
    }

    private void createDesignGroups(JasperDesign design, ReportNode node, ReportNode parent, int treeGroupLevel) throws JRException {
        List<GroupObjectEntity> groups = node.getGroupList();
        for (GroupObjectEntity group : groups) {
            boolean hiddenGroup = hiddenGroupsId.contains(group.getID());
            GroupObjectView groupView = formView.getGroupObject(group);
            List<ReportDrawField> drawFields = new ArrayList<ReportDrawField>();
            PropertyObjectEntity highlightProp = group.propertyHighlight;

            boolean hasColumnGroupProperty = false;
            for (PropertyDrawView property : formView.properties) {
                if (group.equals(property.entity.getToDraw(formView.entity))) {
                    ReportDrawField reportField = property.getReportDrawField();
                    if (reportField != null && (highlightProp == null || highlightProp.property != property.entity.propertyObject.property)) {
                        drawFields.add(reportField);
                        hasColumnGroupProperty = hasColumnGroupProperty || reportField.hasColumnGroupObjects;
                        if (reportField.hasCaptionProperty) {
                            String fieldId = reportField.sID + ReportConstants.captionSuffix;
                            addDesignField(design, fieldId, reportField.captionClass.getName());
                        }
                    }
                }
            }

            String highlightPropertySID = null;
            if (highlightProp != null) {
                ReportDrawField reportField = new ReportDrawField(highlightProp.property.sID, "");
                highlightProp.property.getType().fillReportDrawField(reportField);
                addDesignField(design, reportField);
                highlightPropertySID = reportField.sID;
            }

            if (!hiddenGroup) {
                boolean detail = hierarchy.isLeaf(node) && (group == groups.get(groups.size()-1));
                ReportLayout reportLayout;

                if (detail) {
                    reportLayout = new ReportDetailLayout(design);
                } else {
                    
                    int captionWidth = 0, preferredWidth = 0;
                    for (ReportDrawField reportField : drawFields) {
                        captionWidth += reportField.getCaptionWidth();
                        preferredWidth += reportField.getPreferredWidth();
                    }

                    if (captionWidth + preferredWidth <= pageWidth && !hasColumnGroupProperty) {
                        JRDesignGroup designGroup = addDesignGroup(design, groupView, "designGroup" + group.getID());
                        reportLayout = new ReportGroupRowLayout(designGroup);
                    } else {
                        JRDesignGroup captionGroup = addDesignGroup(design, groupView, "captionGroup" + group.getID());
                        JRDesignGroup textGroup = addDesignGroup(design, groupView, "textGroup" + group.getID());
                        reportLayout = new ReportGroupColumnLayout(captionGroup, textGroup);
                    }
                }

                int groupsCnt = groups.size();
                int minGroupLevel = node.getGroupLevel() - groupsCnt;

                JRDesignStyle groupCellStyle;
                if (parent != null) {
                    int minParentGroupLevel = parent.getGroupLevel()-parent.getGroupList().size();
                    groupCellStyle = DesignStyles.getGroupStyle(groups.indexOf(group), groupsCnt, minGroupLevel, minParentGroupLevel, treeGroupLevel);
                } else {
                    groupCellStyle = DesignStyles.getGroupStyle(groups.indexOf(group), groupsCnt, minGroupLevel, treeGroupLevel, treeGroupLevel);
                }
                design.addStyle(groupCellStyle);
                if (highlightPropertySID != null) {
                    JRDesignConditionalStyle condStyle = new JRDesignConditionalStyle();
                    condStyle.setParentStyle(groupCellStyle);
                    Color oldColor = condStyle.getBackcolor();
                    Color newColor;
                    if (groupView.highlightColor == null) {
                        newColor = new Color(oldColor.getRed(), oldColor.getGreen(), 0);
                    } else {
                        newColor = transformColor(groupView.highlightColor, oldColor.getRed() / 255);
                    }
                    condStyle.setBackcolor(newColor);
                    JRDesignExpression expr =
                            ReportUtils.createExpression("new Boolean($F{" + highlightPropertySID + "} != null)", java.lang.Boolean.class);
                    condStyle.setConditionExpression(expr);
                    groupCellStyle.addConditionalStyle(condStyle);
                }

                for(ReportDrawField reportField : drawFields) {
                    addReportFieldToLayout(reportLayout, reportField, groupCellStyle);
                }
                reportLayout.doLayout(pageWidth);
            }

            for (ObjectView view : groupView) {
                ReportDrawField objField = new ReportDrawField(view.entity.getSID(), "");
                view.entity.baseClass.getType().fillReportDrawField(objField);
                addDesignField(design, objField);
            }

            for (ReportDrawField propertyField : drawFields) {
                addDesignField(design, propertyField);
            }
        }
    }

    private Color transformColor(Color color, double coeff) {
        return new Color((int) (color.getRed() * coeff), (int) (color.getGreen() * coeff), (int) (color.getBlue() * coeff));
    }

    private void addReportFieldToLayout(ReportLayout layout, ReportDrawField reportField, JRDesignStyle style) {
        String designCaptionText;
        if (reportField.hasCaptionProperty) {
            designCaptionText = ReportUtils.createFieldString(reportField.sID + ReportConstants.captionSuffix);
        } else {
            designCaptionText = '"' + reportField.caption + '"';
        }
        JRDesignExpression captionExpr = ReportUtils.createExpression(designCaptionText, reportField.captionClass);
        JRDesignTextField captionField = ReportUtils.createTextField(style, captionExpr);
        captionField.setHorizontalAlignment(HorizontalAlignEnum.CENTER);

        JRDesignExpression dataExpr = ReportUtils.createExpression(ReportUtils.createFieldString(reportField.sID), reportField.valueClass);
        JRDesignTextField dataField = ReportUtils.createTextField(style, dataExpr);
        dataField.setHorizontalAlignment(reportField.alignment);
        dataField.setPositionType(PositionTypeEnum.FLOAT);
        dataField.setBlankWhenNull(true);

        if (!toExcel) {
            dataField.setPattern(reportField.pattern);
        }

        layout.add(reportField, captionField, dataField);
    }

    private JRDesignGroup addDesignGroup(JasperDesign design, GroupObjectView group, String groupName) throws JRException {

        JRDesignGroup designGroup = new JRDesignGroup();
        designGroup.setName(groupName);

        String groupString = "";
        for (ObjectView object : group) {
            groupString = (groupString.length()==0?"":groupString+"+\" \"+")+"String.valueOf($F{"+object.entity.getSID()+"})";
        }
        JRDesignExpression groupExpr = ReportUtils.createExpression(groupString, java.lang.String.class);
        designGroup.setExpression(groupExpr);

        design.addGroup(designGroup);

        return designGroup;
    }

    private JRDesignField addDesignField(JasperDesign design, ReportDrawField reportField) throws JRException {
        return addDesignField(design, reportField.sID, reportField.valueClass.getName());
    }

    private JRDesignField addDesignField(JasperDesign design, String id, String className) throws JRException {
        JRDesignField designField = ReportUtils.createField(id, className);
        design.addField(designField);
        return designField;
    }

    private static void setExpressionsToSubreportElement(JRDesignSubreport subreport, String sid) {
        JRDesignExpression subreportExpr =
                ReportUtils.createExpression(ReportUtils.createParamString(sid + ReportConstants.reportSuffix), JasperReport.class);
        subreport.setExpression(subreportExpr);

        JRDesignExpression sourceExpr =
                ReportUtils.createExpression(ReportUtils.createParamString(sid + ReportConstants.sourceSuffix), JRDataSource.class);
        subreport.setDataSourceExpression(sourceExpr);

        JRDesignExpression paramsExpr =
                ReportUtils.createExpression(ReportUtils.createParamString(sid + ReportConstants.paramsSuffix), Map.class);
        subreport.setParametersMapExpression(paramsExpr);
    }

    private static void addParametersToDesign(JasperDesign design, String sid) throws JRException {
        ReportUtils.addParameter(design, sid + ReportConstants.reportSuffix, JasperReport.class);
        ReportUtils.addParameter(design, sid + ReportConstants.sourceSuffix, JRDataSource.class);
        ReportUtils.addParameter(design, sid + ReportConstants.paramsSuffix, Map.class);
    }

    private JasperDesign createJasperDesignObject(String name, boolean isMain, boolean hasTopMargin) throws JRException {
        JasperDesign design = new JasperDesign();
        if (name.equals(GroupObjectHierarchy.rootNodeName)) {
            design.setName(formView.caption);
        } else {
            design.setName(name);
        }

        design.setPageWidth(defaultPageWidth);
        design.setPageHeight(defaultPageHeight);

        if (!isMain) {
            design.setTopMargin(hasTopMargin ? neighboursGap : 0);
            design.setBottomMargin(0);
            design.setLeftMargin(0);
            design.setRightMargin(0);
        }

        design.setOrientation(OrientationEnum.LANDSCAPE);

        design.addStyle(DesignStyles.getDefaultStyle());
        designs.put(name, design);
        return design;
    }
}

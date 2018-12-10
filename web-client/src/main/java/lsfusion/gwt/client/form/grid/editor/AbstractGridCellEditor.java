package lsfusion.gwt.client.form.grid.editor;

import com.google.gwt.dom.client.DivElement;
import lsfusion.gwt.client.cellview.DataGrid;
import lsfusion.gwt.client.cellview.cell.Cell;
import lsfusion.gwt.shared.view.GExtInt;
import lsfusion.gwt.shared.view.GPropertyDraw;
import lsfusion.gwt.shared.view.classes.*;
import lsfusion.gwt.shared.view.classes.link.*;
import lsfusion.gwt.client.form.grid.EditManager;
import lsfusion.gwt.client.form.grid.editor.rich.RichTextGridCellEditor;

import java.util.ArrayList;

public abstract class AbstractGridCellEditor implements GridCellEditor {
    @Override
    public abstract void renderDom(Cell.Context context, DataGrid table, DivElement cellParent, Object value);

    @Override
    public boolean replaceCellRenderer() {
        return true;
    }

    public static GridCellEditor createGridCellEditor(GType type, final EditManager editManager, final GPropertyDraw editProperty) {
        return createGridCellEditor(type, editManager, editProperty, null, false, false, null, null, false, false);
    }

    public static GridCellEditor createGridCellEditor(GType type, final EditManager editManager, final GPropertyDraw editProperty,
                                                      final String description, final boolean multiple, final boolean storeName, final ArrayList<String> extensions) {
        return createGridCellEditor(type, editManager, editProperty, description, multiple, storeName, extensions, null, false, false);
    }

    public static GridCellEditor createGridCellEditor(GType type, final EditManager editManager, final GPropertyDraw editProperty,
                                                      final GExtInt length, final boolean rich, final boolean blankPadded) {
        return createGridCellEditor(type, editManager, editProperty, null, false, false, null, length, rich, blankPadded);
    }

    public static GridCellEditor createGridCellEditor(GType type, final EditManager editManager, final GPropertyDraw editProperty,
                                                      final String description, final boolean multiple, final boolean storeName, final ArrayList<String> extensions,
                                                      final GExtInt length, final boolean rich, final boolean blankPadded) {
        return type.visit(new GTypeVisitor<GridCellEditor>() {
            @Override
            public GridCellEditor visit(GJSONType type) {
                return new FileGridCellEditor(editManager, editProperty, description, multiple, storeName, extensions);
            }

            @Override
            public GridCellEditor visit(GTableType type) {
                return new FileGridCellEditor(editManager, editProperty, description, multiple, storeName, extensions);
            }

            @Override
            public GridCellEditor visit(GPDFType type) {
                return new FileGridCellEditor(editManager, editProperty, description, multiple, storeName, extensions);
            }

            @Override
            public GridCellEditor visit(GHTMLType type) {
                return new FileGridCellEditor(editManager, editProperty, description, multiple, storeName, extensions);
            }

            @Override
            public GridCellEditor visit(GImageType type) {
                return new FileGridCellEditor(editManager, editProperty, description, multiple, storeName, extensions);
            }

            @Override
            public GridCellEditor visit(GWordType type) {
                return new FileGridCellEditor(editManager, editProperty, description, multiple, storeName, extensions);
            }

            @Override
            public GridCellEditor visit(GExcelType type) {
                return new FileGridCellEditor(editManager, editProperty, description, multiple, storeName, extensions);
            }

            @Override
            public GridCellEditor visit(GCSVType type) {
                return new FileGridCellEditor(editManager, editProperty, description, multiple, storeName, extensions);
            }

            @Override
            public GridCellEditor visit(GXMLType type) {
                return new FileGridCellEditor(editManager, editProperty, description, multiple, storeName, extensions);
            }

            @Override
            public GridCellEditor visit(GCustomDynamicFormatFileType type) {
                return new FileGridCellEditor(editManager, editProperty, description, multiple, storeName, extensions);
            }

            @Override
            public GridCellEditor visit(GCustomStaticFormatFileType type) {
                return new FileGridCellEditor(editManager, editProperty, description, multiple, storeName, extensions);
            }

            @Override
            public GridCellEditor visit(GLinkType type) {
                return new LinkGridCellEditor(editManager, editProperty);
            }

            @Override
            public GridCellEditor visit(GJSONLinkType type) {
                return new LinkGridCellEditor(editManager, editProperty);
            }

            @Override
            public GridCellEditor visit(GTableLinkType type) {
                return new LinkGridCellEditor(editManager, editProperty);
            }

            @Override
            public GridCellEditor visit(GPDFLinkType type) {
                return new LinkGridCellEditor(editManager, editProperty);
            }

            @Override
            public GridCellEditor visit(GHTMLLinkType type) {
                return new LinkGridCellEditor(editManager, editProperty);
            }

            @Override
            public GridCellEditor visit(GImageLinkType type) {
                return new LinkGridCellEditor(editManager, editProperty);
            }

            @Override
            public GridCellEditor visit(GWordLinkType type) {
                return new LinkGridCellEditor(editManager, editProperty);
            }

            @Override
            public GridCellEditor visit(GExcelLinkType type) {
                return new LinkGridCellEditor(editManager, editProperty);
            }

            @Override
            public GridCellEditor visit(GCSVLinkType type) {
                return new LinkGridCellEditor(editManager, editProperty);
            }

            @Override
            public GridCellEditor visit(GXMLLinkType type) {
                return new LinkGridCellEditor(editManager, editProperty);
            }

            @Override
            public GridCellEditor visit(GCustomDynamicFormatLinkType type) {
                return new LinkGridCellEditor(editManager, editProperty);
            }

            @Override
            public GridCellEditor visit(GCustomStaticFormatLinkType type) {
                return new LinkGridCellEditor(editManager, editProperty);
            }

            @Override
            public GridCellEditor visit(GActionType type) {
                return null;
            }

            @Override
            public GridCellEditor visit(GColorType type) {
                return new ColorGridCellEditor(editManager, editProperty);
            }

            @Override
            public GridCellEditor visit(GStringType type) {
                if (length.isUnlimited()) {
                    return rich ? new RichTextGridCellEditor(editManager, editProperty) : new TextGridCellEditor(editManager, editProperty);
                }
                return new StringGridCellEditor(editManager, editProperty, !blankPadded, length.getValue());
            }

            @Override
            public GridCellEditor visit(GLogicalType type) {
                return new LogicalGridCellEditor(editManager);
            }

            @Override
            public GridCellEditor visit(GDateType type) {
                return new DateGridCellEditor(editManager, editProperty);
            }

            @Override
            public GridCellEditor visit(GTimeType type) {
                return new TimeGridCellEditor(editManager, editProperty);
            }

            @Override
            public GridCellEditor visit(GDateTimeType type) {
                return new DateTimeGridCellEditor(editManager, editProperty);
            }

            @Override
            public GridCellEditor visit(GNumericType type) {
                return new NumericGridCellEditor(type, editManager, editProperty, type.getEditFormat(editProperty));
            }

            @Override
            public GridCellEditor visit(GLongType type) {
                return new LongGridCellEditor(editManager, editProperty);
            }

            @Override
            public GridCellEditor visit(GDoubleType type) {
                return new DoubleGridCellEditor(editManager, editProperty, type.getEditFormat(editProperty));
            }

            @Override
            public GridCellEditor visit(GIntegerType type) {
                return new IntegerGridCellEditor(editManager, editProperty);
            }

            @Override
            public GridCellEditor visit(GObjectType type) {
                return new LongGridCellEditor(editManager, editProperty);
            }
        });
    }
}

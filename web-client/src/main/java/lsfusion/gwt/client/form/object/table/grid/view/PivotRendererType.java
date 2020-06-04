package lsfusion.gwt.client.form.object.table.grid.view;

import static lsfusion.gwt.client.ClientMessages.Instance.get;

public enum PivotRendererType {
    TABLE {
        @Override
        public String localize() {
            return get().pivotTableRenderer();
        }
    }, 
    TABLE_BARCHART {
        @Override
        public String localize() {
            return get().pivotTableBarchartRenderer();
        }
    }, 
    TABLE_HEATMAP {
        @Override
        public String localize() {
            return get().pivotTableHeatmapRenderer();
        }
    }, 
    TABLE_ROW_HEATMAP {
        @Override
        public String localize() {
            return get().pivotTableRowHeatmapRenderer();
        }
    }, 
    TABLE_COL_HEATMAP {
        @Override
        public String localize() {
            return get().pivotTableColHeatmapRenderer();
        }
    },
    BARCHART {
        @Override
        public String localize() {
            return get().pivotBarchartRenderer();
        }
    },
    STACKED_BARCHART {
        @Override
        public String localize() {
            return get().pivotStackedBarchartRenderer();
        }
    },
    LINECHART {
        @Override
        public String localize() {
            return get().pivotLinechartRenderer();
        }
    },
    AREACHART {
        @Override
        public String localize() {
            return get().pivotAreachartRenderer();
        }
    },
    SCATTERCHART {
        @Override
        public String localize() {
            return get().pivotScatterchartRenderer();
        }
    },
    MULTIPLE_PIECHART {
        @Override
        public String localize() {
            return get().pivotMultiplePiechartRenderer();
        }
    },
    HORIZONTAL_BARCHART {
        @Override
        public String localize() {
            return get().pivotHorizontalBarchartRenderer();
        }
    },
    HORIZONTAL_STACKED_BARCHART {
        @Override
        public String localize() {
            return get().pivotHorizontalStackedBarchartRenderer();
        }
    },
    TREEMAP {
        @Override
        public String localize() {
            return get().pivotTreemapRenderer();
        }
    };
    
    
    public static PivotRendererType getType(String typeStr) {
        switch (typeStr) {
            case "Table": return TABLE;
            case "Table Bar Chart": return TABLE_BARCHART;
            case "Table Heatmap": return TABLE_HEATMAP;
            case "Table Row Heatmap": return TABLE_ROW_HEATMAP;
            case "Table Col Heatmap": return TABLE_COL_HEATMAP;
            case "Bar Chart": return BARCHART;
            case "Stacked Bar Chart": return STACKED_BARCHART;
            case "Line Chart": return LINECHART;
            case "Area Chart": return AREACHART;
            case "Scatter Chart": return SCATTERCHART;
            case "Multiple Pie Chart": return MULTIPLE_PIECHART;
            case "Horizontal Bar Chart": return HORIZONTAL_BARCHART;
            case "Horizontal Stacked Bar Chart": return HORIZONTAL_STACKED_BARCHART;
            case "Treemap": return TREEMAP;
            default: {
                assert false : "Unknown pivot renderer type";
                return TABLE;
            }
        }
    }
    
    public abstract String localize();
}

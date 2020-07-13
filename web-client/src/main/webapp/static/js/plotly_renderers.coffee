callWithJQuery = (pivotModule) ->
    if typeof exports is "object" and typeof module is "object" # CommonJS
        pivotModule require("jquery"), require("plotly.js")
    else if typeof define is "function" and define.amd # AMD
        define ["jquery", "plotly.js"], pivotModule
    # Plain browser env
    else
        pivotModule jQuery, Plotly

callWithJQuery ($, Plotly) ->
    computedStyle = null
    CSSProps =
        paper_bgcolor: null
        plot_bgcolor: null
        font_color: null
        axis_grid_color: null
        axis_line_color: null
        axis_zeroline_color: null

    getCSSPropertyValue = (propertyName) ->
        if computedStyle == null
            computedStyle = getComputedStyle(document.documentElement)
        return computedStyle.getPropertyValue(propertyName)
    
    getPaperBGColor = () ->
        if CSSProps.paper_bgcolor == null
            CSSProps.paper_bgcolor = getCSSPropertyValue('--background-color')
        return CSSProps.paper_bgcolor

    getPlotBGColor = () ->
        if CSSProps.plot_bgcolor == null
            CSSProps.plot_bgcolor = getCSSPropertyValue('--component-background-color')
        return CSSProps.plot_bgcolor

    getFontColor = () ->
        if CSSProps.font_color == null
            CSSProps.font_color = getCSSPropertyValue('--text-color')
        return CSSProps.font_color

    getAxisGridColor = () ->
        if CSSProps.axis_grid_color == null
            CSSProps.axis_grid_color = getCSSPropertyValue('--grid-separator-border-color')
        return CSSProps.axis_grid_color

    getAxisLineColor = () ->
        if CSSProps.axis_line_color == null
            CSSProps.axis_line_color = getCSSPropertyValue('--component-border-color')
        return CSSProps.axis_line_color

    getAxisZeroLineColor = () ->
        if CSSProps.axis_zeroline_color == null
            CSSProps.axis_zeroline_color = getCSSPropertyValue('--component-border-color')
        return CSSProps.axis_zeroline_color

    getJoinedAttrsNames = (attrs, opts) ->
        attrsString = ''
        for attr in attrs
            if attr != opts.localeStrings.columnAttr
                attrsString += '-' if attrsString != ''
                attrsString += attr
        return attrsString

    makePlotlyChart = (reverse, traceOptions = {}, layoutOptions = {}, transpose = false) ->
        (pivotData, opts) ->
            defaults =
                localeStrings: {vs: "vs", by: "by"}
                plotly: {}
                plotlyConfig: {
                    responsive: true,
                    locale: opts.locale
                }

            opts = $.extend(true, {}, defaults, opts)

            rowKeys = pivotData.getRowKeys()
            colKeys = pivotData.getColKeys()
            if reverse
                tKeys = rowKeys
                rowKeys = colKeys
                colKeys = tKeys
            traceKeys = if transpose then colKeys else rowKeys
            traceKeys.push([]) if traceKeys.length == 0
            datumKeys = if transpose then rowKeys else colKeys
            datumKeys.push([]) if datumKeys.length == 0

            fullAggName = pivotData.aggregatorName
            if pivotData.valAttrs.length
                fullAggName += "(#{pivotData.valAttrs.join(", ")})"

            data = traceKeys.map (traceKey) ->
                values = []
                labels = []
                for datumKey in datumKeys
                    val = parseFloat(pivotData.getAggregator(
                        if transpose ^ reverse then datumKey else traceKey,
                        if transpose ^ reverse then traceKey else datumKey
                    ).value())
                    values.push(if isFinite(val) then val else null)
                    labels.push(datumKey.join('-') || ' ')

                trace = {name: traceKey.join('-') || fullAggName}
                if traceOptions.type == "pie"
                    trace.values = values
                    trace.labels = if labels.length > 1 then labels else [fullAggName]
                else
                    trace.x = if transpose then values else labels
                    trace.y = if transpose then labels else values
                return $.extend(trace, traceOptions)

            if transpose ^ reverse
                hAxisTitle = getJoinedAttrsNames(pivotData.rowAttrs, opts)
                groupByTitle = getJoinedAttrsNames(pivotData.colAttrs, opts)
            else
                hAxisTitle = getJoinedAttrsNames(pivotData.colAttrs, opts)
                groupByTitle = getJoinedAttrsNames(pivotData.rowAttrs, opts)
            titleText = if hAxisTitle != "" then "#{hAxisTitle}" else ""
            titleText += " #{opts.localeStrings.vs} " if groupByTitle != "" and hAxisTitle != ""
            titleText += "#{groupByTitle}" if groupByTitle != ""

            layout =
                title: 
                    text: titleText
                    font:
                        size: 12
                hovermode: 'closest'
                autosize: true
                paper_bgcolor: getPaperBGColor()
                plot_bgcolor: getPlotBGColor()
                font: {
                    color: getFontColor()
                }

            if traceOptions.type == 'pie'
                layout.margin =
                    l: 30
                    r: 30
                    t: if titleText != "" then 40 else 30
                    b: 30
                columns = Math.ceil(Math.sqrt(data.length))
                rows = Math.ceil(data.length / columns)
                layout.grid = {columns, rows}
                for i, d of data
                    d.domain = {
                        row: Math.floor(i / columns),
                        column: i - columns * Math.floor(i / columns),
                    }
                    if data.length > 1
                        d.title = d.name
                layout.showlegend = false if data[0].labels.length == 1
            else
                layout.xaxis =
                    title: 
                        text: if transpose then fullAggName else null
                        font:
                            size: 12
                    automargin: true
                    gridcolor: getAxisGridColor()
                    linecolor: getAxisLineColor()
                    zerolinecolor: getAxisZeroLineColor()
                if transpose
                    layout.xaxis.title.standoff = 10;
                
                layout.yaxis =
                    title:
                        text: if transpose then null else fullAggName
                        font:
                            size: 12
                    automargin: true
                    gridcolor: getAxisGridColor()
                    linecolor: getAxisLineColor()
                    zerolinecolor: getAxisZeroLineColor()
                    
                layout.margin =
                    l: 50
                    r: 30
                    t: if titleText != "" then 40 else 30
                    b: if transpose then 50 else 30
            result = $("<div>").appendTo $("body")
            Plotly.newPlot(result[0], data, $.extend(layout, layoutOptions, opts.plotly), opts.plotlyConfig)
            return result.detach()

    makePlotlyScatterChart = -> (pivotData, opts) ->
        defaults =
            localeStrings: {vs: "vs", by: "by"}
            plotly: {}
            plotlyConfig: {
                responsive: true,
                locale: opts.locale
            }

        opts = $.extend(true, {}, defaults, opts)

        rowKeys = pivotData.getRowKeys()
        rowKeys.push [] if rowKeys.length == 0
        colKeys = pivotData.getColKeys()
        colKeys.push [] if colKeys.length == 0

        data = {x: [], y: [], text: [], type: 'scatter', mode: 'markers'}

        for rowKey in rowKeys
            for colKey in colKeys
                v = pivotData.getAggregator(rowKey, colKey).value()
                if v?
                    data.x.push(colKey.join('-'))
                    data.y.push(rowKey.join('-'))
                    data.text.push(v)

        colAttrsString = getJoinedAttrsNames(pivotData.colAttrs, opts)
        rowAttrsString = getJoinedAttrsNames(pivotData.rowAttrs, opts)
        titleText = if rowAttrsString != "" then "#{rowAttrsString}" else ""
        titleText += " #{opts.localeStrings.vs} " if colAttrsString != "" and rowAttrsString != ""
        titleText += "#{colAttrsString}" if colAttrsString != ""
                
        layout = {
            title:
                text: titleText
                font:
                    size: 12
            margin:
                l: 50
                r: 30
                t: if titleText != "" then 40 else 30
                b: if colAttrsString != '' then 50 else 30
            hovermode: 'closest',
            xaxis:
                title:
                    text: colAttrsString
                    font:
                        size: 12
                    standoff: 10
                automargin: true
            yaxis:
                title:
                    text: rowAttrsString
                    font:
                        size: 12
                    automargin: true
            autosize: true
            paper_bgcolor: getPaperBGColor()
            plot_bgcolor: getPlotBGColor()
            font: {
                color: getFontColor()
            }
        }

        renderArea = $("<div>", style: "display:none;").appendTo $("body")
        result = $("<div>").appendTo renderArea
        Plotly.newPlot(result[0], [data], $.extend(layout, opts.plotly), opts.plotlyConfig)
        result.detach()
        renderArea.remove()
        return result

    $.pivotUtilities.plotly_renderers =
        "BARCHART": makePlotlyChart(true, { type: 'bar' }, { barmode: 'group' }, false),
        "STACKED_BARCHART": makePlotlyChart(true, { type: 'bar' }, { barmode: 'relative' }, false),
        "LINECHART": makePlotlyChart(true, {}, {}, false),
        "AREACHART": makePlotlyChart(true, { stackgroup: 1 }, {}, false),
        "SCATTERCHART": makePlotlyScatterChart(),
        "MULTIPLE_PIECHART": makePlotlyChart(false, {
            type: 'pie',
            scalegroup: 1,
            hoverinfo: 'label+value',
            textinfo: 'none'
        }, {}, false),
        "HORIZONTAL_BARCHART": makePlotlyChart(true, {type: 'bar', orientation: 'h'}, { barmode: 'group' }, true),
        "HORIZONTAL_STACKED_BARCHART": makePlotlyChart(true, { type: 'bar', orientation: 'h'}, { barmode: 'relative' }, true)

    $.pivotUtilities.colorThemeChanged = (plot) ->
        computedStyle = null
        CSSProps.paper_bgcolor = null
        CSSProps.plot_bgcolor = null
        CSSProps.font_color = null
        CSSProps.axis_grid_color = null
        CSSProps.axis_line_color = null
        CSSProps.axis_zeroline_color = null
        
        if (plot != undefined)
            relayout = () ->
                update =
                    paper_bgcolor: getPaperBGColor()
                    plot_bgcolor: getPlotBGColor()
                    font: {
                        color: getFontColor()
                    }
                    xaxis:
                        gridcolor: getAxisGridColor()
                        linecolor: getAxisLineColor()
                        zerolinecolor: getAxisZeroLineColor()
                    yaxis:
                        gridcolor: getAxisGridColor()
                        linecolor: getAxisLineColor()
                        zerolinecolor: getAxisZeroLineColor()
                Plotly.relayout(plot, update)
                
            # to defer the task. otherwise new <theme>.css is not applied yet and getComputedStyle() returns values from the previous one
            setTimeout(relayout)  

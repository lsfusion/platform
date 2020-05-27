callWithJQuery = (pivotModule) ->
    if typeof exports is "object" and typeof module is "object" # CommonJS
        pivotModule require("jquery"), require("plotly.js")
    else if typeof define is "function" and define.amd # AMD
        define ["jquery", "plotly.js"], pivotModule
    # Plain browser env
    else
        pivotModule jQuery, Plotly

callWithJQuery ($, Plotly) ->

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
                hAxisTitle = pivotData.rowAttrs.join("-")
                groupByTitle = pivotData.colAttrs.join("-")
            else
                hAxisTitle = pivotData.colAttrs.join("-")
                groupByTitle = pivotData.rowAttrs.join("-")
            titleText = fullAggName
            titleText += " #{opts.localeStrings.vs} #{hAxisTitle}" if hAxisTitle != ""
            titleText += " #{opts.localeStrings.by} #{groupByTitle}" if groupByTitle != ""

            layout =
                title: titleText
                hovermode: 'closest'
                autosize: true

            if traceOptions.type == 'pie'
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
                    title: if transpose then fullAggName else null
                    automargin: true
                layout.yaxis =
                    title: if transpose then null else fullAggName
                    automargin: true


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

        layout = {
            title: pivotData.rowAttrs.join("-") + ' vs ' + pivotData.colAttrs.join("-")
            hovermode: 'closest',
            xaxis: {title: pivotData.colAttrs.join('-'), automargin: true},
            yaxis: {title: pivotData.rowAttrs.join('-'), automargin: true},
            autosize: true
        }

        renderArea = $("<div>", style: "display:none;").appendTo $("body")
        result = $("<div>").appendTo renderArea
        Plotly.newPlot(result[0], [data], $.extend(layout, opts.plotly), opts.plotlyConfig)
        result.detach()
        renderArea.remove()
        return result

    barchart = makePlotlyChart(true, { type: 'bar' }, { barmode: 'group' }, false)
    stackedbarchart = makePlotlyChart(true, { type: 'bar' }, { barmode: 'relative' }, false)
    linechart = makePlotlyChart(true, {}, {}, false)
    areachart = makePlotlyChart(true, { stackgroup: 1 }, {}, false)
    scatterchart = makePlotlyScatterChart()
    multiplepiechart = makePlotlyChart(false, {
        type: 'pie',
        scalegroup: 1,
        hoverinfo: 'label+value',
        textinfo: 'none'
    }, {}, false)
    horizontalbarchart = makePlotlyChart(true, {type: 'bar', orientation: 'h'}, { barmode: 'group' }, true)
    horizontalstackedbarchart = makePlotlyChart(true, { type: 'bar', orientation: 'h'}, { barmode: 'relative' }, true)
        
        
    $.pivotUtilities.plotly_renderers =
        en:
            "Bar Chart": barchart,
            "Stacked Bar Chart": stackedbarchart,
            "Line Chart": linechart,
            "Area Chart": areachart,
            "Scatter Chart": scatterchart,
            'Multiple Pie Chart': multiplepiechart,
            "Horizontal Bar Chart": horizontalbarchart,
            "Horizontal Stacked Bar Chart": horizontalstackedbarchart
        ru:
            "Столбчатая диаграмма": barchart,
            "Стековая столбчатая диаграмма": stackedbarchart,
            "Линейный график": linechart,
            "Плосткостная диаграмма": areachart,
            "Точечная диаграмма": scatterchart,
            'Круговая диаграмма': multiplepiechart,
            "Столбчатая диаграмма (гор.)": horizontalbarchart,
            "Стековая столбчатая диаграмма (гор.)": horizontalstackedbarchart
        
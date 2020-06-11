callWithJQuery = (pivotModule) ->
    if typeof exports is "object" and typeof module is "object" # CommonJS
        pivotModule require("jquery")
    else if typeof define is "function" and define.amd # AMD
        define ["jquery"], pivotModule
    # Plain browser env
    else
        pivotModule jQuery

callWithJQuery ($) ->

    class SubtotalPivotData extends $.pivotUtilities.PivotData
        constructor: (input, opts) ->
            super input, opts

        processKey = (record, totals, keys, attrs, getAggregator) ->
            key = []
            addKey = false
            for attr in attrs
                key.push record[attr] # ? "null"
                flatKey = key.join String.fromCharCode(0)
                if not totals[flatKey]
                    totals[flatKey] = getAggregator key.slice()
                    addKey = true
                totals[flatKey].push record
            keys.push key if addKey
            return key

        processRecord: (record) -> #this code is called in a tight loop
            rowKey = []
            colKey = []

            @allTotal.push record
            rowKey = processKey record, @rowTotals, @rowKeys, @rowAttrs, (key) =>
                return @aggregator this, key, []
            colKey = processKey record, @colTotals, @colKeys, @colAttrs, (key) =>
                return @aggregator this, [], key
            m = rowKey.length-1
            n = colKey.length-1
            return if m < 0 or n < 0
            for i in [0..m]
                fRowKey = rowKey.slice(0, i+1)
                flatRowKey = fRowKey.join String.fromCharCode(0)
                @tree[flatRowKey] = {} if not @tree[flatRowKey]
                for j in [0..n]
                    fColKey = colKey.slice 0, j+1
                    flatColKey = fColKey.join String.fromCharCode(0)
                    @tree[flatRowKey][flatColKey] = @aggregator this, fRowKey, fColKey if not @tree[flatRowKey][flatColKey]
                    @tree[flatRowKey][flatColKey].push record

    $.pivotUtilities.SubtotalPivotData = SubtotalPivotData

    SubtotalRenderer = (pivotData, opts) ->
        defaults =
            table: clickCallback: null
            localeStrings: totals: "Totals", subtotalOf: "Subtotal of"
            arrowCollapsed: "\u25B6"
            arrowExpanded: "\u25E2"
            rowSubtotalDisplay:
                displayOnTop: true
                collapseAt: 99999
                hideOnExpand: false
                disableExpandCollapse: false
            colSubtotalDisplay:
                displayOnTop: true
                collapseAt: 99999
                hideOnExpand: false
                disableExpandCollapse: false
        opts = $.extend true, {}, defaults, opts

        opts.rowSubtotalDisplay.collapseAt = opts.collapseRowsAt if typeof opts.rowSubtotalDisplay.collapseAt isnt 'undefined' and opts.collapseRowsAt isnt null
        opts.rowSubtotalDisplay.splitPositions = (i for i in [0...pivotData.rowAttrs.length]) if not opts.rowSubtotalDisplay.splitPositions?

        opts.colSubtotalDisplay.collapseAt = opts.collapseColsAt if typeof opts.colSubtotalDisplay.collapseAt isnt 'undefined' and opts.collapseColsAt isnt null
        opts.colSubtotalDisplay.splitPositions = (i for i in [0...pivotData.colAttrs.length]) if not opts.colSubtotalDisplay.splitPositions?

        colAttrs = pivotData.colAttrs
        rowAttrs = pivotData.rowAttrs
        rowKeys = pivotData.getRowKeys()
        colKeys = pivotData.getColKeys()
        tree = pivotData.tree
        callbacks = pivotData.callbacks
        rowTotals = pivotData.rowTotals
        colTotals = pivotData.colTotals
        allTotal = pivotData.allTotal

        classRowHide = "rowhide"
        classRowShow = "rowshow"
        classColHide = "colhide"
        classColShow = "colshow"
        clickStatusExpanded = "expanded"
        clickStatusCollapsed = "collapsed"
        classExpanded = "expanded"
        classCollapsed = "collapsed"
        classZoom = "zoom"
        classRowExpanded = "rowexpanded"
        classRowCollapsed = "rowcollapsed"
        classColExpanded = "colexpanded"
        classColCollapsed = "colcollapsed"
        arrowExpanded = opts.arrowExpanded
        arrowCollapsed = opts.arrowCollapsed
        
        rowSplitPositions = opts.rowSubtotalDisplay.splitPositions

        rowArrowsPadding = 5
        rowArrowsLevelPadding = 10
        
        emptyTopAttrTH = null
        
        # Based on http://stackoverflow.com/questions/195951/change-an-elements-class-with-javascript -- Begin
        hasClass = (element, className) ->
            regExp = new RegExp "(?:^|\\s)" + className + "(?!\\S)", "g"
            element.className.match(regExp) isnt null

        removeClass = (element, className) ->
            for name in className.split " "
                regExp = new RegExp "(?:^|\\s)" + name + "(?!\\S)", "g"
                element.className = element.className.replace regExp, ''

        addClass = (element, className) ->
            for name in className.split " "
                element.className += (" " + name) if not hasClass element, name

        replaceClass = (element, replaceClassName, byClassName) ->
            removeClass element, replaceClassName
            addClass element, byClassName
        # Based on http://stackoverflow.com/questions/195951/change-an-elements-class-with-javascript -- End 

        createElement = (elementType, className, attributes, eventHandlers) ->
            e = document.createElement elementType
            e.className = className if className?
            e.setAttribute attr, val for own attr, val of attributes if attributes?
            e.addEventListener event, handler for own event, handler of eventHandlers if eventHandlers?
            return e

        createValueTD = (value, rowKey, colKey, aggregator, className, attributes, eventHandlers) ->
            td = createElement "td", className, attributes, eventHandlers
            td.ondblclick = (event) -> callbacks.valueCellDblClickHandler event, td, rowKey, colKey if callbacks?
            renderValueCell td, value, rowKey, colKey, aggregator
            return td

        renderValueCell = (td, value, rowKey, colKey, aggregator) ->
            if callbacks?
                callbacks.renderValueCell td, value, rowKey, colKey
            else
                td.textContent = aggregator.format(value)
        
        createArrowAndTextDivs = (th, arrowClass, textClass) ->
            wrapperDiv = createElement "div", "wrapperDiv"
            th.append wrapperDiv

            arrowDiv = createElement "div", arrowClass
            wrapperDiv.appendChild arrowDiv
            th.arrowDiv = arrowDiv

            textDiv = createElement "div", textClass
            wrapperDiv.appendChild textDiv
            th.textDiv = textDiv;
                
        createColAxisHeaderTH = (attr, className, textContent, isExpanded, attributes) ->
            th = createElement "th", className, attributes
            textElement = th
            if isExpanded?
                createArrowAndTextDivs th, "axisHeaderArrowDiv", "axisHeaderTextDiv"
                textElement = th.textDiv
                    
            textElement.ondblclick = (event) -> callbacks.axisHeaderDblClickHandler event, textElement, attr if callbacks?
            renderColAxisHeader th, textContent, attr, isExpanded, false
            return th
            
        renderColAxisHeader = (th, textContent, attr, isExpanded, arrowOnly) ->
            if isExpanded?
                if callbacks?
                    callbacks.renderAxisHeaderCell th.arrowDiv, textContent, attr, isExpanded, true
                    if not arrowOnly
                        callbacks.renderAxisHeaderCell th.textDiv, textContent, attr, isExpanded, false
                else
                    th.arrowDiv.textContent = arrowText isExpanded
                    if not arrowOnly
                        th.textDiv.textContent = textContent
            else if callbacks?                     
                callbacks.renderAxisHeaderCell th, textContent, attr, isExpanded, false
            else
                th.textContent = textContent

        createRowAxisHeaderTH = (attr, className, textContent, isArrow, isExpanded, attributes) ->
            th = createElement "th", className, attributes
            if not isArrow
                th.ondblclick = (event) -> callbacks.axisHeaderDblClickHandler event, th, attr if callbacks?
            renderRowAxisHeader th, textContent, attr, isArrow, isExpanded
            return th
            
        renderRowAxisHeader = (th, textContent, attr, isArrow, isExpanded) ->
            if callbacks?
                callbacks.renderAxisHeaderCell th, textContent, attr, isExpanded, isArrow
            else
                th.textContent = if isArrow then arrowText isExpanded else textContent
                
        createRowAttrHeaderTH = (rowKey, cellAttr, className, value, isArrow, isExpanded, attributes) ->
            th = createElement "th", className, attributes
            if not isArrow
                th.ondblclick = (event) -> callbacks.rowAttrHeaderDblClickHandler th, rowKey, cellAttr if callbacks?
            renderRowAttrHeader th, value, rowKey, cellAttr, isArrow, isExpanded
            return th

        renderRowAttrHeader = (th, value, rowKey, cellAttr, isArrow, isExpanded) ->
            if callbacks?
                callbacks.renderRowAttrHeaderCell th, value, rowKey, cellAttr, isExpanded, isArrow
            else 
                th.textContent = if isArrow then arrowText isExpanded else value
            
        createColAttrHeaderTH = (colKey, isSubtotal, className, value, isExpanded, colsData, attributes) ->
            th = createElement "th", className, attributes
            textElement = th
            if isExpanded?
                createArrowAndTextDivs th, "colAttrHeaderArrowDiv", "colAttrHeaderTextDiv"
                textElement = th.textDiv
                
            textElement.ondblclick = (event) -> callbacks.colAttrHeaderDblClickHandler event, textElement, colKey, isSubtotal if callbacks?
            if colKey.length == colAttrs.length or isSubtotal or colKey.length == 0
                colsData.push { width : getColumnWidth(true, colKey, null, false), colnode : attributes["data-colnode"] }      
            renderColAttrHeader th, value, colKey, isSubtotal, isExpanded, false
            return th

        renderColAttrHeader = (th, value, colKey, isSubtotal, isExpanded, arrowOnly) ->
            if isExpanded?
                if callbacks?
                    callbacks.renderColAttrHeaderCell th.arrowDiv, value, colKey, isSubtotal, isExpanded, true
                    if not arrowOnly
                        callbacks.renderColAttrHeaderCell th.textDiv, value, colKey, isSubtotal, isExpanded, false
                else
                    th.arrowDiv.textContent = arrowText isExpanded
                    if not arrowOnly
                        th.textDiv.textContent = value if not arrowOnly
            else if callbacks?
                callbacks.renderColAttrHeaderCell th, value, colKey, isSubtotal, isExpanded, false
            else
                th.textContent = value
        
        arrowText = (isExpanded) ->
          if isExpanded == true
              return " #{arrowExpanded} "
          else if isExpanded == false
              return " #{arrowCollapsed} "
          else
              return ""
              
        setAttributes = (e, attrs) ->
            e.setAttribute a, v for own a, v of attrs

        processColKeys = (keysArr) ->
            lastIdx = keysArr[0].length-1
            headers = children: []
            row = 0
            keysArr.reduce(
                (val0, k0) => 
                    col = 0
                    k0.reduce(
                        (acc, curVal, curIdx) => 
                            if not acc[curVal]
                                key = k0.slice 0, col+1
                                acc[curVal] =
                                    row: row
                                    col: col
                                    descendants: 0
                                    children: []
                                    value: curVal
                                    key: key 
                                    flatKey: key.join String.fromCharCode(0) 
                                    firstLeaf: null 
                                    leaves: 0
                                    parent: if col isnt 0 then acc else null
                                    childrenSpan: 0
                                acc.children.push curVal
                            if col > 0 
                                acc.descendants++
                            col++
                            if curIdx == lastIdx
                                node = headers
                                for i in [0..lastIdx-1] when lastIdx > 0
                                    node[k0[i]].leaves++
                                    if not node[k0[i]].firstLeaf 
                                        node[k0[i]].firstLeaf = acc[curVal]
                                    node = node[k0[i]]
                                return headers
                            return acc[curVal]
                        headers)
                    row++
                    return headers
                headers)
            return headers

        processRowKeys = (keysArr, className, splitPositions) ->
            lastIdx = keysArr[0].length-1
            headers = children: []
            row = 0
            keysArr.reduce(
                (val0, k0) => 
                    col = 0
                    curElement = []
                    k0.reduce(
                        (acc, curVal, curIdx) => 
                            curElement.push curVal
                            if splitPositions.indexOf(curIdx) != -1
                                flatCurElement = curElement.join String.fromCharCode(0)
                                if not acc[flatCurElement] 
                                    key = k0.slice 0, curIdx+1
                                    acc[flatCurElement] =
                                        row: row
                                        col: col
                                        descendants: 0
                                        children: []
                                        values: curElement
                                        text: flatCurElement
                                        key: key 
                                        flatKey: key.join String.fromCharCode(0)
                                        firstLeaf: null 
                                        leaves: 0
                                        parent: if col isnt 0 then acc else null
                                        childrenSpan: 0
                                    acc.children.push flatCurElement
                                if col > 0 
                                    acc.descendants++
                                col++
                                if curIdx == lastIdx
                                    node = acc
                                    while node?
                                        node.leaves++
                                        if not node.firstLeaf 
                                            node.firstLeaf = acc[flatCurElement]
                                        node = node.parent
                                    return headers 
                                curElement = [] 
                                return acc[flatCurElement]
                            else 
                                return acc
                        headers)
                    row++
                    return headers
                headers)
            return headers

        buildColAxisHeader = (axisHeaders, index, attrs, opts, disabledArrow) ->
            ah =
                text: attrs[index]
                expandedCount: 0
                expandables: 0
                attrHeaders: []
                clickStatus: clickStatusExpanded
                onClick: collapseColAxis

            isExpanded = true if not disabledArrow    
            hClass = classExpanded
            if index >= opts.collapseAt
                isExpanded = false
                hClass = classCollapsed
                ah.clickStatus = clickStatusCollapsed
                ah.onClick = expandAxis

            ah.th = createColAxisHeaderTH ah.text, "pvtAxisLabel #{hClass}", ah.text, isExpanded 
            if not disabledArrow
                ah.th.arrowDiv.onclick = (event) ->
                    event = event || window.event
                    ah.onClick axisHeaders, index, attrs, opts
            axisHeaders.ah.push ah
            return ah 

        arrowColumnIsNeeded = () ->
            return rowSplitPositions.length > 1

        buildRowAxisHeader = (axisHeaders, index, attrs, opts, disabledArrow) ->
            ah =
                text: ""
                values: []
                expandedCount: 0
                expandables: 0
                attrHeaders: []
                clickStatus: clickStatusExpanded
                onClick: collapseRowAxis
                ths: []

            isExpanded = true if not disabledArrow    
            hClass = classExpanded
            if index >= opts.collapseAt
                isExpanded = false
                hClass = classCollapsed
                ah.clickStatus = clickStatusCollapsed
                ah.onClick = expandAxis

            firstIndex = firstIndexInGroup opts.splitPositions, index
            for i in [firstIndex..opts.splitPositions[index]] 
                if i == firstIndex and arrowColumnIsNeeded()
                    zoomClassPart = if isExpanded? then classZoom else ""
                    arrowTh = createRowAxisHeaderTH attrs[i], "pvtAxisLabel #{hClass} #{zoomClassPart}", "", true, isExpanded, {style: "padding-left: #{rowArrowsPadding + index * rowArrowsLevelPadding}px;"}
                    ah.arrowTh = arrowTh
                    ah.ths.push arrowTh 
                th = createRowAxisHeaderTH attrs[i], "pvtAxisLabel #{hClass}", attrs[i], false          
                ah.ths.push th
                ah.values.push attrs[i]

            flatText = attrs[firstIndex..opts.splitPositions[index]].join String.fromCharCode(0)
            ah.text = flatText 
            if not disabledArrow 
                ah.arrowTh.onclick = (event) ->
                    event = event || window.event
                    ah.onClick axisHeaders, index, attrs, opts

            axisHeaders.ah.push ah
            return ah 

        buildAxisHeaders = (thead, rowAttrs, colAttrs, opts) ->
            colAxisHeaders =
                collapseAttrHeader: collapseCol
                expandAttrHeader: expandCol
                ah: []

            rowAxisHeaders =
                collapseAttrHeader: collapseRow
                expandAttrHeader: expandRow
                ah: []

            rowGroupsNumber = rowSplitPositions.length
            longestRowGroupLength = longestGroupLength rowSplitPositions

            rowsNumber = Math.max rowGroupsNumber, colAttrs.length 

            trs = []
            for row in [0...rowsNumber]
                tr = createElement "tr"
                trs.push tr
                thead.appendChild tr
                if row + rowGroupsNumber >= rowsNumber
                    curGroup = row - (rowsNumber - rowGroupsNumber)
                    disabled = curGroup == rowGroupsNumber - 1
                    ah = buildRowAxisHeader rowAxisHeaders, curGroup, rowAttrs, opts.rowSubtotalDisplay, disabled 
                    for th in ah.ths
                        tr.appendChild th  
                    ah.tr = tr
                    firstIndex = firstIndexInGroup rowSplitPositions, curGroup
                    groupLen = rowSplitPositions[curGroup] - firstIndex + 1   
                    if groupLen < longestRowGroupLength
                        tr.appendChild createElement "th", null, {colspan: longestRowGroupLength - groupLen}       
                else if row == 0 and longestRowGroupLength > 0
                    tr.appendChild createElement "th", null, {colspan: longestRowGroupLength + (if arrowColumnIsNeeded() then 1 else 0), rowspan: rowsNumber - rowGroupsNumber}        

                if row + colAttrs.length >= rowsNumber
                    curCol = row - (rowsNumber - colAttrs.length)
                    disabled = not colSubtotalIsEnabled opts.colSubtotalDisplay, curCol
                    ah = buildColAxisHeader colAxisHeaders, curCol, colAttrs, opts.colSubtotalDisplay, disabled
                    tr.appendChild ah.th
                    ah.tr = tr
                else if row == 0 and colAttrs.length > 0
                    tr.appendChild createElement "th", null, {rowspan: rowsNumber - colAttrs.length}      

            return [colAxisHeaders, rowAxisHeaders, trs]

        firstIndexInGroup = (splitPositions, groupIndex) ->
            return if groupIndex == 0 then 0 else splitPositions[groupIndex-1] + 1

        longestGroupLength = (splitPositions) ->
            len = 0
            for pos, i in splitPositions
                prev = if i == 0 then -1 else splitPositions[i-1]   
                len = Math.max len, pos - prev
            return len

        colSubtotalIsEnabled = (subtotalOpts, index) ->
            splitPositions = subtotalOpts.splitPositions
            if index == splitPositions[splitPositions.length - 1] or subtotalOpts.disableExpandCollapse or subtotalOpts.disableSubtotal
                return false
            return splitPositions.indexOf(index) != -1    

        buildColHeader = (axisHeaders, attrHeaders, h, rowAttrs, colAttrs, node, opts, colsData) ->
            # DF Recurse
            buildColHeader axisHeaders, attrHeaders, h[chKey], rowAttrs, colAttrs, node, opts, colsData for chKey in h.children
            # Process
            ah = axisHeaders.ah[h.col]
            ah.attrHeaders.push h

            h.node = node.counter
            h.onClick = collapseCol

            isExpanded = true if colSubtotalIsEnabled(opts.colSubtotalDisplay, h.col) and h.children.length isnt 0
            h.th = createColAttrHeaderTH h.key, false, "pvtColLabel #{classColShow} col#{h.row} colcol#{h.col} #{classColExpanded}", h.value, isExpanded, colsData,
                "data-colnode": h.node    
                "colspan": h.childrenSpan if h.children.length isnt 0
                

            if h.children.length isnt 0 and colSubtotalIsEnabled opts.colSubtotalDisplay, h.col
                ah.expandables++
                ah.expandedCount += 1
                h.th.colSpan++ if not opts.colSubtotalDisplay.hideOnExpand
                if not opts.colSubtotalDisplay.disableExpandCollapse
                    h.th.arrowDiv.onclick = (event) ->
                        event = event || window.event
                        h.onClick axisHeaders, h, opts.colSubtotalDisplay 
                h.sTh = createColAttrHeaderTH h.key, true, "pvtColLabelFiller #{classColShow} col#{h.row} colcol#{h.col} #{classColExpanded}", "", undefined, colsData, 
                    "data-colnode": h.node
                    "rowspan":  colAttrs.length-h.col
                replaceClass h.sTh, classColShow, classColHide if opts.colSubtotalDisplay.hideOnExpand
                h[h.children[0]].tr.appendChild h.sTh

            h.parent?.childrenSpan += h.th.colSpan

            h.clickStatus = clickStatusExpanded
            ah.tr.appendChild h.th
            h.tr = ah.tr
            attrHeaders.push h
            node.counter++ 


        buildRowTotalsHeader = (tr, span, colsWidth) ->
            th = createColAttrHeaderTH [], false, "pvtTotalLabel rowTotal", "", undefined, colsWidth,  
                rowspan: span
            tr.appendChild th

        buildRowHeader = (tbody, axisHeaders, attrHeaders, h, rowAttrs, colAttrs, node, opts) ->
            buildRowHeader tbody, axisHeaders, attrHeaders, h[chKey], rowAttrs, colAttrs, node, opts for chKey in h.children

            ah = axisHeaders.ah[h.col]
            ah.attrHeaders.push h

            h.node = node.counter
            h.onClick = collapseRow
            firstChild = h[h.children[0]] if h.children.length isnt 0

            colSpan = 1 + longestGroupLength(rowSplitPositions) - h.values.length
            colSpan += 1 if colAttrs.length > 0
            h.tr = createElement "tr", "row#{h.row}"
            if h.children.length is 0
                tbody.appendChild h.tr
            else
                tbody.insertBefore h.tr, firstChild.tr

            h.ths = []

            if h.children.length isnt 0
                isExpanded = true
                arrowOpts = {style: "padding-left: #{rowArrowsPadding + h.col * rowArrowsLevelPadding}px;"}
            else
                arrowOpts = {}
            
            zoomClassPart = if isExpanded? then classZoom else ""
            arrowClass = "pvtRowLabel #{classRowShow} row#{h.row} rowcol#{h.col} #{classRowExpanded} #{zoomClassPart}"
            h.arrowTh = createRowAttrHeaderTH h.key, undefined, arrowClass, "", true, isExpanded, arrowOpts
            if arrowColumnIsNeeded()
                h.ths.push h.arrowTh
                h.tr.appendChild h.arrowTh
            
            for i in [0...h.values.length]
                attrIndex = i + firstIndexInGroup rowSplitPositions, h.col
                thClass = "pvtRowLabel #{classRowShow} row#{h.row} rowcol#{h.col} #{classRowExpanded}"
                th = createRowAttrHeaderTH h.key, rowAttrs[attrIndex], thClass, h.values[i], false, undefined, 
                    "data-rownode": h.node
                th.colSpan = colSpan if i+1 == h.values.length
                
                h.ths.push th
                h.tr.appendChild th

            if h.children.length isnt 0 
                ++ah.expandedCount
                ++ah.expandables
                if not opts.rowSubtotalDisplay.disableExpandCollapse and arrowColumnIsNeeded()
                    h.arrowTh.onclick = (event) ->
                        event = event || window.event
                        h.onClick axisHeaders, h, opts.rowSubtotalDisplay

                if not opts.rowSubtotalDisplay.displayOnTop
                    h.sTr = createElement "tr", "row#{h.row}"
                    tbody.appendChild h.sTr

            h.parent?.childrenSpan += 1

            h.clickStatus = clickStatusExpanded
            attrHeaders.push h
            node.counter++

        getTableEventHandlers = (value, rowKey, colKey, rowAttrs, colAttrs, opts) ->
            return if not opts.table?.eventHandlers
            eventHandlers = {}
            for own event, handler of opts.table.eventHandlers
                filters = {}
                filters[attr] = colKey[i] for own i, attr of colAttrs when colKey[i]?
                filters[attr] = rowKey[i] for own i, attr of rowAttrs when rowKey[i]?
                eventHandlers[event] = (e) -> handler(e, value, filters, pivotData)
            return eventHandlers

        buildValues = (tbody, colAttrHeaders, rowAttrHeaders, rowAttrs, colAttrs, opts) ->
            for rh in rowAttrHeaders
                rCls = "pvtVal row#{rh.row} rowcol#{rh.col} #{classRowExpanded}"
                if rh.children.length > 0
                    rCls += " pvtRowSubtotal"
                    rCls += if opts.rowSubtotalDisplay.hideOnExpand then " #{classRowHide}" else "  #{classRowShow}"
                else
                    rCls += " #{classRowShow}"
                tr = if rh.sTr then rh.sTr else rh.tr
                for ch in colAttrHeaders when ch.col is colAttrs.length-1 or (ch.children.length isnt 0 and colSubtotalIsEnabled opts.colSubtotalDisplay, ch.col)
                    aggregator = tree[rh.flatKey][ch.flatKey] ? { value: (-> null), format: -> "" }
                    val = aggregator.value()
                    cls = " #{rCls} col#{ch.row} colcol#{ch.col} #{classColExpanded}"
                    if ch.children.length > 0
                        cls += " pvtColSubtotal"
                        cls += if opts.colSubtotalDisplay.hideOnExpand then " #{classColHide}" else " #{classColShow}"
                    else
                        cls += " #{classColShow}"
                    td = createValueTD val, rh.key, ch.key, aggregator, cls, 
                        "data-value": val
                        "data-rownode": rh.node
                        "data-colnode": ch.node,
                        getTableEventHandlers val, rh.key, ch.key, rowAttrs, colAttrs, opts

                    tr.appendChild td

                    
                # buildRowTotal
                totalAggregator = rowTotals[rh.flatKey]
                val = totalAggregator.value()
                td = createValueTD val, rh.key, [], totalAggregator, "pvtTotal rowTotal #{rCls}",
                    "data-value": val
                    "data-row": "row#{rh.row}"
                    "data-rowcol": "col#{rh.col}"
                    "data-rownode": rh.node,
                    getTableEventHandlers val, rh.key, [], rowAttrs, colAttrs, opts
                tr.appendChild td

        buildColTotalsHeader = (rowHeadersColumns, colAttrs) ->
            tr = createElement "tr"
            colspan = rowHeadersColumns + (if colAttrs.length == 0 then 0 else 1) + (if arrowColumnIsNeeded() then 1 else 0) 
            th = createRowAttrHeaderTH [], undefined, "pvtTotalLabel colTotal", "", false, undefined, {colspan: colspan}
            tr.appendChild th
            return tr

        buildColTotals = (tr, attrHeaders, rowAttrs, colAttrs, opts) ->
            for h in attrHeaders when  h.col is colAttrs.length-1 or (h.children.length isnt 0 and colSubtotalIsEnabled opts.colSubtotalDisplay, h.col)
                clsNames = "pvtVal pvtTotal colTotal #{classColExpanded} col#{h.row} colcol#{h.col}"
                if h.children.length isnt 0
                    clsNames += " pvtColSubtotal" 
                    clsNames += if opts.colSubtotalDisplay.hideOnExpand then " #{classColHide}" else " #{classColShow}"
                else
                    clsNames += " #{classColShow}"
                totalAggregator = colTotals[h.flatKey]
                val = totalAggregator.value()
                td = createValueTD val, [], h.key, totalAggregator, clsNames, 
                    "data-value": val
                    "data-for": "col#{h.col}"
                    "data-colnode": "#{h.node}",
                    getTableEventHandlers val, [], h.key, rowAttrs, colAttrs, opts
                tr.appendChild td

        buildGrandTotal = (tbody, tr, rowAttrs, colAttrs, opts) ->
            totalAggregator = allTotal
            val = totalAggregator.value()
            td = createValueTD val, [], [], totalAggregator, "pvtGrandTotal", 
                {"data-value": val},
                getTableEventHandlers val, [], [], rowAttrs, colAttrs, opts
            tr.appendChild td
            tbody.appendChild tr

        collapseColAxisHeaders = (axisHeaders, col, opts) ->
            for i in [col..axisHeaders.ah.length-2]
                if colSubtotalIsEnabled opts, i
                    ah = axisHeaders.ah[i]
                    replaceClass ah.th, classExpanded, classCollapsed
                    renderColAxisHeader ah.th, ah.text, ah.text, false, true
                    ah.clickStatus = clickStatusCollapsed
                    ah.onClick = expandAxis

        collapseRowAxisHeaders = (axisHeaders, row, opts) ->
            for i in [row..axisHeaders.ah.length-2]
                ah = axisHeaders.ah[i]
                for th in ah.ths
                    replaceClass th, classExpanded, classCollapsed
                renderRowAxisHeader ah.arrowTh, "", ah.text, true, false      
                ah.clickStatus = clickStatusCollapsed
                ah.onClick = expandAxis

        adjustColAxisHeader = (axisHeaders, col, opts) ->
            ah = axisHeaders.ah[col]
            if ah.expandedCount is 0
                collapseColAxisHeaders axisHeaders, col, opts
            else if ah.expandedCount is ah.expandables
                replaceClass ah.th, classCollapsed, classExpanded
                renderColAxisHeader ah.th, ah.text, ah.text, true, true
                ah.clickStatus = clickStatusExpanded
                ah.onClick = collapseColAxis

        adjustRowAxisHeader = (axisHeaders, row, opts) ->
            ah = axisHeaders.ah[row]
            if ah.expandedCount is 0
                collapseRowAxisHeaders axisHeaders, row, opts
            else if ah.expandedCount is ah.expandables
                for th in ah.ths
                    replaceClass th, classCollapsed, classExpanded
                renderRowAxisHeader ah.arrowTh, "", ah.text, true, true
                ah.clickStatus = clickStatusExpanded
                ah.onClick = collapseRowAxis

        hideChildCol = (ch) ->
            outerDiv = $(ch.th).closest 'div.subtotalouterdiv'
            outerDiv
                .find "tbody tr td[data-colnode=\"#{ch.node}\"], th[data-colnode=\"#{ch.node}\"]" 
                .removeClass classColShow 
                .addClass classColHide

            col = outerDiv.find "colgroup col[data-colnode=\"#{ch.node}\"]"
            col?.removeClass classColShow
            col?.addClass classColHide
            
        collapseHiddenColSubtotal = (h, opts) ->
            $(h.th).closest 'div.subtotalouterdiv'
                .find "tbody tr td[data-colnode=\"#{h.node}\"], th[data-colnode=\"#{h.node}\"]" 
                .removeClass classColExpanded
                .addClass classColCollapsed
            if h.children.length isnt 0
                renderColAttrHeader h.th, h.value, h.key, false, false, true
            h.th.colSpan = 1
            
        collapseShowColSubtotal = (h, opts) ->
            outerDiv = $(h.th).closest 'div.subtotalouterdiv'
            outerDiv
                .find "tbody tr td[data-colnode=\"#{h.node}\"], th[data-colnode=\"#{h.node}\"]" 
                .removeClass classColExpanded
                .addClass classColCollapsed
                .removeClass classColHide
                .addClass classColShow

            col = outerDiv.find "colgroup col[data-colnode=\"#{h.node}\"]"
            col?.removeClass classColHide
            col?.addClass classColShow
                        
            if h.children.length isnt 0
                renderColAttrHeader h.th, h.value, h.key, false, false, true
            h.th.colSpan = 1

        collapseChildCol = (ch, h) ->
            collapseChildCol ch[chKey], h for chKey in ch.children when hasClass ch[chKey].th, classColShow
            hideChildCol ch

        collapseCol = (axisHeaders, h, opts) ->
            colSpan = h.th.colSpan - 1
            collapseChildCol h[chKey], h for chKey in h.children when hasClass h[chKey].th, classColShow
            if colSubtotalIsEnabled opts, h.col
                if hasClass h.th, classColHide
                    collapseHiddenColSubtotal h, opts
                else 
                    collapseShowColSubtotal h, opts
            if not hasClass h.th, classColHide        
                p = h.parent
                while p
                    p.th.colSpan -= colSpan
                    p = p.parent
                emptyTopAttrTH?.colSpan -= colSpan    
            h.clickStatus = clickStatusCollapsed
            h.onClick = expandCol
            axisHeaders.ah[h.col].expandedCount--
            adjustColAxisHeader axisHeaders, h.col, opts

        showChildCol = (ch) ->
            outerDiv = $(ch.th).closest 'div.subtotalouterdiv'
            outerDiv
                .find "tbody tr td[data-colnode=\"#{ch.node}\"], th[data-colnode=\"#{ch.node}\"]"
                .removeClass classColHide
                .addClass classColShow

            col = outerDiv.find "colgroup col[data-colnode=\"#{ch.node}\"]"
            col?.removeClass classColHide
            col?.addClass classColShow

        expandHideColSubtotal = (h) ->
            outerDiv = $(h.th).closest 'div.subtotalouterdiv'
            outerDiv
                .find "tbody tr td[data-colnode=\"#{h.node}\"], th[data-colnode=\"#{h.node}\"]" 
                .removeClass "#{classColCollapsed} #{classColShow}" 
                .addClass "#{classColExpanded} #{classColHide}"

            col = outerDiv.find "colgroup col[data-colnode=\"#{h.node}\"]"
            col?.removeClass classColShow
            col?.addClass classColHide
            
            replaceClass h.th, classColHide, classColShow
            renderColAttrHeader h.th, h.value, h.key, false, true, true

        expandShowColSubtotal = (h) ->
            outerDiv = $(h.th).closest 'div.subtotalouterdiv'
            outerDiv
                .find "tbody tr td[data-colnode=\"#{h.node}\"], th[data-colnode=\"#{h.node}\"]" 
                .removeClass "#{classColCollapsed} #{classColHide}"
                .addClass "#{classColExpanded} #{classColShow}"

            col = outerDiv.find "colgroup col[data-colnode=\"#{h.node}\"]"
            col?.removeClass classColHide
            col?.addClass classColShow
            
            h.th.colSpan++
            renderColAttrHeader h.th, h.value, h.key, false, true, true

        expandChildCol = (ch, opts) ->
            if ch.children.length isnt 0 and opts.hideOnExpand and ch.clickStatus is clickStatusExpanded
                replaceClass ch.th, classColHide, classColShow
            else
                showChildCol ch
            if ch.sTh and ch.clickStatus is clickStatusExpanded and opts.hideOnExpand
                replaceClass ch.sTh, classColShow, classColHide
            expandChildCol ch[chKey], opts for chKey in ch.children if (ch.clickStatus is clickStatusExpanded or not colSubtotalIsEnabled opts, ch.col)
            
        expandCol = (axisHeaders, h, opts) ->
            if h.clickStatus is clickStatusExpanded
                adjustColAxisHeader axisHeaders, h.col, opts
                return
            colSpan = 0
            for chKey in h.children
                ch = h[chKey]
                expandChildCol ch, opts
                colSpan += ch.th.colSpan
            h.th.colSpan = colSpan

            if colSubtotalIsEnabled opts, h.col
                if opts.hideOnExpand
                    expandHideColSubtotal h
                    --colSpan
                else
                    expandShowColSubtotal h
            p = h.parent
            while p
                p.th.colSpan += colSpan
                p = p.parent
            emptyTopAttrTH?.colSpan += colSpan

            h.clickStatus = clickStatusExpanded
            h.onClick = collapseCol
            axisHeaders.ah[h.col].expandedCount++
            adjustColAxisHeader axisHeaders, h.col, opts

        hideChildRow = (ch, opts) ->
            replaceClass cell, classRowShow, classRowHide for cell in ch.tr.querySelectorAll "th, td"
            replaceClass cell, classRowShow, classRowHide for cell in ch.sTr.querySelectorAll "th, td" if ch.sTr

        collapseShowRowSubtotal = (h, opts) ->
            renderRowAttrHeader h.arrowTh, "", h.key, undefined, true, false
            for cell in h.tr.querySelectorAll "th, td"
                removeClass cell, "#{classRowExpanded}"
                addClass cell, "#{classRowCollapsed}"
            if h.sTr
                for cell in h.sTr.querySelectorAll "th, td"
                    removeClass cell, "#{classRowExpanded}"
                    addClass cell, "#{classRowCollapsed}"

        collapseChildRow = (ch, h, opts) ->
            collapseChildRow ch[chKey], h, opts for chKey in ch.children
            hideChildRow ch, opts

        collapseRow = (axisHeaders, h, opts) ->
            collapseChildRow h[chKey], h, opts for chKey in h.children
            collapseShowRowSubtotal h, opts
            h.clickStatus = clickStatusCollapsed
            h.onClick = expandRow
            axisHeaders.ah[h.col].expandedCount--
            adjustRowAxisHeader axisHeaders, h.col, opts
            callbacks.checkPadding()

        showChildRow = (ch, opts) ->
            replaceClass cell, classRowHide, classRowShow for cell in ch.tr.querySelectorAll "th, td"
            replaceClass cell, classRowHide, classRowShow for cell in ch.sTr.querySelectorAll "th, td" if ch.sTr

        expandShowRowSubtotal = (h, opts) ->
            renderRowAttrHeader h.arrowTh, "", h.key, undefined, true, true
            for cell in h.tr.querySelectorAll "th, td"
                removeClass cell, "#{classRowCollapsed} #{classRowHide}"
                addClass cell, "#{classRowExpanded} #{classRowShow}"
            if h.sTr
                for cell in h.sTr.querySelectorAll "th, td"
                    removeClass cell, "#{classRowCollapsed} #{classRowHide}"
                    addClass cell, "#{classRowExpanded} #{classRowShow}"

        expandHideRowSubtotal = (h, opts) ->
            renderRowAttrHeader h.arrowTh, "", h.key, undefined, true, true
            for cell in h.tr.querySelectorAll "th, td"
                removeClass cell, "#{classRowCollapsed} #{classRowShow}"
                addClass cell, "#{classRowExpanded} #{classRowHide}"
            for th in h.ths    
                removeClass th, "#{classRowCollapsed} #{classRowHide}"
            addClass cell, "#{classRowExpanded} #{classRowShow}"
            if h.sTr
                for cell in h.sTr.querySelectorAll "th, td"
                    removeClass cell, "#{classRowCollapsed} #{classRowShow}"
                    addClass cell, "#{classRowExpanded} #{classRowHide}"

        expandChildRow = (ch, opts) ->
            if ch.children.length isnt 0 and opts.hideOnExpand and ch.clickStatus is clickStatusExpanded
                for th in ch.ths
                    replaceClass th, classRowHide, classRowShow
            else
                showChildRow ch, opts
            if ch.sTh and ch.clickStatus is clickStatusExpanded and opts.hideOnExpand
                replaceClass ch.sTh, classRowShow, classRowHide
            expandChildRow ch[chKey], opts for chKey in ch.children if (ch.clickStatus is clickStatusExpanded)

        expandRow = (axisHeaders, h, opts) ->
            if h.clickStatus is clickStatusExpanded
                adjustRowAxisHeader axisHeaders, h.col, opts
                return
            for chKey in h.children
                ch = h[chKey]
                expandChildRow ch, opts
            if h.children.length isnt 0 
                if opts.hideOnExpand
                    expandHideRowSubtotal h, opts
                else
                    expandShowRowSubtotal h, opts
            h.clickStatus = clickStatusExpanded
            h.onClick = collapseRow
            axisHeaders.ah[h.col].expandedCount++
            adjustRowAxisHeader axisHeaders, h.col, opts
            callbacks.checkPadding()
    
        collapseColAxis = (axisHeaders, col, attrs, opts) ->
            for i in [attrs.length-2..col] by -1
                if colSubtotalIsEnabled opts, i
                    for h in axisHeaders.ah[i].attrHeaders when h.clickStatus is clickStatusExpanded and h.children.length isnt 0
                        axisHeaders.collapseAttrHeader axisHeaders, h, opts

        collapseRowAxis = (axisHeaders, row, attrs, opts) ->
            for i in [axisHeaders.ah.length-2..row] by -1
                for h in axisHeaders.ah[i].attrHeaders when h.clickStatus is clickStatusExpanded and h.children.length isnt 0
                    axisHeaders.collapseAttrHeader axisHeaders, h, opts 

        expandAxis = (axisHeaders, col, attrs, opts) ->
            ah = axisHeaders.ah[col]
            axisHeaders.expandAttrHeader axisHeaders, h, opts for h in axisHeaders.ah[i].attrHeaders for i in [0..col] 
            # when h.clickStatus is clickStatusCollapsed and h.children.length isnt 0 for i in [0..col] 

        createColGroup = (columnData) ->
            colgroup = createElement "colgroup"
            for cdata in columnData
                column = createElement "col", null, { style :  "width: #{cdata.width}px", "data-colnode" : cdata.colnode }
                colgroup.appendChild column
            return colgroup
        
        getColumnWidth = (isAttrColumn, colKeyValues, axisValues, isArrow) ->
            if callbacks?
                return callbacks.getColumnWidth isAttrColumn, colKeyValues, axisValues, isArrow, rowSplitPositions.length - 1
            else
                return if isArrow then 15 + 10 * (rowSplitPositions.length - 1) else 50
            
        rowHeaderColsData = (trs, rowAttrsCnt) ->
            if trs.length > 0
                colCnt = findAxisHeadersColCount trs[0]
                columns = ([] for i in [0...colCnt])
                colsData = ({ width: 0 } for i in [0...colCnt])
                
                first = 0
                if arrowColumnIsNeeded() 
                    colsData[0].width = getColumnWidth false, null, null, true
                    first = 1

                lastShift = 0                    
                if colAttrs.length > 0
                    colsData[colCnt-1].width = getColumnWidth false, null, [], false
                    lastShift = 1
                    
                for rowIndex in [(trs.length-rowAttrsCnt)...trs.length]
                    tr = trs[rowIndex]
                    curColumn = first
                    for i in [first...(tr.cells.length - lastShift)]
                        th = tr.cells[i]
                        columns[curColumn].push th.textContent if th.textContent
                        curColumn += th.colSpan
                
                for i in [first...(colCnt - lastShift)] 
                    colsData[i].width = getColumnWidth false, null, columns[i], false  
                return colsData
            else    
                return [getColumnWidth(false, null, [], false)]
            
        findAxisHeadersColCount = (tr) ->
            colCnt = 0
            for i in [0...tr.cells.length]
                th = tr.cells[i]
                colCnt += th.colSpan
            return colCnt
            
        main = (rowAttrs, rowKeys, colAttrs, colKeys) ->
            rowAttrHeaders = []
            colAttrHeaders = []

            colKeyHeaders = processColKeys colKeys if colAttrs.length isnt 0 and colKeys.length isnt 0
            rowKeyHeaders = processRowKeys rowKeys, "pvtRowLabel", rowSplitPositions if rowAttrs.length isnt 0 and rowKeys.length isnt 0

            outerDiv = createElement "div", "subtotalouterdiv"

            headerDiv = createElement "div", "headerdiv" 
            headerTable = createElement "table", "headertable pvtTable"

            thead = createElement "thead"

            outerDiv.appendChild headerDiv
            headerDiv.appendChild headerTable
            headerTable.appendChild thead

            [colAxisHeaders, rowAxisHeaders, trs] = buildAxisHeaders thead, rowAttrs, colAttrs, opts
            colsData = rowHeaderColsData trs, rowAxisHeaders.ah.length

            if colAttrs.length isnt 0 
                overallSpan = 0
                if colKeyHeaders?
                    node = counter: 0
                    for chKey in colKeyHeaders.children
                        buildColHeader colAxisHeaders, colAttrHeaders, colKeyHeaders[chKey], rowAttrs, colAttrs, node, opts, colsData 
                        overallSpan += colKeyHeaders[chKey].th.colSpan

                buildRowTotalsHeader colAxisHeaders.ah[0].tr, colAttrs.length, colsData
                rowAttrHeadersCount = rowSplitPositions.length
                if rowAttrHeadersCount > colAttrs.length
                    emptyTopAttrTH = createElement "th", null, {colspan: overallSpan + 1, rowspan: rowAttrHeadersCount - colAttrs.length}
                    rowAxisHeaders.ah[0].tr.appendChild emptyTopAttrTH

            bodyDiv = createElement "div", "bodydiv"
            scrollDiv = createElement "div", "scrolldiv"
            scrollDiv.onscroll = () ->
                sLeft = scrollDiv.scrollLeft
                headerDiv.scrollLeft = sLeft
            bodyTable = createElement "table", "bodytable pvtTable"
            tbody = createElement "tbody"

            outerDiv.appendChild bodyDiv
            bodyDiv.appendChild scrollDiv
            scrollDiv.appendChild bodyTable
            bodyTable.appendChild tbody
            
            if rowAttrs.length isnt 0
                buildRowTotalsHeader rowAxisHeaders.ah[0].tr, rowSplitPositions.length, colsData if colAttrs.length is 0
                if rowKeyHeaders?
                    node = counter: 0
                    buildRowHeader tbody, rowAxisHeaders, rowAttrHeaders, rowKeyHeaders[chKey], rowAttrs, colAttrs, node, opts for chKey in rowKeyHeaders.children

            buildValues tbody, colAttrHeaders, rowAttrHeaders, rowAttrs, colAttrs, opts
            tr = buildColTotalsHeader longestGroupLength(rowSplitPositions), colAttrs
            buildColTotals tr, colAttrHeaders, rowAttrs, colAttrs, opts if colAttrs.length > 0
            buildGrandTotal tbody, tr, rowAttrs, colAttrs, opts

            collapseColAxis colAxisHeaders, opts.colSubtotalDisplay.collapseAt, colAttrs, opts.colSubtotalDisplay
            collapseRowAxis rowAxisHeaders, opts.rowSubtotalDisplay.collapseAt, rowAttrs, opts.rowSubtotalDisplay

            headerTable.setAttribute "data-numrows", rowKeys.length
            headerTable.setAttribute "data-numcols", colKeys.length
            headerTable.style.display = ""
            
            bodyTable.setAttribute "data-numrows", rowKeys.length
            bodyTable.setAttribute "data-numcols", colKeys.length
            bodyTable.style.display = ""

            headerTable.insertBefore createColGroup(colsData), headerTable.firstChild
            bodyTable.insertBefore createColGroup(colsData), bodyTable.firstChild
            
            return outerDiv

        return main rowAttrs, rowKeys, colAttrs, colKeys

    $.pivotUtilities.subtotal_renderers =
        "TABLE"             : (pvtData, opts) -> SubtotalRenderer pvtData, opts
        "TABLE_BARCHART"    : (pvtData, opts) -> $(SubtotalRenderer pvtData, opts).barchart()
        "TABLE_HEATMAP"     : (pvtData, opts) -> $(SubtotalRenderer pvtData, opts).heatmap "heatmap", opts
        "TABLE_ROW_HEATMAP" : (pvtData, opts) -> $(SubtotalRenderer pvtData, opts).heatmap "rowheatmap", opts
        "TABLE_COL_HEATMAP" : (pvtData, opts) -> $(SubtotalRenderer pvtData, opts).heatmap "colheatmap", opts
            
    # 
    # Aggregators
    #
    usFmtPct = $.pivotUtilities.numberFormat digitsAfterDecimal:1, scaler: 100, suffix: "%"
    aggregatorTemplates = $.pivotUtilities.aggregatorTemplates;

    subtotalAggregatorTemplates =
        fractionOf: (wrapped, type="row", formatter=usFmtPct) -> (x...) -> (data, rowKey, colKey) ->
            rowKey = [] if typeof rowKey is "undefined"
            colKey = [] if typeof colKey is "undefined"
            selector: {row: [rowKey.slice(0, -1),[]], col: [[], colKey.slice(0, -1)]}[type]
            inner: wrapped(x...)(data, rowKey, colKey)
            push: (record) -> @inner.push record
            format: formatter
            value: -> @inner.value() / data.getAggregator(@selector...).inner.value()
            numInputs: wrapped(x...)().numInputs

    $.pivotUtilities.subtotalAggregatorTemplates = subtotalAggregatorTemplates

    $.pivotUtilities.subtotal_aggregators = do (tpl = aggregatorTemplates, sTpl = subtotalAggregatorTemplates) ->
        "Sum As Fraction Of Parent Row":        sTpl.fractionOf(tpl.sum(), "row", usFmtPct)
        "Sum As Fraction Of Parent Column":     sTpl.fractionOf(tpl.sum(), "col", usFmtPct)
        "Count As Fraction Of Parent Row":      sTpl.fractionOf(tpl.count(), "row", usFmtPct)
        "Count As Fraction Of Parent Column":   sTpl.fractionOf(tpl.count(), "col", usFmtPct)


function renderTable(element, listJsObject) {
    if (typeof listJsObject !== 'undefined') {
        var len = listJsObject.length;
        var _table_ = document.createElement('table'),
            _tr_ = document.createElement('tr'),
            _th_ = document.createElement('th'),
            _td_ = document.createElement('td');

        var table = _table_.cloneNode(false),
            columns = addAllColumnHeaders(listJsObject, table);
        for (var i = 0, maxi = len; i < maxi; ++i) {
            var tr = _tr_.cloneNode(false);
            for (var j = 0, maxj = columns.length; j < maxj; ++j) {
                var td = _td_.cloneNode(false);
                cellValue = listJsObject[i][columns[j]];
                td.appendChild(document.createTextNode(listJsObject[i][columns[j]] || ''));
                tr.appendChild(td);
            }
            table.appendChild(tr);
        }
        element.appendChild(table);
        element.style.overflow = 'auto';
    }

    function addAllColumnHeaders(arr, table) {
        var columnSet = [],
            tr = _tr_.cloneNode(false);
        for (var i = 0, l = arr.length; i < l; i++) {
            for (var key in arr[i]) {
                if (arr[i].hasOwnProperty(key) && columnSet.indexOf(key) === -1) {
                    columnSet.push(key);
                    var th = _th_.cloneNode(false);
                    th.appendChild(document.createTextNode(key));
                    tr.appendChild(th);
                }
            }
        }
        table.appendChild(tr);
        return columnSet;
    }
}

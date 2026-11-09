// the view UiReactRowEditorsIT drives: its own value beside each editor the platform draws, no build
function UiRreEditors(props) {
    const { data, controller } = props;
    const { Platform } = window.lsfusion;
    return <div className="ui-rre-board" tabIndex={0}>
        <div>
            <button onClick={() => { controller.uiRreAdd.exec(); }}>Add row</button>
            <button onClick={() => { controller.uiRreToggle.exec(); }}>Hide/show editors</button>
            <button onClick={() => { controller.uiRreLast.exec(); }}>Last from server</button>
            <button onClick={() => { controller.i.change(data.i.list[data.i.list.length - 1]); }}>Next page</button>
            <button onClick={() => { controller.i.filters.change([{property: 'qty', compare: '>=', value: 20}]); }}>Qty at least 20</button>
            <button onClick={() => { controller.i.filters.change([]); }}>Drop filter</button>
            <button onClick={() => { controller.i.orders.change([{property: 'qty', desc: true}]); }}>Qty descending</button>
        </div>
        {data.i.list.map(row => <div key={row.key} className="ui-rre-row" data-current={String(!!row.current)}
                                   style={{display: 'flex', alignItems: 'center', gap: 12, minHeight: 36}}>
            <span className="ui-rre-name">{row.name.value}</span>
            <span className="ui-rre-projected">{row.qtyShown.value}</span>
            <Platform name="i.qty" row={row} className="ui-rre-qty"/>
            <button className="ui-rre-select" onClick={() => { controller.i.change(row); }}>Make current</button>
            <button className="ui-rre-delete" onClick={() => { window.uiRreDeleteRequest = controller.i.removeRow.exec(row); }}>Delete row</button>
        </div>)}
    </div>;
}

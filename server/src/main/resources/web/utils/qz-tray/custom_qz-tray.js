async function QZConnect() {
    return new Promise(function(resolve, reject) {
        if (qz.websocket.isActive()) {	// if already active, resolve immediately
            resolve();
        } else {
            // try to connect once before firing the mimetype launcher
            qz.websocket.connect().then(resolve, function retry() {
                // if a connect was not successful, launch the mimetime, try 3 more times
                window.location.assign("qz:launch");
                qz.websocket.connect({ retries: 2, delay: 1 }).then(resolve, reject);
            });
        }
    });
}

async function QZFindPrinters() {
    QZConnect().then(function() {
        qz.printers.find().then(function(data) {
            var list = '';
            for(var i = 0; i < data.length; i++) {
                list += data[i] + "\n";
            }
            alert("Available printers:\n" + list);
        });
    }).catch(function(e) {
        console.error(e);
        alert('Error when finding printers : ' + e.message);
    });
}

async function QZPrintDefault (options, data) {
    QZConnect().then(function() {
        qz.printers.getDefault().then(function (printer) {
            QZPrint(printer, options, data)
        });
    }).catch(function(e) {
        console.error(e);
        alert('Error when getting default printer : \n' + e.message);
    });
}

async function QZPrint (printer, options, data) {
    QZConnect().then(function() {
        qz.print(qz.configs.create(printer, options), [data]).catch(function(e) {
            console.error(e);
            alert('Error when printing : \n' + e.message);
        })
    }).catch(function(e) {
        console.error(e);
        alert('Error when connecting QZ : \n' + e.message);
    });
}
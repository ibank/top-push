/*
    test multi-connects to ws-frontend in single process
    will ping
    just receive
*/

var base = require('./base'),
    VAR = base.var(),
    ws = base.ws,

	uri = process.argv[2],
    connects = parseInt(process.argv[3]),
    mode = process.argv[4];

function connect() {
    ws(null, null, true, true).connect(
        uri, 
        'wamp', //subprotocol using "Sec-WebSocket-Protocol" header
        'origin', //origin
        {
            id: 'top-push-client01'
        }//headers
    );
}

var i = 0;
function doConnect() {
    if(i++ == connects) return;
    connect();
    setTimeout(doConnect, 100);
}
function doParallelConnect() {
    for(var j = 0;j < connects; j++)
        connect();
}
function doParallelConnect2() {
    for(var j = 0;j < 100; j++) {
        if(i == connects) return;
        connect();
        i++;
    }
    setTimeout(doParallelConnect2, 200);
}

if(mode == 1)
    doParallelConnect();
else if(mode == 2)
    doParallelConnect2();
else
    doConnect();


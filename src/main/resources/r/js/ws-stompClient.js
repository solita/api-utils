// targetElement will receive 'stompConnected' and stompReceived' events
function stompClient(path, targetElement, headers) {
    var client = Stomp.client(window.location.protocol.replace('http','ws') + '//' + window.location.host + window.location.pathname + "stomp");
        client.connect(headers, c => {
            targetElement.dispatchEvent(new CustomEvent('stompConnected', { detail: c }));
            client.subscribe(path, m => { targetElement.dispatchEvent(new CustomEvent('stompReceived', { detail: {message: m} })); });
        });
    return client;
};

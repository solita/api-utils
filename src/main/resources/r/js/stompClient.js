function stompClient(path, handler) {
    var client = Stomp.client(window.location.protocol.replace('http','ws') + '//' + window.location.host + window.location.pathname + "stomp");
        client.connect({}, () => {
            client.subscribe(path, handler);
        });
    return client;
};
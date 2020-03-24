var time = function(timeInput) {
    timeInput.placeholder = new Date().toISOString().slice(0,11) + '00:00:00Z';
    var params = new URLSearchParams(window.location.search);
    if (params.get('time')) {
        timeInput.value = params.get('time');
    }
    timeInput.onchange = function() {
        let params = new URLSearchParams(window.location.search);
        if (timeInput.value) {
            params.set('time', timeInput.value);
        } else {
            params.delete('time');
        }
        window.location.search = decodeURIComponent(params);
    }
};

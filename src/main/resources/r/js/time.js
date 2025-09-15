var time = function(timeInput) {
    timeInput.placeholder = new Date().toISOString().slice(0,11) + '00:00:00Z';
    var params = new URLSearchParams(window.location.search);
    if (params.get('time')) {
        timeInput.value = params.get('time');
        timeInput.setAttribute('data-duration', durationFns.durationFns.toString(durationFns.durationFns.between(new Date(timeInput.value), new Date())));
    }
    timeInput.onchange = function() {
        let params = new URLSearchParams(window.location.search);
        if (timeInput.value) {
            params.set('time', timeInput.value);
            timeInput.setAttribute('data-duration', durationFns.durationFns.toString(durationFns.durationFns.between(new Date(timeInput.value), new Date())));
        } else {
            params.delete('time');
            timeInput.removeAttribute('data-duration');
        }
        window.location.search = decodeURIComponent(params);
    }
};

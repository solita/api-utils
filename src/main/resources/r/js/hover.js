var hover = function(map, callbackOver, callbackOut, callbackMultiple) {
    var hoverInteraction = new ol.interaction.Select({
        hitTolerance: 3,
        multi: true,
        condition: ol.events.condition.pointerMove
    });
    map.addInteraction(hoverInteraction);

    hoverInteraction.on('select', function(evt){
        var coord = evt.mapBrowserEvent.coordinate;
        var selected = evt.selected.filter(function(v,i) { return evt.selected.findIndex(function(e) { return e.getProperties().tunniste === v.getProperties().tunniste; }) === i; });
        
        if (selected.length > 1 && callbackMultiple) {
            callbackMultiple(selected, coord);
        } else if (selected.length > 0){
            callbackOver(selected[0], coord);
        } else {
            callbackOut(evt.deselected);
        }
    });
    return hoverInteraction;
};

var click = function(map, layers, callbackOver, callbackOut, callbackMultiple) {
    var clickInteraction = new ol.interaction.Select({
        hitTolerance: 3,
        multi: false,
        condition: ol.events.condition.singleClick
    });
    map.addInteraction(clickInteraction);

    clickInteraction.on('select', function(evt){
        var coord = evt.mapBrowserEvent.coordinate;
        var selected = evt.selected.filter(function(v,i) { return evt.selected.findIndex(function(e) { return e.getProperties().tunniste === v.getProperties().tunniste; }) === i; });
        
        if (selected.length > 1 && callbackMultiple) {
            callbackMultiple(selected, coord);
        } else if (selected.length > 0){
            callbackOver(selected[0], coord);
        } else {
            callbackOut(evt.deselected);
        }
    });
    return clickInteraction;
};

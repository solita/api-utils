var hover = function(map, layers, callbackOver, callbackOut, callbackMultiple) {
    var hoverInteraction = new ol.interaction.Select({
        hitTolerance: 3,
        multi: true,
        condition: ol.events.condition.pointerMove,
        layers: [].concat.apply([], layers.map(function(l) { return (l instanceof ol.layer.Group) ? l.getLayers().getArray() : l; }))
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

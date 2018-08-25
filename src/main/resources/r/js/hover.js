var hover = function(map, layers, callbackOver, callbackOut) {
    var hoverInteraction = new ol.interaction.Select({
        hitTolerance: 3,
        condition: ol.events.condition.pointerMove,
        layers: [].concat.apply([], layers.map(function(l) { return (l instanceof ol.layer.Group) ? l.getLayers().getArray() : l; }))
    });
    map.addInteraction(hoverInteraction);

    hoverInteraction.on('select', function(evt){
        var coord = evt.mapBrowserEvent.coordinate;
        if(evt.selected.length > 0){
            callbackOver(evt.selected[0], coord);
        } else {
            callbackOut();
        }
    });
    return hoverInteraction;
};

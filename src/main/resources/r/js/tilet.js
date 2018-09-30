var tilet = function(map, projection, tiletCheckbox, olstuff) {

	var tiletSource = new ol.source.Vector();
	var tileLayer = new ol.layer.Vector({
	    source: tiletSource,
	    style: new ol.style.Style({
	        fill: new ol.style.Fill({
	            color: [1, 5, 2, 0.3]
	        }),
	        stroke: new ol.style.Stroke({
	            color: [255, 255, 255, 1]
	        })
	    }),
	    projection: projection,
	    updateWhileInteracting: true,
	    updateWhileAnimating: true
	});
	
	var piirraTilet = function() {
	    var extent = map.getView().calculateExtent(map.getSize());
	
	    var reso = map.getView().getResolution();
	    var s = ol.loadingstrategy.tile(olstuff.tileGrid);
	
	    var tilet = s(extent, reso);
	
	    tilet.map(ol.geom.Polygon.fromExtent).forEach(function(poly) {
	        tiletSource.addFeature(new ol.Feature(poly));
	    });
	};
	
	var poistaTilet = function() {
	    tiletSource.clear();
	};
	
	var paivitaTilet = function() {
	    poistaTilet();
	    if ($(tiletCheckbox).is(':checked')) {
	        piirraTilet();
	    }
	};
	
	map.addLayer(tileLayer);
	map.on('moveend', paivitaTilet);
	
	$(tiletCheckbox).click(paivitaTilet);
};
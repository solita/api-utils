var constants = function() {
	return {
		// EPSG 3067
		world: [50199.4814, 6582464.0358, 761274.6247, 7799839.8902],
		
		// JHS-180
		jhs: [-548576, 6291456, 1548576, 8388608],
		
		dataExtent: [-24288,6553600,762144,7864320],
		
		// JHS-180-recommendation, with two removed from beginning and two appended to the end
		resolutions: [ /*8192, 4096,*/ 2048,  1024,   512,   256,   128,    64,    32,    16,     8,     4,     2,     1,    0.5,   0.25,  0.125,  0.0625],
		tileSizes:   [ /* 128,  256,*/  128, 256*1, 256*2, 256*4, 256*1, 256*2, 256*4, 256*8, 256*1, 256*2, 256*4, 256*8, 256*16, 256*32, 256*64, 256*128],
		
		// corresponding "good guesses" to svg-icon scalings
		scales:      [ /*0.25, 0.25,*/ 0.25,  0.25,  0.25,  0.25,  0.25,  0.25,  0.25,  0.25,  0.25,   0.5,  0.75,     1,   1.25,    1.5,      2,       3],
		
		renderBuffer: 500
	};
};
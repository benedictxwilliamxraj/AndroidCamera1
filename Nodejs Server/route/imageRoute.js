var express = require('express'),
	multer = require('multer');

const router = express.Router();

// configuring multer file limits and destination
var upload = multer({
	limits : {
		fieldNameSize : 999999999,
		fieldSize     : 999999999
	},
	dest   : `uploads/${new Date().getTime().toString()}/`
});

// requiring imageController for router
const imageController = require('../controller/imageController');

// defining routes
router.post('/image', upload.any(), imageController.uploadImage);

module.exports = router;

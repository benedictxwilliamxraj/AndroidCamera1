// requiring dependencies
var express = require('express'),
	path = require('path'),
	morgan = require('morgan'),
	bodyParser = require('body-parser'),
	multer = require('multer'),
	base64ToImage = require('base64-to-image'),
	fs = require('fs');

const app = express();

app.use(express.static('uploads/converted/'));
app.use(morgan('dev'));
app.use(bodyParser.json());
app.use(bodyParser.urlencoded({ extended: true }));
app.set('view engine', 'ejs');

const storage = multer.diskStorage({
	destination : function(req, file, cb) {
		cb(null, 'uploads/');
	},

	filename    : function(req, file, cb) {
		cb(null, file.fieldname + '-' + Date.now() + path.extname(file.originalname));
	}
});

app.get('/', function(req, res) {
	res.render('home');
});

app.use('/api', require('./route/imageRoute'));

app.post('/processBase64', (req, res) => {
	let upload = multer({ storage: storage }).single('base64File');

	upload(req, res, function(err) {
		if (req.fileValidationError) {
			return res.send(req.fileValidationError);
		} else if (!req.file) {
			return res.send('Please select a file to upload');
		} else if (err instanceof multer.MulterError) {
			return res.send(err);
		} else if (err) {
			return res.send(err);
		}

		console.log('File Path: ' + req.file.path);

		fs.readFile(req.file.path, 'utf-8', async function(err, data) {
			var filePath = './uploads/converted/';
			//removing new line characters from text file
			data = data.replace(/(\r\n|\n|\r)/gm, '');
			imageData = 'data:image/jpg;base64,' + data;
			fs.writeFile('finalString.txt', imageData, function(err) {
				if (err) return console.log(err);
				console.log('Written');
			});
			console.log(imageData.substr(0, 100));
			var fileObj = {
				fileName : 'image' + Date.now(),
				type     : 'jpg'
			};
			try {
				var imageInfo = await base64ToImage(imageData, filePath, fileObj);
				console.log(imageInfo);
				url = fileObj.fileName + '.' + fileObj.type;
				res.render('image', { url: url });
			} catch (e) {
				res.send('Some Error Occoured!');
				console.log('Some error occoured!' + e);
			}
		});
	});
});

// defining ports
const port = 3000 || process.env.PORT;

// listening on port 3000
app.listen(port, () => {
	console.log(`We are listening for requests at port ${3000}`);
});

// multer to upload image
var multer = require("multer"),
  fs = require("fs"),
  dateFormat = require("dateformat"),
  path = require("path");

module.exports = {
  uploadImage: async (req, res) => {
    let tmp_path = req.files[0].path;
    let folder_path = `./uploads/${dateFormat(
      new Date(),
      "yyyy-mm-dd_HH-MM"
    )}/`;
    let target_path = folder_path + req.files[0].originalname;
    if (!fs.existsSync(folder_path)) {
      fs.mkdirSync(folder_path);
    }
    var src = fs.createReadStream(tmp_path);
    var dest = fs.createWriteStream(target_path);
    console.log("targetPath: " + path.resolve(target_path));
    src.pipe(dest);
    src.on("end", function () {
      var fileName = req.files[0].originalname;
      var fullName = fileName.split("_");
      var frameName = fullName[0];
      var timestamp = fullName[1].split(".")[0];
      var timeNow = new Date().getTime();
      var timeDiff = parseInt(timeNow) - parseInt(timestamp);
      var log =
        frameName + "," + timestamp + "," + timeNow + "," + timeDiff + "ms";
      console.log(log);
      var stream = fs.createWriteStream("./deltaT.txt", { flags: "a" });
      stream.write(log + "\n");
      stream.end();

      const contents = fs.readFileSync(path.resolve(target_path), {
        encoding: "base64",
      });
      let currentTime=new Date();
      res.send(JSON.stringify({ date: currentTime.getHours()+"-"+currentTime.getMinutes()+"-"+currentTime.getSeconds()+"-"+currentTime.getMilliseconds(), file: contents }));
    });
    src.on("error", function (err) {
      res.sendStatus(500);
    });
  },
};

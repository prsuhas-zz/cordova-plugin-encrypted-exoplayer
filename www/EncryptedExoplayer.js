/**
 * EncryptedExoplayer.js
 *
 * @author Suhas P R
 * @license MIT
*/
var exec = require('cordova/exec');

var PLUGIN_NAME = 'EncryptedExoplayer';

function EncryptedExoplayer() {
}

EncryptedExoplayer.prototype.encrypt = function (filePath, outputPath, key, success, error) {
	exec(success || null, error || null, PLUGIN_NAME, "encrypt", [filePath, outputPath, key]);
};

EncryptedExoplayer.prototype.play = function (filePath, key, success, error) {
	exec(success || null, error || null, PLUGIN_NAME, "play", [filePath, key]);
};


EncryptedExoplayer.install = function () {
	if (!window.plugins) {
		window.plugins = {};
	}
	window.plugins.encryptedExoplayer = new EncryptedExoplayer();
	return window.plugins.encryptedExoplayer;
};

cordova.addConstructor(EncryptedExoplayer.install);

/**
 * EncryptedExoplayer.js
 *
 * @author Suhas P R
 * @license MIT
*/
var exec = require('cordova/exec');

var PLUGIN_NAME = 'EncryptedExoplayer';

var EncryptedExoplayer = {
	encrypt: function (filePath, outputPath, key, success, error) {
	    exec(success || null, error || null, PLUGIN_NAME, 'encrypt', [filePath, outputPath, key]);
	  },
	  play: function (filePath, key, success, error) {
	    exec(success || null, error || null, PLUGIN_NAME, 'play', [filePath, key]);
	  },
};

module.exports = EncryptedExoplayer;

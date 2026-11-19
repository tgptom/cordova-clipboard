var cordova = require('cordova');

var DEFAULT_EXPIRES_IN_MS = 120000;

/**
 * Clipboard plugin for Cordova
 * 
 * @constructor
 */
function Clipboard () {}

function isOptionsObject (value) {
	return value !== null && typeof value === "object";
}

function normalizeOptions (options) {
	options = isOptionsObject(options) ? options : {};

	var expiresInMs = Number(options.expiresInMs);
	if (!isFinite(expiresInMs) || expiresInMs < DEFAULT_EXPIRES_IN_MS) {
		expiresInMs = DEFAULT_EXPIRES_IN_MS;
	}

	return {
		expiresInMs: expiresInMs,
		sensitive: options.sensitive === true,
		localOnly: options.localOnly === true
	};
}

/**
 * Sets the clipboard content
 *
 * Supported call forms:
 *   copy(text, onSuccess, onFail)
 *   copy(text, onSuccess, onFail, options)
 *   copy(text, options, onSuccess, onFail)
 *
 * @param {String}   text      The content to copy to the clipboard
 * @param {Function} onSuccess The function to call in case of success (takes the copied text as argument)
 * @param {Function} onFail    The function to call in case of error
 * @param {Object}   options   Optional settings:
 *                               expiresInMs {Number}  Time until the copied text is removed from the clipboard.
 *                                                     Defaults to (and is never less than) 120000.
 *                               sensitive   {Boolean} Android 13+: hide the system clipboard preview.
 *                               localOnly   {Boolean} iOS: do not sync via Universal Clipboard.
 */
Clipboard.prototype.copy = function (text, onSuccess, onFail, options) {
	if (isOptionsObject(onSuccess)) {
		var shiftedOnFail = options;
		options = onSuccess;
		onSuccess = onFail;
		onFail = shiftedOnFail;
	}

	if (typeof text === "undefined" || text === null) text = "";
	cordova.exec(onSuccess, onFail, "Clipboard", "copy", [text, normalizeOptions(options)]);
};

/**
 * Gets the clipboard content
 *
 * @param {Function} onSuccess The function to call in case of success
 * @param {Function} onFail    The function to call in case of error
 */
Clipboard.prototype.paste = function (onSuccess, onFail) {
	cordova.exec(onSuccess, onFail, "Clipboard", "paste", []);
};

/**
 * Clear the clipboard content
 *
 * @param {Function} onSuccess The function to call in case of success
 * @param {Function} onFail    The function to call in case of error
 */
Clipboard.prototype.clear = function (onSuccess, onFail) {
	cordova.exec(onSuccess, onFail, "Clipboard", "clear", []);
};

// Register the plugin
var clipboard = new Clipboard();
module.exports = clipboard;

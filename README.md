Clipboard
=========

Clipboard management plugin for Cordova/PhoneGap that supports Android and iOS.

## Usage

```
cordova plugin add cordova-clipboard
```

The plugin creates the object `cordova.plugins.clipboard` with the methods `copy(text, onSuccess, onError)`, `paste(onSuccess, onError)` and `clear(onSuccess, onError)`.

`copy` also accepts an options object, either as the second or the fourth argument:

	cordova.plugins.clipboard.copy(text, onSuccess, onError);
	cordova.plugins.clipboard.copy(text, options, onSuccess, onError);
	cordova.plugins.clipboard.copy(text, onSuccess, onError, options);

| Option        | Default  | Description |
| ------------- | -------- | ----------- |
| `expiresInMs` | none     | Opt-in. Time after which the copied text is removed from the clipboard. Values below `120000` (or invalid values) fall back to `120000`. When omitted, the text stays on the clipboard as before. |
| `sensitive`   | `false`  | Android 13+: marks the clip with `ClipDescription.EXTRA_IS_SENSITIVE`, which hides the system clipboard preview. Other apps can still paste it. Ignored on iOS. |
| `localOnly`   | `false`  | iOS: sets `UIPasteboardOptionLocalOnly`, which prevents Universal Clipboard sync to other devices. Paste on the same device still works. Ignored on Android, which has no local-only clipboard. |

Copied text only expires when `expiresInMs` is passed. Calls without it keep the previous behaviour.

Example:

	var text = "Hello World!";

	cordova.plugins.clipboard.copy(text);

	cordova.plugins.clipboard.copy(text, { expiresInMs: 300000, sensitive: true, localOnly: true });

	cordova.plugins.clipboard.paste(function (text) { alert(text); });

	cordova.plugins.clipboard.clear();

## Supported platforms

- Android (`cordova-android >=15.0.0`)
- iOS (`cordova-ios >=8.0.0`)

## Notes

### All platforms

- The plugin only works with text content.

### Android

- Android clipboard access behavior is controlled by the OS. Reading clipboard content can be restricted when apps are in the background.
- Newer Android versions may also show system UI/notifications when clipboard content is read.
- Expiry (when `expiresInMs` is passed) is scheduled by the plugin. When it fires, the clipboard is cleared only if it still holds exactly the clip written by that `copy` call (same text and, on Android 8+, same clip timestamp). Content copied afterwards by the user or another app is never wiped. Calling `copy` again cancels and replaces the pending clear; calling `clear` cancels it.
- Android 10+ blocks clipboard reads while the app is not focused. If expiry fires while the app is in the background, the check runs as soon as the app regains focus. A pending clear is lost if the app process is killed.

### iOS

- iOS may show privacy indicators/prompts/notifications when clipboard content is read, depending on OS version and app state.
- Text is written with `setItems:options:` as `public.utf8-plain-text`, and `UIPasteboardOptionExpirationDate` is set from `expiresInMs` when it is passed. The system removes the item at expiry, even while the app is in the background.

## Acknowledgements

This plugin was inspired by [ClipboardManagerPlugin](https://github.com/jacob/ClipboardManagerPlugin) (Android) and [ClipboardPlugin](https://github.com/phonegap/phonegap-plugins/tree/master/iPhone/ClipboardPlugin) (iOS).

## License

The MIT License (MIT)

Copyright (c) 2013 Verso Solutions LLC

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.

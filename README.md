Clipboard
=========

Clipboard management plugin for Cordova/PhoneGap that supports Android and iOS.

## Usage

```
cordova plugin add cordova-clipboard
```

The plugin creates the object `cordova.plugins.clipboard` with the methods `copy(text, onSuccess, onError)`, `paste(onSuccess, onError)` and `clear(onSuccess, onError)`

Example:

	var text = "Hello World!";

	cordova.plugins.clipboard.copy(text);

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

### iOS

- iOS may show privacy indicators/prompts/notifications when clipboard content is read, depending on OS version and app state.

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

#import <Foundation/Foundation.h>
#import <UIKit/UIKit.h>
#import <Cordova/CDVPlugin.h>
#import <Cordova/CDVPluginResult.h>
#import "CDVClipboard.h"

static NSString * const CDVClipboardPlainTextType = @"public.utf8-plain-text";
static const double CDVClipboardDefaultExpiresInMs = 120000.0;

@implementation CDVClipboard

- (void)copy:(CDVInvokedUrlCommand*)command {
	id            rawText = [command argumentAtIndex:0 withDefault:@""];
	NSDictionary *options = [command argumentAtIndex:1 withDefault:nil andClass:[NSDictionary class]];

	NSString *text = @"";
	if ([rawText isKindOfClass:[NSString class]]) {
		text = rawText;
	} else if ([rawText respondsToSelector:@selector(stringValue)]) {
		text = [rawText stringValue];
	}

	// Expiry is opt-in: only enabled when expiresInMs is passed.
	double expiresInMs = 0;
	id expiresValue = options[@"expiresInMs"];
	if (expiresValue != nil && expiresValue != [NSNull null]) {
		expiresInMs = CDVClipboardDefaultExpiresInMs;
		if ([expiresValue isKindOfClass:[NSNumber class]]) {
			double requested = [expiresValue doubleValue];
			if (isfinite(requested) && requested > CDVClipboardDefaultExpiresInMs) {
				expiresInMs = requested;
			}
		}
	}

	id localOnlyValue = options[@"localOnly"];
	BOOL localOnly = [localOnlyValue isKindOfClass:[NSNumber class]] && [localOnlyValue boolValue];

	dispatch_async(dispatch_get_main_queue(), ^{
		NSMutableDictionary<UIPasteboardOption, id> *pasteboardOptions = [NSMutableDictionary dictionary];
		if (expiresInMs > 0) {
			pasteboardOptions[UIPasteboardOptionExpirationDate] = [NSDate dateWithTimeIntervalSinceNow:(expiresInMs / 1000.0)];
		}
		if (localOnly) {
			// Blocks Universal Clipboard (Handoff) sync only; same-device paste still works.
			pasteboardOptions[UIPasteboardOptionLocalOnly] = @YES;
		}

		[[UIPasteboard generalPasteboard] setItems:@[@{ CDVClipboardPlainTextType: text }]
		                                   options:pasteboardOptions];

		CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:text];
		[self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
	});
}

- (void)paste:(CDVInvokedUrlCommand*)command {
	dispatch_async(dispatch_get_main_queue(), ^{
		UIPasteboard *pasteboard = [UIPasteboard generalPasteboard];
		NSString     *text       = pasteboard.string ?: @"";

		CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:text];
		[self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
	});
}

- (void)clear:(CDVInvokedUrlCommand*)command {
	dispatch_async(dispatch_get_main_queue(), ^{
		UIPasteboard *pasteboard = [UIPasteboard generalPasteboard];
		pasteboard.string = @"";

		CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsBool:true];
		[self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
	});
}

@end

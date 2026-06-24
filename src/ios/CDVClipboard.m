#import <Foundation/Foundation.h>
#import <Cordova/CDVPlugin.h>
#import <Cordova/CDVPluginResult.h>
#import "CDVClipboard.h"

@implementation CDVClipboard

- (void)copy:(CDVInvokedUrlCommand*)command {
	dispatch_async(dispatch_get_main_queue(), ^{
		UIPasteboard *pasteboard = [UIPasteboard generalPasteboard];
		NSString     *text       = [command argumentAtIndex:0 withDefault:@""];

		pasteboard.string = text ?: @"";

		CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:(text ?: @"")];
		[self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
	}];
}

- (void)paste:(CDVInvokedUrlCommand*)command {
	dispatch_async(dispatch_get_main_queue(), ^{
		UIPasteboard *pasteboard = [UIPasteboard generalPasteboard];
		NSString     *text       = pasteboard.string ?: @"";

		CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:text];
		[self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
	}];
}

- (void)clear:(CDVInvokedUrlCommand*)command {
	dispatch_async(dispatch_get_main_queue(), ^{
		UIPasteboard *pasteboard = [UIPasteboard generalPasteboard];
		pasteboard.string = @"";

		CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsBool:true];
		[self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
	}];
}

@end

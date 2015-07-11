#import <Cordova/CDV.h>
#import <AVFoundation/AVFoundation.h>

@interface AudioRecorderAPI : CDVPlugin {
  NSString *recorderFilePath;
  NSNumber *duration;
  AVAudioRecorder *recorder;
  CDVPluginResult *pluginResult;
  CDVInvokedUrlCommand *_command;
}

@property(assign) id<AVAudioRecorderDelegate> delegate;

- (void)record:(CDVInvokedUrlCommand*)command;
- (void)stop:(CDVInvokedUrlCommand*)command;

@end

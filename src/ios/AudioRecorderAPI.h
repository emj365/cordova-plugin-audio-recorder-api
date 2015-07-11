#import <Cordova/CDV.h>
#import <AVFoundation/AVFoundation.h>

@interface AudioRecorderAPI : CDVPlugin {
  NSString *recorderFilePath;
  NSNumber *duration;
  AVAudioRecorder *recorder;
  AVAudioPlayer *player;
  CDVPluginResult *pluginResult;
  CDVInvokedUrlCommand *_command;
}

- (void)record:(CDVInvokedUrlCommand*)command;
- (void)stop:(CDVInvokedUrlCommand*)command;
- (void)playback:(CDVInvokedUrlCommand*)command;

@end

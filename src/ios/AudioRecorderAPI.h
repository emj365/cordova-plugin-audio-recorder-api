#import <Cordova/CDV.h>
#import <AVFoundation/AVFoundation.h>
#import <AudioToolbox/AudioServices.h>

@interface AudioRecorderAPI : CDVPlugin {
  AVAudioRecorder *         myRecorder;
  AVAudioPlayer *           myPlayer;
  NSString *                outputFile;
  NSNumber *                seconds;
  NSNumber *                sampleRate;
  NSNumber *                bitRate;
  CDVPluginResult *         pluginResult;
  CDVInvokedUrlCommand *    _command;
}

- (void)record:(CDVInvokedUrlCommand*)command;
- (void)stop:(CDVInvokedUrlCommand*)command;
- (void)playback:(CDVInvokedUrlCommand*)command;

@end

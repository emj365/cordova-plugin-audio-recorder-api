#import <Cordova/CDV.h>
#import <AVFoundation/AVFoundation.h>

@interface AudioRecorderAPI : CDVPlugin {
    NSString *recorderFilePath;
    NSNumber *duration;
    AVAudioRecorder *recorder;
    AVAudioPlayer *player;
    NSTimer *levelTimer;
    CDVPluginResult *pluginResult;
    CDVInvokedUrlCommand *_command;
    CDVInvokedUrlCommand *_VolumeCommand;
}

- (void)record:(CDVInvokedUrlCommand*)command;
- (void)stop:(CDVInvokedUrlCommand*)command;
- (void)playback:(CDVInvokedUrlCommand*)command;
- (void)getvolume:(CDVInvokedUrlCommand*)command;

@end

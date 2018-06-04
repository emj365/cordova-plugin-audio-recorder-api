#import "AudioRecorderAPI.h"
#import <Cordova/CDV.h>
#import <AVFoundation/AVFoundation.h>

@implementation AudioRecorderAPI

#define RECORDINGS_FOLDER [NSHomeDirectory() stringByAppendingPathComponent:@"Library/NoCloud"]

- (BOOL)hasAudioPermission
{
    AVAuthorizationStatus videoAuthStatus = [AVCaptureDevice authorizationStatusForMediaType:AVMediaTypeAudio];
    if (videoAuthStatus == AVAuthorizationStatusNotDetermined) {// 未询问用户是否授权
        //第一次询问用户是否进行授权
        [[AVAudioSession sharedInstance] requestRecordPermission:^(BOOL granted) {
            if (granted) {
                // Microphone enabled code
            }
            else {
                // Microphone disabled code
                CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"未获得授权使用麦克风，请在设置中打开"];
                [self.commandDelegate sendPluginResult:pluginResult callbackId:_command.callbackId];
            }
        }];
    }
    else if(videoAuthStatus == AVAuthorizationStatusRestricted || videoAuthStatus == AVAuthorizationStatusDenied) {
        // 未授权
        CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"未获得授权使用麦克风，请在设置中打开"];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:_command.callbackId];
        return NO;
    }
    else{
        // 已授权
        return YES;
    }
    return NO;
}

//申请录音权限
- (void)requestPermission:(CDVInvokedUrlCommand*)command {
    _command = command;

    [AVCaptureDevice requestAccessForMediaType:AVMediaTypeAudio completionHandler:^(BOOL granted) {
        if (granted){// 用户同意授权

        }else {// 用户拒绝授权
          CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"未获得授权使用麦克风，请在设置中打开"];
          [self.commandDelegate sendPluginResult:pluginResult callbackId:_command.callbackId];
        }
    }];

  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:@"false"];
	[self.commandDelegate sendPluginResult:pluginResult callbackId:_command.callbackId];
}


- (void)record:(CDVInvokedUrlCommand*)command {
  _command = command;
  duration = [_command.arguments objectAtIndex:0];
  //判断麦克风权限
  if (![self hasAudioPermission])
  {
	return;
  }
   //end权限判断

  [self.commandDelegate runInBackground:^{

    AVAudioSession *audioSession = [AVAudioSession sharedInstance];

    NSError *err;
    [audioSession setCategory:AVAudioSessionCategoryPlayAndRecord error:&err];
	
    if (err)
    {
      NSLog(@"%@ %d %@", [err domain], [err code], [[err userInfo] description]);
    }
    err = nil;
    [audioSession setActive:YES error:&err];
    if (err)
    {
      NSLog(@"%@ %d %@", [err domain], [err code], [[err userInfo] description]);
    }

    NSMutableDictionary *recordSettings = [[NSMutableDictionary alloc] init];
    [recordSettings setObject:[NSNumber numberWithInt: kAudioFormatMPEG4AAC] forKey: AVFormatIDKey];
    [recordSettings setObject:[NSNumber numberWithFloat:8000.0] forKey: AVSampleRateKey];
    [recordSettings setObject:[NSNumber numberWithInt:1] forKey:AVNumberOfChannelsKey];
    [recordSettings setObject:[NSNumber numberWithInt:12000] forKey:AVEncoderBitRateKey];
    [recordSettings setObject:[NSNumber numberWithInt:8] forKey:AVLinearPCMBitDepthKey];
    [recordSettings setObject:[NSNumber numberWithInt: AVAudioQualityLow] forKey: AVEncoderAudioQualityKey];

    // Create a new dated file
    NSString *uuid = [[NSUUID UUID] UUIDString];
    recorderFilePath = [NSString stringWithFormat:@"%@/%@.m4a", RECORDINGS_FOLDER, uuid];
    NSLog(@"recording file path: %@", recorderFilePath);

    NSURL *url = [NSURL fileURLWithPath:recorderFilePath];
    err = nil;
    recorder = [[AVAudioRecorder alloc] initWithURL:url settings:recordSettings error:&err];
    if(!recorder){
      NSLog(@"recorder: %@ %d %@", [err domain], [err code], [[err userInfo] description]);
      return;
    }

    [recorder setDelegate:self];

    if (![recorder prepareToRecord]) {
      NSLog(@"prepareToRecord failed");
      return;
    }

    if (![recorder recordForDuration:(NSTimeInterval)[duration intValue]]) {
      NSLog(@"recordForDuration failed");
      return;
    }

	CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:@"success"];
	[self.commandDelegate sendPluginResult:pluginResult callbackId:_command.callbackId];

  }];
}

- (void)stop:(CDVInvokedUrlCommand*)command {
  _command = command;
  NSLog(@"stopRecording");
  [recorder stop];
  NSLog(@"stopped");
}

- (void)playback:(CDVInvokedUrlCommand*)command {
  _command = command;
  [self.commandDelegate runInBackground:^{
    NSLog(@"recording playback");
    NSURL *url = [NSURL fileURLWithPath:recorderFilePath];
    NSError *err;
    player = [[AVAudioPlayer alloc] initWithContentsOfURL:url error:&err];
    player.numberOfLoops = 0;
    player.delegate = self;
    [player prepareToPlay];
    [player play];
    if (err) {
      NSLog(@"%@ %d %@", [err domain], [err code], [[err userInfo] description]);
    }
    NSLog(@"playing");
  }];
}

- (void)audioPlayerDidFinishPlaying:(AVAudioPlayer *)player successfully:(BOOL)flag {
  NSLog(@"audioPlayerDidFinishPlaying");
  pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:@"playbackComplete"];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:_command.callbackId];
}

- (void)audioRecorderDidFinishRecording:(AVAudioRecorder *)recorder successfully:(BOOL)flag {
  NSURL *url = [NSURL fileURLWithPath: recorderFilePath];
  NSError *err = nil;
  NSData *audioData = [NSData dataWithContentsOfFile:[url path] options: 0 error:&err];
  if(!audioData) {
	NSString *errorInfo = [NSString stringWithFormat:@"audio data: %@ %d %@", [err domain], [err code], [[err userInfo] description]];
    NSLog(errorInfo);
	pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"录音失败"];
	[self.commandDelegate sendPluginResult:pluginResult callbackId:_command.callbackId];
  } else {
    NSLog(@"recording saved: %@", recorderFilePath);
    pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:recorderFilePath];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:_command.callbackId];
  }
}

@end

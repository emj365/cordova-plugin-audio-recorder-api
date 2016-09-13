#import "AudioRecorderAPI.h"
#import <Cordova/CDV.h>

@implementation AudioRecorderAPI

#define RECORDINGS_FOLDER [NSHomeDirectory() stringByAppendingPathComponent:@"Library/NoCloud"]




- (void)record:(CDVInvokedUrlCommand*)command {
  _command = command;

  if([_command.arguments count] > 0)    { seconds = [_command.arguments objectAtIndex:0]; }
  else                                  { seconds = @0; }

  if([_command.arguments count] > 1)    { sampleRate = [_command.arguments objectAtIndex:1]; }
  else                                  { sampleRate = @44100; }

  if([_command.arguments count] > 2)    { bitRate = [_command.arguments objectAtIndex:2]; }
  else                                  { bitRate = @32000; }




  [self.commandDelegate runInBackground:^{
    NSError * err = nil;

    AVAudioSession *audioSession = [AVAudioSession sharedInstance];

    [audioSession setCategory:AVAudioSessionCategoryPlayAndRecord error:&err];
    if(err) { NSLog(@"%@ %d %@", [err domain], [err code], [[err userInfo] description]); err = nil; }

    [audioSession setActive:YES error:&err];
    if(err) { NSLog(@"%@ %d %@", [err domain], [err code], [[err userInfo] description]); err = nil; }

    // UInt32 audioRouteOverride = kAudioSessionOverrideAudioRoute_Speaker; AudioSessionSetProperty (kAudioSessionProperty_OverrideAudioRoute, sizeof (audioRouteOverride),&audioRouteOverride);

    NSMutableDictionary *recordSettings = [[NSMutableDictionary alloc] init];
    [recordSettings setObject:[NSNumber numberWithInt: kAudioFormatMPEG4AAC] forKey: AVFormatIDKey];
    [recordSettings setObject:[NSNumber numberWithFloat:44100.0] forKey: AVSampleRateKey];
    [recordSettings setObject:[NSNumber numberWithInt:1] forKey:AVNumberOfChannelsKey];
    [recordSettings setObject:[NSNumber numberWithInt:32000] forKey:AVEncoderBitRateKey];
    [recordSettings setObject:[NSNumber numberWithInt:8] forKey:AVLinearPCMBitDepthKey];
    [recordSettings setObject:[NSNumber numberWithInt: AVAudioQualityHigh] forKey: AVEncoderAudioQualityKey];

    NSString *uuid = [[NSUUID UUID] UUIDString];
    outputFile = [NSString stringWithFormat:@"%@/%@.m4a", RECORDINGS_FOLDER, uuid];
    NSLog(@"recording file path: %@", outputFile);

    NSURL *url = [NSURL fileURLWithPath:outputFile];

    myRecorder = [[AVAudioRecorder alloc] initWithURL:url settings:recordSettings error:&err];
    if(!myRecorder) { NSLog(@"myRecorder: %@ %d %@", [err domain], [err code], [[err userInfo] description]); return; }

    [myRecorder setDelegate:(id)self];

    if(![myRecorder prepareToRecord]) { NSLog(@"prepareToRecord failed"); return; }

    if(seconds > 0) { if(![myRecorder recordForDuration:(NSTimeInterval)[seconds intValue]]) { NSLog(@"recordForDuration failed"); return; } }
    else            { if(![myRecorder record]) { NSLog(@"record failed"); return; } }
  }];
}




- (void)stop:(CDVInvokedUrlCommand*)command {
  _command = command;

  NSLog(@"stopRecording");

  if(myRecorder) { [myRecorder stop]; }

  NSLog(@"stopped");
}




- (void)playback:(CDVInvokedUrlCommand*)command {
  _command = command;

  [self.commandDelegate runInBackground:^{
    NSError * err = nil;

    NSLog(@"recording playback");

    NSURL *url = [NSURL fileURLWithPath:outputFile];

    myPlayer = [[AVAudioPlayer alloc] initWithContentsOfURL:url error:&err];

    myPlayer.numberOfLoops = 0;

    [myPlayer setDelegate:(id)self];

    [myPlayer prepareToPlay];
    [myPlayer play];

    if(err) { NSLog(@"%@ %d %@", [err domain], [err code], [[err userInfo] description]); return; }

    NSLog(@"playing");
  }];
}




- (void)audioPlayerDidFinishPlaying:(AVAudioPlayer *)myPlayer successfully:(BOOL)flag {
  NSLog(@"audioPlayerDidFinishPlaying");

  pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:@"playbackComplete"];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:_command.callbackId];
}




- (void)audioRecorderDidFinishRecording:(AVAudioRecorder *)myRecorder successfully:(BOOL)flag {
  NSError * err = nil;

  NSLog(@"audioRecorderDidFinishRecording");

  NSURL *url = [NSURL fileURLWithPath: outputFile];

  NSData *audioData = [NSData dataWithContentsOfFile:[url path] options: 0 error:&err];
  if(audioData) {
    NSLog(@"recording saved: %@", outputFile);
    pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:outputFile];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:_command.callbackId];
  } else { NSLog(@"audio data: %@ %d %@", [err domain], [err code], [[err userInfo] description]); }
}

@end

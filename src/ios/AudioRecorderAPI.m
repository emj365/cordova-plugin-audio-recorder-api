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
  else                                  { bitRate = @118000; }




  [self.commandDelegate runInBackground:^{
    NSError * err = nil;

    AVAudioSession *audioSession = [AVAudioSession sharedInstance];

    [audioSession setCategory:AVAudioSessionCategoryPlayAndRecord error:&err];
    [audioSession setActive:YES error:&err];

    // UInt32 audioRouteOverride = kAudioSessionOverrideAudioRoute_Speaker; AudioSessionSetProperty (kAudioSessionProperty_OverrideAudioRoute, sizeof (audioRouteOverride),&audioRouteOverride);

    NSMutableDictionary * recordSettings = [[NSMutableDictionary alloc] init];
    [recordSettings setObject:[NSNumber numberWithInt: kAudioFormatMPEG4AAC] forKey: AVFormatIDKey];
    [recordSettings setObject:[NSNumber numberWithInt: AVAudioQualityMax] forKey: AVEncoderAudioQualityKey];
    [recordSettings setObject:sampleRate forKey: AVSampleRateKey];
    [recordSettings setObject:[NSNumber numberWithInt:1] forKey:AVNumberOfChannelsKey];
    [recordSettings setObject:bitRate forKey:AVEncoderBitRateKey];

    NSString *uuid = [[NSUUID UUID] UUIDString];
    outputFile = [NSString stringWithFormat:@"%@/%@.m4a", RECORDINGS_FOLDER, uuid];

    NSURL *url = [NSURL fileURLWithPath:outputFile];

    myRecorder = [[AVAudioRecorder alloc] initWithURL:url settings:recordSettings error:&err];
    if(!myRecorder) { return; }

    [myRecorder setDelegate:(id)self];
    [myRecorder prepareToRecord];

    if(seconds > 0) { [myRecorder recordForDuration:(NSTimeInterval)[seconds intValue]]; }
    else            { [myRecorder record]; }
  }];
}




- (void)stop:(CDVInvokedUrlCommand*)command {
  _command = command;

  if(myRecorder)    { [myRecorder stop]; }
  if(myPlayer)      { [myPlayer stop]; }
}




- (void)playback:(CDVInvokedUrlCommand*)command {
  _command = command;

  [self.commandDelegate runInBackground:^{
    NSError * err = nil;

    NSURL *url = [NSURL fileURLWithPath:outputFile];

    myPlayer = [[AVAudioPlayer alloc] initWithContentsOfURL:url error:&err];

    myPlayer.numberOfLoops = 0;

    [myPlayer setDelegate:(id)self];

    [myPlayer prepareToPlay];
    [myPlayer play];

    NSLog(@"playing");
  }];
}




- (void)audioRecorderDidFinishRecording:(AVAudioRecorder *)myRecorder successfully:(BOOL)flag {
  NSError * err         = nil;
  NSURL *   url         = [NSURL fileURLWithPath: outputFile];
  NSData *  audioData   = [NSData dataWithContentsOfFile:[url path] options: 0 error:&err];

  if(audioData) {
    pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:outputFile];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:_command.callbackId];
  }
}




- (void)audioPlayerDidFinishPlaying:(AVAudioPlayer *)myPlayer successfully:(BOOL)flag {
  pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:@"playbackComplete"];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:_command.callbackId];
}

@end

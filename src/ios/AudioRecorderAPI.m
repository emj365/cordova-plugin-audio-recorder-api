#import "AudioRecorderAPI.h"
#import <Cordova/CDV.h>

@implementation AudioRecorderAPI




#define RECORDINGS_FOLDER [NSHomeDirectory() stringByAppendingPathComponent:@"Library/NoCloud"]




- (void)init:(CDVInvokedUrlCommand*)command {
  NSLog(@"init");

  NSError * err = nil;

  AVAudioSession * audioSession = [AVAudioSession sharedInstance];
  [audioSession setCategory:AVAudioSessionCategoryPlayAndRecord error:&err];
  if (err)
  {
    NSLog(@"%@ %d %@", [err domain], [err code], [[err userInfo] description]);
    [self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR] callbackId:command.callbackId];
  }

  [audioSession setActive:YES error:&err];
  if (err)
  {
    NSLog(@"%@ %d %@", [err domain], [err code], [[err userInfo] description]);
    [self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR] callbackId:command.callbackId];
  }

  [audioSession requestRecordPermission:^(BOOL granted) {
    if (granted) {
      NSLog(@"Microphone access granted");
      [self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_OK] callbackId:command.callbackId];
    }
    else {
      NSLog(@"Microphone access denied");
      [self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR] callbackId:command.callbackId];
    }
  }];
}




- (void)record:(CDVInvokedUrlCommand*)command {
  NSLog(@"record");

  [self.commandDelegate runInBackground:^{
    NSError * err = nil;

    _recordCommand = command;

    if([command.arguments count] > 0)   { seconds = [command.arguments objectAtIndex:0]; }
    else                                { seconds = @0; }

    if([command.arguments count] > 1)   { sampleRate = [command.arguments objectAtIndex:1]; }
    else                                { sampleRate = @44100; }

    if([command.arguments count] > 2)   { bitRate = [command.arguments objectAtIndex:2]; }
    else                                { bitRate = @118000; }

    NSLog(@"Recording %d seconds at %d / %d", [seconds intValue], [sampleRate intValue], [bitRate intValue]);

    NSMutableDictionary * recordSettings = [[NSMutableDictionary alloc] init];
    [recordSettings setObject:[NSNumber numberWithInt: kAudioFormatMPEG4AAC] forKey: AVFormatIDKey];
    [recordSettings setObject:[NSNumber numberWithInt: AVAudioQualityMax] forKey: AVEncoderAudioQualityKey];
    [recordSettings setObject:sampleRate forKey: AVSampleRateKey];
    [recordSettings setObject:[NSNumber numberWithInt:1] forKey:AVNumberOfChannelsKey];
    [recordSettings setObject:bitRate forKey:AVEncoderBitRateKey];

    NSString * uuid = [[NSUUID UUID] UUIDString];
    outputFile = [NSString stringWithFormat:@"%@/%@.m4a", RECORDINGS_FOLDER, uuid];

    NSLog(@"recording file path: %@", outputFile);

    NSURL * url = [NSURL fileURLWithPath:outputFile];

    myRecorder = [[AVAudioRecorder alloc] initWithURL:url settings:recordSettings error:&err];
    if(!myRecorder){
      NSLog(@"recorder error: %@ %d %@", [err domain], [err code], [[err userInfo] description]);
      return;
    }

    [myRecorder setDelegate:(id)self];
    [myRecorder prepareToRecord];

    if(seconds > 0) { [myRecorder recordForDuration:(NSTimeInterval)[seconds intValue]]; }
    else            { [myRecorder record]; }

    NSLog(@"recording");
  }];
}




- (void)stop:(CDVInvokedUrlCommand*)command {
  NSLog(@"stop");

  _stopCommand = command;

  if(myRecorder)    { [myRecorder stop]; }
  if(myPlayer)      { [myPlayer stop]; }

  NSLog(@"stopped");
}




- (void)playback:(CDVInvokedUrlCommand*)command {
  NSLog(@"playback");

  [self.commandDelegate runInBackground:^{
    NSError * err = nil;

    _playBackCommand = command;

    NSURL * url = [NSURL fileURLWithPath:outputFile];

    myPlayer = [[AVAudioPlayer alloc] initWithContentsOfURL:url error:&err];
    if (err) {
      NSLog(@"%@ %d %@", [err domain], [err code], [[err userInfo] description]);
      return;
    }

    myPlayer.numberOfLoops = 0;

    [myPlayer setDelegate:(id)self];
    [myPlayer prepareToPlay];
    [myPlayer play];

    NSLog(@"playing");
  }];
}




- (void)audioPlayerDidFinishPlaying:(AVAudioPlayer *)myPlayer successfully:(BOOL)flag {
  NSLog(@"audioPlayerDidFinishPlaying");

  [self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:@"playbackComplete"] callbackId:_playBackCommand.callbackId];

  _stopCommand = nil;

  NSLog(@"Playback finished");
}

- (void)audioRecorderDidFinishRecording:(AVAudioRecorder *)myRecorder successfully:(BOOL)flag {
  NSLog(@"audioRecorderDidFinishRecording");

  NSError *         err             = nil;
  NSURL *           url             = [NSURL fileURLWithPath: outputFile];
  NSData *audioData = [NSData dataWithContentsOfFile:[url path] options: 0 error:&err];
  if(!audioData) {
    NSLog(@"audio data: %@ %d %@", [err domain], [err code], [[err userInfo] description]);

    if (_stopCommand) {
      NSLog(@"End of recording was from stop");
      [self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR] callbackId:_stopCommand.callbackId];
    } else {
      NSLog(@"End of recording was from record with duration");
      [self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR] callbackId:_recordCommand.callbackId];
    }

    return;
  }

  NSLog(@"recording saved: %@", outputFile);

  if (_stopCommand) {
    NSLog(@"End of recording was from stop");
    [self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:outputFile] callbackId:_stopCommand.callbackId];
  } else {
    NSLog(@"End of recording was from record with duration");
    [self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:outputFile] callbackId:_recordCommand.callbackId];
  }

  _stopCommand   = nil;
  _recordCommand = nil;

  NSLog(@"Record finished");
}


@end

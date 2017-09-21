#import "AudioRecorderAPI.h"
#import <Cordova/CDV.h>

@implementation AudioRecorderAPI

#define RECORDINGS_FOLDER [NSHomeDirectory() stringByAppendingPathComponent:@"Library/NoCloud"]

- (void)record:(CDVInvokedUrlCommand*)command {
    _command = command;
    duration = [_command.arguments objectAtIndex:0];
    
    [self.commandDelegate runInBackground:^{
        
        AVAudioSession *audioSession = [AVAudioSession sharedInstance];
        
        NSError *err;
        [audioSession setCategory:AVAudioSessionCategoryPlayAndRecord withOptions:AVAudioSessionCategoryOptionMixWithOthers error:&err];
        if (err)
        {
            NSLog(@"%@ %ld %@", [err domain], (long)[err code], [[err userInfo] description]);
        }
        err = nil;
        [audioSession setActive:YES error:&err];
        if (err)
        {
            NSLog(@"%@ %ld %@", [err domain], (long)[err code], [[err userInfo] description]);
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
            NSLog(@"recorder: %@ %ld %@", [err domain], (long)[err code], [[err userInfo] description]);
            return;
        }
        
        recorder.meteringEnabled = YES;
        [recorder setDelegate:self];
        
        if (![recorder prepareToRecord]) {
            NSLog(@"prepareToRecord failed");
            return;
        }
        
        if (![recorder recordForDuration:(NSTimeInterval)[duration intValue]]) {
            NSLog(@"recordForDuration failed");
            return;
        }
        
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:@"recording started"];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:_command.callbackId];
    }];
}

- (void)stop:(CDVInvokedUrlCommand*)command {
    _command = command;
    NSLog(@"stopRecording");
    [recorder stop];
    [levelTimer invalidate];
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
            NSLog(@"%@ %ld %@", [err domain], (long)[err code], [[err userInfo] description]);
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
        NSLog(@"audio data: %@ %ld %@", [err domain], (long)[err code], [[err userInfo] description]);
    } else {
        NSLog(@"recording saved: %@", recorderFilePath);
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:recorderFilePath];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:_command.callbackId];
    }
}

- (void)getvolume:(CDVInvokedUrlCommand*)command {
    levelTimer = [NSTimer scheduledTimerWithTimeInterval:0.01f target:self selector: @selector(levelTimerCallback:) userInfo: nil repeats: YES];
    [recorder updateMeters];
    
    _VolumeCommand = command;
    pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:@"0"];
    [pluginResult setKeepCallbackAsBool:YES];
    
    [self.commandDelegate sendPluginResult:pluginResult callbackId:_VolumeCommand.callbackId];
}

- (void)levelTimerCallback:(NSTimer *)timer {
    [recorder updateMeters];
    
    //float peakDecebels = [recorder peakPowerForChannel:0];
    float averagePower = [recorder averagePowerForChannel:0] * 1.5;
    
    pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:[NSString stringWithFormat:@"%f", averagePower]];
    [pluginResult setKeepCallbackAsBool:YES];
    
    [self.commandDelegate sendPluginResult:pluginResult callbackId:_VolumeCommand.callbackId];
}

@end

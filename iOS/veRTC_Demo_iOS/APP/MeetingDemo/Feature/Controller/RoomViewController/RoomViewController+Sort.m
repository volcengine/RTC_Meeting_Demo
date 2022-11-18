//
//  RoomViewController+Sort.m
//  veRTC_Demo
//
//  Created by on 2021/6/8.
//  
//

#import <objc/runtime.h>
#import "RoomViewController+Sort.h"
#import "GCDTimer.h"

@interface RoomViewController (Sort)

@property (nonatomic, strong) NSMutableArray<RoomVideoSession *> *sortUserLists;

@property (nonatomic, strong) GCDTimer *timer;

@end

static const void *kSortUserLists = @"sortUserLists";
static const void *kGCDTimer = @"GCDTimer";

@implementation RoomViewController (Sort)

#pragma mark - Publish Action

- (void)statrSort:(void (^)(NSMutableArray *userLists))block {
    if (!self.sortUserLists) {
        self.sortUserLists = [[NSMutableArray alloc] init];
    }
    if (!self.timer) {
        self.timer = [[GCDTimer alloc] init];
    }
    
    __weak __typeof(self) wself = self;
    [self.timer startTimerWithSpace:1 block:^(BOOL result) {
        [wself sortEndCallbackWithBlock:block];
    }];
}

- (void)stopSort {
    [self.timer stopTimer];
}

- (void)sortEndCallbackWithBlock:(void (^)(NSMutableArray *userLists))block {
    [self.sortUserLists removeAllObjects];
    NSArray *sortedNameArray = [self.userDataPool sortedArrayUsingComparator:^NSComparisonResult(RoomVideoSession *  _Nonnull obj1, RoomVideoSession *  _Nonnull obj2) {
        return [obj1.userUniform compare:obj2.userUniform options:NSCaseInsensitiveSearch];
    }];
    
    RoomVideoSession *tempMaxVolumeUser = nil;
    NSMutableArray *hostLists = [[NSMutableArray alloc] init];
    NSMutableArray *selfLists = [[NSMutableArray alloc] init];
    NSMutableArray *volumeLists = [[NSMutableArray alloc] init];
    NSMutableArray *sorce1 = [[NSMutableArray alloc] init];
    NSMutableArray *sorce2 = [[NSMutableArray alloc] init];
    NSMutableArray *sorce3 = [[NSMutableArray alloc] init];
    NSMutableArray *sorce4 = [[NSMutableArray alloc] init];
    for (int i = 0; i < sortedNameArray.count; i++) {
        RoomVideoSession *userModel = sortedNameArray[i];
        BOOL isLoginUser = [userModel.uid isEqualToString:self.localVideoSession.uid];
        if (userModel.isHost) {
            [hostLists addObject:userModel];
        } else if (isLoginUser) {
            [selfLists addObject:userModel];
        } else if (self.maxVolumeUserModel &&
                   [self.maxVolumeUserModel.uid isEqualToString:userModel.uid]) {
            [volumeLists addObject:userModel];
        } else {
            if ([self isEnableMic:userModel] && userModel.isEnableVideo) {
                [sorce1 addObject:userModel];
            } else if ([self isEnableMic:userModel] && !userModel.isEnableVideo) {
                [sorce2 addObject:userModel];
            } else if (![self isEnableMic:userModel] && userModel.isEnableVideo) {
                [sorce3 addObject:userModel];
            } else {
                [sorce4 addObject:userModel];
            }
        }
        
        //排序使用
        if ([self isEnableMic:userModel] &&
            userModel.volume > 0 &&
            !userModel.isHost &&
            !isLoginUser) {
            self.maxVolumeUserModel = userModel;
        }
        
        //展示说话动画使用
        userModel.isMaxVolume = NO;
        if ([self isEnableMic:userModel] &&
            userModel.volume > 0 &&
            userModel.volume > tempMaxVolumeUser.volume) {
            tempMaxVolumeUser = userModel;
        }
    }
    
    if (tempMaxVolumeUser) {
        tempMaxVolumeUser.isMaxVolume = YES;
    }
    
    if (hostLists.count > 0) {
        [self.sortUserLists addObjectsFromArray:hostLists];
    }
    if (selfLists.count > 0) {
        [self.sortUserLists addObjectsFromArray:selfLists];
    }
    if (volumeLists.count > 0) {
        [self.sortUserLists addObjectsFromArray:volumeLists];
    }
    if (sorce1.count > 0) {
        [self.sortUserLists addObjectsFromArray:sorce1];
    }
    if (sorce2.count > 0) {
        [self.sortUserLists addObjectsFromArray:sorce2];
    }
    if (sorce3.count > 0) {
        [self.sortUserLists addObjectsFromArray:sorce3];
    }
    if (sorce4.count > 0) {
        [self.sortUserLists addObjectsFromArray:sorce4];
    }
    if (block) {
        block(self.sortUserLists);
    }
}

- (NSMutableArray *)removeSortListsPromptly:(NSString *)uid {
    RoomVideoSession *deleteModel = nil;
    for (RoomVideoSession *roomUserModel in self.userDataPool) {
        if ([roomUserModel.uid isEqualToString:uid]) {
            deleteModel = roomUserModel;
            break;
        }
    }
    if (deleteModel) {
        [self.userDataPool removeObject:deleteModel];
    }
    
    deleteModel = nil;
    for (RoomVideoSession *roomUserModel in self.sortUserLists) {
        if ([roomUserModel.uid isEqualToString:uid]) {
            deleteModel = roomUserModel;
            break;
        }
    }
    if (deleteModel) {
        [self.sortUserLists removeObject:deleteModel];
    }
    return self.sortUserLists;
}

- (NSMutableArray *)updateSortListsPromptly {
    [self.sortUserLists removeAllObjects];
    NSArray *array = [self.userDataPool copy];
    [self.sortUserLists addObjectsFromArray:array];
    return self.sortUserLists;
}

- (NSMutableArray<RoomVideoSession *> *)getSortUserLists {
    return self.sortUserLists;
}

#pragma mark - Private Action

- (BOOL)isEnableMic:(RoomVideoSession *)userModel {
    BOOL isEnableMic = NO;
    if ([userModel.uid isEqualToString:self.localVideoSession.uid]) {
        isEnableMic = self.localVideoSession.isEnableAudio;
    } else {
        isEnableMic = userModel.isEnableAudio;
    }
    return isEnableMic;
}


#pragma mark - getter

- (void)setSortUserLists:(NSMutableArray<RoomVideoSession *> *)sortUserLists {
    objc_setAssociatedObject(self, kSortUserLists, sortUserLists, OBJC_ASSOCIATION_RETAIN_NONATOMIC);
}

- (NSMutableArray<RoomVideoSession *> *)sortUserLists {
    return objc_getAssociatedObject(self, kSortUserLists);
}

- (void)setTimer:(GCDTimer *)timer {
    objc_setAssociatedObject(self, kGCDTimer, timer, OBJC_ASSOCIATION_RETAIN_NONATOMIC);
}

- (GCDTimer *)timer {
    return objc_getAssociatedObject(self, kGCDTimer);
}

@end

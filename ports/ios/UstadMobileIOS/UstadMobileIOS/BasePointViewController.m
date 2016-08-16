//
//  BasePointViewController.m
//  UstadMobileIOS
//
//  Created by Mike Dawson on 16/08/16.
//  Copyright © 2016 UstadMobile FZ-LLC. All rights reserved.
//

#import "BasePointViewController.h"
#import "java/util/Hashtable.h"

@interface BasePointViewController ()
@property JavaUtilHashtable *args;
@property jint direction;
@end

@implementation BasePointViewController

- (void)viewDidLoad {
    [super viewDidLoad];
    // Do any additional setup after loading the view.
}

- (void)didReceiveMemoryWarning {
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

-(void) setArgsWithHashtable:(JavaUtilHashtable *)args {
    self.args = args;
}

- (void)refreshCatalogWithInt:(jint)column;{
    
}

- (void)setClassListVisibleWithBoolean:(jboolean)visible {
    //class list is not implemented in iOS
}

- (id)getContext {
    return self;
}

- (jint)getDirection {
    return self.direction;
}

- (void)setDirectionWithInt:(jint)dir {
    self.direction = dir;
}

- (void)setUIStrings {
    //right now there's no non tab components here with localizable ui strings
}

- (void)setAppMenuCommandsWithNSStringArray:(IOSObjectArray *)labels
                               withIntArray:(IOSIntArray *)ids {
    //not implemented yet...
}


/*
#pragma mark - Navigation

// In a storyboard-based application, you will often want to do a little preparation before navigation
- (void)prepareForSegue:(UIStoryboardSegue *)segue sender:(id)sender {
    // Get the new view controller using [segue destinationViewController].
    // Pass the selected object to the new view controller.
}
*/

@end

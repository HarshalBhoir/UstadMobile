import { Component } from '@angular/core';
import { Router, ActivatedRoute, NavigationEnd } from '@angular/router';
import { Location } from '@angular/common';
import { UmBaseComponent } from '../um-base-component';
import { UmBaseService } from '../../service/um-base.service';
import { Subscription } from 'rxjs/internal/Subscription';
import { UmAngularUtil } from '../../util/UmAngularUtil';
import util from 'UstadMobile-lib-util';
import core from 'UstadMobile-core'

@Component({
  selector: 'app-home',
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.css']
})
export class HomeComponent extends UmBaseComponent implements core.com.ustadmobile.core.view.HomeView {

  menu_libaries: string;
  menu_reports: string;
  class_icon_position: string;
  class_icon_toolbar: string;
  class_toolbar_arrow: string;
  class_toolbar_title: string;
  class_drawer_menu: string;
  class_open_profile: string;
  supportedLanguages = [];
  
  private navigationSubscription: Subscription;
  showReports = false
  userName: string = "Guest User"
  userEmail: string = "guestmail"
  static toolBarTitle: string = ".."
  activeState = [true, false]
  private presenter: core.com.ustadmobile.core.controller.HomePresenter;

  constructor(private location: Location, umService: UmBaseService,
    router: Router, route: ActivatedRoute) {
    super(umService, router, route);
    this.class_icon_position = this.umService.isLTRDirectionality() ? "left" : "right icon-left-spacing";
    this.class_icon_toolbar = this.umService.isLTRDirectionality() ? "left icon-right-spacing" : "right icon-left-spacing";
    this.class_toolbar_arrow = this.umService.isLTRDirectionality() ? "arrow_back" : "arrow_forward";
    this.class_toolbar_title = this.umService.isLTRDirectionality() ? "brand-logo-ltr" : "brand-logo-rtl";
    this.class_drawer_menu = this.umService.isLTRDirectionality() ? "right drawer-menu-ltr" : "left drawer-menu-rtl";
    this.class_open_profile = this.umService.isLTRDirectionality() ? "right":"left"

    //Listen for the navigation changes - changes on url
    this.navigationSubscription = this.router.events.filter(event => event instanceof NavigationEnd)
      .subscribe(_ => {
        UmAngularUtil.registerResourceReadyListener(this)
        UmAngularUtil.registerTitleChangeListener(this)
        this.activeState = UmAngularUtil.getActiveMenu(this.routes)
      });
  }


  ngOnInit() {
    super.ngOnInit()
  }


  onCreate() {
    super.onCreate()
    this.supportedLanguages = util.com.ustadmobile.lib.util.UMUtil.kotlinMapToJsArray(
      this.systemImpl.getAllUiLanguage(this.context))
    this.presenter = new core.com.ustadmobile.core.controller.HomePresenter(
      this.context, UmAngularUtil.getArgumentsFromQueryParams(), this, this.umService.getDbInstance().personDao)
    this.presenter.onCreate(null)
  }

  openProfile(){
    this.presenter.handleClickPersonIcon()
  }

  showReportMenu(show) {
    this.showReports = show
  }

  showDownloadAllButton() {}

  setLoggedPerson(person) {
    this.userEmail = person.emailAddr
    this.userName = person.firstNames + " " + person.lastName
  }

  loadProfileIcon(profile) {
    if (profile != "") {
      this.userProfile = profile
    }
  }

  goBack() {
    if (!window.location.search.includes(this.umService.ROOT_UID + "")) {
      this.location.back();
    }
  }

  navigateTo(route) {
    const activeAcount = core.com.ustadmobile.core.impl.UmAccountManager.getActiveAccountWithContext(this.context)
    const isDashboard = route == "ReportDashboard"
    const routeTo =  isDashboard && !activeAcount ? this.routes.login : route 
    const args = UmAngularUtil.getArgumentsFromQueryParams({params:!isDashboard ? "?entryid="+this.umService.ROOT_UID: null, route: routeTo})
    this.systemImpl.go(route, args, this.context);
  }

  setToolbarTitle(title) {
    this.toolBarTitle = this.umService.isMobile ? super.truncate(title, 3): title
  }


  ngOnDestroy(): void {
    super.ngOnDestroy();
    if (this.presenter) {
      this.presenter.onDestroy();
    }
    if (this.navigationSubscription) {
      this.navigationSubscription.unsubscribe();
    }
  }

}

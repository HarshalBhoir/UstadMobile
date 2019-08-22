import { UmAngularUtil } from './../../util/UmAngularUtil';
import { Component, Renderer2, ElementRef } from '@angular/core';
import { UmBaseComponent } from '../um-base-component';
import { UmBaseService } from '../../service/um-base.service';
import { Router, ActivatedRoute, NavigationEnd } from '@angular/router';
import { UmDbMockService } from '../../core/db/um-db-mock.service';
import core from 'UstadMobile-core';

@Component({
  selector: 'app-report-dashboard',
  templateUrl: './report-dashboard.component.html',
  styleUrls: ['./report-dashboard.component.css']
})
export class ReportDashboardComponent extends UmBaseComponent implements
 core.com.ustadmobile.core.view.XapiReportOptionsView {

  tagList = ["All Tags","Session","Perfomance", "Session","Performance","Session","Perfomance","20+ More"]
  title = '';
   type = 'ColumnChart';
   data = [
      ["2012", 900, 390],
      ["2013", 1000, 400],
      ["2014", 1170, 440],
      ["2015", 1250, 480],
      ["2016", 1530, 540]
   ];
   columnNames = ['Year', 'Asia','Europe'];
   options = {};
   width = 450;
   height = 400;

   modalOptions: Materialize.ModalOptions = {
    dismissible: false, 
    opacity: .5,
    inDuration: 300, 
    outDuration: 200, 
    startingTop: '100%',
    endingTop: '10%'
  };

  presenter: core.com.ustadmobile.core.controller.XapiReportOptionsPresenter;
  private navigationSubscription;
  constructor(umService: UmBaseService, router: Router, route: ActivatedRoute, 
    umDb: UmDbMockService, private renderer:Renderer2, private elem: ElementRef) { 
      super(umService, router, route, umDb);

      this.navigationSubscription = this.router.events.filter(event => event instanceof NavigationEnd)
      .subscribe( _ => {
      if(this.umDatabase.xObjectDao){
        this.onCreate()
      }
    }); 
    }

    private onCreate(){
      if(this.umDatabase.xObjectDao){
        this.presenter = new core.com.ustadmobile.core.controller.XapiReportOptionsPresenter(
          this.context, UmAngularUtil.queryParamsToMap(), this.umDatabase.personDao,
          this.umDatabase.xObjectDao, this.umDatabase.xLangMapEntryDao);
        this.presenter.onCreate(null);
      }
    }


    onDeleteTag(event){
      console.log(event)
    }

    onSelectTag(event){
      let elements = this.elem.nativeElement.querySelectorAll('.chip');
      elements.forEach(element => {
        this.renderer.removeClass(element,"selected-chip");
      })
      this.renderer.addClass(event.target,"selected-chip");
    }

    onAddTag(event){
      
    }

    onViewMore(reportId){
      const args = UmAngularUtil.queryParamsToMap("?reportId=" + reportId)
      this.systemImpl.go("/ReportDetails", args, this.context, 0)
      console.log(args)
    }

    fillVisualChartType(translatedGraphList){}
    
    fillYAxisData(translatedYAxisList){}

    fillXAxisAndSubGroupData(translatedXAxisList){}

    updateWhoDataAdapter(whoList){}

    updateDidDataAdapter(didList){}

    updateFromDialogText(fromDate){}

    updateToDialogText(toDate){}

    updateWhenRangeText(rangeText){}

    updateChartTypeSelected(indexChart){}

    updateYAxisTypeSelected(indexYAxis){}

    updateXAxisTypeSelected(indexXAxis){}

    updateSubgroupTypeSelected(indexSubgroup){}

    updateWhoListSelected(personList){}

    updateDidListSelected(verbs){}

    ngOnDestroy(){
      super.ngOnDestroy()
      this.presenter.onDestroy();
      if (this.navigationSubscription) {  
        this.navigationSubscription.unsubscribe();
      }
    }

}

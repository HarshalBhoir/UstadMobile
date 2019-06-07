import { Injectable } from '@angular/core';
import { com as util} from 'lib-util';
import { com as db } from 'lib-database';
import { com as door } from 'lib-door-runtime';
import {UmAngularUtil} from "../../util/UmAngularUtil";
import { HttpClient } from '@angular/common/http';

@Injectable({
  providedIn: 'root'
})
export class UmDbMockService extends db.ustadmobile.core.db.UmAppDatabase {
  
  ROOT_UID = 1311236;

  private entriesLoaded = false;
  private initialized: boolean = false;
  private entriesJson = {};

  constructor(private http: HttpClient) {
    super()
    if (!this.initialized) {
      this.initialized = true;
      db.ustadmobile.core.db.UmAppDatabase.Companion.setInstance(this);
    }
  }

  contentEntryDao;
  contentEntryStatusDao = new ContentEntryStatusDao();
  contentEntryRelatedEntryJoinDao = new ContentEntryRelatedEntryJoinDao();
  containerDao = new ContainerDao();
  networkNodeDao = new NetworkNodeDao();

  getData(entryUid) {
    const data: ContentEntry[] = this.entriesJson[entryUid];
    return data;
  }

  loadEntries(){
    const languageUrl = "assets/entries.json";
    if(!this.entriesLoaded){
      this.entriesLoaded = true;
      this.http.get <any> (languageUrl).subscribe(entries => {
        this.contentEntryDao = new ContentEntryDao(entries);
        console.log("event", "entries loaded", this.contentEntryDao);
        this.entriesJson = entries;
      }); 
    }
  }

}



/**DAO */
class ContentEntryDao {

  
  constructor(private entries) {}

  findByUidWithContentEntryStatusAsync(entryUid){
    const entry: any = UmAngularUtil.findObjectByLabel(this.entries, 'contentEntryUid', entryUid);
    entry['contentEntryStatus'] = {downloadStatus:24};
    return entry;
  }

  getChildrenByParentUidWithCategoryFilter(entryUid, language, category): any {
    var entries = this.entries[entryUid] as db.ustadmobile.lib.db.entities.ContentEntry[];
    if(language != 0){
      entries = entries.splice(0,entries.length - 2);
    }
    if(category != 0){
      entries = entries.splice(0,entries.length - 3);
    }
    return UmAngularUtil.createObserver(entries);
  }

  getContentByUuidAsync(entryUid) {
    return UmAngularUtil.findObjectByLabel(this.entries, 'contentEntryUid', entryUid);
  }

  findUniqueLanguagesInListAsync(entryUid) {
    return util.ustadmobile.lib.util.UMUtil.jsArrayToKotlinList(languages[entryUid]);
  }

  findByUidAsync(entryUid) {
    return UmAngularUtil.findObjectByLabel(this.entries, 'contentEntryUid', entryUid);
  }


  findListOfCategoriesAsync(entryUid) {
    const schemas: DistinctCategorySchema[] = [
      {
        contentCategoryUid: 1,
        categoryName: "Category Name 1",
        contentCategorySchemaUid: 12,
        schemaName: "Schema"
      },
      {
        contentCategoryUid: 2,
        categoryName: "Category Name 2",
        contentCategorySchemaUid: 12,
        schemaName: "Schema"
      },
      {
        contentCategoryUid: 3,
        categoryName: "Category Name 3",
        contentCategorySchemaUid: 12,
        schemaName: "Schema"
      }
    ];
    return util.ustadmobile.lib.util.UMUtil.jsArrayToKotlinList([schemas])
  }
}

class ContainerDao{
  findFilesByContentEntryUid(entryUid){
    return util.ustadmobile.lib.util.UMUtil.jsArrayToKotlinList([]);
  }

  getMostRecentDownloadedContainerForContentEntryAsync(entryUid){
    const container = {
      mimeType: "application/zip",
      containerUid: 8989,
      fileSize: 8989898
    }

    return container as db.ustadmobile.lib.db.entities.Container;
  }

}

class NetworkNodeDao{

}

class ContentEntryStatusDao{
  constructor() {}
  findContentEntryStatusByUid(entryUid) : door.ustadmobile.door.DoorLiveData {
    return UmAngularUtil.createObserver(0) 
  }
}

class ContentEntryRelatedEntryJoinDao{
  findAllTranslationsForContentEntryAsync(entryUid){
    
    var relatedEntries = [
      {
        cerejContentEntryUid: entryUid,
        cerejRelatedEntryUid: 41250,
        languageName: "Sample1"
      }
    ];
  return util.ustadmobile.lib.util.UMUtil.jsArrayToKotlinList(relatedEntries);
}
}


/**Entities */
export interface ContentEntry {
  contentEntryUid: number;
  title: string;
  description: string;
  entryId: number;
  author: string;
  publisher: string;
  licenseType: number;
  licenseName: string;
  licenseUrl: string;
  sourceUrl: string;
  thumbnailUrl: string;
  lastModified: string;
  leaf: boolean;
}

export interface Language {
  langUid: number;
  name: string;
  iso_639_1_standard: string;
  iso_639_2_standard: string;
  iso_639_3_standard: string;
  langLocalChangeSeqNum: number;
  langMasterChangeSeqNum: number;
  langLastChangedBy: number;
}
export interface DistinctCategorySchema {
  contentCategoryUid: number
  categoryName: String
  contentCategorySchemaUid: number
  schemaName: String
}

const languages = {
  "1311236": [{
    "langUid": 1,
    "name": "pellentesque at",
    "iso_639_1_standard": "dui",
    "iso_639_2_standard": "quis",
    "iso_639_3_standard": "magna",
    "langLocalChangeSeqNum": 73801,
    "langMasterChangeSeqNum": 75240,
    "langLastChangedBy": 1624
  }, {
    "langUid": 2,
    "name": "lobortis sapien",
    "iso_639_1_standard": "ut",
    "iso_639_2_standard": "natoque",
    "iso_639_3_standard": "cum",
    "langLocalChangeSeqNum": 75482,
    "langMasterChangeSeqNum": 11068,
    "langLastChangedBy": 62140
  }, {
    "langUid": 3,
    "name": "iaculis congue",
    "iso_639_1_standard": "velit",
    "iso_639_2_standard": "pede",
    "iso_639_3_standard": "metus",
    "langLocalChangeSeqNum": 75370,
    "langMasterChangeSeqNum": 50569,
    "langLastChangedBy": 93444
  }, {
    "langUid": 4,
    "name": "sit amet",
    "iso_639_1_standard": "amet",
    "iso_639_2_standard": "pellentesque",
    "iso_639_3_standard": "vulputate",
    "langLocalChangeSeqNum": 39410,
    "langMasterChangeSeqNum": 46112,
    "langLastChangedBy": 82190
  }, {
    "langUid": 5,
    "name": "ut nunc",
    "iso_639_1_standard": "praesent",
    "iso_639_2_standard": "nisi",
    "iso_639_3_standard": "nam",
    "langLocalChangeSeqNum": 24164,
    "langMasterChangeSeqNum": 34629,
    "langLastChangedBy": 12748
  }, {
    "langUid": 6,
    "name": "phasellus in",
    "iso_639_1_standard": "amet",
    "iso_639_2_standard": "curae",
    "iso_639_3_standard": "placerat",
    "langLocalChangeSeqNum": 7131,
    "langMasterChangeSeqNum": 6606,
    "langLastChangedBy": 97838
  }],
  "41250": [{
    "langUid": 1,
    "name": "pellentesque at",
    "iso_639_1_standard": "dui",
    "iso_639_2_standard": "quis",
    "iso_639_3_standard": "magna",
    "langLocalChangeSeqNum": 73801,
    "langMasterChangeSeqNum": 75240,
    "langLastChangedBy": 1624
  }, {
    "langUid": 2,
    "name": "lobortis sapien",
    "iso_639_1_standard": "ut",
    "iso_639_2_standard": "natoque",
    "iso_639_3_standard": "cum",
    "langLocalChangeSeqNum": 75482,
    "langMasterChangeSeqNum": 11068,
    "langLastChangedBy": 62140
  }, {
    "langUid": 3,
    "name": "iaculis congue",
    "iso_639_1_standard": "velit",
    "iso_639_2_standard": "pede",
    "iso_639_3_standard": "metus",
    "langLocalChangeSeqNum": 75370,
    "langMasterChangeSeqNum": 50569,
    "langLastChangedBy": 93444
  }, {
    "langUid": 4,
    "name": "sit amet",
    "iso_639_1_standard": "amet",
    "iso_639_2_standard": "pellentesque",
    "iso_639_3_standard": "vulputate",
    "langLocalChangeSeqNum": 39410,
    "langMasterChangeSeqNum": 46112,
    "langLastChangedBy": 82190
  }, {
    "langUid": 5,
    "name": "ut nunc",
    "iso_639_1_standard": "praesent",
    "iso_639_2_standard": "nisi",
    "iso_639_3_standard": "nam",
    "langLocalChangeSeqNum": 24164,
    "langMasterChangeSeqNum": 34629,
    "langLastChangedBy": 12748
  }, {
    "langUid": 6,
    "name": "phasellus in",
    "iso_639_1_standard": "amet",
    "iso_639_2_standard": "curae",
    "iso_639_3_standard": "placerat",
    "langLocalChangeSeqNum": 7131,
    "langMasterChangeSeqNum": 6606,
    "langLastChangedBy": 97838
  }],
  "83098": [{
    "langUid": 1,
    "name": "pellentesque at",
    "iso_639_1_standard": "dui",
    "iso_639_2_standard": "quis",
    "iso_639_3_standard": "magna",
    "langLocalChangeSeqNum": 73801,
    "langMasterChangeSeqNum": 75240,
    "langLastChangedBy": 1624
  }, {
    "langUid": 2,
    "name": "lobortis sapien",
    "iso_639_1_standard": "ut",
    "iso_639_2_standard": "natoque",
    "iso_639_3_standard": "cum",
    "langLocalChangeSeqNum": 75482,
    "langMasterChangeSeqNum": 11068,
    "langLastChangedBy": 62140
  }, {
    "langUid": 3,
    "name": "iaculis congue",
    "iso_639_1_standard": "velit",
    "iso_639_2_standard": "pede",
    "iso_639_3_standard": "metus",
    "langLocalChangeSeqNum": 75370,
    "langMasterChangeSeqNum": 50569,
    "langLastChangedBy": 93444
  }, {
    "langUid": 4,
    "name": "sit amet",
    "iso_639_1_standard": "amet",
    "iso_639_2_standard": "pellentesque",
    "iso_639_3_standard": "vulputate",
    "langLocalChangeSeqNum": 39410,
    "langMasterChangeSeqNum": 46112,
    "langLastChangedBy": 82190
  }, {
    "langUid": 5,
    "name": "ut nunc",
    "iso_639_1_standard": "praesent",
    "iso_639_2_standard": "nisi",
    "iso_639_3_standard": "nam",
    "langLocalChangeSeqNum": 24164,
    "langMasterChangeSeqNum": 34629,
    "langLastChangedBy": 12748
  }, {
    "langUid": 6,
    "name": "phasellus in",
    "iso_639_1_standard": "amet",
    "iso_639_2_standard": "curae",
    "iso_639_3_standard": "placerat",
    "langLocalChangeSeqNum": 7131,
    "langMasterChangeSeqNum": 6606,
    "langLastChangedBy": 97838
  }]
}




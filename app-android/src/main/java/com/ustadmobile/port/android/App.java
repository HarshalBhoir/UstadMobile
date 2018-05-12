package com.ustadmobile.port.android;

import android.content.Context;

//import com.evernote.android.job.Job;
//import com.evernote.android.job.JobManager;
//import com.evernote.android.job.JobRequest;
import com.toughra.ustadmobile.BuildConfig;

import org.acra.ACRA;
import org.acra.config.CoreConfigurationBuilder;
import org.acra.config.HttpSenderConfigurationBuilder;
import org.acra.data.StringFormat;
import org.acra.sender.HttpSender;

/**
 * Created by varuna on 8/23/2017.
 *
 * Note: UmBaseApplication extends MultidexApplication on the multidex variant, but extends the
 * normal android.app.Application on non-multidex variants.
 *
 */
public class App extends UmBaseApplication {

    private int syncJobId;

//    private JobManager mJobManager;

    @Override
    public void onCreate() {
        System.out.println("UMSyncJob:App:");
        super.onCreate();

//        if(mJobManager == null){
//            JobManager.create(this).addJobCreator(new UMSyncJobCreator());
//            mJobManager = JobManager.instance();
//        }
//
//
//        Set<Job> allJobs = mJobManager.getAllJobs();
//        Set<Job> allUMSyncJobs = mJobManager.getAllJobsForTag(UMSyncJob.TAG);
//        Set<JobRequest> allJobRequests = mJobManager.getAllJobRequests();
//        Set<JobRequest> allUMSyncJobRequests = mJobManager.getAllJobRequestsForTag(UMSyncJob.TAG);
//
//        System.out.println(" UMSyncJob:App: Currently there are :"
//                + allJobs.size() + " jobs running.");
//        System.out.println(" UMSyncJob:App: Currently there are :"
//                + allUMSyncJobs.size() +
//                " UMSyncJobs running.");
//
//        System.out.println(" UMSyncJob:App: Currently there are: "
//                + allJobRequests.size() + " job REQUESTS.");
//        System.out.println(" UMSyncJob:App: Currently there are: "
//                + allUMSyncJobRequests.size() + " UMSyncJob REQUESTS.");
//
//        for(JobRequest thisJobRequest:allUMSyncJobRequests){
//            //sup?
//        }
//
//        /* just in case condition */
//        if(allUMSyncJobRequests.size() > 5){
//            System.out.println(" >>UMSyncJob:App: More than 5 job requests. Cancelling all.");
//            mJobManager.cancelAllForTag(UMSyncJob.TAG);
//        }
//
//        if(mJobManager.getAllJobRequestsForTag(UMSyncJob.TAG).size() > 0){
//            System.out.println(" UMSyncJob:App UMSyncJob Requests running. Monitor this: size: "
//                    + allUMSyncJobRequests.size());
//            System.out.println(" >>UMSyncJob:App UMSyncJob Not running any more jobs..");
//        }
//        else
//        if(syncJobId > -1 && mJobManager.getJobRequest(syncJobId) == null
//                && mJobManager.getJob(syncJobId) == null){
//            System.out.println(" >>UMSyncJob:App: Job : " + syncJobId + " not running. Scheduling it..");
//            //System.out.println("UMSyncJob:App: DISABLING SCHEDULING FOR NOW..TODO: FIX");
//            scheduleJob();
//            System.out.print("\n");
//        }else{
//            System.out.println(" >>UMSyncJob:App: Job: " + syncJobId + " is already running. " +
//                    "Not scheduling again.");
//        }
    }

    public void scheduleJob(){
        //Running job . We can check if this job id is running or closed.
        //Not checking network
        //Not checking device idle
        //Not checking device charging
        //Nothing to force requirement check (as above)

        //mJobManager = JobManager.instance();
//        syncJobId = new JobRequest.Builder(UMSyncJob.TAG)
//                //15 min is min and flex min is 5 min
//                .setPeriodic(TimeUnit.MINUTES.toMillis(15), TimeUnit.MINUTES.toMillis(5))
//                //Will make sure it gets run on boot
//                .setPersisted(true)
//                .build()
//                .schedule();
//        System.out.println("UMSyncJob:App: Job scheduled. ID: " + syncJobId);
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);

        CoreConfigurationBuilder builder = new CoreConfigurationBuilder(this);
        builder.setReportFormat(StringFormat.JSON);

        builder.getPluginConfigurationBuilder(HttpSenderConfigurationBuilder.class)
                .setUri(BuildConfig.ACRA_HTTP_URI)
                .setBasicAuthLogin(BuildConfig.ACRA_BASIC_LOGIN)
                .setBasicAuthPassword(BuildConfig.ACRA_BASIC_PASS)
                .setHttpMethod(HttpSender.Method.POST).setEnabled(true);

        ACRA.init(this, builder);
    }
}


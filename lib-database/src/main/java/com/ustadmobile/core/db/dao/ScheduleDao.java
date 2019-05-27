package com.ustadmobile.core.db.dao;

import com.ustadmobile.core.db.UmAppDatabase;
import com.ustadmobile.core.db.UmProvider;
import com.ustadmobile.core.impl.UmCallback;
import com.ustadmobile.core.util.UMCalendarUtil;
import com.ustadmobile.lib.database.annotation.UmDao;
import com.ustadmobile.lib.database.annotation.UmInsert;
import com.ustadmobile.lib.database.annotation.UmQuery;
import com.ustadmobile.lib.database.annotation.UmRepository;
import com.ustadmobile.lib.database.annotation.UmUpdate;
import com.ustadmobile.lib.db.entities.ClazzLog;
import com.ustadmobile.lib.db.entities.ClazzWithTimeZone;
import com.ustadmobile.lib.db.entities.DateRange;
import com.ustadmobile.lib.db.entities.Schedule;
import com.ustadmobile.lib.db.entities.ScheduledCheck;
import com.ustadmobile.lib.db.sync.dao.SyncableDao;

import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

@UmDao(inheritPermissionFrom = ClazzDao.class,
inheritPermissionForeignKey = "scheduleClazzUid",
inheritPermissionJoinedPrimaryKey = "clazzUid")
@UmRepository
public abstract class ScheduleDao implements SyncableDao<Schedule, ScheduleDao> {

    @UmInsert
    public abstract long insert(Schedule entity);

    @UmUpdate
    public abstract void update(Schedule entity);

    @UmInsert
    public abstract void insertAsync(Schedule entity, UmCallback<Long> resultObject);

    @UmQuery("SELECT * FROM Schedule")
    public abstract UmProvider<Schedule> findAllSchedules();

    @UmQuery("SELECT * FROM SCHEDULE")
    public abstract List<Schedule> findAllSchedulesAsList();

    @UmUpdate
    public abstract void updateAsync(Schedule entity, UmCallback<Integer> resultObject);

    @UmQuery("SELECT * FROM Schedule WHERE scheduleUid = :uid")
    public abstract Schedule findByUid(long uid);

    @UmQuery("SELECT * FROM Schedule WHERE scheduleUid = :uid")
    public abstract void findByUidAsync(long uid, UmCallback<Schedule> resultObject);

    @UmQuery("SELECT * FROM Schedule WHERE scheduleClazzUid = :clazzUid AND scheduleActive = 1")
    public abstract UmProvider<Schedule> findAllSchedulesByClazzUid(long clazzUid);

    @UmQuery("SELECT * FROM Schedule WHERE scheduleClazzUid = :clazzUid AND scheduleActive = 1")
    public abstract List<Schedule> findAllSchedulesByClazzUidAsList(long clazzUid);

    public void disableSchedule(long scheduleUid){
        findByUidAsync(scheduleUid, new UmCallback<Schedule>() {
            @Override
            public void onSuccess(Schedule result) {
                result.setScheduleActive(false);
                update(result);
            }

            @Override
            public void onFailure(Throwable exception) {

            }
        });
    }

    @UmQuery("SELECT * FROM DateRange " +
            " LEFT JOIN Clazz ON Clazz.clazzUid = :clazzUid " +
            " WHERE DateRange.dateRangeUMCalendarUid = Clazz.clazzHolidayUMCalendarUid " )
    public abstract List<DateRange> findAllHolidayDateRanges(long clazzUid);

    /**
     * Checks if a given date is a holiday in the clazz uid specified.
     * @param checkDate The date to check if its a holiday
     * @param clazzUid  The clazz to check for's clazzUid
     * @return  true if it is a holiday, false if not.
     */
    public boolean checkGivenDateAHolidayForClazz(long checkDate, long clazzUid){
        //1. Get all date ranges for the given clazz day
        List<DateRange> holidays = findAllHolidayDateRanges(clazzUid);
        for(DateRange everyHoliday :holidays){
            //2. Null checkDate's year even if its not present TODO
            long fromDate = everyHoliday.getDateRangeFromDate();
            long toDate = everyHoliday.getDateRangeToDate();
            //3. Null year in fromDate and toDate
            //3. Compare
            if(toDate != 0){
                return (checkDate >= fromDate && checkDate <= toDate);
            }else{
                return (checkDate == fromDate);
            }
        }

        return false;
    }

    /**
     * Creates ClazzLogs for every clazzes the account person has access to between start and end
     * time.
     *
     * Note: We always create ClazzLogs in the TimeZone.
     * Note 2: the startTime and endTime are times in the phone's timezone.
     *
     * @param startTime             between start time
     * @param endTime               AND end time
     * @param accountPersonUid      The person
     * @param db                    The database
     */
    public void
    createClazzLogs(long startTime, long endTime, long accountPersonUid, UmAppDatabase db) {
        //This method will usually be called from the Workmanager in Android every day. Making the
        // start time 00:00 and end tim 23:59 : Note: This is the device's timzone. (not class)
        Calendar startCalendar = Calendar.getInstance();
        startCalendar.setTimeInMillis(startTime);
        UMCalendarUtil.normalizeSecondsAndMillis(startCalendar);

        Calendar endCalendar = Calendar.getInstance();
        endCalendar.setTimeInMillis(endTime);
        UMCalendarUtil.normalizeSecondsAndMillis(endCalendar);

        long startMsOfDay = ((startCalendar.get(Calendar.HOUR_OF_DAY) * 24) +
                startCalendar.get(Calendar.MINUTE)) * 60 * 1000;

        //Get a list of all classes the logged in user has access to:
        List<ClazzWithTimeZone> clazzList = db.getClazzDao().findAllClazzesWithSelectPermission(
                accountPersonUid);
        //Loop over the classes
        for(ClazzWithTimeZone clazz : clazzList) {
            //Skipp classes that have no time zone
            if(clazz.getTimeZone() == null) {
                System.err.println("Warning: cannot create schedules for clazz" +
                        clazz.getClazzName() + ", uid:" +
                        clazz.getClazzUid() + " as it has no timezone");
                continue;
            }

            String timeZone = clazz.getTimeZone();


            //Get a list of schedules for the classes
            List<Schedule> clazzSchedules = findAllSchedulesByClazzUidAsList(clazz.getClazzUid());
            for(Schedule schedule : clazzSchedules) {

                boolean incToday = startMsOfDay <= schedule.getSceduleStartTime();
                long startTimeMins = schedule.getSceduleStartTime() / (1000 * 60);

                Calendar nextScheduleOccurence = null;

                if(schedule.getScheduleFrequency() == Schedule.SCHEDULE_FREQUENCY_DAILY){

                    Calendar tomorrow = Calendar.getInstance();
                    tomorrow.add(Calendar.DATE, 1);
                    int tomorrowDay = tomorrow.get(Calendar.DAY_OF_WEEK);
                    int today = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);

                    int dayOfWeek;
                    if(!incToday){
                        dayOfWeek = tomorrowDay;
                    }else{
                        dayOfWeek = today;
                    }
                    //TODO: Associate with weekend feature in the future
                    if(dayOfWeek == Calendar.SUNDAY){
                        //skip
                        System.out.println("Today is a weekend. Skipping ClazzLog creation for today.");

                    }else if(checkGivenDateAHolidayForClazz(startCalendar.getTimeInMillis(),
                            clazz.getClazzUid())){
                        //Its a holiday. Skip
                        System.out.println("Skipping holiday");

                    } else if(clazz.getClazzEndTime() != 0 &&
                            startCalendar.getTimeInMillis() > clazz.getClazzEndTime()){
                        //Date is ahead of clazz end date. Skipping.
                        System.out.println("Skipping cause current date is after Class's end date.");

                    } else if(clazz.getClazzStartTime() != 0 &&
                            startCalendar.getTimeInMillis() < clazz.getClazzStartTime()){
                        //Date is before Clazz's start date. Skipping
                        System.out.println("Skipping cause current date is before Class's start date.");
                    } else {

                        //This will get the next schedule for that day. For the same day, it will
                        //return itself if incToday is set to true, else it will go to next week.
                        nextScheduleOccurence = UMCalendarUtil.copyCalendarAndAdvanceTo(
                                startCalendar, timeZone ,dayOfWeek, incToday);

                        //Set to 00:00
                        nextScheduleOccurence.set(Calendar.HOUR_OF_DAY, 0);
                        nextScheduleOccurence.set(Calendar.MINUTE, 0);
                        nextScheduleOccurence.set(Calendar.SECOND, 0);
                        nextScheduleOccurence.set(Calendar.MILLISECOND, 0);

                        //Now move it to desired hour:
                        nextScheduleOccurence.set(Calendar.HOUR_OF_DAY, (int) (startTimeMins / 60));
                        nextScheduleOccurence.set(Calendar.MINUTE, (int) (startTimeMins % 60));
                        nextScheduleOccurence.set(Calendar.SECOND, 0);
                        nextScheduleOccurence.set(Calendar.MILLISECOND, 0);
                    }

                }else if(schedule.getScheduleFrequency() == Schedule.SCHEDULE_FREQUENCY_WEEKLY) {

                    if(checkGivenDateAHolidayForClazz(startCalendar.getTimeInMillis(),
                            clazz.getClazzUid())) {
                        //Its a holiday. Skip it.
                        System.out.println("Skipping holiday");
                    }else if(clazz.getClazzEndTime() != 0 &&
                            startCalendar.getTimeInMillis() > clazz.getClazzEndTime()){
                        //Date is ahead of clazz end date. Skipping.
                        System.out.println("Skipping cause current date is after Class's end date.");

                    } else if(clazz.getClazzStartTime() != 0 &&
                            startCalendar.getTimeInMillis() < clazz.getClazzStartTime()){
                        //Date is before Clazz's start date. Skipping
                        System.out.println("Skipping cause current date is before Class's start date.");
                    }else{

                        //Will be true if today is schedule day
                        incToday = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
                                == schedule.getScheduleDay();

                        //Get the day of next occurence.
                        nextScheduleOccurence = UMCalendarUtil.copyCalendarAndAdvanceTo(
                                startCalendar, timeZone, schedule.getScheduleDay(), incToday);

                        //Set the day's timezone to Clazz
                        nextScheduleOccurence.setTimeZone(TimeZone.getTimeZone(timeZone));

                        nextScheduleOccurence = UMCalendarUtil.copyCalendarAndAdvanceTo(
                                startCalendar, clazz.getTimeZone(), schedule.getScheduleDay(), incToday);

                        //Set to 00:00
                        nextScheduleOccurence.set(Calendar.HOUR_OF_DAY, 0);
                        nextScheduleOccurence.set(Calendar.MINUTE, 0);
                        nextScheduleOccurence.set(Calendar.SECOND, 0);
                        nextScheduleOccurence.set(Calendar.MILLISECOND, 0);

                        //Now move it to desired hour:
                        nextScheduleOccurence.set(Calendar.HOUR_OF_DAY, (int) (startTimeMins / 60));
                        nextScheduleOccurence.set(Calendar.MINUTE, (int) (startTimeMins % 60));
                        nextScheduleOccurence.set(Calendar.SECOND, 0);
                        nextScheduleOccurence.set(Calendar.MILLISECOND, 0);
                    }
                }

                if (nextScheduleOccurence != null && nextScheduleOccurence.before(endCalendar)) {
                    //this represents an instance of this class that should take place and
                    //according to the arguments provided, we should check that this instance exists
                    int logInstanceHash = ClazzLogDao.generateClazzLogUid(clazz.getClazzUid(),
                            nextScheduleOccurence.getTimeInMillis());
                    ClazzLog existingLog = db.getClazzLogDao().findByUid(logInstanceHash);

                    if (existingLog == null || existingLog.isCanceled()) {
                        ClazzLog newLog = new ClazzLog(logInstanceHash, clazz.getClazzUid(),
                                nextScheduleOccurence.getTimeInMillis(), schedule.getScheduleUid());
                        db.getClazzLogDao().replace(newLog);
                    }
                }
            }
        }
    }

    @UmInsert
    public abstract void insertScheduledCheck(ScheduledCheck check);

    /**
     * Used in testing.
     *
     * @param days
     * @param accountPersonUid
     * @param db
     */
    public void createClazzLogsForEveryDayFromDays(int days, long accountPersonUid,
                                                   UmAppDatabase db){
        for(int i=1;i<=days;i++){
            Calendar dayCal = Calendar.getInstance();
            dayCal.add(Calendar.DATE, -i);
            dayCal.set(Calendar.HOUR_OF_DAY,0);
            dayCal.set(Calendar.MINUTE, 0);
            dayCal.set(Calendar.SECOND, 0);
            dayCal.set(Calendar.MILLISECOND, 0);
            long startTime = dayCal.getTimeInMillis();

            dayCal.set(Calendar.HOUR_OF_DAY, 23);
            dayCal.set(Calendar.MINUTE, 59);
            dayCal.set(Calendar.SECOND, 59);
            dayCal.set(Calendar.MILLISECOND, 999);
            long endTime = dayCal.getTimeInMillis();
            createClazzLogs(startTime, endTime, accountPersonUid, db);
        }
    }
    /**
     *  Creates clazzLog for today since clazzlogs are generated for the next day
     *  automatically.
     *  Called when a new Schedule is created in AddScheduleDialogPresenter , AND
     *  Called by ClazzLogScheduleWorker work manager to be run everyday 00:00
     *
     *  The method creates ClazzLog from the device's time zone.
     *  ie: today is device's 00:00 to device's 23:59.
     */
    public void createClazzLogsForToday(long accountPersonUid, UmAppDatabase db) {

        //Note this calendar is created on the device's time zone.
        Calendar dayCal = Calendar.getInstance();
        dayCal.set(Calendar.HOUR_OF_DAY, 0);
        dayCal.set(Calendar.MINUTE, 0);
        dayCal.set(Calendar.SECOND, 0);
        dayCal.set(Calendar.MILLISECOND, 0);
        long startTime = dayCal.getTimeInMillis();

        dayCal.set(Calendar.HOUR_OF_DAY, 23);
        dayCal.set(Calendar.MINUTE, 59);
        dayCal.set(Calendar.SECOND, 59);
        dayCal.set(Calendar.MILLISECOND, 999);
        long endTime = dayCal.getTimeInMillis();
        createClazzLogs(startTime, endTime, accountPersonUid, db);
    }

}

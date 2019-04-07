package com.ustadmobile.lib.db.entities;

import java.util.LinkedList;
import java.util.List;

/**
 * This is used as a memory efficient summary of the status of a download
 *
 */
public class DownloadJobItemStatus {

    private int jobItemUid;

    private transient LinkedList<DownloadJobItemStatus> parents;

    private long contentEntryUid;

    private long bytesSoFar;

    private long totalBytes;


    public DownloadJobItemStatus() {

    }

    public DownloadJobItemStatus(DownloadJobItem item) {
        jobItemUid = (int)item.getDjiUid();
        contentEntryUid = item.getDjiContentEntryUid();
        bytesSoFar = item.getDownloadedSoFar();
        totalBytes = item.getDownloadLength();
    }

    public int getJobItemUid() {
        return jobItemUid;
    }

    public long getBytesSoFar() {
        return bytesSoFar;
    }

    public long getTotalBytes() {
        return totalBytes;
    }

    public void setJobItemUid(int jobItemUid) {
        this.jobItemUid = jobItemUid;
    }

    public void setContentEntryUid(long contentEntryUid) {
        this.contentEntryUid = contentEntryUid;
    }

    public void setBytesSoFar(long bytesSoFar) {
        this.bytesSoFar = bytesSoFar;
    }

    public void setTotalBytes(long totalBytes) {
        this.totalBytes = totalBytes;
    }

    public final void incrementTotalBytes(long increment) {
        totalBytes += increment;
    }

    public final void incrementBytesSoFar(long increment) {
        bytesSoFar += increment;
    }

    public List<DownloadJobItemStatus> getParents() {
        return parents;
    }

    public void addParent(DownloadJobItemStatus parent) {
        if(parents == null)
            parents = new LinkedList<>();

        parents.add(parent);
    }

    public long getContentEntryUid() {
        return contentEntryUid;
    }

    @Override
    public int hashCode() {
        return jobItemUid;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof DownloadJobItemStatus
                && ((DownloadJobItemStatus)o).jobItemUid == this.jobItemUid;
    }
}

package com.ustadmobile.core.db.dao;

import com.ustadmobile.lib.database.annotation.UmDao;
import com.ustadmobile.lib.database.annotation.UmInsert;
import com.ustadmobile.lib.database.annotation.UmQuery;
import com.ustadmobile.lib.db.entities.EntryStatusResponse;
import com.ustadmobile.lib.db.entities.EntryStatusResponseWithNode;

import java.util.List;

/**
 * Created by mike on 1/31/18.
 */
@UmDao
public abstract class EntryStatusResponseDao {

    @UmInsert
    public abstract void insert(List<EntryStatusResponse> responses);


    @UmQuery("SELECT (COUNT(*) > 0) FROM EntryStatusResponse WHERE entryId = :entryId and available = 1 ")
    public abstract boolean isEntryAvailableLocally(String entryId);


    @UmQuery("SELECT * FROM EntryStatusResponse " +
            " LEFT JOIN NetworkNode ON EntryStatusResponse.responderNodeId = NetworkNode.nodeId " +
            "WHERE entryId = :entryId AND available = :available ")
    public abstract List<EntryStatusResponseWithNode> findByEntryIdAndAvailability(String entryId, boolean available);


    public class EntryWithoutRecentResponse {
        private long contentUid;

        private int nodeId;

        public long getContentUid() {
            return contentUid;
        }

        public void setContentUid(long contentUid) {
            this.contentUid = contentUid;
        }

        public int getNodeId() {
            return nodeId;
        }

        public void setNodeId(int nodeId) {
            this.nodeId = nodeId;
        }
    }

    @UmQuery("SELECT Content.contentUid, NetworkNode.nodeId FROM Content, NetworkNode," +
            " WHERE Content.contentUid IN (:contentUids) " +
            " AND NetworkNode.nodeId IN (:nodeIds)  " +
            " AND NOT EXISTS(Select id FROM EntryStatusResponse WHERE entryId = ContentUids.contentUid AND responderNodeId = NodeIds.nodeId) ")
    public abstract List<EntryWithoutRecentResponse> findEntriesWithoutRecentResponse(List<Long> contentUids, List<Integer> nodeIds);
}

package com.ustadmobile.lib.db.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ustadmobile.door.annotation.LastChangedBy
import com.ustadmobile.door.annotation.LocalChangeSeqNum
import com.ustadmobile.door.annotation.MasterChangeSeqNum
import com.ustadmobile.door.annotation.SyncableEntity
import com.ustadmobile.lib.db.entities.ContentEntry.Companion.TABLE_ID

/**
 * Entity that represents content as it is browsed by the user. A ContentEntry can be either:
 * 1. An actual piece of content (e.g. book, course, etc), in which case there should be an associated
 * ContentEntryFile.
 * 2. A navigation directory (e.g. a category as it is scraped from another site, etc), in which case
 * there should be the appropriate ContentEntryParentChildJoin entities present.
 */
@Entity
@SyncableEntity(tableId = TABLE_ID)
open class ContentEntry() {


    @PrimaryKey(autoGenerate = true)
    var contentEntryUid: Long = 0

    var title: String? = null

    var description: String? = null

    /**
     * Get the embedded unique ID which can be found in the underlying file, if any. For
     * example the EPUB identifier for EPUB files, or the ID attribute of an xAPI zip file.
     *
     * @return The embedded unique ID which can be found in the underlying file
     */
    /**
     * Set the embedded unique ID which can be found in the underlying file, if any. For
     * example the EPUB identifier for EPUB files, or the ID attribute of an xAPI zip file.
     *
     * @param entryId The embedded unique ID which can be found in the underlying file
     */
    var entryId: String? = null

    var author: String? = null

    var publisher: String? = null

    var licenseType: Int = 0

    var licenseName: String? = null

    var licenseUrl: String? = null

    /**
     * Get the original URL this resource came from. In the case of resources that
     * were generated by scraping, this refers to the URL that the scraper targeted to
     * generated the resource.
     *
     * @return the original URL this resource came from
     */
    /**
     * Set the original URL this resource came from. In the case of resources that
     * were generated by scraping, this refers to the URL that the scraper targeted to
     * generated the resource.
     *
     * @param sourceUrl the original URL this resource came from
     */
    var sourceUrl: String? = null

    var thumbnailUrl: String? = null

    var lastModified: Long = 0

    //TODO: Migration : add to migration
    @ColumnInfo(index = true)
    var primaryLanguageUid: Long = 0

    var languageVariantUid: Long = 0

    var leaf: Boolean = false

    var imported: Boolean = false

    var inAppContent: Boolean = false

    /**
     * Represents if this content entry is public for anyone to use
     *
     * @return true if this content entry is public for anyone to use, false otherwise
     */
    /**
     * Set if this content entry is public for anyone to use
     *
     * @param publik true if this content entry is public for anyone to use, false otherwise
     */
    var publik: Boolean = false

    var contentTypeFlag: Int = 0

    @LocalChangeSeqNum
    var contentEntryLocalChangeSeqNum: Long = 0

    @MasterChangeSeqNum
    var contentEntryMasterChangeSeqNum: Long = 0

    @LastChangedBy
    var contentEntryLastChangedBy: Int = 0

    constructor(title: String, description: String, leaf: Boolean, publik: Boolean) : this() {
        this.title = title
        this.description = description
        this.leaf = leaf
        this.publik = publik
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true

        val entry = other as ContentEntry?

        if (contentEntryUid != entry!!.contentEntryUid) return false
        if (licenseType != entry.licenseType) return false
        if (primaryLanguageUid != entry.primaryLanguageUid) return false
        if (languageVariantUid != entry.languageVariantUid) return false
        if (leaf != entry.leaf) return false
        if (contentTypeFlag != entry.contentTypeFlag) return false
        if (if (title != null) title != entry.title else entry.title != null) return false
        if (if (description != null) description != entry.description else entry.description != null)
            return false
        if (if (entryId != null) entryId != entry.entryId else entry.entryId != null) return false
        if (if (author != null) author != entry.author else entry.author != null) return false
        if (if (publisher != null) publisher != entry.publisher else entry.publisher != null)
            return false
        if (if (licenseName != null) licenseName != entry.licenseName else entry.licenseName != null)
            return false
        if (if (licenseUrl != null) licenseUrl != entry.licenseUrl else entry.licenseUrl != null)
            return false
        if (if (sourceUrl != null) sourceUrl != entry.sourceUrl else entry.sourceUrl != null)
            return false

        return if (thumbnailUrl != null) thumbnailUrl == entry.thumbnailUrl else entry.thumbnailUrl == null
    }

    override fun hashCode(): Int {
        var result = (contentEntryUid xor contentEntryUid.ushr(32)).toInt()
        result = 31 * result + if (title != null) title!!.hashCode() else 0
        result = 31 * result + if (description != null) description!!.hashCode() else 0
        result = 31 * result + if (entryId != null) entryId!!.hashCode() else 0
        result = 31 * result + if (author != null) author!!.hashCode() else 0
        result = 31 * result + if (publisher != null) publisher!!.hashCode() else 0
        result = 31 * result + licenseType
        result = 31 * result + if (licenseName != null) licenseName!!.hashCode() else 0
        result = 31 * result + if (licenseUrl != null) licenseUrl!!.hashCode() else 0
        result = 31 * result + if (sourceUrl != null) sourceUrl!!.hashCode() else 0
        result = 31 * result + if (thumbnailUrl != null) thumbnailUrl!!.hashCode() else 0
        result = 31 * result + (primaryLanguageUid xor primaryLanguageUid.ushr(32)).toInt()
        result = 31 * result + (languageVariantUid xor languageVariantUid.ushr(32)).toInt()
        result = 31 * result + contentTypeFlag
        result = 31 * result + if (leaf) 1 else 0
        return result
    }

    companion object {

        const val TABLE_ID = 42

        const val LICENSE_TYPE_CC_BY = 1

        const val LICENSE_TYPE_CC_BY_SA = 2

        const val LICENSE_TYPE_CC_BY_SA_NC = 3

        const val LICENSE_TYPE_CC_BY_NC = 4

        const val ALL_RIGHTS_RESERVED = 5

        const val LICESNE_TYPE_CC_BY_NC_SA = 6

        const val PUBLIC_DOMAIN = 7

        const val UNDEFINED_TYPE = 0

        const val COLLECTION_TYPE = 1

        const val EBOOK_TYPE = 2

        const val INTERACTIVE_EXERICSE_TYPE = 3

        const val VIDEO_TYPE = 4

        const val AUDIO_TYPE = 5

        const val DOCUMENT_TYPE = 6

        const val ARTICLE_TYPE = 7

        const val LICENSE_TYPE_OTHER = 8
    }

}

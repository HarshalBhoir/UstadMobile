/*
    This file is part of Ustad Mobile.

    Ustad Mobile Copyright (C) 2011-2014 UstadMobile Inc.

    Ustad Mobile is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version with the following additional terms:

    All names, links, and logos of Ustad Mobile and Toughra Technologies FZ
    LLC must be kept as they are in the original distribution.  If any new
    screens are added you must include the Ustad Mobile logo as it has been
    used in the original distribution.  You may not create any new
    functionality whose purpose is to diminish or remove the Ustad Mobile
    Logo.  You must leave the Ustad Mobile logo as the logo for the
    application to be used with any launcher (e.g. the mobile app launcher).

    If you want a commercial license to remove the above restriction you must
    contact us.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.

    Ustad Mobile is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

 */
package com.ustadmobile.core.opds;
import com.ustadmobile.core.impl.UMLog;
import com.ustadmobile.core.impl.UstadMobileSystemImpl;
import com.ustadmobile.core.util.UMIOUtils;
import com.ustadmobile.core.util.UMUtil;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Vector;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

/**
 * Represents an OPDS Feed containing entries.
 *
 * @author varuna
 */
public class UstadJSOPDSFeed extends UstadJSOPDSItem{
    
    public UstadJSOPDSEntry[] entries;

    /**
     * The absolute URL of this catalog (HTTP or Filesystem based)
     */
    public String href;

    /**
     * Cached reference to the OPDS self link
     */
    private String[] selfLink;

    
    public UstadJSOPDSFeed() {
        href = null;
    }
    
    /**
     * Full argument constructor used to create a new OPDS feed with mandatory
     * elements: 
     * 
     * @param srcHref The base HREF for relative links of this feed
     * @param title The OPDS Title
     * @param id The OPDS ID of the feed itself
     */
    public UstadJSOPDSFeed(String srcHref, String title, String id) {
        this.href = srcHref;
        this.entries = new UstadJSOPDSEntry[0];
        this.title = title;
        this.id = id;
    }


    /**
     * Load the given feed from an XmlPullParser. This will parse the feed itself and all child
     * entries.
     *
     * @param xpp XmlPullParser to read from
     * @throws XmlPullParserException
     * @throws IOException
     */
    public void loadFromXpp(XmlPullParser xpp) throws XmlPullParserException, IOException{
        loadFromXpp(xpp, this);
    }


    
    /**
     * Add an entry to the end of this feed
     * 
     * @param entry  The entry to be added
     */
    public void addEntry(UstadJSOPDSEntry entry) {
        UstadJSOPDSEntry[] newEntries = new UstadJSOPDSEntry[entries.length + 1];
        System.arraycopy(this.entries, 0, newEntries, 0, this.entries.length);
        newEntries[entries.length] = entry;
        this.entries = newEntries;
    }
    
    /**
     * Sort entries use a given comparer
     * 
     * @param comparer 
     */
    public void sortEntries(UMUtil.Comparer comparer) {
        UMUtil.bubbleSort(entries, comparer);
    }
    
    
    public UstadJSOPDSEntry getEntryById (String id) {
        UstadJSOPDSEntry entry;
        for (int i = 0;i < this.entries.length; i++){
            if (this.entries[i].id.equals(id)){
                entry = this.entries[i];
                return entry;
            }
        }
        
        return null;
    }


    
    public UstadJSOPDSEntry[] getEntriesByLinkParams(String linkRel, 
        String linkType, boolean relByPrefix, boolean mimeTypeByPrefix){
        Vector matches = new Vector();
        for (int i=0; i< this.entries.length; i++){
            Vector entryResult = this.entries[i].getLinks(linkRel, linkType, 
                    relByPrefix, mimeTypeByPrefix);
            if(entryResult.size() > 0) {
                matches.addElement(this.entries[i]);
            }
        }
        
        UstadJSOPDSEntry[] matchEntries = new UstadJSOPDSEntry[matches.size()];
        matches.copyInto(matchEntries);
        return matchEntries;
    }
    
    public boolean isAcquisitionFeed(){
        Object[] entries = this.getEntriesByLinkParams(
                UstadJSOPDSEntry.LINK_ACQUIRE, null, true, false);
        return (entries.length > 0);
    }

    /**
     * Serialize the entire feed to an XmlSerializer. Creates a new Xml document using the XmlSerializer
     *
     * @param xs XmlSerializer to use
     *
     * @throws IOException
     */
    public void serialize(XmlSerializer xs) throws IOException {
        serializeStartDoc(xs);
        xs.startTag(NS_ATOM, "feed");

        //set the href where this came from if not already added as a link
        Vector absoluteSelfLink = getLinks(LINK_REL_SELF_ABSOLUTE, null);
        if(absoluteSelfLink.isEmpty()) {
            //Todo: this should be the feeds mime type
            addLink(LINK_REL_SELF_ABSOLUTE, "application/xml", href);
        }

        serializeAttrs(xs);
        
        for(int i = 0; i < entries.length; i++) {
            entries[i].serializeEntryTag(xs);
        }
        xs.endTag(NS_ATOM, "feed");
        xs.endDocument();
    }


    
    /**
     * Serializes the feed as XML to the given output stream.  This will flush
     * and close the output stream.  Closing will happen even if there is
     * an exception when writing
     * 
     * @param out
     * @throws IOException 
     */
    public void serialize(OutputStream out) throws IOException {
        IOException ioe = null;
        XmlSerializer serializer = UstadMobileSystemImpl.getInstance().newXMLSerializer();
        try {
            serializer.setOutput(out, "UTF-8");
            serialize(serializer);
            out.flush();
        }catch(IOException e) {
            UstadMobileSystemImpl.l(UMLog.ERROR, 170, title, e);
            ioe = e;
        }finally {
            UMIOUtils.closeOutputStream(out);
            UMIOUtils.throwIfNotNullIO(ioe);
        }
        
    }

    /**
     *
     */
    public String[] getSelfLink() {
        if(selfLink == null) {
            selfLink = getFirstLink(LINK_REL_SELF, null);
        }

        return selfLink;
    }


    /**
     * Get an absolute link to where this catalog was loaded from
     * @return
     */
    public String[] getAbsoluteSelfLink() {
        String[] absoluteSelfLink = getFirstLink(LINK_REL_SELF_ABSOLUTE, null);
        if(absoluteSelfLink == null && href != null){
            absoluteSelfLink = addLink(LINK_REL_SELF_ABSOLUTE, TYPE_ACQUISITIONFEED, href);
        }


        return absoluteSelfLink;
    }

    /**
     * Returns the number of entries in this feed
     *
     * @return Number of entries in this feed
     */
    public int size() {
        return entries.length;
    }


    /**
     * Return the entry at the given index in this feed
     *
     * @param index Index of the entry
     * @return Entry at the given index
     */
    public UstadJSOPDSEntry getEntry(int index) {
        return entries[index];
    }

    /**
     * Given acquisition preferences : select the links to actually download
     *
     * @param preferredMimeTypes
     * @param preferredLanguages
     */
    public UstadJSOPDSFeed selectAcquisitionLinks(Vector selectedEntries, final String[] preferredMimeTypes, final String[] preferredLanguages, final int mimeWeight, final int langWeight) {
        UstadJSOPDSFeed retFeed = new UstadJSOPDSFeed(href, title, id);
        retFeed.addLink(getAbsoluteSelfLink());

        int numEntries = selectedEntries.size();
        final int noMatchFactor = 500;

        for(int i = 0; i < numEntries; i++) {
            UstadJSOPDSEntry selectedEntry = (UstadJSOPDSEntry)selectedEntries.elementAt(i);

            Vector entryTranslationLinks = getEntryById(selectedEntry.id).getAlternativeTranslations();
            Vector translatedEntriesVector = new Vector();
            String[] entryTranslationLink;

            for(int j = 0; j < entryTranslationLinks.size(); j++) {
                entryTranslationLink = (String[])entryTranslationLinks.elementAt(j);
                UstadJSOPDSEntry translatedEntry = findEntryForAlternateTranslation(
                        entryTranslationLink[UstadJSOPDSEntry.ATTR_HREF]);
                if(translatedEntry != null)
                    translatedEntriesVector.addElement(translatedEntry);
            }

            UstadJSOPDSEntry[] entriesToSelectFrom = new UstadJSOPDSEntry[
                    translatedEntriesVector.size() + 1];
            translatedEntriesVector.copyInto(entriesToSelectFrom);
            entriesToSelectFrom[entriesToSelectFrom.length-1] = selectedEntry;

            UMUtil.bubbleSort(entriesToSelectFrom, new UMUtil.Comparer() {
                @Override
                public int compare(Object o1, Object o2) {
                    UstadJSOPDSEntry entry1 = (UstadJSOPDSEntry)o1;
                    UstadJSOPDSEntry entry2 = (UstadJSOPDSEntry)o2;

                    Object link1Obj = entry1.getBestAcquisitionLink(preferredMimeTypes);
                    Object link2Obj = entry2.getBestAcquisitionLink(preferredMimeTypes);

                    //TODO: Handle when there is no acquisition link at all.
                    //getFirstAcquisitionLink is redundant, this is done by getBestAcquisitionLink anyway
                    String[] link1 = link1Obj != null ? (String[])link1Obj : entry1.getFirstAcquisitionLink(null);
                    String[] link2 = link2Obj != null ? (String[])link2Obj : entry2.getFirstAcquisitionLink(null);



                    int mimeDiff1 = UMUtil.indexInArray(preferredMimeTypes,
                            link1[UstadJSOPDSEntry.LINK_MIMETYPE]);
                    if(mimeDiff1 == -1)
                        mimeDiff1 = preferredMimeTypes.length+1;

                    int mimeDiff2 = UMUtil.indexInArray(preferredMimeTypes,
                            link2[UstadJSOPDSEntry.LINK_MIMETYPE]);
                    if(mimeDiff2 == -1)
                        mimeDiff2 = preferredMimeTypes.length+1;

                    int mimeDiff = (mimeDiff1 - mimeDiff2) * mimeWeight;

                    int langDiff1 = UMUtil.indexInArray(preferredLanguages, entry1.getLanguage());
                    if(langDiff1 == -1)
                        langDiff1 = preferredLanguages.length+1;

                    int langDiff2 = UMUtil.indexInArray(preferredLanguages, entry2.getLanguage());
                    if(langDiff2 == -1)
                        langDiff2 = preferredLanguages.length+1;

                    int langDiff = (langDiff1 - langDiff2) * langWeight;

                    return langDiff + mimeDiff;
                }
            });

            if(entriesToSelectFrom.length > 0) {
                UstadJSOPDSEntry acquisitionEntry = new UstadJSOPDSEntry(retFeed,
                        entriesToSelectFrom[0], false);
                acquisitionEntry.addLink(entriesToSelectFrom[0].getBestAcquisitionLink(preferredMimeTypes));
                retFeed.addEntry(acquisitionEntry);
            }
        }

        return retFeed;
    }

    /**
     * Finds the langauges for which entries from this feed are available.
     *
     * @param linkRel Link relationship to search for - e.g. acquisition
     * @param mimeType mime type to search for
     * @param relByPrefix whether to match by prefix for relationship as per getLinks
     * @param mimeTypeByPrefix whether to match mime type by prefix as per getLinks
     * @param languageList Optionally an existing vector to use to add languages found to
     * @return A vector with a list of all distinct languages found in all entries of this feed
     */
    public Vector getLinkHrefLanguageOptions(String linkRel, String mimeType, boolean relByPrefix, boolean mimeTypeByPrefix, Vector languageList) {
        int numEntries = size();
        if(languageList == null)
            languageList = new Vector();

        Vector linkVector;
        for(int i = 0; i < numEntries; i++) {
            linkVector = this.entries[i].getLinks(linkRel, mimeType, relByPrefix, mimeTypeByPrefix);
            this.entries[i].getHrefLanguagesFromLinks(linkVector, languageList);
        }

        return languageList;
    }

    public Vector getEntriesByLinkParams(String linkRel, String mimeType, String href, boolean relByPrefix, boolean mimeTypeByPrefix, boolean hrefByPrefix, int limit) {
        Vector matchingEntries = new Vector();
        Vector entryResult;
        for(int i = 0; i < size(); i++) {
            entryResult = getEntry(i).getLinks(linkRel, mimeType, href, relByPrefix, mimeTypeByPrefix,
                    hrefByPrefix, 1);
            if(entryResult != null && entryResult.size() > 0)
                matchingEntries.addElement(getEntry(i));
        }

        return matchingEntries;
    }

    public UstadJSOPDSEntry getFirstEntryByLinkParams(String linkRel, String mimeType, String href, boolean relByPrefix, boolean mimeTypeByPrefix, boolean hrefByPrefix) {
        Vector matches = getEntriesByLinkParams(linkRel, mimeType, href, relByPrefix, mimeTypeByPrefix, hrefByPrefix, 1);
        return matches.size() > 0 ? (UstadJSOPDSEntry)matches.elementAt(0) : null;
    }

    public UstadJSOPDSEntry findEntryForAlternateTranslation(String alternateHref) {
        String entryLang;
        String[] currentAlternateLinks;
        for(int i = 0; i < size(); i++) {
            entryLang = getEntry(i).getLanguage();
            currentAlternateLinks = getEntry(i).getFirstLink(LINK_REL_ALTERNATE, null, alternateHref,
                    false, false, false);

            if(currentAlternateLinks != null
                    && (currentAlternateLinks[ATTR_HREFLANG] == null
                        ||UMUtil.isSameLanguage(currentAlternateLinks[ATTR_HREFLANG], entryLang))) {
                return getEntry(i);
            }
        }

        return null;
    }


    /**
     * Filter this feed to produce a new OPDS feed
     *
     * @param filter
     * @param newId
     * @param title
     * @param srcHref
     * @return
     */
    public UstadJSOPDSFeed filter(OPDSEntryFilter filter, String title, String srcHref, String newId) {
        UstadJSOPDSFeed filteredFeed = new UstadJSOPDSFeed(srcHref, title, newId);

        int numEntries = size();
        boolean accept;
        for(int i = 0; i < numEntries; i++) {
            accept = filter.accept(getEntry(i));
            if(accept) {
                filteredFeed.addEntry(new UstadJSOPDSEntry(filteredFeed, getEntry(i)));
            }
        }

        return filteredFeed;
    }

    /**
     * Find which languages are available for entries in this feed
     *
     * @return
     */
    public Vector getAvailableLanguages() {
        int size = size();
        Vector languages = new Vector();
        Vector alternateTranslations;
        int i, j;
        String[] currentLink;
        String currentLang;
        for(i = 0; i < size; i++) {
            alternateTranslations = getEntry(i).getAlternativeTranslations();

            for(j = 0; j < alternateTranslations.size(); j++){
                currentLink = (String[])alternateTranslations.elementAt(j);
                currentLang =currentLink[ATTR_HREFLANG];
                if(!languages.contains(currentLang))
                    languages.addElement(currentLang);
            }
        }

        return languages;
    }






}

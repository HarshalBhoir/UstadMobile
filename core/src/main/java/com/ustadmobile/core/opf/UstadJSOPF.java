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
package com.ustadmobile.core.opf;

import com.ustadmobile.core.impl.UMLog;
import com.ustadmobile.core.impl.UstadMobileSystemImpl;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.Hashtable;
import java.util.Vector;

/**
 *
 * @author varuna
 */
public class UstadJSOPF {
    
    private Hashtable mimeExceptions;
    //defaultMimeTypes.put("","");
    
    static Hashtable defaultMimeTypes;
    public static String DEFAULT_MIMETYPE = "application/octet-stream";
    
    public UstadJSOPFItem[] spine;

    private Vector coverImages;
    
    /**
     * The item from the OPF that contains "nav" in it's properties.  As per the 
     * EPUB spec there must be exactly one such item
     */
    public UstadJSOPFItem navItem;
    
    public String title;
    
    public String id;
    
    /**
     * Flag value to indicate we should parse the metadata (e.g. title, identifier, description)
     */
    public static final int PARSE_METADATA = 1;
    
    /**
     * Flag value to indicate we should parse the manifest
     */
    public static final int PARSE_MANIFEST = 2;
    
    
    static {
        setupDefaultMimeTypes();
    }
    
    
    private static void setupDefaultMimeTypes() {
        defaultMimeTypes = new Hashtable();
        defaultMimeTypes.put("gif", "image/gif");
        defaultMimeTypes.put("js", "application/javascript");
        defaultMimeTypes.put("jpg","image/jpg");
        defaultMimeTypes.put("jpeg", "image/jpg");
        defaultMimeTypes.put("png", "image/png");
        defaultMimeTypes.put("svg","image/svg+xml");
        defaultMimeTypes.put("css","text/css");
        defaultMimeTypes.put("html","text/html");
        defaultMimeTypes.put("xml","application/xml");
        defaultMimeTypes.put("xhtml","application/xhtml+xml");
        defaultMimeTypes.put("mp4","video/mp4");
        defaultMimeTypes.put("3gp","video/3gpp");
        defaultMimeTypes.put("avi","video/x-msvideo");
        defaultMimeTypes.put("wmv","video/x-ms-wmv");
        defaultMimeTypes.put("bmp", "image/bmp");
        defaultMimeTypes.put("tiff","image/tiff");
        defaultMimeTypes.put("woff","application/x-font-woff");
        defaultMimeTypes.put("mp3","audio/mpeg");
        defaultMimeTypes.put("wav","audio/wav");
        defaultMimeTypes.put("mid", "audio/midi");
        defaultMimeTypes.put("midi","audio/midi");
        defaultMimeTypes.put("aac","audio/x-aac");
        defaultMimeTypes.put("mj2","video/mj2");
    }
    
    public static final String getExtension(String filename) {
        int dotPos = filename.lastIndexOf('.');
        return dotPos != -1 ? filename.substring(dotPos + 1) : null;
    }
    
    public UstadJSOPF() {
        mimeExceptions = new Hashtable();
    }
    
    
    public static UstadJSOPF loadFromOPF(XmlPullParser xpp) throws XmlPullParserException, IOException {
        return loadFromOPF(xpp, PARSE_METADATA | PARSE_MANIFEST);
    }
    
    /*
     * xpp: Parser of the OPF
     */
    public static UstadJSOPF loadFromOPF(XmlPullParser xpp, int parseFlags) throws XmlPullParserException, IOException {
        UstadJSOPF result = new UstadJSOPF();
        
        
        boolean parseMetadata = (parseFlags & PARSE_METADATA) == PARSE_METADATA;
        boolean parseManifest = (parseFlags & PARSE_MANIFEST) == PARSE_MANIFEST;
        
        
        String extension = null;
        String defMimeType = null;
        int evtType = xpp.getEventType();
        String filename=null;
        String itemMime=null;
        String id=null;
        String properties=null;
        String idref=null;
        boolean isLinear = true;
        String isLinearStrVal = null;
        Hashtable allItems = new Hashtable();
        Vector spineItems = new Vector();        

        
        /*
         * the dc:identifier attribute as per 
         * http://www.idpf.org/epub/30/spec/epub30-publications.html#sec-opf-metadata-identifiers-uid
         */
        String uniqueIdentifier = null;

        
        boolean inMetadata = false;
        do
        {
            filename=null;
            itemMime=null;
            id=null;
            properties=null;
            defMimeType = null;
            extension=null;
            idref=null;
            isLinear = true;
            isLinearStrVal = null;
            
                        
            
            //If we are parsing the manifest
            if(parseManifest) {
                if(evtType == XmlPullParser.START_TAG){
                    if(xpp.getName() != null && xpp.getName().equals("item")){

                        filename=xpp.getAttributeValue(null, "href");
                        itemMime=xpp.getAttributeValue(null, "media-type");
                        id = xpp.getAttributeValue(null, "id");
                        properties = xpp.getAttributeValue(null, "properties");


                        extension=getExtension(filename);
                        if(extension != null && defaultMimeTypes.containsKey(extension)){
                            defMimeType = (String)defaultMimeTypes.get(extension);
                        }

                        if(extension == null || defMimeType == null ||
                                !itemMime.equals(defMimeType)){
                            result.mimeExceptions.put(filename, itemMime);
                        }
                        UstadJSOPFItem item2 = new UstadJSOPFItem();
                        item2.href = filename;
                        item2.mimeType = itemMime;
                        item2.properties = properties;
                        item2.id = id;
                        
                        if(properties != null && properties.indexOf("nav") != -1) {
                            result.navItem = item2;
                        }
                        if(properties != null && properties.indexOf("cover-image") != -1) {
                            result.addCoverImage(item2);
                        }


                        allItems.put(id, item2);

                    }else if(xpp.getName() != null && xpp.getName().equals("itemref")){
                        //for each itemRef in spine
                        idref=xpp.getAttributeValue(null, "idref");
                        isLinearStrVal = xpp.getAttributeValue(null, "linear");

                        Object spineItem = allItems.get(idref);
                        if(spineItem != null) {
                            if(isLinearStrVal != null) {
                                char isLinearChar = isLinearStrVal.charAt(0);
                                isLinear = !(isLinearChar == 'n' | isLinearChar == 'N');
                                ((UstadJSOPFItem)allItems.get(idref)).linear = isLinear;
                            }

                            spineItems.addElement(allItems.get(idref)); 
                        }else {
                            UstadMobileSystemImpl.l(UMLog.WARN, 209, idref);
                        }
                    }
                }else if(evtType == XmlPullParser.END_TAG){
                    if(xpp.getName().equals("spine")){
                        result.spine = new UstadJSOPFItem[spineItems.size()];
                        spineItems.copyInto(result.spine);
                    }
                }
            }
            
            if(parseMetadata) {
                if(evtType == XmlPullParser.START_TAG) {
                    if(uniqueIdentifier == null && xpp.getName().equals("package")) {
                        uniqueIdentifier = xpp.getAttributeValue(null, 
                                "unique-identifier");
                    }else if(inMetadata == false && xpp.getName().equals("metadata")) {
                        inMetadata = true;
                    }
                    
                    if(inMetadata) {
                        if(xpp.getName().equals("dc:title")) {
                            result.title = xpp.nextText();
                        }else if(xpp.getName().equals("dc:identifier")) {
                            String idAttr = xpp.getAttributeValue(null, "id");
                            if(idAttr != null && idAttr.equals(uniqueIdentifier)) {
                                result.id = xpp.nextText();
                            }
                        }
                    }
                }else if(evtType == XmlPullParser.END_TAG) {
                    if(inMetadata == true && xpp.getName().equals("metadata")) {
                        inMetadata = false;
                    }
                }
            }
            
            
            evtType = xpp.next();
            
        }while(evtType != XmlPullParser.END_DOCUMENT);
        
        return result;
    }
    
    public String getMimeType(String filename) {
        if(mimeExceptions.containsKey(filename)) {
            return (String)mimeExceptions.get(filename);
        }
        
        String extension = filename.substring(filename.lastIndexOf('.'));
        if(defaultMimeTypes.containsKey(extension)) {
            return (String)defaultMimeTypes.get(extension);
        }
        
        return DEFAULT_MIMETYPE;
    }
    
    /**
     * Get the an Array of all the URLs in the spine
     * 
     * @return 
     */
    public String[] getSpineURLS() {
        String[] spineURLs = new String[this.spine.length];
        for(int i = 0; i < this.spine.length; i++) {
            spineURLs[i] = this.spine[i].href;
        }
        return spineURLs;
    }
    
    /**
     * Gets an array of linear hrefs from the spine
     * 
     * @return String array of all the HREFs that are in the linear spine order
     */
    public String[] getLinearSpineHREFs() {
        Vector spineHREFs = new Vector();
        for(int i = 0; i < this.spine.length; i++) {
            if(this.spine[i].linear) {
                spineHREFs.addElement(this.spine[i].href);
            }
        }
        
        String[] linearSpine = new String[spineHREFs.size()];
        spineHREFs.copyInto(linearSpine);
        return linearSpine;
    }
    
     /**
     * Find the position of a particular spine item
     * 
     * @param href the href to find the position of in the spine (as it appears in the OPF (relative)
     * @return position of that item in the linear spine or -1 if not found
     */
    public int getLinearSpinePositionByHREF(String href) {
        String[] linearSpine = getLinearSpineHREFs();
        for(int i = 0; i < linearSpine.length; i++) {
            if(linearSpine[i].equals(href)) {
                return i;
            }
        }
        
        return -1;
    }

    /**
     * Add a cover image item.
     *
     * @param coverImage UstadJSOPFItem representing the cover image (including href and mime type)
     */
    public void addCoverImage(UstadJSOPFItem coverImage) {
        if(coverImages == null)
            coverImages = new Vector();

        coverImages.addElement(coverImage);
    }

    /**
     * Get the cover image for this publication.
     *
     * @param mimeType Preferred mime type (unimplemented)
     *
     * @return UstadJSOPFItem representing the cover image
     */
    public UstadJSOPFItem getCoverImage(String mimeType) {
        //TODO: implement mime type preference here

        if(coverImages == null || coverImages.size() == 0)
            return null;

        return (UstadJSOPFItem)coverImages.elementAt(0);
    }

}

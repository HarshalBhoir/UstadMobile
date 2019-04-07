package com.ustadmobile.core.catalog.contenttype

import com.ustadmobile.core.view.H5PContentView

import java.util.Arrays

/**
 * Created by mike on 2/15/18.
 */

class H5PContentType : ContentTypePlugin() {

    override val viewName: String
        get() = H5PContentView.VIEW_NAME

    override val mimeTypes: List<String>
        get() = Arrays.asList("application/h5p+zip")

    override val fileExtensions: List<String>
        get() = Arrays.asList("h5p")
}

package com.ustadmobile.port.sharedse.impl.http

import com.ustadmobile.core.impl.UstadMobileSystemImpl
import com.ustadmobile.lib.util.UMUtil
import org.kmp.io.KMPSerializerParser
import org.kmp.io.KMPXmlParser
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream

/**
 * Performs some minor tweaks on HTML being served to enable EPUB pagination and handling html
 * autoplay:
 * - Add a script immediately after the body tag so that it can, if desired, apply columns style.
 * This is only applied to HTML which is a top level frame, allownig content in an iframe to work
 * as expected.
 * - Add a meta viewport tag  -
 *
 */
class EpubHtmlFilterSerializer {

    var scriptSrcToAdd: String? = null

    private var `in`: InputStream? = null

    //add the script
    val output: ByteArray
        @Throws(IOException::class, XmlPullParserException::class)
        get() {
            val bout = ByteArrayOutputStream()
            val xs = UstadMobileSystemImpl.instance.newXMLSerializer()
            xs.setOutput(bout, "UTF-8")

            val xpp = UstadMobileSystemImpl.instance.newPullParser(`in`!!, "UTF-8")
            xs.startDocument("UTF-8", false)
            UMUtil.passXmlThrough(xpp, xs, true, object : UMUtil.PassXmlThroughFilter {
                @Throws(IOException::class)
                override fun beforePassthrough(evtType: Int, parser: KMPXmlParser, serializer: KMPSerializerParser): Boolean {
                    if (evtType == XmlPullParser.END_TAG && parser.getName() == "head") {
                        serializer.startTag(parser.getNamespace(), "meta")
                        serializer.attribute(parser.getNamespace(), "name", "viewport")
                        serializer.attribute(parser.getNamespace(), "content",
                                "height=device-height, initial-scale=1,user-scalable=no")
                        serializer.endTag(parser.getNamespace(), "meta")
                    }
                    return true
                }

                @Throws(IOException::class, XmlPullParserException::class)
                override fun afterPassthrough(evtType: Int, parser: KMPXmlParser, serializer: KMPSerializerParser): Boolean {
                    if (evtType == XmlPullParser.START_TAG && parser.getName() == "body") {
                        serializer.startTag(parser.getNamespace(), "script")
                        serializer.attribute(parser.getNamespace(), "src", scriptSrcToAdd?: "")
                        serializer.attribute(parser.getNamespace(), "type", "text/javascript")
                        serializer.text(" ")
                        serializer.endTag(parser.getNamespace(), "script")
                    }

                    return true
                }
            })

            xs.endDocument()
            bout.flush()
            return bout.toByteArray()
        }

    fun setIntput(`in`: InputStream) {
        this.`in` = `in`
    }


}

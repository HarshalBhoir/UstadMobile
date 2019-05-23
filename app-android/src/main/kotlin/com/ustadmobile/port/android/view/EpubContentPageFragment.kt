package com.ustadmobile.port.android.view

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.support.v4.app.Fragment
import android.support.v4.view.GestureDetectorCompat
import android.view.*
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.RelativeLayout
import com.toughra.ustadmobile.R
import com.ustadmobile.core.impl.UMLog
import com.ustadmobile.core.util.UMFileUtil


/**
 * A simple fragment that uses a webview to show content one page of an EPUB.
 */
class EpubContentPageFragment : Fragment() {

    private var mUrl: String? = null

    private var mPageIndex: Int = 0

    /**
     * The webView for the given URL
     */
    private var webView: WebView? = null

    /**
     * Main root view here
     */
    private var viewGroup: ViewGroup? = null

    private var mTapToHideToolbarHandler: TapToHideToolbarHandler? = null

    private var webViewTouchHandler: Handler? = null

    private val touchDownEvent: MotionEvent? = null

    private val touchDownTime: Long = 0

    private var gestureDetector: GestureDetectorCompat? = null

    private val webViewClickCallback = Handler.Callback{ msg ->
        if (msg.what == HANDLER_CLICK_ON_VIEW) {
            mTapToHideToolbarHandler!!.onTap(mPageIndex)
        }

        true
    }

    internal interface TapToHideToolbarHandler {

        fun onTap(pageIndex: Int)

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mUrl = if (arguments != null)
            arguments!!.getString(ARG_PAGE_URL, "about:blank")
        else
            "about:blank"
        mPageIndex = if (arguments != null) arguments!!.getInt(ARG_PAGE_INDEX) else 0
    }

    @SuppressLint("SetJavaScriptEnabled", "ObsoleteSdkInt")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        if (viewGroup == null) {
            viewGroup = inflater.inflate(R.layout.fragment_epubcontent_page,
                    container, false) as RelativeLayout
            webView = viewGroup!!.findViewById(R.id.fragment_container_page_webview)
        } else {
            UMLog.l(UMLog.DEBUG, 517, "Containerpage: recycled onCreateView")
        }

        webView!!.tag = mPageIndex

        //Android after Version 17 (4.4) by default requires a gesture before any media playback happens
        if (Build.VERSION.SDK_INT >= 17) {
            webView!!.settings.mediaPlaybackRequiresUserGesture = false
        }

        webView!!.settings.javaScriptEnabled = true
        webView!!.settings.domStorageEnabled = true
        webView!!.settings.cacheMode = WebSettings.LOAD_DEFAULT
        webView!!.webViewClient = WebViewClient()
        webView!!.webChromeClient = WebChromeClient()
        webView!!.loadUrl(mUrl)
        webView!!.setDownloadListener { url, userAgent, contentDisposition, mimetype, contentLength ->
            val request = DownloadManager.Request(Uri.parse(url))
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS,
                    UMFileUtil.getFilename(url))
            val downloadManager = context!!.getSystemService(
                    Context.DOWNLOAD_SERVICE) as DownloadManager
            downloadManager.enqueue(request)
        }

        webViewTouchHandler = Handler(webViewClickCallback)

        gestureDetector = GestureDetectorCompat(webView!!.context,
                object : GestureDetector.SimpleOnGestureListener() {
                    override fun onSingleTapUp(e: MotionEvent): Boolean {
                        webViewTouchHandler!!.sendEmptyMessageDelayed(HANDLER_CLICK_ON_VIEW, 200)
                        return super.onSingleTapUp(e)
                    }
                })

        webView!!.setOnTouchListener { view, motionEvent -> gestureDetector!!.onTouchEvent(motionEvent) }

        return viewGroup
    }

    override fun onPause() {
        if (webView != null) {
            webView!!.onPause()
        }

        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        if (webView != null) {
            webView!!.onResume()
        }
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        if (context is TapToHideToolbarHandler)
            mTapToHideToolbarHandler = context
    }

    override fun onDetach() {
        super.onDetach()
        mTapToHideToolbarHandler = null
    }

    companion object {

        /**
         * Argument with the entire, absolute url for this page
         */
        val ARG_PAGE_URL = "pg_url"

        /**
         * Page index argument: used to tag the webview so it can be specified during testing
         */
        val ARG_PAGE_INDEX = "pg_index"

        val HANDLER_CLICK_ON_LINK = 1

        val HANDLER_CLICK_ON_VIEW = 2

        fun newInstance(url: String, pageIndex: Int): EpubContentPageFragment {
            val fragment = EpubContentPageFragment()
            val args = Bundle()
            args.putString(ARG_PAGE_URL, url)
            args.putInt(ARG_PAGE_INDEX, pageIndex)
            fragment.arguments = args
            return fragment
        }
    }
}// Required empty public constructor

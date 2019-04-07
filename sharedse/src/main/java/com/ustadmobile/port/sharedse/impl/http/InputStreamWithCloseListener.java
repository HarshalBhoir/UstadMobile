package com.ustadmobile.port.sharedse.impl.http;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

import fi.iki.elonen.NanoHTTPD;

/**
 * A simple FilterInputStream that is used so an event can be received when the underlying stream
 * is closed.
 */
public class InputStreamWithCloseListener extends FilterInputStream{

    private volatile OnCloseListener onCloseListener;

    /**
     * A listener that will receive an event notification when the stream is closed.
     */
    public interface OnCloseListener {

        /**
         * Event that signifies the stream has been closed. It will be called only once no matter
         * how many times the underlying stream is closed.
         */
        void onStreamClosed();
    }

    public InputStreamWithCloseListener(InputStream inputStream, OnCloseListener onCloseListener) {
        super(inputStream);
        this.onCloseListener = onCloseListener;
    }


    @Override
    public void close() throws IOException {
        super.close();
        if(onCloseListener != null) {
            onCloseListener.onStreamClosed();

            /*
              As per the java spec an already closed input stream can be closed again, with no
              effect. Therefor this event should be delivered once, and only once.
             */
            onCloseListener = null;
        }
    }
}
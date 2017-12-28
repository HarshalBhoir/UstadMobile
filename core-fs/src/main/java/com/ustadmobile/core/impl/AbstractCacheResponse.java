package com.ustadmobile.core.impl;

import com.ustadmobile.core.impl.http.UmHttpResponse;

/**
 * An HttpResponse provided by the cache. All UmHttpResponse objects provided by HttpCache are
 * descendants of this class.
 *
 * Created by mike on 12/27/17.
 */
public abstract class AbstractCacheResponse extends UmHttpResponse{

    private int cacheResponse;

    /**
     * Represents a respnose that was served entirely from the network.
     */
    public static final int MISS = 0;

    /**
     * Represents a response that was served directly from the cache without any http traffic
     * required. It was either within the default period that it was considered fresh, or the original
     * cache control / expiry information was sufficient.
     */
    public static final int HIT_DIRECT = 1;

    /**
     * Represents a response that was served from the cache, but needed an http trip to validate
     * the response.
     */
    public static final int HIT_VALIDATED = 2;


    protected void setCacheResponse(int cacheResponse) {
        this.cacheResponse = cacheResponse;
    }

    /**
     * Determine if the item was returned from the network or from the cache.
     *
     * @see #MISS
     * @see #HIT_DIRECT
     * @see #HIT_VALIDATED
     *
     * @return Flag indicating whether the respnose is from the cache or the network
     */
    public int getCacheResponse() {
        return cacheResponse;
    }

    /**
     * Determine if the response is considered a hit or a miss.
     *
     * @return true if the response was a cache hit, false otherwise
     */
    public boolean isHit() {
        return cacheResponse == HIT_DIRECT || cacheResponse == HIT_VALIDATED;
    }

    /**
     * Get the file path to where this entry is stored on disk.
     *
     * @return File path to where this entry is stored on disk.
     */
    public abstract String getFilePath();


}

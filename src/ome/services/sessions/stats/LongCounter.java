/*
 *   $Id$
 *
 *   Copyright 2008 Glencoe Software, Inc. All rights reserved.
 *   Use is subject to license terms supplied in LICENSE.txt
 */
package ome.services.sessions.stats;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import ome.system.OmeroContext;
import ome.util.messages.InternalMessage;


/**
 * Counter object which increments an internal long by some integer value,
 * and according to some strategy publishes an {@link InternalMessage} subclass.
 * 
 * @author Josh Moore, josh at glencoesoftware.com
 * @since Beta4
 */
public abstract class LongCounter implements ApplicationContextAware{

    private final Log log = LogFactory.getLog(getClass());
    
    private OmeroContext ctx;
    
    private int interval = 0;
    
    private long last = 0;
    
    private final Object mutex = new Object();
    
    protected long count = 0;
    
    public LongCounter(int interval) {
        this.interval = interval;
    }

    public void setApplicationContext(ApplicationContext arg0)
            throws BeansException {
        this.ctx = (OmeroContext) arg0;
    }

    public void increment(int incr) {
        synchronized (mutex) {
            count++;
            if (count >= (last+interval)) {
                last = count;
                InternalMessage message = message();
                try {
                    log.info("Publishing "+ message);
                    ctx.publishMessage(message);
                } catch (Throwable t) {
                    log.error(message + " produced an error: "+t);
                }
            }
        }
    }
 
    /**
     * 
     * @return
     */
    protected abstract InternalMessage message();

}
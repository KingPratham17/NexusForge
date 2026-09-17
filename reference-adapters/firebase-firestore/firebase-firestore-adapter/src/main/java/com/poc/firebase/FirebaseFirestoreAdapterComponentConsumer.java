package com.poc.firebase;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.support.ScheduledPollConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FirebaseFirestoreAdapterComponentConsumer
        extends ScheduledPollConsumer {

    private static final transient Logger LOG =
            LoggerFactory.getLogger(FirebaseFirestoreAdapterComponentConsumer.class);

    private final FirebaseFirestoreAdapterComponentEndpoint endpoint;

    public FirebaseFirestoreAdapterComponentConsumer(
            FirebaseFirestoreAdapterComponentEndpoint endpoint,
            Processor processor) {

        super(endpoint, processor);
        this.endpoint = endpoint;
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();
    }

    @Override
    protected int poll() throws Exception {

        LOG.info("Firebase Firestore Consumer polling...");

        /*
         * Sender-side Firestore implementation
         * will be added later.
         */

        return 0;
    }
}
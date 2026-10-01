package com.poc.firebase;

import org.apache.camel.CamelContext;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultRegistry;
import org.apache.camel.support.DefaultPollingEndpoint;
import org.apache.camel.support.DefaultComponent;
import org.apache.camel.Endpoint;
import org.junit.Test;
import java.util.Map;

public class CamelEndpointResolutionTest {
    @Test
    public void testEndpoint() throws Exception {
        DefaultRegistry registry = new DefaultRegistry();
        registry.bind("MessageFlow_5.pollConsumerScheduler", new MyScheduler());
        
        registry.bind("adkPollStrategy_MessageFlow_5", new org.apache.camel.spi.PollingConsumerPollStrategy() {
            @Override public boolean begin(org.apache.camel.Consumer consumer, org.apache.camel.Endpoint endpoint) { return true; }
            @Override public void commit(org.apache.camel.Consumer consumer, org.apache.camel.Endpoint endpoint, int polledMessages) {}
            @Override public boolean rollback(org.apache.camel.Consumer consumer, org.apache.camel.Endpoint endpoint, int retryCounter, Exception cause) throws Exception { return false; }
        });

        CamelContext context = new DefaultCamelContext(registry);
        
        context.addComponent("my-polling", new DefaultComponent() {
            @Override
            protected Endpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters) throws Exception {
                Endpoint endpoint = new MyAnnotatedEndpoint(uri, this);
                setProperties(endpoint, parameters);
                return endpoint;
            }
        });
        
        Endpoint endpoint = context.getEndpoint("my-polling://foo?pollStrategy=#adkPollStrategy_MessageFlow_5&scheduler=MessageFlow_5.pollConsumerScheduler&scheduler.cron=0/15 * 0-23 ? * * *&scheduler.timeZone=Etc/GMT");
        endpoint.createConsumer(null);
        System.out.println("SUCCESSFULLY RESOLVED ENDPOINT");
        org.apache.camel.support.ScheduledPollEndpoint spe = (org.apache.camel.support.ScheduledPollEndpoint) endpoint;
        System.out.println("SchedulerProperties: " + spe.getSchedulerProperties());
    }

    static class MyScheduler implements org.apache.camel.spi.ScheduledPollConsumerScheduler {
        public void onInit(org.apache.camel.Consumer consumer) {}
        public void scheduleTask(Runnable task) {}
        public void unscheduleTask() {}
        public void startScheduler() {}
        public boolean isSchedulerStarted() { return true; }
        public void setCamelContext(CamelContext camelContext) {}
        public CamelContext getCamelContext() { return null; }
        public void start() {}
        public void stop() {}
        public void shutdown() {}
    }

    @org.apache.camel.spi.UriEndpoint(scheme = "my-polling", syntax = "my-polling", title = "My Polling")
    public static class MyAnnotatedEndpoint extends DefaultPollingEndpoint {
        public MyAnnotatedEndpoint(String uri, org.apache.camel.Component component) {
            super(uri, component);
        }
        @Override
        public org.apache.camel.Consumer createConsumer(org.apache.camel.Processor processor) { return null; }
        @Override
        public org.apache.camel.Producer createProducer() { return null; }
        @Override
        public boolean isSingleton() { return true; }
    }

    public static void main(String[] args) throws Exception {
        new CamelEndpointResolutionTest().testEndpoint();
    }
}

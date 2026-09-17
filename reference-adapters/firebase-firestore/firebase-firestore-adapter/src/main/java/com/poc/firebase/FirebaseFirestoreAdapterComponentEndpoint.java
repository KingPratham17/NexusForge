package com.poc.firebase;

import java.net.URISyntaxException;

import org.apache.camel.Consumer;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.spi.UriEndpoint;
import org.apache.camel.spi.UriParam;
import org.apache.camel.support.DefaultPollingEndpoint;

@UriEndpoint(
        firstVersion = "1.0.0",
        scheme = "firebase-firestore",
        syntax = "firebase-firestore",
        title = "Firebase Firestore"
)
public class FirebaseFirestoreAdapterComponentEndpoint
        extends DefaultPollingEndpoint {

    private final FirebaseFirestoreAdapterComponentComponent component;

    /**
     * Firebase project ID.
     *
     * Deliberately named firebaseProjectId instead of projectId
     * to avoid collision with SAP/ADK iFlow project-related
     * properties.
     */
    @UriParam
    private String firebaseProjectId;

    /**
     * Alias of the SAP Cloud Integration Secure Parameter
     * containing the Firebase service-account JSON.
     */
    @UriParam
    private String credentialAlias;

    /**
     * Firestore collection name.
     */
    @UriParam
    private String collection;

    /**
     * Firestore document ID.
     */
    @UriParam
    private String documentId;

    /**
     * Firestore operation.
     */
    @UriParam
    private String operation;

    public FirebaseFirestoreAdapterComponentEndpoint() {
        this.component = null;
    }

    public FirebaseFirestoreAdapterComponentEndpoint(
            final String endpointUri,
            final FirebaseFirestoreAdapterComponentComponent component)
            throws URISyntaxException {

        super(endpointUri, component);
        this.component = component;
    }

    public FirebaseFirestoreAdapterComponentEndpoint(
            final String uri,
            final String remaining,
            final FirebaseFirestoreAdapterComponentComponent component)
            throws URISyntaxException {

        this(uri, component);
    }

    public String getFirebaseProjectId() {
        return firebaseProjectId;
    }

    public void setFirebaseProjectId(String firebaseProjectId) {
        this.firebaseProjectId = firebaseProjectId;
    }

    public String getCredentialAlias() {
        return credentialAlias;
    }

    public void setCredentialAlias(String credentialAlias) {
        this.credentialAlias = credentialAlias;
    }

    public String getCollection() {
        return collection;
    }

    public void setCollection(String collection) {
        this.collection = collection;
    }

    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    @Override
    public Producer createProducer() throws Exception {
        return new FirebaseFirestoreAdapterComponentProducer(this);
    }

    @Override
    public Consumer createConsumer(Processor processor) throws Exception {
        throw new UnsupportedOperationException(
                "Firebase Firestore adapter is a receiver-only adapter");
    }

    @Override
    public boolean isSingleton() {
        return true;
    }
}
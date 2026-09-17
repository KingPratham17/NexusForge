package com.poc.firebase;

import java.util.Map;

public interface FirestoreDocumentService {

    void createDocument(
            String collection,
            String documentId,
            Map<String, Object> data) throws Exception;
}

package it.gov.pagopa.self.expense.connector;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public interface FileStorageConnector {
    void uploadFile(InputStream inputStream, String fileName, String contentType);
    ByteArrayOutputStream downloadFile(String fileName);

    void delete(String content);

}

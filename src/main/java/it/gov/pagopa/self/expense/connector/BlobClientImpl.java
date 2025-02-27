package it.gov.pagopa.self.expense.connector;


import it.gov.pagopa.common.azure.storage.AzureBlobClientImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

@Service
public class BlobClientImpl extends AzureBlobClientImpl implements FileStorageConnector {

    BlobClientImpl(@Value("${blobStorage.connectionString}") String storageConnectionString,
                   @Value("${blobStorage.file.containerReference}") String fileContainerReference) {
        super(storageConnectionString, fileContainerReference);
    }

    @Override
    public void uploadFile(InputStream inputStream, String fileName, String contentType) {
        upload(inputStream, fileName, contentType);
    }

    @Override
    public ByteArrayOutputStream downloadFile(String fileName) {
        return download(fileName);
    }

    @Override
    public void delete(String content){
        deleteFile(content);
    }


}

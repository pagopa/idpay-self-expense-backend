package it.gov.pagopa.self.expense.connector;


import com.azure.core.http.rest.Response;
import it.gov.pagopa.common.azure.storage.AzureBlobAsyncClientImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.ByteBuffer;

@Service
public class BlobAsyncAsyncClientImpl extends AzureBlobAsyncClientImpl implements FileStorageAsyncConnector {

    BlobAsyncAsyncClientImpl(@Value("${blobStorage.connectionString}") String storageConnectionString,
                             @Value("${blobStorage.file.containerReference}") String fileContainerReference) {
        super(storageConnectionString, fileContainerReference);
    }

    @Override
    public Mono<Boolean> uploadFile(Flux<ByteBuffer> data, String fileName, String contentType) {
        return upload(data, fileName, contentType).map(blockBlobItemResponse -> blockBlobItemResponse.getStatusCode() == 201);
    }

    @Override
    public Flux<ByteBuffer> downloadFile(String fileName) {
        return download(fileName);
    }

    @Override
    public Mono<Boolean> delete(String content){
        return deleteFile(content).map(Response::getValue);
    }


}

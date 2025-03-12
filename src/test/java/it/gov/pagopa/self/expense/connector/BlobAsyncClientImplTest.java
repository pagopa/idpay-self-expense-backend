package it.gov.pagopa.self.expense.connector;

import com.azure.core.http.rest.Response;
import it.gov.pagopa.common.azure.storage.AzureBlobAsyncClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.ByteBuffer;

class BlobAsyncClientImplTest {

    private FileStorageAsyncConnector merchantFileStorageConnector;

    @BeforeEach
    void init(){
        merchantFileStorageConnector = Mockito.spy(new BlobAsyncAsyncClientImpl("UseDevelopmentStorage=true;", "test"));
    }

    @Test
    void whenDownloadFileThenDownloadMethodIsInvoked() {
        // Given
        String filename = "FILENAME";
        Flux<ByteBuffer> expectedResult = Flux.just(ByteBuffer.wrap(new byte[0]));
        Mockito.doReturn(expectedResult)
                .when((AzureBlobAsyncClient)merchantFileStorageConnector)
                .download(filename);

        // When
        Flux<ByteBuffer> result = merchantFileStorageConnector.downloadFile(filename);

        // Then
        Assertions.assertSame(expectedResult, result);
    }

    @Test
    void whenUploadFileThenUploadMethodIsInvoked() {
        // Given
        Flux<ByteBuffer> byteBuffer = Flux.just(ByteBuffer.wrap(new byte[0]));
        String destination = "FILENAME";
        String contentType = "text";
        Mockito.doReturn(Mono.just(Mockito.mock(Response.class)))
                .when((AzureBlobAsyncClient)merchantFileStorageConnector)
                .upload(byteBuffer, destination, contentType);

        // When
        merchantFileStorageConnector.uploadFile(byteBuffer, destination, contentType);

        // Then
        Mockito.verify((AzureBlobAsyncClient)merchantFileStorageConnector)
                .upload(byteBuffer, destination, contentType);
    }

    @Test
    void whenDeleteFileThenDeleteMethodIsInvoked() {
        // Given
        String destination = "FILENAME";
        Mockito.doReturn(Mono.just(Mockito.mock(Response.class)))
                .when((AzureBlobAsyncClient)merchantFileStorageConnector)
                .deleteFile(destination);

        // When
        merchantFileStorageConnector.delete(destination);

        // Then
        Mockito.verify((AzureBlobAsyncClient)merchantFileStorageConnector)
                .deleteFile(destination);
    }
}

package it.gov.pagopa.self.expense.connector;

import com.azure.core.http.rest.Response;
import it.gov.pagopa.common.azure.storage.AzureBlobClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

class BlobClientImplTest {

    private FileStorageConnector merchantFileStorageConnector;

    @BeforeEach
    void init(){
        merchantFileStorageConnector = Mockito.spy(new BlobClientImpl("UseDevelopmentStorage=true;", "test"));
    }

    @Test
    void whenDownloadMerchantFileThenDownloadMethodIsInvoked(){
        // Given
        String filename = "FILENAME";
        ByteArrayOutputStream expectedResult = Mockito.mock(ByteArrayOutputStream.class);
        Mockito.doReturn(expectedResult)
                .when((AzureBlobClient)merchantFileStorageConnector)
                        .download(filename);

        // When
        ByteArrayOutputStream result = merchantFileStorageConnector.downloadFile(filename);

        // Then
        Assertions.assertSame(expectedResult, result);
    }

    @Test
    void whenUploadMerchantFileThenUploadMethodIsInvoked(){
        // Given
        InputStream is = Mockito.mock(InputStream.class);
        String destination = "FILENAME";
        String contentType = "text";
        Mockito.doReturn(Mockito.mock(Response.class))
                .when((AzureBlobClient)merchantFileStorageConnector)
                .upload(is, destination, contentType);

        // When
        merchantFileStorageConnector.uploadFile(is, destination, contentType);

        // Then
        Mockito.verify((AzureBlobClient)merchantFileStorageConnector)
                .upload(is, destination, contentType);
    }
}

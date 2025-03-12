package it.gov.pagopa.common.azure.storage;

import com.azure.core.http.HttpResponse;
import com.azure.core.http.rest.PagedFlux;
import com.azure.core.http.rest.Response;
import com.azure.storage.blob.BlobAsyncClient;
import com.azure.storage.blob.BlobContainerAsyncClient;
import com.azure.storage.blob.models.*;
import com.azure.storage.blob.options.BlobDownloadToFileOptions;
import com.azure.storage.blob.options.BlobParallelUploadOptions;
import com.azure.storage.blob.options.BlobUploadFromFileOptions;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.util.ReflectionUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.*;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

class AzureBlobAsyncClientImplTest {

    private AzureBlobAsyncClient blobClient;

    @BeforeEach
    void init() {
        blobClient = buildBlobInstance();
    }

    protected AzureBlobAsyncClient buildBlobInstance() {
        return new AzureBlobAsyncClientImpl("UseDevelopmentStorage=true;", "test");
    }

    @Test
    void testFile() throws IOException {
        // Given
        File testFile = new File("README.md");
        String destination = "baseAzureBlobAsyncClientTest/README.md";
        Path downloadPath = Path.of("target/README.md");
        Files.deleteIfExists(downloadPath.toAbsolutePath());

        BlobContainerAsyncClient mockClient = mockClientFileOps(testFile, destination, downloadPath);

        // When Upload
        Response<BlockBlobItem> uploadResult = blobClient.uploadFile(testFile, destination, "text").block();

        // Then uploadResult
        Assertions.assertNotNull(uploadResult);
        Assertions.assertEquals(201, uploadResult.getStatusCode());

        // When List
        List<BlobItem> listResult = blobClient.listFiles(destination).collectList().block();

        // Then listResult
        Assertions.assertNotNull(listResult);
        Assertions.assertEquals(
                List.of("baseAzureBlobAsyncClientTest/README.md")
                , listResult.stream().map(BlobItem::getName).toList());

        // When download
        File downloadedFile = downloadPath.toFile();
        Assertions.assertFalse(downloadedFile.exists());
        Response<BlobProperties> downloadResult = blobClient.download(destination, downloadPath).block();

        // Then downloadResult
        Assertions.assertNotNull(downloadResult);
        Assertions.assertEquals(206, downloadResult.getStatusCode());
        if (mockClient == null) {
            Assertions.assertTrue(downloadedFile.exists());
            Assertions.assertEquals(testFile.length(), downloadedFile.length());
            Assertions.assertTrue(downloadedFile.delete());
        }

        // When Delete
        Response<Boolean> deleteResult = blobClient.deleteFile(destination).block();

        // Then deleteResult
        Assertions.assertNotNull(deleteResult);
        Assertions.assertTrue(deleteResult.getValue());

        // When List after delete
        if (mockClient != null) {
            mockListFilesOperation(destination, Collections.emptyList(), mockClient);
        }
        List<BlobItem> listAfterDeleteResult = blobClient.listFiles(destination).collectList().block();

        // Then listAfterDeleteResult
        Assertions.assertNotNull(listAfterDeleteResult);
        Assertions.assertEquals(Collections.emptyList(), listAfterDeleteResult);

        // When downloadAfterDeleteResult
        if (mockClient != null) {
            mockDownloadFileOperation(destination, downloadPath, false, mockClient);

            // Then downloadResult
            Assertions.assertNull(blobClient.download(destination, downloadPath));
        }

    }

    @Test
    void testStream() throws IOException {
        // Given
        File testFile = new File("README.md");
        String destination = "baseAzureBlobAsyncClientTest/README.md";

        try(InputStream inputStream = new BufferedInputStream(new FileInputStream(testFile))) {

            Flux<ByteBuffer> byteBufferFlux = DataBufferUtils.readInputStream(() -> inputStream, new DefaultDataBufferFactory(), 4096)
                    .map(DataBuffer::toByteBuffer);

            BlobContainerAsyncClient mockClient = mockClientStreamOps(byteBufferFlux, destination);
              // When Upload
            Response<BlockBlobItem> uploadResult = blobClient.upload(byteBufferFlux, destination, "text").block();

            // Then uploadResult
            Assertions.assertNotNull(uploadResult);
            Assertions.assertEquals(201, uploadResult.getStatusCode());

            // When download
            Flux<ByteBuffer> downloadedOutputStream = blobClient.download(destination);
            // Then downloadedOutputStream
            Assertions.assertNotNull(downloadedOutputStream);
            if (mockClient == null) {
                Assertions.assertEquals(testFile.length(), Objects.requireNonNull(downloadedOutputStream.collectList().block()).size());
            }


            // When Delete
            Response<Boolean> deleteResult = blobClient.deleteFile(destination).block();

            // Then deleteResult
            Assertions.assertNotNull(deleteResult);
            Assertions.assertTrue(deleteResult.getValue());

            // When downloadAfterDeleteResult
            if (mockClient != null) {
                mockDownloadStreamOperation(destination, false, mockClient);
                // Then downloadedOutputStream
                Assertions.assertNull(blobClient.download(destination));
            }
        }
    }

    protected BlobContainerAsyncClient mockClientFileOps(File file, String destination, Path downloadPath) {
        try {
            Field clientField = ReflectionUtils.findField(AzureBlobAsyncClientImpl.class, "blobContainerAsyncClient");
            Assertions.assertNotNull(clientField);
            clientField.setAccessible(true);

            BlobContainerAsyncClient clientMock = Mockito.mock(BlobContainerAsyncClient.class, Mockito.RETURNS_DEEP_STUBS);

            mockUploadFileOperation(file, destination, clientMock);

            BlobItem mockBlobItem = new BlobItem();
            mockBlobItem.setName(destination);
            mockListFilesOperation(destination, List.of(mockBlobItem), clientMock);

            mockDeleteOperation(destination, clientMock);

            mockDownloadFileOperation(destination, downloadPath, true, clientMock);

            clientField.set(blobClient, clientMock);

            return clientMock;
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    protected BlobContainerAsyncClient mockClientStreamOps(Flux<ByteBuffer> byteBufferFlux, String destination) {
        try {
            Field clientField = ReflectionUtils.findField(AzureBlobAsyncClientImpl.class, "blobContainerAsyncClient");
            Assertions.assertNotNull(clientField);
            clientField.setAccessible(true);

            BlobContainerAsyncClient clientMock = Mockito.mock(BlobContainerAsyncClient.class, Mockito.RETURNS_DEEP_STUBS);

            mockUploadStreamOperation(byteBufferFlux, destination, clientMock);

            BlobItem mockBlobItem = new BlobItem();
            mockBlobItem.setName(destination);
            mockListFilesOperation(destination, List.of(mockBlobItem), clientMock);

            mockDeleteOperation(destination, clientMock);

            mockDownloadStreamOperation(destination, true, clientMock);

            clientField.set(blobClient, clientMock);

            return clientMock;
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private static void mockUploadFileOperation(File file, String destination, BlobContainerAsyncClient clientMock) {
        @SuppressWarnings("rawtypes") Response responseMock = Mockito.mock(Response.class);
        Mockito.when(responseMock.getStatusCode()).thenReturn(201);

        //noinspection unchecked
        Mockito.when(clientMock.getBlobAsyncClient(destination).uploadFromFileWithResponse(Mockito.any(BlobUploadFromFileOptions.class)))
                .thenReturn(Mono.just(responseMock));
    }
    private static void mockUploadStreamOperation(Flux<ByteBuffer> data, String destination, BlobContainerAsyncClient clientMock) {
        @SuppressWarnings("rawtypes") Response responseMock = Mockito.mock(Response.class);
        Mockito.when(responseMock.getStatusCode()).thenReturn(201);

        //noinspection unchecked
        Mockito.when(clientMock.getBlobAsyncClient(destination).uploadWithResponse(Mockito.any(BlobParallelUploadOptions.class)))
                .thenReturn(Mono.just(responseMock));
    }

    private static void mockListFilesOperation(String destination, List<BlobItem> mockedResult, BlobContainerAsyncClient clientMock) {
        @SuppressWarnings("rawtypes") PagedFlux responseMock = Mockito.mock(PagedFlux.class, Mockito.RETURNS_DEEP_STUBS);
        Mockito.when(responseMock.collectList()).thenReturn(Mono.just(mockedResult));

        //noinspection unchecked
        Mockito.when(clientMock.listBlobsByHierarchy(destination)).thenReturn(responseMock);
    }

    private static void mockDeleteOperation(String destination, BlobContainerAsyncClient clientMock) {
        @SuppressWarnings("rawtypes") Response responseMock = Mockito.mock(Response.class);
        Mockito.when(responseMock.getValue()).thenReturn(true);

        //noinspection unchecked
        Mockito.when(clientMock.getBlobAsyncClient(destination).deleteIfExistsWithResponse(Mockito.eq(DeleteSnapshotsOptionType.INCLUDE), Mockito.isNull()))
                .thenReturn(Mono.just(responseMock));
    }

    private void mockDownloadFileOperation(String destination, Path downloadPath, boolean fileExists, BlobContainerAsyncClient clientMock) {

        if (fileExists) {
            @SuppressWarnings("rawtypes") Response responseMock = Mockito.mock(Response.class);
            Mockito.when(responseMock.getStatusCode()).thenReturn(206);
            //noinspection unchecked
            Mockito.when(clientMock.getBlobAsyncClient(destination)
                    .downloadToFileWithResponse(Mockito.any(BlobDownloadToFileOptions.class)
                            )).thenReturn(Mono.just(responseMock));

        } else {
            HttpResponse responseMock = Mockito.mock(HttpResponse.class);
            Mockito.when(responseMock.getStatusCode()).thenReturn(404);

            Mockito.doThrow(new BlobStorageException("NOT FOUND", responseMock, null))
                    .when(clientMock).getBlobAsyncClient(destination);
        }
    }

    private void mockDownloadStreamOperation(String destination, boolean fileExists, BlobContainerAsyncClient clientMock) {
        BlobAsyncClient blobClientMock = clientMock.getBlobAsyncClient(destination);

        if (!fileExists) {
            HttpResponse responseMock = Mockito.mock(HttpResponse.class);
            Mockito.when(responseMock.getStatusCode()).thenReturn(404);

            Mockito.doThrow(new BlobStorageException("NOT FOUND", responseMock, null))
                    .when(blobClientMock)
                    .downloadStream();
        }
    }
}

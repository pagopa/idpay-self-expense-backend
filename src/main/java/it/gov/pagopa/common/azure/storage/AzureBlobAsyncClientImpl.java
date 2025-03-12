package it.gov.pagopa.common.azure.storage;

import com.azure.core.http.rest.PagedFlux;
import com.azure.core.http.rest.Response;
import com.azure.storage.blob.BlobContainerAsyncClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.*;
import com.azure.storage.blob.options.BlobDownloadToFileOptions;
import com.azure.storage.blob.options.BlobParallelUploadOptions;
import com.azure.storage.blob.options.BlobUploadFromFileOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Set;

@Slf4j
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class AzureBlobAsyncClientImpl implements AzureBlobAsyncClient {

    private final BlobContainerAsyncClient blobContainerAsyncClient;

    public AzureBlobAsyncClientImpl(String storageConnectionString, String blobContainerName) {
        this.blobContainerAsyncClient = new BlobServiceClientBuilder()
                .connectionString(storageConnectionString)
                .buildAsyncClient()
                .getBlobContainerAsyncClient(blobContainerName);
    }


    @Override
    public Mono<Response<BlockBlobItem>> uploadFile(File file, String destination, String contentType) {
        log.info("Uploading file {} (contentType={}) into azure blob at destination {}", file.getName(), contentType, destination);

        return blobContainerAsyncClient.getBlobAsyncClient(destination)
                .uploadFromFileWithResponse(new BlobUploadFromFileOptions(file.getPath()));
    }

    @Override
    public Mono<Response<BlockBlobItem>>  upload(Flux<ByteBuffer> data, String destination, String contentType) {
        log.info("Uploading (contentType={}) into azure blob at destination {}", contentType, destination);

        return blobContainerAsyncClient.getBlobAsyncClient(destination)
                .uploadWithResponse(new BlobParallelUploadOptions(data));
    }

    @Override
    public Mono<Response<Boolean>> deleteFile(String destination) {
        log.info("Deleting file {} from azure blob container", destination);

        return blobContainerAsyncClient.getBlobAsyncClient(destination)
                .deleteIfExistsWithResponse(DeleteSnapshotsOptionType.INCLUDE, null);
    }

    @Override
    public PagedFlux<BlobItem> listFiles(String path) {
        return blobContainerAsyncClient.listBlobsByHierarchy(path);
    }


    @Override
    public Mono<Response<BlobProperties>> download(String filePath, Path destination) {
        log.info("Downloading file {} from azure blob container", filePath);

        createDirectoryIfNotExists(destination);

        try {
            return blobContainerAsyncClient.getBlobAsyncClient(filePath)
                    .downloadToFileWithResponse(new BlobDownloadToFileOptions(destination.toString())
                                    // override options
                                    .setOpenOptions(Set.of(
                                            StandardOpenOption.CREATE,
                                            StandardOpenOption.TRUNCATE_EXISTING,
                                            StandardOpenOption.READ,
                                            StandardOpenOption.WRITE))
                    );
        } catch (BlobStorageException e) {
            if(e.getStatusCode()!=404){
                throw e;
            } else {
                return null;
            }
        }
    }


    @Override
    public Flux<ByteBuffer> download(String filePath) {
        log.info("Downloading file {} from azure blob container", filePath);

        try {
            return blobContainerAsyncClient.getBlobAsyncClient(filePath)
                    .downloadStream();
        } catch (BlobStorageException e) {
            if(e.getStatusCode()!=404){
                throw e;
            } else {
                return null;
            }
        }
    }

    private static void createDirectoryIfNotExists(Path localFile) {
        Path directory = localFile.getParent();
        if (!Files.exists(directory)) {
            try {
                Files.createDirectories(directory);
            } catch (IOException e) {
                throw new IllegalStateException("Cannot create directory to store downloaded zip %s".formatted(localFile), e);
            }
        }
    }
}


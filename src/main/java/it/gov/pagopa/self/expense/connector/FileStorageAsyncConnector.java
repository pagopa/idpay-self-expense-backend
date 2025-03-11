package it.gov.pagopa.self.expense.connector;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.ByteBuffer;

public interface FileStorageAsyncConnector {

    Mono<Boolean> uploadFile(Flux<ByteBuffer> data, String fileName, String contentType);
    Flux<ByteBuffer> downloadFile(String fileName);
    Mono<Boolean> delete(String content);

}

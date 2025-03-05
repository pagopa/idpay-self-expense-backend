package it.gov.pagopa.self.expense.utils;

import org.jetbrains.annotations.NotNull;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MockFilePart implements FilePart {
    private final String name;
    private final String filename;
    private final MediaType contentType;
    private final byte[] content;

    public MockFilePart(String name, String filename, MediaType contentType, byte[] content) {
        this.name = name;
        this.filename = filename;
        this.contentType = contentType;
        this.content = content;
    }

    @NotNull
    @Override
    public String name() {
        return name;
    }

    @NotNull
    @Override
    public String filename() {
        return filename;
    }

    @NotNull
    @Override
    public Mono<Void> transferTo(@NotNull java.nio.file.Path dest) {
        return Mono.empty();
    }

    @NotNull
    @Override
    public HttpHeaders headers() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(contentType);
        return headers;
    }

    @NotNull
    @Override
    public Flux<DataBuffer> content() {
        DataBuffer dataBuffer = new DefaultDataBufferFactory().wrap(content);
        return Flux.just(dataBuffer);
    }

    public static List<FilePart> generateMockFileParts() {
        byte[] bytes = new byte[10];
        bytes[0] = 0x00;
        bytes[1] = 0x01;
        bytes[2] = 0x02;

        MockFilePart mockFilePart = new MockFilePart("file", "filename.pdf", MediaType.APPLICATION_PDF, bytes);
        return Stream.of(mockFilePart).collect(Collectors.toList());
    }

    public static List<FilePart> generateMockWrongTypeFileParts() {
        byte[] bytes = new byte[10];
        bytes[0] = 0x00;
        bytes[1] = 0x01;
        bytes[2] = 0x02;

        MockFilePart mockFilePart = new MockFilePart("file", "filename.txt", MediaType.TEXT_PLAIN, bytes);
        return Stream.of(mockFilePart).collect(Collectors.toList());
    }

    public static List<FilePart> generateMockEmptyFileParts() {
        byte[] bytes = new byte[0];
        MockFilePart mockFilePart = new MockFilePart("file", "filename.txt", MediaType.APPLICATION_PDF, bytes);
        return Stream.of(mockFilePart).collect(Collectors.toList());
    }
}
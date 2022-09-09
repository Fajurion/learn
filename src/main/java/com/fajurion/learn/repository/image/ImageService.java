package com.fajurion.learn.repository.image;

import com.fajurion.learn.util.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.ByteArrayOutputStream;
import java.util.Objects;

@Service
public class ImageService {

    /**
     * Turns a file part into a byte array
     * Original source: https://stackoverflow.com/questions/60397619/how-can-i-convert-filepart-to-byte-in-spring-5-mvc
     *
     * @param file FilePart object
     * @return the byte array
     */
    public Mono<byte[]> filePartToByteArray(Mono<FilePart> file) {
        return file.flatMap(fp -> fp.content().flatMap(dataBuffer -> Flux.just(dataBuffer.asByteBuffer().array())).collectList())
                .map(list -> {
                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    list.forEach(bytes -> {
                        try {
                            stream.write(bytes);
                        } catch (Exception ignored) {}
                    });
                    return stream.toByteArray();
                })
                .flatMap(array ->  {
                    if(array.length > Configuration.settings.get("max.file.size")) {
                        return Mono.error(new RuntimeException("file.too_large"));
                    }

                    return Mono.just(array);
                });
    }

    public Mono<String> checkFileType(Mono<FilePart> file) {
        return file.map(fp -> Objects.requireNonNull(fp.headers().getContentType()).getType());
    }

}

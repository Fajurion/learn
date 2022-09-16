package com.fajurion.learn.controller.image;

import com.fajurion.learn.repository.account.AccountRepository;
import com.fajurion.learn.repository.account.ranks.RankRepository;
import com.fajurion.learn.repository.account.session.SessionService;
import com.fajurion.learn.repository.image.Image;
import com.fajurion.learn.repository.image.ImageRepository;
import com.fajurion.learn.repository.image.ImageService;
import com.fajurion.learn.util.Configuration;
import com.fajurion.learn.util.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/image")
public class ImageController {

    // Repository for getting rank permission level
    private final RankRepository rankRepository;

    // Repository for getting account data
    private final AccountRepository accountRepository;

    // Service for checking sessions
    private final SessionService sessionService;

    // Repository for uploading/downloading images
    private final ImageRepository imageRepository;

    // Service for image converting
    private final ImageService service;

    @Autowired
    public ImageController(RankRepository rankRepository, AccountRepository accountRepository, SessionService sessionService, ImageRepository imageRepository, ImageService service) {
        this.rankRepository = rankRepository;
        this.accountRepository = accountRepository;
        this.sessionService = sessionService;
        this.imageRepository = imageRepository;
        this.service = service;
    }

    /**
     * Upload an image to the database
     *
     * @param file The image
     * @param token The token of the user
     * @param contentLength The content length of the file
     * @return The image upload response
     */
    @PostMapping(value = "/upload", consumes = {"multipart/form-data"})
    @CrossOrigin
    public Mono<ImageUploadResponse> uploadImage(@RequestPart("file") Mono<FilePart> file,
                                    @RequestPart("token") String token,
                                    @RequestHeader("Content-Length") int contentLength) {

        // Check if request is valid
        if(token == null) {
            return Mono.just(new ImageUploadResponse(false, false, "invalid"));
        }

        // Check if file is too large
        if(contentLength > Configuration.settings.get("max.file.size")) {
            return Mono.just(new ImageUploadResponse(false, false, "file.too_large"));
        }

        return sessionService.checkAndRefreshSession(token).flatMap(session -> {

            if(session == null) {
                return Mono.error(new CustomException("session.expired"));
            }

            // Check for correct file type
            return Mono.zip(service.checkFileType(file), Mono.just(session.getAccount()));
        }).flatMap(tuple2 -> {

            if(!(tuple2.getT1().equals(MediaType.IMAGE_PNG.getType()) || tuple2.getT1().equals(MediaType.IMAGE_JPEG.getType()))) {
                return Mono.error(new CustomException("upload.failed"));
            }

            // Transform file part into byte array                 account id                    file type
            return Mono.zip(service.filePartToByteArray(file), Mono.just(tuple2.getT2()), Mono.just(tuple2.getT1()));
        }).flatMap(tuple2 -> imageRepository.save(new Image(tuple2.getT3(), tuple2.getT2(), tuple2.getT1())))
                .flatMap(image -> {

                    if(image == null) {
                        return Mono.error(new CustomException("upload.failed"));
                    }

                    return Mono.just(new ImageUploadResponse(true, false, image.getId() + ""));
                })

                // Error handling
                .onErrorResume(CustomException.class, e -> Mono.just(new ImageUploadResponse(false, false, e.getMessage())))
                .onErrorResume(e -> Mono.just(new ImageUploadResponse(false, true, "server.error")));
    }

    // Response to uploading an image
    public record ImageUploadResponse(boolean success, boolean error, String data) {}

    @GetMapping(value = "/download/{id}/{token}", produces = {MediaType.IMAGE_PNG_VALUE, MediaType.IMAGE_PNG_VALUE})
    @CrossOrigin
    public Mono<Resource> download(@PathVariable int id, @PathVariable String token) {

        // Check if session is valid
        return sessionService.checkAndRefreshSession(token).flatMap(session -> {

            if(session == null) {
                return Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND));
            }

            // Get the image
            return imageRepository.findById(id);
        }).flatMap(image -> {

            // Check if image exists
            if(image == null) {
                return Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND));
            }

            // Return image
            return Mono.just(new ByteArrayResource(image.getImage()));
        });
    }

}

package com.fajurion.learn.frontend.controller.image;

import com.fajurion.learn.repository.account.AccountRepository;
import com.fajurion.learn.repository.account.ranks.RankRepository;
import com.fajurion.learn.repository.account.session.SessionRepository;
import com.fajurion.learn.repository.image.Image;
import com.fajurion.learn.repository.image.ImageRepository;
import com.fajurion.learn.repository.image.ImageService;
import com.fajurion.learn.util.ConstantConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicReference;

@RestController
@RequestMapping("/api/image")
public class ImageController {

    @Autowired
    private RankRepository rankRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private SessionRepository sessionRepository;

    @Autowired
    private ImageRepository imageRepository;

    @Autowired
    private ImageService service;

    /**
     * Upload an image to the database
     *
     * @param file The image
     * @param token The token of the user
     * @param contentLength The content length of the file
     * @return The image upload response
     */
    @PostMapping(value = "/upload", consumes = {"multipart/form-data"})
    @ResponseBody
    public Mono<ImageUploadResponse> uploadImage(@RequestPart("file") Mono<FilePart> file,
                                    @RequestHeader("token") String token,
                                    @RequestHeader("Content-Length") int contentLength) {

        // Check if file is too large
        if(contentLength > ConstantConfiguration.MAX_FILE_SIZE) {
            return Mono.error(new RuntimeException("file.too_large"));
        }

        AtomicReference<Integer> userID = new AtomicReference<>();

        return sessionRepository.findById(token).flatMap(session -> {

            if(session == null) {
                return Mono.error(new RuntimeException("session.expired"));
            }

            // Get account from session
            return accountRepository.findById(session.getId());
        }).flatMap(account -> {

            if(account == null) {
                return Mono.error(new RuntimeException("session.expired.deleted"));
            }

            // Set id in atomic reference
            userID.set(account.getId());

            // Get the rank of the user
            return rankRepository.getRankByName(account.getRank());
        }).flatMap(rank ->  {

            // Check if the account has the required permission level
            if(rank.getLevel() < ConstantConfiguration.PERMISSION_LEVEL_UPLOAD_IMAGE) {
                return Mono.error(new RuntimeException("no_permission"));
            }

            // Check for correct file type
            return service.checkFileType(file);
        }).flatMap(check -> {
            
            if(!check) {
                return Mono.error(new RuntimeException("upload.failed"));
            }

            // Transform file part into byte array
            return service.filePartToByteArray(file);
        }).flatMap(array -> imageRepository.save(new Image(userID.get(), array)))
                .flatMap(image -> {

                    if(image == null) {
                        return Mono.error(new RuntimeException("upload.failed"));
                    }

                    return Mono.just(new ImageUploadResponse(true, false, image.getId() + ""));
                }).onErrorResume(RuntimeException.class, e -> Mono.just(new ImageUploadResponse(false, false, e.getMessage())))
                .onErrorResume(e -> Mono.just(new ImageUploadResponse(false, true, "server.error")));
    }

    // Response to uploading an image
    public record ImageUploadResponse(boolean success, boolean error, String data) {}

    @GetMapping(value = "/download/{id}", produces = MediaType.IMAGE_JPEG_VALUE)
    public Mono<Resource> download(@PathVariable int id, @RequestHeader("token") String token) {

        // Check if session is valid
        return sessionRepository.findById(token).flatMap(session -> {

            if(session == null) {
                return Mono.error(new RuntimeException("session.expired"));
            }

            // Get account from session
            return accountRepository.findById(session.getId());
        }).flatMap(account -> {

            if(account == null) {
                return Mono.error(new RuntimeException("session.expired.deleted"));
            }

            // Get the rank of the user
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

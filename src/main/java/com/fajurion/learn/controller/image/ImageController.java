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

import java.util.concurrent.atomic.AtomicReference;

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
    @ResponseBody @CrossOrigin
    public Mono<ImageUploadResponse> uploadImage(@RequestPart("file") Mono<FilePart> file,
                                    @RequestHeader("token") String token,
                                    @RequestHeader("Content-Length") int contentLength) {

        // Check if file is too large
        if(contentLength > Configuration.settings.get("max.file.size")) {
            return Mono.just(new ImageUploadResponse(false, false, "file.too_large"));
        }

        AtomicReference<Integer> userID = new AtomicReference<>();

        return sessionService.checkAndRefreshSession(token).flatMap(session -> {

            if(session == null) {
                return Mono.error(new CustomException("session.expired"));
            }

            // Get account from session
            return accountRepository.findById(session.getAccount());
        }).flatMap(account -> {

            if(account == null) {
                return Mono.error(new CustomException("session.expired.deleted"));
            }

            // Set id in atomic reference
            userID.set(account.getId());

            // Get the rank of the user
            return rankRepository.getRankByName(account.getRank());
        }).flatMap(rank ->  {

            // Check if the account has the required permission level
            if(rank.getLevel() < Configuration.permissions.get("upload.image")) {
                return Mono.error(new CustomException("no_permission"));
            }

            // Check for correct file type
            return service.checkFileType(file);
        }).flatMap(check -> {
            
            if(!check) {
                return Mono.error(new CustomException("upload.failed"));
            }

            // Transform file part into byte array
            return service.filePartToByteArray(file);
        }).flatMap(array -> imageRepository.save(new Image(userID.get(), array)))
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

    @GetMapping(value = "/download/{id}", produces = MediaType.IMAGE_JPEG_VALUE) @CrossOrigin
    public Mono<Resource> download(@PathVariable int id, @RequestHeader("token") String token) {

        // Check if session is valid
        return sessionService.checkAndRefreshSession(token).flatMap(session -> {

            if(session == null) {
                return Mono.error(new CustomException("session.expired"));
            }

            // Get account from session
            return accountRepository.findById(session.getAccount());
        }).flatMap(account -> {

            if(account == null) {
                return Mono.error(new CustomException("session.expired.deleted"));
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

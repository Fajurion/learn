package com.fajurion.learn.controller.report;

import com.fajurion.learn.repository.account.Account;
import com.fajurion.learn.repository.account.AccountRepository;
import com.fajurion.learn.repository.account.ranks.RankRepository;
import com.fajurion.learn.repository.account.session.SessionService;
import com.fajurion.learn.repository.report.Report;
import com.fajurion.learn.repository.report.ReportRepository;
import com.fajurion.learn.util.Configuration;
import com.fajurion.learn.util.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/api/report")
public class ReportController {

    // Service for checking sessions
    private final SessionService sessionService;

    // Repository for getting accounts
    private final AccountRepository accountRepository;

    // Repository for getting reports
    private final ReportRepository reportRepository;

    // Repository for getting ranks
    private final RankRepository rankRepository;

    @Autowired
    public ReportController(SessionService sessionService,
                            AccountRepository accountRepository,
                            ReportRepository reportRepository,
                            RankRepository rankRepository) {
        this.sessionService = sessionService;
        this.accountRepository = accountRepository;
        this.reportRepository = reportRepository;
        this.rankRepository = rankRepository;
    }

    @PostMapping("/list")
    @CrossOrigin
    public Mono<ReportListResponse> list(@RequestBody ReportListForm form) {

        // Check if form is valid
        if(form.token() == null || form.offset < 0) {
            return Mono.just(new ReportListResponse(true, false, "invalid", null));
        }

        // Check if session token is valid
        return sessionService.checkAndRefreshSession(form.token()).flatMap(session -> {

            if(session == null) {
                return Mono.error(new CustomException("session.expired"));
            }

            // Get account
            return accountRepository.findById(session.getAccount()).onErrorReturn(new Account("", "", "", "", "", -1));
        }).flatMap(account -> {

            // Get rank
            return rankRepository.getRankByName(account.getRank());
        }).flatMap(rank -> {

            // Check permission
            if(rank.getLevel() < Configuration.permissions.get("view.admin.panel")) {
                return Mono.error(new CustomException("no_permission"));
            }

            // Return list of reports
            return reportRepository.sortReportsByDate(20, form.offset()).collectList();
        }).map(list -> new ReportListResponse(true, false, "success", list))

                // Error handling
                .onErrorResume(CustomException.class, e -> Mono.just(new ReportListResponse(false, false, e.getMessage(), null)))
                .onErrorReturn(new ReportListResponse(false, true, "server.error", null));
    }

    // Form to request list of reports
    public record ReportListForm(String token, int offset) {}

    // Response to requesting a list of reports
    public record ReportListResponse(boolean success, boolean error, String message, List<Report> reports) {}

    @PostMapping("/create")
    @CrossOrigin
    public Mono<DefaultResponse> create(@RequestBody ReportCreateForm form) {

        // Check if form is valid
        if(form.token() == null || form.description() == null || form.url() == null || form.url().length() < 3
        || form.url.length() > 200 || form.description().length() > 200) {
            return Mono.just(new DefaultResponse(true, false, "invalid"));
        }

        // Check if session token is valid
        return sessionService.checkAndRefreshSession(form.token()).flatMap(session -> {

            if(session == null) {
                return Mono.error(new CustomException("session.expired"));
            }

            // Get account
            return accountRepository.findById(session.getAccount()).onErrorReturn(new Account("", "", "", "", "", -1));
        }).flatMap(account -> {

            // Get rank (and zip with account)
            return Mono.zip(rankRepository.getRankByName(account.getRank()), Mono.just(account));
        }).flatMap(tuple2 -> {

            // Check permission
            if(tuple2.getT1().getLevel() < Configuration.permissions.get("view.admin.panel")) {
                return Mono.error(new CustomException("no_permission"));
            }

            // Create report
            return reportRepository.save(new Report(tuple2.getT2().getId(), System.currentTimeMillis(), form.url(), form.description()));
        }).map(report -> new DefaultResponse(true, false, "success"))

                // Error handling
                .onErrorResume(CustomException.class, e -> Mono.just(new DefaultResponse(false, false, e.getMessage())))
                .onErrorReturn(new DefaultResponse(false, true, "server.error"));
    }

    // Form to create a report
    public record ReportCreateForm(String token, String url, String description) {}

    @PostMapping("/delete")
    @CrossOrigin
    public Mono<DefaultResponse> delete(@RequestBody ReportDeleteForm form) {

        // Check if form is valid
        if(form.token() == null || form.report() < 0) {
            return Mono.just(new DefaultResponse(true, false, "invalid"));
        }

        // Check if session token is valid
        return sessionService.checkAndRefreshSession(form.token()).flatMap(session -> {

            if(session == null) {
                return Mono.error(new CustomException("session.expired"));
            }

            // Get account
            return accountRepository.findById(session.getAccount()).onErrorReturn(new Account("", "", "", "", "", -1));
        }).flatMap(account -> {

            // Get rank (and zip with account)
            return Mono.zip(rankRepository.getRankByName(account.getRank()), Mono.just(account));
        }).flatMap(tuple2 -> {

            // Check permission
            if(tuple2.getT1().getLevel() < Configuration.permissions.get("view.admin.panel")) {
                return Mono.error(new CustomException("no_permission"));
            }

            // Get report
            return reportRepository.findById(form.report()).onErrorReturn(new Report(-1, -1, "", ""));
        }).flatMap(report -> {

            // Check if report exists
            if(report.getDate() == -1) {
                return Mono.error(new CustomException("not_found"));
            }

            // Delete report
            return reportRepository.delete(report).thenReturn(report);
        }).flatMap(s ->{
                    return Mono.just(new DefaultResponse(true, false, "success"));
        })

                // Error handling
                .onErrorResume(CustomException.class, e -> Mono.just(new DefaultResponse(false, false, e.getMessage())))
                .onErrorReturn(new DefaultResponse(false, true, "server.error"));
    }

    // Form to delete a report
    public record ReportDeleteForm(String token, int report) {}

    // Response to deleting/creating a report
    public record DefaultResponse(boolean success, boolean error, String message) {}

}

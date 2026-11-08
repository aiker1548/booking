
package com.hotelbooking.booking.service;

import com.hotelbooking.booking.dto.BookingRequest;
import com.hotelbooking.booking.dto.ConfirmAvailabilityRequest;
import com.hotelbooking.booking.entity.Booking;
import com.hotelbooking.booking.entity.BookingStatus;
import com.hotelbooking.booking.repository.BookingRepository;
import com.hotelbooking.booking.repository.SagaStateRepository;
import com.hotelbooking.booking.saga.SagaState;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import io.github.resilience4j.core.functions.CheckedRunnable;
import io.github.resilience4j.core.functions.CheckedSupplier;

@Service
public class SagaOrchestratorService {

    private static final Logger log =
            LoggerFactory.getLogger(SagaOrchestratorService.class);

    private static final String CIRCUIT_BREAKER_NAME =
            "hotelServiceCircuitBreaker";

    private final BookingRepository bookingRepository;
    private final SagaStateRepository sagaStateRepository;
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final RestTemplate restTemplate;
    private final TransactionTemplate transactionTemplate;

    @Value("${hotel.service.url}")
    private String hotelServiceUrl;

    public SagaOrchestratorService(
            BookingRepository bookingRepository,
            SagaStateRepository sagaStateRepository,
            CircuitBreakerRegistry circuitBreakerRegistry,
            RestTemplate restTemplate,
            PlatformTransactionManager transactionManager) {

        this.bookingRepository = bookingRepository;
        this.sagaStateRepository = sagaStateRepository;
        this.circuitBreakerRegistry = circuitBreakerRegistry;
        this.restTemplate = restTemplate;
        this.transactionTemplate =
                new TransactionTemplate(transactionManager);
    }

    /**
     * Booking creation Saga:
     * PENDING -> room lock -> Hotel confirmation -> CONFIRMED.
     */
    public Booking startBookingSaga(BookingRequest request) {

        validateRequest(request);

        log.info(
                "Starting booking saga: user={}, room={}, dates={}-{}",
                request.getUserId(),
                request.getRoomId(),
                request.getStartDate(),
                request.getEndDate()
        );

        // Transaction 1: persist PENDING booking and Saga state.
        Booking booking = transactionTemplate.execute(status -> {

            Booking newBooking = new Booking(
                    UUID.randomUUID(),
                    request.getUserId(),
                    request.getHotelId(),
                    request.getRoomId(),
                    request.getStartDate(),
                    request.getEndDate(),
                    BookingStatus.PENDING,
                    UUID.randomUUID()
            );

            Booking savedBooking = bookingRepository.save(newBooking);

            sagaStateRepository.save(new SagaState(
                    savedBooking.getRequestId(),
                    savedBooking.getBookingId(),
                    savedBooking.getRoomId(),
                    SagaState.STATUS_PENDING
            ));

            return savedBooking;
        });

        if (booking == null) {
            throw new IllegalStateException(
                    "Failed to persist pending booking"
            );
        }

        CircuitBreaker circuitBreaker =
                circuitBreakerRegistry.circuitBreaker(CIRCUIT_BREAKER_NAME);

        boolean lockRequestAttempted = false;
        boolean confirmationRequestAttempted = false;

        try {

            // Step 1: acquire temporary room lock.
            ConfirmAvailabilityRequest confirmRequest =
                    buildConfirmAvailabilityRequest(booking);

            lockRequestAttempted = true;

            ResponseEntity<Map> lockResponse =
                    executeCheckedSupplier(circuitBreaker, () ->
                            restTemplate.postForEntity(
                                    buildConfirmAvailabilityUrl(booking),
                                    confirmRequest,
                                    Map.class
                            )
                    );

            ensureSuccessfulResponse(
                    lockResponse,
                    "Hotel Service failed to acquire room lock"
            );

            // Step 2: confirm booking in Hotel Service.
            confirmationRequestAttempted = true;

            ResponseEntity<Map> confirmResponse =
                    executeCheckedSupplier(circuitBreaker, () ->
                            restTemplate.postForEntity(
                                    buildConfirmBookingUrl(booking),
                                    null,
                                    Map.class
                            )
                    );

            ensureSuccessfulResponse(
                    confirmResponse,
                    "Hotel Service failed to confirm booking"
            );

            // Transaction 2: mark booking and Saga as completed.
            Booking confirmedBooking = transactionTemplate.execute(status -> {

                Booking current = bookingRepository.findById(
                        booking.getBookingId()
                ).orElseThrow(() ->
                        new IllegalStateException(
                                "Booking disappeared during Saga execution"
                        )
                );

                current.setStatus(BookingStatus.CONFIRMED);
                Booking saved = bookingRepository.save(current);

                sagaStateRepository.findByRequestId(
                        booking.getRequestId()
                ).ifPresent(sagaState -> {
                    sagaState.setStatus(SagaState.STATUS_COMPLETED);
                    sagaState.setUpdatedAt(LocalDateTime.now());
                    sagaStateRepository.save(sagaState);
                });

                return saved;
            });

            if (confirmedBooking == null) {
                throw new IllegalStateException(
                        "Failed to persist confirmed booking"
                );
            }

            log.info(
                    "Booking saga completed: {}",
                    booking.getBookingId()
            );

            return confirmedBooking;

        } catch (RuntimeException sagaException) {

            log.error(
                    "Booking saga failed: {}",
                    booking.getBookingId(),
                    sagaException
            );

            try {
                compensate(
                        booking,
                        sagaException,
                        lockRequestAttempted,
                        confirmationRequestAttempted
                );
            } catch (RuntimeException persistenceException) {
                sagaException.addSuppressed(persistenceException);

                log.error(
                        "Failed to persist compensation state for booking {}",
                        booking.getBookingId(),
                        persistenceException
                );
            }

            throw sagaException;
        }
    }

    /**
     * User cancellation of an already confirmed booking.
     */
    public void cancelBookingSaga(UUID bookingId, UUID userId) {

        Booking booking = bookingRepository.findByBookingIdAndUserId(
                bookingId,
                userId
        ).orElseThrow(() ->
                new IllegalStateException(
                        "Booking not found or access denied"
                )
        );

        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.CONFLICT,
                    "Only confirmed bookings can be cancelled"
            );
        }

        CircuitBreaker circuitBreaker =
                circuitBreakerRegistry.circuitBreaker(CIRCUIT_BREAKER_NAME);

        // Cancel the confirmed booking in Hotel Service.
        // Do not use /release here: that endpoint releases temporary locks.
        ResponseEntity<Map> response =
                executeCheckedSupplier(circuitBreaker, () ->
                        restTemplate.postForEntity(
                                buildCancelBookingUrl(booking),
                                null,
                                Map.class
                        )
                );

        ensureSuccessfulResponse(
                response,
                "Hotel Service failed to cancel booking"
        );

        // Transaction: update local state only after remote success.
        transactionTemplate.executeWithoutResult(status -> {

            Booking current = bookingRepository.findById(bookingId)
                    .orElseThrow(() ->
                            new IllegalStateException(
                                    "Booking not found during cancellation"
                            )
                    );

            if (current.getStatus() == BookingStatus.CONFIRMED) {
                current.setStatus(BookingStatus.CANCELLED);
                bookingRepository.save(current);
            } else if (current.getStatus() != BookingStatus.CANCELLED) {
                throw new IllegalStateException(
                        "Booking status changed during cancellation"
                );
            }

            sagaStateRepository.findByRequestId(
                    booking.getRequestId()
            ).ifPresent(sagaState -> {
                sagaState.setStatus(SagaState.STATUS_CANCELLED);
                sagaState.setUpdatedAt(LocalDateTime.now());
                sagaStateRepository.save(sagaState);
            });
        });

        log.info("Booking {} cancelled successfully", bookingId);
    }

    // -----------------------------------------------------------------
    // Circuit Breaker
    // -----------------------------------------------------------------

    private <T> T executeCheckedSupplier(
            CircuitBreaker circuitBreaker,
            CheckedSupplier<T> supplier) {

        try {
            return circuitBreaker.executeCheckedSupplier(supplier);
        } catch (org.springframework.web.client.HttpStatusCodeException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    private void executeCheckedRunnable(
            CircuitBreaker circuitBreaker,
            CheckedRunnable action) {
        try {
            circuitBreaker.executeCheckedRunnable(action);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    // -----------------------------------------------------------------
    // Validation
    // -----------------------------------------------------------------

    private void validateRequest(BookingRequest request) {

        if (request == null) {
            throw new IllegalArgumentException(
                    "Booking request must not be null"
            );
        }

        if (request.getUserId() == null ||
                request.getHotelId() == null ||
                request.getRoomId() == null) {
            throw new IllegalArgumentException(
                    "User, hotel and room IDs are required"
            );
        }

        if (request.getStartDate() == null ||
                request.getEndDate() == null ||
                !request.getEndDate().isAfter(request.getStartDate())) {
            throw new IllegalArgumentException(
                    "End date must be later than start date"
            );
        }
    }

    private void ensureSuccessfulResponse(
            ResponseEntity<?> response,
            String errorMessage) {

        if (response == null ||
                !response.getStatusCode().is2xxSuccessful()) {
            throw new IllegalStateException(errorMessage);
        }
    }

    // -----------------------------------------------------------------
    // Request DTO
    // -----------------------------------------------------------------

    private ConfirmAvailabilityRequest buildConfirmAvailabilityRequest(
            Booking booking) {

        ConfirmAvailabilityRequest request =
                new ConfirmAvailabilityRequest();

        request.setBookingId(booking.getBookingId());
        request.setCheckIn(booking.getStartDate());
        request.setCheckOut(booking.getEndDate());

        return request;
    }

    // -----------------------------------------------------------------
    // Hotel Service URLs
    // -----------------------------------------------------------------

    private String buildConfirmAvailabilityUrl(Booking booking) {

        return UriComponentsBuilder.fromHttpUrl(hotelServiceUrl)
                .path("/internal/rooms/{roomId}/confirm-availability")
                .queryParam("requestId", booking.getRequestId())
                .buildAndExpand(booking.getRoomId())
                .toUriString();
    }

    private String buildConfirmBookingUrl(Booking booking) {

        return UriComponentsBuilder.fromHttpUrl(hotelServiceUrl)
                .path("/internal/rooms/{roomId}/confirm-booking")
                .queryParam("requestId", booking.getRequestId())
                .queryParam("bookingId", booking.getBookingId())
                .buildAndExpand(booking.getRoomId())
                .toUriString();
    }

    private String buildReleaseLockUrl(Booking booking) {

        return UriComponentsBuilder.fromHttpUrl(hotelServiceUrl)
                .path("/internal/rooms/{roomId}/release")
                .buildAndExpand(booking.getRoomId())
                .toUriString();
    }

    private String buildCancelBookingUrl(Booking booking) {

        return UriComponentsBuilder.fromHttpUrl(hotelServiceUrl)
                .path("/internal/rooms/{roomId}/cancel-booking")
                .queryParam("bookingId", booking.getBookingId())
                .buildAndExpand(booking.getRoomId())
                .toUriString();
    }

    // -----------------------------------------------------------------
    // Compensation
    // -----------------------------------------------------------------

    private void compensate(
            Booking booking,
            RuntimeException sagaException,
            boolean lockRequestAttempted,
            boolean confirmationRequestAttempted) {

        log.info(
                "Compensating booking saga: {}",
                booking.getBookingId()
        );

        boolean compensationSucceeded = true;

        String errorMessage =
                "Saga failed: " + sagaException.getMessage();

        CircuitBreaker circuitBreaker =
                circuitBreakerRegistry.circuitBreaker(CIRCUIT_BREAKER_NAME);

        try {

            if (confirmationRequestAttempted) {

                // Hotel may already have confirmed the booking,
                // even if the HTTP response was lost.
                ResponseEntity<Map> response =
                        executeCheckedSupplier(circuitBreaker, () ->
                                restTemplate.postForEntity(
                                        buildCancelBookingUrl(booking),
                                        null,
                                        Map.class
                                )
                        );

                ensureSuccessfulResponse(
                        response,
                        "Failed to cancel confirmed booking in Hotel Service"
                );

            } else if (lockRequestAttempted) {

                // Only the temporary lock was requested.
                // ReleaseLockRequest expects JSON body, not query parameter.
                ResponseEntity<Map> response =
                        executeCheckedSupplier(circuitBreaker, () ->
                                restTemplate.postForEntity(
                                        buildReleaseLockUrl(booking),
                                        Map.of(
                                                "requestId",
                                                booking.getRequestId()
                                        ),
                                        Map.class
                                )
                        );

                ensureSuccessfulResponse(
                        response,
                        "Failed to release temporary room lock"
                );
            }

        } catch (RuntimeException compensationException) {

            compensationSucceeded = false;

            errorMessage +=
                    "; compensation failed: " +
                    compensationException.getMessage();

            sagaException.addSuppressed(compensationException);

            log.error(
                    "Compensation failed for booking {}",
                    booking.getBookingId(),
                    compensationException
            );
        }

        final boolean finalCompensationSucceeded =
                compensationSucceeded;

        final String finalErrorMessage = errorMessage;

        // Persist compensation result in its own local transaction.
        transactionTemplate.executeWithoutResult(status -> {

            Booking current = bookingRepository.findById(
                    booking.getBookingId()
            ).orElseThrow(() ->
                    new IllegalStateException(
                            "Booking not found during compensation"
                    )
            );

            current.setStatus(BookingStatus.CANCELLED);
            bookingRepository.save(current);

            sagaStateRepository.findByRequestId(
                    booking.getRequestId()
            ).ifPresent(sagaState -> {
                sagaState.setStatus(
                        finalCompensationSucceeded
                                ? SagaState.STATUS_COMPENSATED
                                : SagaState.STATUS_COMPENSATION_FAILED
                );

                sagaState.setErrorMessage(finalErrorMessage);
                sagaState.setUpdatedAt(LocalDateTime.now());

                sagaStateRepository.save(sagaState);
            });
        });
    }
}

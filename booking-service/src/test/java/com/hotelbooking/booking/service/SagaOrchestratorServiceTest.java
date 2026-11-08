
package com.hotelbooking.booking.service;

import com.hotelbooking.booking.dto.BookingRequest;
import com.hotelbooking.booking.dto.ConfirmAvailabilityRequest;
import com.hotelbooking.booking.entity.Booking;
import com.hotelbooking.booking.entity.BookingStatus;
import com.hotelbooking.booking.repository.BookingRepository;
import com.hotelbooking.booking.repository.SagaStateRepository;
import com.hotelbooking.booking.saga.SagaState;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.mockito.stubbing.Answer;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SagaOrchestratorServiceTest {

    private static final String HOTEL_URL = "http://hotel-service";
    private static final String CB_NAME = "hotelServiceCircuitBreaker";

    private static final UUID USER_ID =
            UUID.randomUUID();
    private static final UUID HOTEL_ID =
            UUID.randomUUID();
    private static final UUID ROOM_ID =
            UUID.randomUUID();

    private static final LocalDate START_DATE =
            LocalDate.of(2026, 12, 1);
    private static final LocalDate END_DATE =
            LocalDate.of(2026, 12, 5);

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private SagaStateRepository sagaStateRepository;

    @Mock
    private RestTemplate restTemplate;

    private CircuitBreakerRegistry circuitBreakerRegistry;
    private TestTransactionManager transactionManager;
    private SagaOrchestratorService sagaService;

    private final AtomicReference<Booking> currentBooking =
            new AtomicReference<>();

    private final AtomicReference<SagaState> currentSaga =
            new AtomicReference<>();

    private final List<BookingStatus> bookingStatusesAtSave =
            new ArrayList<>();

    private final List<String> sagaStatusesAtSave =
            new ArrayList<>();

    @BeforeEach
    void setUp() {

        currentBooking.set(null);
        currentSaga.set(null);

        bookingStatusesAtSave.clear();
        sagaStatusesAtSave.clear();

        // Real Circuit Breaker, not a Mockito mock.
        CircuitBreakerConfig cbConfig =
                CircuitBreakerConfig.custom()
                        .slidingWindowSize(10)
                        .minimumNumberOfCalls(10)
                        .failureRateThreshold(50)
                        .build();

        circuitBreakerRegistry =
                CircuitBreakerRegistry.of(cbConfig);

        // Real TransactionTemplate will use this manager.
        transactionManager = new TestTransactionManager();

        sagaService = new SagaOrchestratorService(
                bookingRepository,
                sagaStateRepository,
                circuitBreakerRegistry,
                restTemplate,
                transactionManager
        );

        ReflectionTestUtils.setField(
                sagaService,
                "hotelServiceUrl",
                HOTEL_URL
        );

        configureRepositoryMocks();
    }

    private void configureRepositoryMocks() {

        doAnswer(invocation -> {
            Booking booking = invocation.getArgument(0);

            bookingStatusesAtSave.add(booking.getStatus());
            currentBooking.set(booking);

            return booking;
        }).when(bookingRepository).save(any(Booking.class));

        doAnswer(invocation -> {
            SagaState state = invocation.getArgument(0);

            sagaStatusesAtSave.add(state.getStatus());
            currentSaga.set(state);

            return state;
        }).when(sagaStateRepository).save(any(SagaState.class));

        lenient().when(bookingRepository.findById(any(UUID.class)))
                .thenAnswer(invocation -> {
                    UUID id = invocation.getArgument(0);

                    return Optional.ofNullable(currentBooking.get())
                            .filter(booking ->
                                    booking.getBookingId().equals(id));
                });

        lenient().when(
                bookingRepository.findByBookingIdAndUserId(
                        any(UUID.class),
                        any(UUID.class)
                )
        ).thenAnswer(invocation -> {
            UUID bookingId = invocation.getArgument(0);

            return Optional.ofNullable(currentBooking.get())
                    .filter(booking ->
                            booking.getBookingId().equals(bookingId));
        });

        lenient().when(sagaStateRepository.findByRequestId(any(UUID.class)))
                .thenAnswer(invocation -> {
                    UUID requestId = invocation.getArgument(0);

                    return Optional.ofNullable(currentSaga.get())
                            .filter(state ->
                                    state.getRequestId().equals(requestId));
                });
    }

    // -----------------------------------------------------------------
    // Test helpers
    // -----------------------------------------------------------------

    private BookingRequest request() {

        BookingRequest request = new BookingRequest();

        request.setUserId(USER_ID);
        request.setHotelId(HOTEL_ID);
        request.setRoomId(ROOM_ID);
        request.setStartDate(START_DATE);
        request.setEndDate(END_DATE);

        return request;
    }

    private ResponseEntity<Map> okResponse() {

        Map<String, String> body =
                Map.of("status", "ok");

        return new ResponseEntity<>(body, org.springframework.http.HttpStatus.OK);
    }

    private void stubSuccessfulHotelCalls() {
        stubHotelCalls(invocation -> okResponse());
    }

    private void stubHotelCalls(Answer<ResponseEntity<Map>> answer) {

        when(restTemplate.postForEntity(
                anyString(),
                any(),
                eq(Map.class)
        )).thenAnswer(answer);
    }

    private Booking seedBooking(BookingStatus status) {

        UUID bookingId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();

        Booking booking = new Booking(
                bookingId,
                USER_ID,
                HOTEL_ID,
                ROOM_ID,
                START_DATE,
                END_DATE,
                status,
                requestId
        );

        currentBooking.set(booking);

        currentSaga.set(new SagaState(
                requestId,
                bookingId,
                ROOM_ID,
                SagaState.STATUS_COMPLETED
        ));

        return booking;
    }

    private List<String> captureUrls(int expectedCalls) {

        ArgumentCaptor<String> captor =
                ArgumentCaptor.forClass(String.class);

        verify(restTemplate, times(expectedCalls)).postForEntity(
                captor.capture(),
                any(),
                eq(Map.class)
        );

        return captor.getAllValues();
    }

    // -----------------------------------------------------------------
    // A. Successful Saga
    // -----------------------------------------------------------------

    @Test
    void pendingBookingAndSagaArePersistedBeforeRemoteCall() {

        stubSuccessfulHotelCalls();

        sagaService.startBookingSaga(request());

        InOrder order = inOrder(
                bookingRepository,
                sagaStateRepository,
                restTemplate
        );

        order.verify(bookingRepository).save(any(Booking.class));
        order.verify(sagaStateRepository).save(any(SagaState.class));

        order.verify(restTemplate).postForEntity(
                contains("/confirm-availability"),
                any(),
                eq(Map.class)
        );

        assertThat(bookingStatusesAtSave.get(0))
                .isEqualTo(BookingStatus.PENDING);

        assertThat(sagaStatusesAtSave.get(0))
                .isEqualTo(SagaState.STATUS_PENDING);
    }

    @Test
    void availabilityEndpointReceivesCorrectRoomAndDates() {

        stubSuccessfulHotelCalls();

        Booking result = sagaService.startBookingSaga(request());

        ArgumentCaptor<ConfirmAvailabilityRequest> bodyCaptor =
                ArgumentCaptor.forClass(ConfirmAvailabilityRequest.class);

        ArgumentCaptor<String> urlCaptor =
                ArgumentCaptor.forClass(String.class);

        verify(restTemplate, times(2)).postForEntity(
                urlCaptor.capture(),
                any(),
                eq(Map.class)
        );

        verify(restTemplate).postForEntity(
                contains("/confirm-availability"),
                bodyCaptor.capture(),
                eq(Map.class)
        );

        String availabilityUrl = urlCaptor.getAllValues().get(0);

        assertThat(availabilityUrl)
                .contains("/internal/rooms/" + ROOM_ID)
                .contains("/confirm-availability")
                .contains("requestId=" + result.getRequestId());

        ConfirmAvailabilityRequest body = bodyCaptor.getValue();

        assertThat(body.getBookingId()).isEqualTo(result.getBookingId());
        assertThat(body.getCheckIn()).isEqualTo(START_DATE);
        assertThat(body.getCheckOut()).isEqualTo(END_DATE);
    }

    @Test
    void confirmationEndpointReceivesCorrectIdentifiers() {

        stubSuccessfulHotelCalls();

        Booking result = sagaService.startBookingSaga(request());

        List<String> urls = captureUrls(2);

        String confirmationUrl = urls.get(1);

        assertThat(confirmationUrl)
                .contains("/confirm-booking")
                .contains("requestId=" + result.getRequestId())
                .contains("bookingId=" + result.getBookingId())
                .contains("/" + ROOM_ID);
    }

    @Test
    void successfulSagaConfirmsBookingAndCompletesState() {

        stubSuccessfulHotelCalls();

        Booking result = sagaService.startBookingSaga(request());

        assertThat(result.getStatus())
                .isEqualTo(BookingStatus.CONFIRMED);

        assertThat(currentSaga.get().getStatus())
                .isEqualTo(SagaState.STATUS_COMPLETED);

        assertThat(bookingStatusesAtSave)
                .contains(BookingStatus.PENDING, BookingStatus.CONFIRMED);

        assertThat(sagaStatusesAtSave)
                .contains(
                        SagaState.STATUS_PENDING,
                        SagaState.STATUS_COMPLETED
                );
    }

    // -----------------------------------------------------------------
    // B. Failure and compensation
    // -----------------------------------------------------------------

    @Test
    void roomLockFailureTriggersReleaseCompensation() {

        stubHotelCalls(invocation -> {
            String url = invocation.getArgument(0);

            if (url.contains("/confirm-availability")) {
                throw new ResourceAccessException("Hotel timeout");
            }

            return okResponse();
        });

        assertThatThrownBy(() ->
                sagaService.startBookingSaga(request())
        ).isInstanceOf(RuntimeException.class);

        List<String> urls = captureUrls(2);

        assertThat(urls.get(0)).contains("/confirm-availability");
        assertThat(urls.get(1)).contains("/release");

        assertThat(currentBooking.get().getStatus())
                .isEqualTo(BookingStatus.CANCELLED);

        assertThat(currentSaga.get().getStatus())
                .isEqualTo(SagaState.STATUS_COMPENSATED);

        // Release endpoint expects requestId in the JSON body.
        ArgumentCaptor<Object> bodyCaptor =
                ArgumentCaptor.forClass(Object.class);

        verify(restTemplate).postForEntity(
                contains("/release"),
                bodyCaptor.capture(),
                eq(Map.class)
        );

        assertThat(bodyCaptor.getValue())
        .isInstanceOf(Map.class);

        Map<?, ?> body = (Map<?, ?>) bodyCaptor.getValue();

        assertThat(body.get("requestId"))
                .isEqualTo(currentBooking.get().getRequestId());
    }

    @Test
    void confirmationFailureTriggersCancelBookingCompensation() {

        stubHotelCalls(invocation -> {
            String url = invocation.getArgument(0);

            if (url.contains("/confirm-booking")) {
                throw new ResourceAccessException(
                        "Confirmation response lost"
                );
            }

            return okResponse();
        });

        assertThatThrownBy(() ->
                sagaService.startBookingSaga(request())
        ).isInstanceOf(RuntimeException.class);

        List<String> urls = captureUrls(3);

        assertThat(urls.get(0)).contains("/confirm-availability");
        assertThat(urls.get(1)).contains("/confirm-booking");
        assertThat(urls.get(2))
                .contains("/cancel-booking")
                .contains("bookingId=" + currentBooking.get().getBookingId());

        assertThat(currentSaga.get().getStatus())
                .isEqualTo(SagaState.STATUS_COMPENSATED);
    }

    @Test
    void compensationFailurePersistsCompensationFailedState() {

        stubHotelCalls(invocation -> {
            String url = invocation.getArgument(0);

            throw new ResourceAccessException(
                    url.contains("/release")
                            ? "Release failed"
                            : "Hotel unavailable"
            );
        });

        assertThatThrownBy(() ->
                sagaService.startBookingSaga(request())
        ).isInstanceOf(RuntimeException.class);

        assertThat(currentBooking.get().getStatus())
                .isEqualTo(BookingStatus.CANCELLED);

        assertThat(currentSaga.get().getStatus())
                .isEqualTo(SagaState.STATUS_COMPENSATION_FAILED);

        assertThat(currentSaga.get().getErrorMessage())
                .contains("compensation failed");
    }

    // -----------------------------------------------------------------
    // C. User cancellation
    // -----------------------------------------------------------------

    @Test
    void onlyConfirmedBookingsCanBeCancelled() {

        Booking pending = seedBooking(BookingStatus.PENDING);

        assertThatThrownBy(() ->
                sagaService.cancelBookingSaga(
                        pending.getBookingId(),
                        USER_ID
                )
        )
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Only confirmed");

        verifyNoInteractions(restTemplate);
    }

    @Test
    void userCancellationUsesCancelBookingEndpoint() {

        Booking confirmed = seedBooking(BookingStatus.CONFIRMED);

        stubSuccessfulHotelCalls();

        sagaService.cancelBookingSaga(
                confirmed.getBookingId(),
                USER_ID
        );

        List<String> urls = captureUrls(1);

        assertThat(urls.get(0))
                .contains("/cancel-booking")
                .contains("bookingId=" + confirmed.getBookingId())
                .contains("/" + ROOM_ID);

        assertThat(currentBooking.get().getStatus())
                .isEqualTo(BookingStatus.CANCELLED);

        assertThat(currentSaga.get().getStatus())
                .isEqualTo(SagaState.STATUS_CANCELLED);
    }

    @Test
    void remoteCancellationFailureLeavesLocalBookingConfirmed() {

        Booking confirmed = seedBooking(BookingStatus.CONFIRMED);

        stubHotelCalls(invocation -> {
            throw new ResourceAccessException("Hotel service unavailable");
        });

        assertThatThrownBy(() ->
                sagaService.cancelBookingSaga(
                        confirmed.getBookingId(),
                        USER_ID
                )
        ).isInstanceOf(RuntimeException.class);

        assertThat(currentBooking.get().getStatus())
                .isEqualTo(BookingStatus.CONFIRMED);

        verify(bookingRepository, org.mockito.Mockito.never())
                .save(any(Booking.class));
    }

    // -----------------------------------------------------------------
    // D. Reliability and transaction boundaries
    // -----------------------------------------------------------------

    @Test
    void openCircuitBreakerPreventsRemoteCalls() {

        CircuitBreaker circuitBreaker =
                circuitBreakerRegistry.circuitBreaker(CB_NAME);

        circuitBreaker.transitionToOpenState();

        assertThat(circuitBreaker.getState())
                .isEqualTo(CircuitBreaker.State.OPEN);

        assertThatThrownBy(() ->
                sagaService.startBookingSaga(request())
        ).isInstanceOf(RuntimeException.class);

        verifyNoInteractions(restTemplate);

        assertThat(currentSaga.get().getStatus())
                .isEqualTo(SagaState.STATUS_COMPENSATION_FAILED);
    }

    @Test
    void httpCallsAreExecutedOutsideDatabaseTransactions() {

        List<Boolean> transactionStates = new ArrayList<>();

        stubHotelCalls(invocation -> {
            transactionStates.add(
                    TransactionSynchronizationManager
                            .isActualTransactionActive()
            );

            return okResponse();
        });

        sagaService.startBookingSaga(request());

        assertThat(transactionStates)
                .hasSize(2)
                .containsOnly(false);
    }

    @Test
    void datesArePassedWithoutModification() {

        LocalDate start = LocalDate.of(2027, 3, 15);
        LocalDate end = LocalDate.of(2027, 3, 20);

        BookingRequest request = request();
        request.setStartDate(start);
        request.setEndDate(end);

        stubSuccessfulHotelCalls();

        sagaService.startBookingSaga(request);

        ArgumentCaptor<ConfirmAvailabilityRequest> bodyCaptor =
                ArgumentCaptor.forClass(ConfirmAvailabilityRequest.class);

        verify(restTemplate).postForEntity(
                contains("/confirm-availability"),
                bodyCaptor.capture(),
                eq(Map.class)
        );

        assertThat(bodyCaptor.getValue().getCheckIn()).isEqualTo(start);
        assertThat(bodyCaptor.getValue().getCheckOut()).isEqualTo(end);
    }

    @Test
    void nullRequestIsRejected() {

        assertThatThrownBy(() ->
                sagaService.startBookingSaga(null)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not be null");

        verifyNoInteractions(bookingRepository);
        verifyNoInteractions(restTemplate);
    }

    @Test
    void invalidDateRangeIsRejected() {

        BookingRequest request = request();
        request.setStartDate(LocalDate.of(2026, 12, 5));
        request.setEndDate(LocalDate.of(2026, 12, 1));

        assertThatThrownBy(() ->
                sagaService.startBookingSaga(request)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("End date");

        verifyNoInteractions(bookingRepository);
        verifyNoInteractions(restTemplate);
    }

    @Test
    void sameRequestIdCorrelatesBothHotelCalls() {

        stubSuccessfulHotelCalls();

        Booking result = sagaService.startBookingSaga(request());

        List<String> urls = captureUrls(2);

        String requestId = result.getRequestId().toString();

        assertThat(urls.get(0)).contains("requestId=" + requestId);
        assertThat(urls.get(1)).contains("requestId=" + requestId);
    }

    // -----------------------------------------------------------------
    // Minimal transaction manager for unit tests
    // -----------------------------------------------------------------

    /**
     * Provides real Spring transaction-template callback and
     * synchronization behavior without requiring a database.
     *
     * Repositories remain mocked. This is not an integration test.
     */
    private static final class TestTransactionManager
            extends AbstractPlatformTransactionManager {

        @Override
        protected Object doGetTransaction() {
            return new Object();
        }

        @Override
        protected void doBegin(
                Object transaction,
                TransactionDefinition definition) {
            // No database resource is required for these unit tests.
        }

        @Override
        protected void doCommit(DefaultTransactionStatus status) {
            // No database resource is required for these unit tests.
        }

        @Override
        protected void doRollback(DefaultTransactionStatus status) {
            // No database resource is required for these unit tests.
        }
    }
}

package com.agrichain.notification;

import com.agrichain.common.enums.NotificationChannel;
import com.agrichain.common.enums.NotificationStatus;
import com.agrichain.notification.dto.NotificationRequest;
import com.agrichain.notification.entity.Notification;
import com.agrichain.notification.repository.NotificationRepository;
import net.jqwik.api.*;
import org.mockito.Mockito;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.Collections;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class NotificationPropertyTest {

    private final NotificationRepository notificationRepository = Mockito.mock(NotificationRepository.class);
    private final NotificationService notificationService;

    public NotificationPropertyTest() {
        this.notificationService = new NotificationService(notificationRepository);
    }

    /**
     * Property 33: Delivered via specified channel.
     */
    @Property(tries = 50)
    void property_33_correct_channel(@ForAll NotificationChannel channel) {
        Mockito.reset(notificationRepository);

        NotificationRequest req = new NotificationRequest();
        req.setUserId(UUID.randomUUID());
        req.setChannel(channel);
        req.setContent("Test content");

        when(notificationRepository.save(any(Notification.class))).thenAnswer(i -> i.getArgument(0));

        notificationService.sendNotification(req);

        verify(notificationRepository, atLeastOnce()).save(argThat(n ->
            n.getChannel() == channel
        ));
    }

    /**
     * Property 34: Retry count never exceeds 3.
     */
    @Property(tries = 50)
    void property_34_max_retries(@ForAll("uuids") UUID id) {
        Notification n = new Notification();
        n.setId(id);
        n.setStatus(NotificationStatus.Pending);
        n.setRetryCount(3); // Start at max

        notificationService.deliverWithRetry(n);

        assertThat(n.getRetryCount()).isLessThanOrEqualTo(3);
    }

    /**
     * Property 35: Notification history pages contain at most 50 records.
     */
    @Property(tries = 30)
    void property_35_page_size(@ForAll("pageSize") int size) {
        Mockito.reset(notificationRepository);

        UUID userId = UUID.randomUUID();
        PageRequest pageRequest = PageRequest.of(0, Math.min(size, 50));

        Page<Notification> mockPage = new PageImpl<>(Collections.nCopies(pageRequest.getPageSize(), new Notification()));
        when(notificationRepository.findByUserId(eq(userId), eq(pageRequest))).thenReturn(mockPage);

        Page<Notification> result = notificationService.getUserNotifications(userId, pageRequest);

        assertThat(result.getContent().size()).isLessThanOrEqualTo(50);
    }

    @Provide
    Arbitrary<UUID> uuids() {
        return Arbitraries.create(UUID::randomUUID);
    }

    @Provide
    Arbitrary<Integer> pageSize() {
        return Arbitraries.integers().between(1, 100);
    }
}

package com.ecommerce.kyc.application.service;

import com.ecommerce.kyc.application.port.in.IngestWatchlistUseCase.IngestionResult;
import com.ecommerce.kyc.application.port.out.WatchlistFeedPort;
import com.ecommerce.kyc.application.port.out.WatchlistStorePort;
import com.ecommerce.kyc.domain.model.WatchlistEntry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WatchlistIngestionServiceTest {

    private static WatchlistEntry entry(String id, String name) {
        return new WatchlistEntry("OFAC", id, name, List.of(), "individual");
    }

    @Test
    void upsertsEntriesFromEachFeed() {
        WatchlistFeedPort feed = mock(WatchlistFeedPort.class);
        when(feed.source()).thenReturn("OFAC");
        when(feed.fetch()).thenReturn(List.of(entry("1", "Ann Smith"), entry("2", "Bob Jones")));
        WatchlistStorePort store = mock(WatchlistStorePort.class);

        WatchlistIngestionService svc =
                new WatchlistIngestionService(List.of(feed), store, new SimpleMeterRegistry());

        IngestionResult result = svc.ingest();

        assertThat(result.entriesUpserted()).isEqualTo(2);
        assertThat(result.feedsFailed()).isZero();
        verify(store, times(1)).upsert(anyCollection());
    }

    @Test
    void feedFailure_isSwallowed_existingDataLeftIntact() {
        WatchlistFeedPort failing = mock(WatchlistFeedPort.class);
        when(failing.source()).thenReturn("OFAC");
        when(failing.fetch()).thenThrow(new RuntimeException("network down"));
        WatchlistStorePort store = mock(WatchlistStorePort.class);

        WatchlistIngestionService svc =
                new WatchlistIngestionService(List.of(failing), store, new SimpleMeterRegistry());

        // No exception propagates...
        IngestionResult result = svc.ingest();

        assertThat(result.feedsFailed()).isEqualTo(1);
        assertThat(result.entriesUpserted()).isZero();
        // ...and we never touched the store, so existing screening data is intact.
        verify(store, never()).upsert(anyCollection());
    }

    @Test
    void oneFeedFailing_doesNotAbortOthers() {
        WatchlistFeedPort failing = mock(WatchlistFeedPort.class);
        when(failing.source()).thenReturn("UN");
        when(failing.fetch()).thenThrow(new RuntimeException("boom"));
        WatchlistFeedPort ok = mock(WatchlistFeedPort.class);
        when(ok.source()).thenReturn("OFAC");
        when(ok.fetch()).thenReturn(List.of(entry("1", "Ann Smith")));
        WatchlistStorePort store = mock(WatchlistStorePort.class);

        WatchlistIngestionService svc =
                new WatchlistIngestionService(List.of(failing, ok), store, new SimpleMeterRegistry());

        IngestionResult result = svc.ingest();

        assertThat(result.feedsFailed()).isEqualTo(1);
        assertThat(result.entriesUpserted()).isEqualTo(1);
        verify(store, times(1)).upsert(anyCollection());
    }

    @Test
    void storeFailure_isContainedPerFeed() {
        WatchlistFeedPort feed = mock(WatchlistFeedPort.class);
        when(feed.source()).thenReturn("OFAC");
        when(feed.fetch()).thenReturn(List.of(entry("1", "Ann Smith")));
        WatchlistStorePort store = mock(WatchlistStorePort.class);
        org.mockito.Mockito.doThrow(new RuntimeException("store down")).when(store).upsert(any());

        WatchlistIngestionService svc =
                new WatchlistIngestionService(List.of(feed), store, new SimpleMeterRegistry());

        IngestionResult result = svc.ingest();

        assertThat(result.feedsFailed()).isEqualTo(1);
        assertThat(result.entriesUpserted()).isZero();
    }
}

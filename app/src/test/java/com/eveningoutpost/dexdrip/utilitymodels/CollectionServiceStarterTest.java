package com.eveningoutpost.dexdrip.utilitymodels;

import static com.google.common.truth.Truth.assertWithMessage;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.services.DexCollectionService;
import com.eveningoutpost.dexdrip.services.UiBasedCollector;
import com.eveningoutpost.dexdrip.services.WifiCollectionService;

import org.junit.Test;

public class CollectionServiceStarterTest extends RobolectricTestWithConfig {

    @Test
    public void canStartAsForegroundService_rejectsNotificationListenerService() {
        assertWithMessage("UiBasedCollector is a NotificationListenerService")
                .that(CollectionServiceStarter.canStartAsForegroundService(UiBasedCollector.class))
                .isFalse();
    }

    @Test
    public void canStartAsForegroundService_allowsBtCollector() {
        assertWithMessage("DexCollectionService is a foreground service")
                .that(CollectionServiceStarter.canStartAsForegroundService(DexCollectionService.class))
                .isTrue();
    }

    @Test
    public void canStartAsForegroundService_allowsWifiCollector() {
        assertWithMessage("WifiCollectionService is a foreground service")
                .that(CollectionServiceStarter.canStartAsForegroundService(WifiCollectionService.class))
                .isTrue();
    }
}

package com.eveningoutpost.dexdrip.services;

import static com.google.common.truth.Truth.assertWithMessage;
import static java.lang.reflect.Modifier.isPublic;
import static java.lang.reflect.Modifier.isStatic;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.cgm.carelinkfollow.CareLinkFollowService;
import com.eveningoutpost.dexdrip.cgm.nsfollow.NightscoutFollowService;

import org.junit.Test;

import java.lang.reflect.Method;

public class CollectorCollectingContractTest extends RobolectricTestWithConfig {

    private void assertReflectiveContract(final Class<?> clazz) throws Exception {
        final Method method = clazz.getMethod("isCollecting");
        assertWithMessage(clazz.getSimpleName() + " isCollecting is public")
                .that(isPublic(method.getModifiers())).isTrue();
        assertWithMessage(clazz.getSimpleName() + " isCollecting is static")
                .that(isStatic(method.getModifiers())).isTrue();
        assertWithMessage(clazz.getSimpleName() + " isCollecting returns boolean")
                .that(method.getReturnType()).isEqualTo(boolean.class);
        final Object result = method.invoke(null);
        assertWithMessage(clazz.getSimpleName() + " isCollecting returns a Boolean")
                .that(result).isInstanceOf(Boolean.class);
    }

    @Test
    public void uiBasedCollector_satisfiesReflectiveContract() throws Exception {
        assertReflectiveContract(UiBasedCollector.class);
    }

    @Test
    public void doNothingService_satisfiesReflectiveContract() throws Exception {
        assertReflectiveContract(DoNothingService.class);
    }

    @Test
    public void nightscoutFollowService_satisfiesReflectiveContract() throws Exception {
        assertReflectiveContract(NightscoutFollowService.class);
    }

    @Test
    public void careLinkFollowService_satisfiesReflectiveContract() throws Exception {
        assertReflectiveContract(CareLinkFollowService.class);
    }
}

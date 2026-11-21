package com.vanmo.booking_app_be.ai;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;

/**
 * A model too busy to answer within {@code AI_TIMEOUT_SECONDS} used to end the request as AI_UNAVAILABLE, which
 * {@code withModelFallback} does not retry on - so the fallback chain never ran and the guest was told the
 * assistant was off while two other models sat unused. Spring buries the socket timeout several layers down, so
 * recognising it is the whole fix.
 */
class AiClientTimeoutTest {

    @Test
    void aSocketTimeoutIsFoundHoweverDeeplySpringWrappedIt() {
        assertThat(AiClient.timedOut(new SocketTimeoutException("Read timed out")))
                .isTrue();
        assertThat(AiClient.timedOut(new ResourceAccessException("I/O error", new SocketTimeoutException())))
                .isTrue();
        assertThat(AiClient.timedOut(new RestClientException(
                        "Error while extracting response for type [JsonNode] and content type"
                                + " [application/octet-stream]",
                        new ResourceAccessException("I/O error", new SocketTimeoutException()))))
                .isTrue();
    }

    @Test
    void anyOtherInterruptedReadCountsTooButAPlainFailureDoesNot() {
        assertThat(AiClient.timedOut(new InterruptedIOException())).isTrue();

        // A host that does not resolve or a body that will not parse is not a busy model: trying the next model
        // would only repeat the same failure three times.
        assertThat(AiClient.timedOut(new ResourceAccessException("I/O error", new UnknownHostException("nope"))))
                .isFalse();
        assertThat(AiClient.timedOut(new RestClientException("broken json"))).isFalse();
    }

    @Test
    void aSelfReferencingCauseDoesNotHangTheWalk() {
        // A cause chain that points at itself must not spin forever inside a request thread.
        IOException loop = new IOException("looping") {
            @Override
            public synchronized Throwable getCause() {
                return this;
            }
        };

        assertThat(AiClient.timedOut(loop)).isFalse();
        assertThat(AiClient.rootCause(loop)).isSameAs(loop);
    }

    @Test
    void theLoggedCauseIsTheRealOneNotSpringsWrapper() {
        UnknownHostException real = new UnknownHostException("api.example.invalid");

        assertThat(AiClient.rootCause(new RestClientException("I/O error", new ResourceAccessException("x", real))))
                .isSameAs(real);
    }
}

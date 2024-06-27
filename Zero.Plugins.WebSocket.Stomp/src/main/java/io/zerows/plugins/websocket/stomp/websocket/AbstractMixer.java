package io.zerows.plugins.websocket.stomp.websocket;

import io.vertx.core.Vertx;
import io.vertx.up.util.Ut;
import io.zerows.core.metadata.uca.logging.OLog;

import java.util.Objects;

/**
 * @author <a href="http://www.origin-x.cn">Lang</a>
 */
abstract class AbstractMixer implements Mixer {

    protected Vertx vertx;

    protected AbstractMixer(final Vertx vertx) {
        this.vertx = vertx;
    }

    protected OLog logger() {
        return Ut.Log.websocket(this.getClass());
    }

    @SuppressWarnings("unchecked")
    protected <T> T finished(final Object returned) {
        if (Objects.isNull(returned)) {
            return null;
        } else {
            return (T) returned;
        }
    }

    protected <T> T finished() {
        return this.finished(null);
    }
}

package io.zerows.plugins.websocket.stomp.handler;

import io.vertx.core.Vertx;
import io.vertx.ext.stomp.StompServerHandler;
import io.vertx.ext.stomp.StompServerOptions;
import io.zerows.core.security.atom.Aegis;
import io.zerows.plugins.websocket.stomp.command.FrameWsHandler;

/**
 * @author <a href="http://www.origin-x.cn">Lang</a>
 */
public class MixerHandler extends AbstractMixer {
    private transient final Aegis aegis;

    public MixerHandler(final Vertx vertx, final Aegis aegis) {
        super(vertx);
        this.aegis = aegis;
    }

    @Override
    public <T> T mount(final StompServerHandler handler, final StompServerOptions options) {
        // Replace Connect Handler because of Security Needed.
        final FrameWsHandler connectHandler = FrameWsHandler.connector(this.vertx);
        handler.connectHandler(connectHandler.bind(this.aegis));

        handler.beginHandler(begin -> {
            System.out.println("Begin");
        });
        return this.finished();
    }
}

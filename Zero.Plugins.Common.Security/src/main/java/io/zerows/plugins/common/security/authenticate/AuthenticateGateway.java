package io.zerows.plugins.common.security.authenticate;

import io.horizon.exception.web._401UnauthorizedException;
import io.horizon.fn.Actuator;
import io.horizon.uca.log.Annal;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.up.eon.KName;
import io.vertx.up.eon.KWeb;
import io.vertx.up.util.Ut;
import io.zerows.core.feature.web.cache.Rapid;
import io.zerows.core.metadata.uca.environment.DevEnv;
import io.zerows.core.security.atom.Aegis;
import io.zerows.core.security.atom.Against;

import java.lang.reflect.Method;
import java.util.Objects;

/**
 * @author <a href="http://www.origin-x.cn">Lang</a>
 */
public class AuthenticateGateway {

    private static final Annal LOGGER = Annal.get(AuthenticateGateway.class);

    public static void userCached(final JsonObject credentials, final Actuator actuator, final Actuator fnCache) {
        final String habitus = credentials.getString(KName.HABITUS);
        final Rapid<String, JsonObject> rapid = Rapid.object(habitus);
        rapid.read(KWeb.CACHE.User.AUTHENTICATE).onComplete(res -> {
            if (res.succeeded()) {
                final JsonObject cached = res.result();
                if (Objects.isNull(cached)) {
                    actuator.execute();
                } else {
                    if (DevEnv.devAuthorized()) {
                        LOGGER.info("[ Auth ]\u001b[0;32m 401 Authenticated Cached successfully!\u001b[m");
                    }
                    fnCache.execute();
                }
            }
        });
    }

    public static void userCached(final JsonObject credentials, final Actuator actuator) {
        final String habitus = credentials.getString(KName.HABITUS);
        final Rapid<String, JsonObject> rapid = Rapid.object(habitus);
        rapid.write(KWeb.CACHE.User.AUTHENTICATE, credentials).onComplete(next -> actuator.execute());
    }

    /*
     *  Executing Aegis Method and this code split because of re-use in two points
     *  1) AuthenticateBuiltInProvider code     HTTP Workflow
     *  2) SicStompServerHandler       code     WebSocket Workflow
     */
    public static void userVerified(final JsonObject credentials, final Aegis aegis, final Handler<AsyncResult<Boolean>> handler) {
        final Against against = aegis.getAuthorizer();
        final Method method = against.getAuthenticate();
        if (Objects.isNull(method)) {
            // Exception for method is null ( This situation should not happen )
            handler.handle(Ut.Bnd.failOut(_401UnauthorizedException.class, AuthenticateGateway.class));
        } else {
            // Verify the data by @Wall's method that has been annotated by @Authenticate
            final Object proxy = aegis.getProxy();
            final Future<Boolean> checkedFuture = Ut.invokeAsync(proxy, method, credentials);
            checkedFuture.onComplete(res -> {
                if (res.succeeded()) {
                    Boolean checked = res.result();
                    checked = !Objects.isNull(checked) && checked;
                    handler.handle(Future.succeededFuture(checked));
                } else {
                    // Exception Throw
                    final Throwable ex = res.cause();
                    if (Objects.isNull(ex)) {
                        // 401 Without Exception
                        handler.handle(Ut.Bnd.failOut(_401UnauthorizedException.class, AuthenticateGateway.class));
                    } else {
                        // 401 With Throwable
                        handler.handle(Future.failedFuture(ex));
                    }
                }
            });
        }
    }
}

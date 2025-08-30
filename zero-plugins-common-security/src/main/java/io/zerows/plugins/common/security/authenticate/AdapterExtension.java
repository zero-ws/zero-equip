package io.zerows.plugins.common.security.authenticate;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.authentication.AuthenticationProvider;
import io.vertx.ext.auth.authentication.Credentials;
import io.vertx.ext.auth.authentication.TokenCredentials;
import io.zerows.core.exception.WebException;
import io.zerows.core.exception.web._401UnauthorizedException;
import io.zerows.module.security.atom.Aegis;

/**
 * @author <a href="http://www.origin-x.cn">Lang</a>
 */
class AdapterExtension extends AbstractAdapter {

    private final AuthenticationProvider standard;

    AdapterExtension(final AuthenticationProvider standard) {
        this.standard = standard;
    }

    @Override
    public AuthenticationProvider provider(final Aegis aegis) {
        final AuthenticateBuiltInProvider provider = AuthenticateBuiltInProvider.provider(aegis);
        return new AuthenticationProvider() {
            @Override
            public void authenticate(final JsonObject jsonObject, final Handler<AsyncResult<User>> handler) {
                // First Standard Trigger
                final Credentials credentials = new TokenCredentials(jsonObject);
                AdapterExtension.this.standard.authenticate(credentials, res -> {
                    if (res.succeeded()) {
                        // BuildIn Trigger for User Validation
                        provider.authenticate(jsonObject, handler);
                    } else {
                        final WebException error = new _401UnauthorizedException(this.getClass());
                        handler.handle(Future.failedFuture(error));
                    }
                });
            }
        };
    }
}

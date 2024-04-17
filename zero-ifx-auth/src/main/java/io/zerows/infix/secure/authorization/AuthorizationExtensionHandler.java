package io.zerows.infix.secure.authorization;

import io.vertx.ext.web.handler.AuthorizationHandler;
import io.zerows.core.domain.atom.commune.secure.Aegis;

/**
 * @author <a href="http://www.origin-x.cn">Lang</a>
 */
public interface AuthorizationExtensionHandler extends AuthorizationHandler {

    default AuthorizationExtensionHandler configure(final Aegis aegis) {
        return this;
    }
}

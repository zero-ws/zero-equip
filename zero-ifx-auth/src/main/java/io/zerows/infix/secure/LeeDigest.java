package io.zerows.infix.secure;

import io.horizon.exception.web._501NotSupportException;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.authentication.AuthenticationProvider;
import io.vertx.ext.auth.htdigest.HtdigestAuth;
import io.vertx.ext.web.handler.AuthenticationHandler;
import io.vertx.ext.web.handler.impl.HTTPAuthorizationHandler;
import io.zerows.core.domain.atom.commune.secure.Aegis;
import io.zerows.core.domain.atom.commune.secure.AegisItem;
import io.vertx.up.eon.configure.YmlCore;
import io.vertx.up.util.Ut;
import io.zerows.infix.secure.authenticate.AdapterProvider;

/**
 * @author <a href="http://www.origin-x.cn">Lang</a>
 */
class LeeDigest extends AbstractLee {
    @Override
    public AuthenticationHandler authenticate(final Vertx vertx, final Aegis config) {
        // provider processing
        final HtdigestAuth standard = this.providerInternal(vertx, config);
        // handler building
        final AuthenticationHandler handler = this.buildHandler(standard, config,
            HTTPAuthorizationHandler.Type.DIGEST);
        return this.wrapHandler(handler, config);
    }

    @Override
    public AuthenticationProvider provider(final Vertx vertx, final Aegis config) {
        final HtdigestAuth standard = this.providerInternal(vertx, config);
        final AdapterProvider extension = AdapterProvider.extension(standard);
        return extension.provider(config);
    }

    @Override
    @SuppressWarnings("unchecked")
    public HtdigestAuth providerInternal(final Vertx vertx, final Aegis config) {
        final String filename = this.option(config,
            YmlCore.secure.digest.options.FILENAME);
        final HtdigestAuth standard;
        if (Ut.isNil(filename)) {
            standard = HtdigestAuth.create(vertx);
        } else {
            standard = HtdigestAuth.create(vertx, filename);
        }
        return standard;
    }

    @Override
    public String encode(final JsonObject data, final AegisItem config) {
        throw new _501NotSupportException(this.getClass());
    }

    @Override
    public JsonObject decode(final String token, final AegisItem config) {
        throw new _501NotSupportException(this.getClass());
    }
}
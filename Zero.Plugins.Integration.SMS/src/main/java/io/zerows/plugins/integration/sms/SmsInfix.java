package io.zerows.plugins.integration.sms;

import io.horizon.uca.cache.Cc;
import io.vertx.core.Vertx;
import io.vertx.up.annotations.Infusion;
import io.zerows.core.metadata.zdk.plugins.Infix;

@Infusion
@SuppressWarnings("all")
public class SmsInfix implements Infix {

    private static final String NAME = "ZERO_SMS_ALI_POOL";

    private static final Cc<String, SmsClient> CC_CLIENT = Cc.open();

    public static void init(final Vertx vertx) {
        CC_CLIENT.pick(() -> Infix.init(SmsConfig.CONFIG_KEY,
            (config) -> SmsClient.createShared(vertx),
            SmsInfix.class), NAME);
    }

    public static SmsClient getClient() {
        return CC_CLIENT.store(NAME);
    }

    @Override
    public SmsClient get() {
        return getClient();
    }
}

package io.zerows.infix.sms;

import io.horizon.uca.cache.Cc;
import io.vertx.core.Vertx;
import io.vertx.up.annotations.Infusion;
import io.zerows.core.metadata.zdk.plugins.Infix;
import io.zerows.infix.sms.sms.SmsClient;

@Infusion
@SuppressWarnings("all")
public class SmsInfix implements Infix {

    private static final String NAME = "ZERO_ALI_SMS_POOL";

    private static final Cc<String, SmsClient> CC_CLIENT = Cc.open();

    private static void initInternal(final Vertx vertx,
                                     final String name) {
        CC_CLIENT.pick(() -> Infix.init("ali-sms",
            (config) -> SmsClient.createShared(vertx),
            SmsInfix.class), name);
    }

    public static void init(final Vertx vertx) {
        initInternal(vertx, NAME);
    }

    public static SmsClient getClient() {
        return CC_CLIENT.store(NAME);
    }

    @Override
    public SmsClient get() {
        return getClient();
    }
}

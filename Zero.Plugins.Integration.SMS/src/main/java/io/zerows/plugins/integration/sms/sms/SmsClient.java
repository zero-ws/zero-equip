package io.zerows.plugins.integration.sms.sms;

import io.vertx.codegen.annotations.Fluent;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.zerows.core.metadata.zdk.plugins.InfixClient;

/**
 * AliSmsClient for platform of https://dysms.console.aliyun.com/dysms.htm
 * Message open sdk
 */
public interface SmsClient extends InfixClient<SmsClient> {

    static SmsClient createShared(final Vertx vertx) {
        return new SmsClientImpl(vertx, SmsConfig.create());
    }

    @Fluent
    @Override
    SmsClient init(JsonObject params);

    /**
     * Send messsage to mobile by template
     *
     * @param mobile  mobile number
     * @param tplCode default template codes
     * @param params  params for template
     *
     * @return self reference
     */
    @Fluent
    SmsClient send(String mobile, String tplCode, JsonObject params,
                   Handler<AsyncResult<JsonObject>> handler);
}

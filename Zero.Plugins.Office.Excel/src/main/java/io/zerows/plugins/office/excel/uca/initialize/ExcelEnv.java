package io.zerows.plugins.office.excel.uca.initialize;

import io.horizon.uca.cache.Cc;
import io.vertx.core.json.JsonObject;
import io.vertx.up.util.Ut;
import io.zerows.core.metadata.uca.logging.OLog;

/**
 * @author lang : 2024-06-12
 */
public interface ExcelEnv<R> {

    Cc<String, ExcelEnv<?>> CC_SKELETON = Cc.openThread();

    static ExcelEnv<?> of(final Class<?> classImpl) {
        return CC_SKELETON.pick(() -> Ut.instance(classImpl), classImpl.getName());
    }

    R prepare(JsonObject config);

    default OLog logger() {
        return Ut.Log.plugin(this.getClass());
    }
}

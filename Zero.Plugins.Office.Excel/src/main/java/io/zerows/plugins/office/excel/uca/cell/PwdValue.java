package io.zerows.plugins.office.excel.uca.cell;

import io.horizon.eon.VPath;
import io.horizon.eon.VString;
import io.vertx.core.json.JsonObject;
import io.vertx.up.eon.KName;
import io.vertx.up.util.Ut;

import java.util.concurrent.ConcurrentMap;

/**
 * PWD Processing
 */
public class PwdValue implements ExValue {

    @Override
    @SuppressWarnings("all")
    public Object to(final Object value, final ConcurrentMap<String, String> paramMap) {
        final String pathRoot = paramMap.get(KName.DIRECTORY);
        final String field = paramMap.get(KName.FIELD);
        final String filepath = Ut.ioPath(pathRoot, field) + VString.DOT + VPath.SUFFIX.JSON;
        final JsonObject content = Ut.ioJObject(filepath);
        return content.encode();
    }
}

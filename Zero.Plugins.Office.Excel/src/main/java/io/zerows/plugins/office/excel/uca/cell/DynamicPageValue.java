package io.zerows.plugins.office.excel.uca.cell;

import io.horizon.eon.VString;
import io.vertx.core.json.JsonObject;
import io.vertx.up.eon.KName;
import io.vertx.up.util.Ut;
import io.zerows.plugins.office.excel.eon.ExConstant;

import java.util.concurrent.ConcurrentMap;

/**
 * @author lang : 2024-06-26
 */
public class DynamicPageValue implements ExValue {

    @Override
    public Object to(final Object value, final ConcurrentMap<String, String> paramMap) {
        final String[] pathArr = value.toString().split(VString.COLON);
        String literal = value.toString();
        if (2 == pathArr.length) {
            final String path = pathArr[1];
            if (Ut.isNotNil(path)) {
                final JsonObject valueJ = new JsonObject();
                valueJ.put(ExConstant.K_TYPE, ExConstant.CELL.P_PAGE);

                final JsonObject content = new JsonObject();
                content.put(KName.App.PATH, path);
                valueJ.put(ExConstant.K_CONTENT, content);
                literal = valueJ.encodePrettily();
                this.logger().info("[ Έξοδος ] （ExJson）Page = {0}, Page Value built `{1}`", path, literal);
            }
        }
        return literal;
    }
}

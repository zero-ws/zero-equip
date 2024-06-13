package io.zerows.plugins.office.excel.uca.cell;

import io.horizon.eon.VName;
import io.horizon.eon.VPath;
import io.horizon.eon.VString;
import io.horizon.eon.VValue;
import io.horizon.exception.web._501NotSupportException;
import io.vertx.core.json.JsonArray;
import io.vertx.up.eon.KName;
import io.vertx.up.util.Ut;
import io.zerows.plugins.office.excel.eon.ExConstant;

import java.io.File;
import java.util.concurrent.ConcurrentMap;

/**
 * Processing
 * - CODE:config
 * - NAME:config
 * - CODE:NAME:config
 */
public class ExprValue implements ExValue {

    @Override
    @SuppressWarnings("all")
    public Object to(final Object value, final ConcurrentMap<String, String> paramMap) {
        final String valueExpr = value.toString();
        final String path = this.getPath(valueExpr, paramMap);
        if (valueExpr.endsWith(VName.CLASS)) {
            final JsonArray content = Ut.ioJArray(path);
            if (VValue.ONE == content.size()) {
                return content.getString(VValue.IDX);
            } else {
                return value;
            }
        } else if (valueExpr.endsWith(VName.CONFIG)) {
            // 此处直接提取字符串
            return Ut.ioString(path);
        } else {
            return value;
        }
    }

    private String getPath(final String value, final ConcurrentMap<String, String> paramMap) {
        final String pathRoot = paramMap.get(KName.DIRECTORY);
        final String name = paramMap.get(KName.NAME);
        final String code = paramMap.get(KName.CODE);
        final String field = paramMap.get(KName.FIELD);

        final String filepath;
        if (ExConstant.CELL.CODE_CONFIG.equals(value)) {
            // CODE:config
            filepath = Ut.ioPath(pathRoot, code);
        } else if (ExConstant.CELL.NAME_CONFIG.equals(value)) {
            // NAME:config
            filepath = Ut.ioPath(pathRoot, name);
        } else if (ExConstant.CELL.CODE_NAME_CONFIG.equals(value)) {
            // CODE:NAME:config
            filepath = Ut.ioPath(pathRoot, code) + File.pathSeparator + name;
        } else if (ExConstant.CELL.CODE_CLASS.equals(value)) {
            // CODE:class
            filepath = Ut.ioPath(pathRoot, code);
        } else {
            throw Ut.Bnd.failureWeb(_501NotSupportException.class, this.getClass());
        }
        return Ut.ioPath(filepath, field) + VString.DOT + VPath.SUFFIX.JSON;
    }
}

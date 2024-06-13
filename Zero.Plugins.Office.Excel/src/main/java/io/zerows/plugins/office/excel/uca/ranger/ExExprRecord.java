package io.zerows.plugins.office.excel.uca.ranger;

import io.vertx.core.json.JsonObject;
import io.vertx.up.eon.KName;
import io.zerows.plugins.office.excel.atom.ExRecord;
import io.zerows.plugins.office.excel.eon.ExConstant;
import io.zerows.plugins.office.excel.uca.cell.ExValue;
import io.zerows.plugins.office.excel.uca.cell.PureValue;

import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;

/**
 * @author lang : 2024-06-13
 */
class ExExprRecord implements ExExpr {
    @Override
    public JsonObject parse(final ExRecord record) {
        final ConcurrentMap<String, String> paramMap = new ConcurrentHashMap<>();
        paramMap.put(KName.DIRECTORY, record.refTable().getDirectory());
        paramMap.put(KName.NAME, record.get(KName.NAME));
        paramMap.put(KName.CODE, record.get(KName.CODE));


        // CURRENT 可以直接从 ExRecord -> ExTable 中提取，此处可忽略，主要是先构造 paramMap（第三参）
        record.keySet().forEach(field -> {
            final Object value = record.get(field);
            final ExValue component = this.pickComponent(value);
            // 必须传递 field
            paramMap.put(KName.FIELD, field);
            final Object result = component.to(value, paramMap);
            record.put(field, result);
        });
        return record.toJson();
    }

    private ExValue pickComponent(final Object value) {
        if (Objects.isNull(value)) {
            // null -> PureValue
            return ExValue.of(PureValue::new);
        }

        final String valueLiteral = value.toString().trim();
        /*
         * MATCH ->
         * - {UUID}
         * - PWD
         * - CODE:config
         * - NAME:config
         * - CODE:class
         * - CODE:NAME:config
         */
        final Supplier<ExValue> valueFn = Meta.FN_MATCH.get(valueLiteral);
        if (Objects.nonNull(valueFn)) {
            return ExValue.of(valueFn);
        }

        /*
         * PREFIX ->
         * - JSON:
         * - FILE:
         */
        final String found = Arrays.stream(ExConstant.CELL.PREFIX)
            .filter(valueLiteral::startsWith)
            .findFirst().orElse(null);
        if (Objects.isNull(found)) {
            return ExValue.of(PureValue::new);
        } else {
            return ExValue.of(Meta.FN_PREFIX.get(found));
        }
    }
}

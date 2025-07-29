package io.zerows.plugins.common.trash;

import io.horizon.uca.cache.Cc;
import io.vertx.core.Vertx;
import io.vertx.up.annotations.Infusion;
import io.vertx.up.eon.configure.YmlCore;
import io.zerows.core.metadata.zdk.plugins.Infix;

@Infusion
@SuppressWarnings("all")
public class TrashInfix implements Infix {

    private static final String NAME = "ZERO_TRASH_POOL";
    private static final Cc<String, TrashPlatform> CC_CLIENT = Cc.open();

    private static void initInternal(final Vertx vertx,
                                     final String name) {
        CC_CLIENT.pick(() -> Infix.init(YmlCore.inject.TRASH,
            (config) -> TrashPlatform.createShared(vertx, config),
            TrashPlatform.class), name);
    }

    public static void init(final Vertx vertx) {
        initInternal(vertx, NAME);
    }

    public static TrashPlatform getClient() {
        return CC_CLIENT.store(NAME);
    }

    @Override
    public TrashPlatform get() {
        return getClient();
    }
}

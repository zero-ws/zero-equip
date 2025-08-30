package io.zerows.plugins.store.neo4j;

import io.vertx.core.Vertx;
import io.zerows.core.annotations.Infusion;
import io.zerows.core.constant.configure.YmlCore;
import io.zerows.core.uca.cache.Cc;
import io.zerows.module.metadata.zdk.plugins.Infix;

@Infusion
@SuppressWarnings("all")
public class Neo4jInfix implements Infix {

    private static final String NAME = "ZERO_NEO4J_POOL";
    private static final Cc<String, Neo4jClient> CC_CLIENT = Cc.open();

    private static void initInternal(final Vertx vertx, final String name) {
        CC_CLIENT.pick(() -> Infix.init(YmlCore.inject.NEO4J,
            config -> Neo4jClient.createShared(vertx, config),
            Neo4jInfix.class), name);
    }

    public static void init(final Vertx vertx) {
        initInternal(vertx, NAME);
    }

    public static Neo4jClient getClient() {
        return CC_CLIENT.store(NAME);
    }

    @Override
    public Neo4jClient get() {
        return getClient();
    }

}

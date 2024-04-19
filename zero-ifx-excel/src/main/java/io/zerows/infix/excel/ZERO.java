package io.zerows.infix.excel;

import io.vertx.boot.extension.KConnect;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

interface Pool {

    ConcurrentMap<String, KConnect> CONNECTS = new ConcurrentHashMap<>();
}

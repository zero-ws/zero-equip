package io.zerows.infix.excel;

import io.zerows.core.web.metadata.extension.KConnect;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

interface Pool {

    ConcurrentMap<String, KConnect> CONNECTS = new ConcurrentHashMap<>();
}

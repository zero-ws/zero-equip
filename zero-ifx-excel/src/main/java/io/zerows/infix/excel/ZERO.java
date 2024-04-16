package io.zerows.infix.excel;

import io.zerows.macro.plugin.booting.KConnect;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

interface Pool {

    ConcurrentMap<String, KConnect> CONNECTS = new ConcurrentHashMap<>();
}

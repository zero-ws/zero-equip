package io.zerows.plugins.office.excel;

import io.zerows.core.web.model.atom.io.modeling.MDConnect;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

interface Pool {

    ConcurrentMap<String, MDConnect> CONNECTS = new ConcurrentHashMap<>();
}

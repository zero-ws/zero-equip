package io.zerows.plugins.office.excel;

import io.horizon.eon.VPath;
import io.horizon.eon.VString;
import io.horizon.uca.cache.Cc;
import io.horizon.uca.log.Annal;
import io.modello.specification.meta.HMetaAtom;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.up.eon.KName;
import io.vertx.up.eon.configure.YmlCore;
import io.vertx.up.fn.Fn;
import io.vertx.up.unity.Ux;
import io.vertx.up.util.Ut;
import io.zerows.core.metadata.uca.logging.OLog;
import io.zerows.core.web.model.atom.io.MDConfiguration;
import io.zerows.core.web.model.atom.io.modeling.MDConnect;
import io.zerows.core.web.model.extension.HExtension;
import io.zerows.core.web.model.store.module.OCacheExtension;
import io.zerows.core.web.model.uca.normalize.EquipAt;
import io.zerows.core.web.model.uca.normalize.Oneness;
import io.zerows.core.web.model.uca.normalize.Replacer;
import io.zerows.plugins.office.excel.atom.ExRecord;
import io.zerows.plugins.office.excel.atom.ExTable;
import io.zerows.plugins.office.excel.atom.ExTenant;
import io.zerows.plugins.office.excel.exception._404ExcelFileNullException;
import io.zerows.plugins.office.excel.ranger.ExBound;
import io.zerows.plugins.office.excel.ranger.RowBound;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

;

/*
 * Excel Helper to help ExcelClient to do some object building
 */
class ExcelHelper {

    private static final OLog LOGGER = Ut.Log.plugin(ExcelHelper.class);

    private static final Cc<String, ExcelHelper> CC_HELPER = Cc.open();
    private static final Cc<String, Workbook> CC_WORKBOOK = Cc.open();
    private static final Cc<Integer, Workbook> CC_WORKBOOK_STREAM = Cc.open();
    private static final Map<String, Workbook> REFERENCES = new ConcurrentHashMap<>();
    private transient final Class<?> target;
    private transient ExTpl tpl;
    private transient ExTenant tenant;

    private ExcelHelper(final Class<?> target) {
        this.target = target;
    }

    static ExcelHelper helper(final Class<?> target) {
        return CC_HELPER.pick(() -> new ExcelHelper(target), target.getName());
        // Fn.po?l(Pool.HELPERS, target.getName(), () -> new ExcelHelper(target));
    }

    Future<JsonArray> extract(final Set<ExTable> tables) {
        final List<Future<JsonArray>> futures = new ArrayList<>();
        tables.forEach(table -> futures.add(this.extract(table)));
        return Fn.compressA(futures);
    }

    /*
     * {
     *      "source",
     *      "mapping"
     * }
     */
    private Future<JsonArray> extractDynamic(final JsonArray dataArray, final String name) {
        /* Source Processing */
        if (Objects.isNull(this.tenant)) {
            return Ux.future(dataArray);
        } else {
            final JsonArray normalized;
            final JsonObject valueDefault = this.tenant.valueDefault();
            if (Ut.isNotNil(valueDefault)) {
                normalized = new JsonArray();
                // Append Global
                Ut.itJArray(dataArray).forEach(json -> normalized.add(valueDefault.copy().mergeIn(json, true)));
            } else {
                normalized = dataArray.copy();
            }

            // Extract Mapping
            final ConcurrentMap<String, String> first = this.tenant.dictionaryDefinition(name);
            if (first.isEmpty()) {
                return Ux.future(normalized);
            } else {
                // Directory
                return this.tenant.dictionary().compose(dataMap -> {
                    if (!dataMap.isEmpty()) {
                        /*
                         * mapping
                         * field = name
                         * dataMap
                         * name = JsonObject ( from = to )
                         * --->
                         *
                         * field -> JsonObject
                         */
                        final ConcurrentMap<String, JsonObject> combine
                            = Ut.elementZip(first, dataMap);

                        combine.forEach((key, value) -> Ut.itJArray(normalized).forEach(json -> {
                            final String fromValue = json.getString(key);
                            if (Ut.isNotNil(fromValue) && value.containsKey(fromValue)) {
                                final Object toValue = value.getValue(fromValue);
                                // Replace
                                json.put(key, toValue);
                            }
                        }));
                    }
                    return Ux.future(normalized);
                });
            }
        }
    }

    /*
     * static
     * {
     *      "dictionary"
     * }
     */
    private JsonArray extractStatic(final JsonArray dataArray, final String name) {
        final ConcurrentMap<String, ConcurrentMap<String, String>> tree = this.tenant.tree(name);
        if (!tree.isEmpty()) {
            tree.forEach((field, map) -> Ut.itJArray(dataArray).forEach(record -> {
                final String input = record.getString(field);
                if (map.containsKey(input)) {
                    final String output = map.get(input);
                    record.put(field, output);
                }
            }));
        }
        return dataArray;
    }

    private Future<JsonArray> extractForbidden(final JsonArray dataArray, final String name) {
        final ConcurrentMap<String, Set<String>> forbidden = this.tenant.valueCriteria(name);
        if (forbidden.isEmpty()) {
            return Ux.future(dataArray);
        } else {
            final JsonArray normalized = new JsonArray();
            Ut.itJArray(dataArray).filter(item -> forbidden.keySet().stream().allMatch(field -> {
                if (item.containsKey(field)) {
                    final Set<String> values = forbidden.get(field);
                    final String value = item.getString(field);
                    return !values.contains(value);
                } else {
                    return true;
                }
            })).forEach(normalized::add);
            return Ux.future(normalized);
        }
    }

    Future<JsonArray> extract(final ExTable table) {
        /* Records extracting */
        final List<ExRecord> records = table.get();
        final String tableName = table.getName();
        /* Pojo Processing */
        final JsonArray dataArray = new JsonArray();
        records.stream().filter(Objects::nonNull)
            .map(ExRecord::toJson)
            .forEach(dataArray::add);

        /* dictionary for static part */
        return Ux.future(this.extractStatic(dataArray, tableName))
            /* dictionary for dynamic part */
            .compose(extracted -> this.extractDynamic(extracted, tableName))
            /* forbidden record filter */
            .compose(extracted -> this.extractForbidden(extracted, tableName));
    }

    private void extractIngest(final Set<ExTable> dataSet) {
        if (Objects.nonNull(this.tenant)) {
            final JsonObject dataGlobal = this.tenant.valueDefault();
            if (Ut.isNotNil(dataGlobal)) {
                /*
                 * New for developer account importing cross different
                 * apps
                 * {
                 *     "developer":
                 * }
                 */
                final JsonObject developer = Ut.valueJObject(dataGlobal, KName.DEVELOPER).copy();
                final JsonObject normalized = dataGlobal.copy();
                normalized.remove(KName.DEVELOPER);
                dataSet.forEach(table -> {
                    // Developer Checking
                    if ("S_USER".equals(table.getName()) && Ut.isNotNil(developer)) {
                        // JsonObject ( user = employeeId )
                        table.get().forEach(record -> {
                            // Mount Global Data
                            record.putOr(normalized);
                            // EmployeeId Replacement for `lang.yu` or other developer account
                            final String username = record.get(KName.USERNAME);
                            if (developer.containsKey(username)) {
                                record.put(KName.MODEL_KEY, developer.getString(username));
                            }
                        });
                    } else {
                        // Mount Global Data into the ingest data.
                        table.get().forEach(record -> record.putOr(normalized));
                    }
                });
            }
        }
    }

    /*
     * Read file from path to build Excel Workbook object.
     * If this function get null dot file or file object, zero system
     * will throw exception out.
     */
    @SuppressWarnings("all")
    Workbook getWorkbook(final String filename) {
        Fn.outWeb(null == filename, _404ExcelFileNullException.class, this.target, filename);
        /*
         * Here the InputStream directly from
         */
        final InputStream in = Ut.ioStream(filename, getClass());
        Fn.outWeb(null == in, _404ExcelFileNullException.class, this.target, filename);
        final Workbook workbook;
        if (filename.endsWith(VPath.SUFFIX.EXCEL_2003)) {
            workbook = CC_WORKBOOK.pick(() -> Fn.failOr(() -> new HSSFWorkbook(in)), filename);
            // Fn.po?l(Pool.WORKBOOKS, filename, () -> Fn.getJvm(() -> new HSSFWorkbook(in)));
        } else {
            workbook = CC_WORKBOOK.pick(() -> Fn.failOr(() -> new XSSFWorkbook(in)), filename);
            // Fn.po?l(Pool.WORKBOOKS, filename, () -> Fn.getJvm(() -> new XSSFWorkbook(in)));
        }
        return workbook;
    }

    @SuppressWarnings("all")
    Workbook getWorkbook(final InputStream in, final boolean isXlsx) {
        Fn.outWeb(null == in, _404ExcelFileNullException.class, this.target, "Stream");
        final Workbook workbook;
        if (isXlsx) {
            workbook = CC_WORKBOOK_STREAM.pick(() -> Fn.failOr(() -> new XSSFWorkbook(in)), in.hashCode());
            // Fn.po?l(Pool.WORKBOOKS_STREAM, in.hashCode(), () -> Fn.getJvm(() -> new XSSFWorkbook(in)));
        } else {
            workbook = CC_WORKBOOK_STREAM.pick(() -> Fn.failOr(() -> new HSSFWorkbook(in)), in.hashCode());
            // Fn.po?l(Pool.WORKBOOKS_STREAM, in.hashCode(), () -> Fn.getJvm(() -> new HSSFWorkbook(in)));
        }
        /* Force to recalculation for evaluator */
        workbook.setForceFormulaRecalculation(Boolean.TRUE);
        return workbook;
    }

    /*
     * Get Set<ExSheet> collection based on workbook
     */
    Set<ExTable> getExTables(final Workbook workbook, final HMetaAtom metaAtom) {
        return Fn.runOr(new HashSet<>(), () -> {
            /* FormulaEvaluator reference */
            final FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();

            /*
             * Workbook pool for FormulaEvaluator
             * 1）Local variable to replace global
             **/
            final Map<String, FormulaEvaluator> references = new ConcurrentHashMap<>();
            REFERENCES.forEach((field, workbookRef) -> {
                /*
                 * Reference executor processing
                 * Here you must put self reference evaluator and all related here.
                 * It should fix issue: Could not set environment etc.
                 */
                final FormulaEvaluator executorRef = workbookRef.getCreationHelper().createFormulaEvaluator();
                references.put(field, executorRef);
            });
            /*
             * Self evaluator for current calculation
             */
            references.put(workbook.createName().getNameName(), evaluator);

            /*
             * Above one line code resolved following issue:
             * org.apache.poi.ss.formula.CollaboratingWorkbooksEnvironment$WorkbookNotFoundException:
             * Could not resolve external workbook name 'environment.ambient.xlsx'. Workbook environment has not been set up.
             */
            evaluator.setupReferencedWorkbooks(references);
            /*
             * Sheet process
             */
            final Iterator<Sheet> it = workbook.sheetIterator();
            final Set<ExTable> sheets = new HashSet<>();
            while (it.hasNext()) {
                /* Build temp ExSheet */
                final Sheet sheet = it.next();
                /* Build Range ( Row Start - End ) */
                final ExBound range = new RowBound(sheet);

                final SheetAnalyzer exSheet = new SheetAnalyzer(sheet).on(evaluator);
                /* Build Set */
                final Set<ExTable> dataSet = exSheet.analyzed(range, metaAtom);
                /*
                 * Here for critical injection, mount the data of
                 * {
                 *      "global": {
                 *      }
                 * }
                 * */
                this.extractIngest(dataSet);
                sheets.addAll(dataSet);
            }
            return sheets;
        }, workbook);
    }

    void brush(final Workbook workbook, final Sheet sheet, final HMetaAtom metaAtom) {
        if (Objects.nonNull(this.tpl)) {
            this.tpl.bind(workbook);
            this.tpl.applyStyle(sheet, metaAtom);
        }
    }

    void initPen(final String componentStr) {
        if (Ut.isNotNil(componentStr)) {
            final Class<?> tplCls = Ut.clazz(componentStr, null);
            if (Ut.isImplement(tplCls, ExTpl.class)) {
                this.tpl = Ut.singleton(componentStr);
            }
        }
    }

    /**
     * 此处直接初始化环境中的连接配置信息，配置信息来源根据不同信息会有所区别：
     * <pre><code>
     *     Norm 环境（原始单机环境）
     *     1. 当前项目一般是启动器 Launcher，配置文件直接位于 src/main/resources 之下
     *     2. 旧版直接走 {@link HExtension}（每个模块一个）读取配置信息
     *     Osgi 环境
     *     1. 当前项目一般是一个独立 Bundle，配置文件依旧位于 src/main/resources 之下
     *     2. 读取配置时，直接读取当前环境中的配置信息
     * </code></pre>
     * 所以新版核心配置处理流程直接依赖 {@link OCacheExtension} 来实现，新版本的 vertx-excel.yml 配置会发生简单变化
     * <pre><code>
     * excel:
     *   pen: "io.zerows.plugins.office.excel.tpl.BlueTpl"
     *   environment:
     *     - name: environment.ambient.xlsx
     *       path: "init/oob/environment.ambient.xlsx"
     *       alias:
     *         - /src/main/resources/init/oob/environment.ambient.xlsx
     *   temp: /tmp/
     *   tenant: "init/environment.json"
     *   # 旧版
     *   mapping:
     *     # 导入内容处理
     *   # 新版
     *   mapping: "字符串格式，直接提供配置目录，此处配置目录为启动器或启动 Bundle 的目录"
     * </code></pre>
     *
     * @param excelJ 当前环境中的连接配置
     */
    void initConnect(final JsonObject excelJ) {
        final String configId = Ut.valueString(excelJ, "configuration");
        if (Ut.isNil(configId)) {
            LOGGER.warn("The excel configuration is wrong, please contact the administrator.");
            return;
        }
        /*
         * 初始化当前环境中的基本配置信息，启动器配置位于
         * plugins/<configId>/ 目录之下，配置目录结构为新版结构
         */
        final OCacheExtension extension = OCacheExtension.of();
        MDConfiguration configuration = extension.valueGet(configId);
        if (Objects.isNull(configuration)) {
            LOGGER.debug("[ Έξοδος ] Could not find configuration: id = {}, the system will build new one", configId);
            configuration = new MDConfiguration(configId);
        }
        final EquipAt component = EquipAt.of(configuration.id());
        component.initialize(configuration);            // 已执行初始化的情况下此处不会再执行


        /*
         * 额外 attached 的基础配置信息，在执行此处之前，已经执行过内置的反射扫描流程了，所以此处不再担心找不到 Table 的情况，如果此处
         * 找不到 table 证明扫描过程出了问题，而这里的构造流程是构造内部 MDConnect 相关信息，而且和实体无关，主要是附加相关内容到环境里
         * 如果是 OSGI 环境，除非是 APP 类型的 Bundle 会包含此配置，由于其他类型的 Bundle 没有 HSetting，自然不会包含此配置。
         */
        final JsonArray connectA = Ut.valueJArray(excelJ, KName.MAPPING);
        if (Ut.isNotNil(connectA)) {
            final Replacer<MDConnect> connectReplacer = Replacer.ofConnect();
            final List<MDConnect> connectList = connectReplacer.build(connectA);
            configuration.addConnect(connectList);
            LOGGER.debug("[ Έξοδος ] Configuration of connect: {} has been added into current environment: id = {}",
                connectList.size(), configuration.id().value());
        }
    }

    void initEnvironment(final JsonArray environments) {
        environments.stream().filter(Objects::nonNull)
            .map(item -> (JsonObject) item)
            .forEach(each -> {
                /*
                 * Build reference
                 */
                final String path = each.getString(YmlCore.excel.environment.PATH);
                /*
                 * Reference Evaluator
                 */
                final String name = each.getString(YmlCore.excel.environment.NAME);
                final Workbook workbook = this.getWorkbook(path);
                REFERENCES.put(name, workbook);
                this.initEnvironment(each, workbook);
            });
    }

    void initTenant(final ExTenant tenant) {
        this.tenant = tenant;
    }

    private void initEnvironment(final JsonObject each, final Workbook workbook) {
        /*
         * Alias Parsing
         */
        if (each.containsKey(YmlCore.excel.environment.ALIAS)) {
            final JsonArray alias = each.getJsonArray(YmlCore.excel.environment.ALIAS);
            final File current = new File(VString.EMPTY);
            Ut.itJArray(alias, String.class, (item, index) -> {
                final String filename = current.getAbsolutePath() + item;
                final File file = new File(filename);
                if (file.exists()) {
                    REFERENCES.put(file.getAbsolutePath(), workbook);
                }
            });
        }
    }

    /*
     * For Insert to avoid duplicated situation
     * 1. Key duplicated
     * 2. Unique duplicated
     */
    <T> List<T> compress(final List<T> input, final ExTable table) {

        final MDConnect connect = table.getConnect();
        final Oneness<MDConnect> oneness = Oneness.ofConnect();
        final String keyPrimary = oneness.keyPrimary(connect);

        if (Objects.isNull(keyPrimary)) {
            // Relation Table
            return input;
        }
        final List<T> keyList = new ArrayList<>();
        final Set<Object> keys = new HashSet<>();
        final AtomicInteger counter = new AtomicInteger(0);
        input.forEach(item -> {
            final Object value = Ut.field(item, keyPrimary);
            if (Objects.nonNull(value) && !keys.contains(value)) {
                keys.add(value);
                keyList.add(item);
            } else {
                counter.incrementAndGet();
            }
        });
        final int ignored = counter.get();
        if (0 < ignored) {
            final Annal annal = Annal.get(this.target);
            annal.warn("[ Έξοδος ] Ignore table `{0}` with size `{1}`", table.getName(), ignored);
        }
        // Entity Release
        return keyList;
    }
}

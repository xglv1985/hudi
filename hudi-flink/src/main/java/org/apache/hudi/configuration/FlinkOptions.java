/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hudi.configuration;

import org.apache.hudi.common.model.HoodieTableType;
import org.apache.hudi.common.model.OverwriteWithLatestAvroPayload;
import org.apache.hudi.config.HoodieWriteConfig;
import org.apache.hudi.exception.HoodieException;
import org.apache.hudi.hive.SlashEncodedDayPartitionValueExtractor;
import org.apache.hudi.keygen.SimpleAvroKeyGenerator;
import org.apache.hudi.keygen.constant.KeyGeneratorOptions;
import org.apache.hudi.streamer.FlinkStreamerConfig;
import org.apache.hudi.util.StreamerUtil;

import org.apache.flink.configuration.ConfigOption;
import org.apache.flink.configuration.ConfigOptions;
import org.apache.flink.configuration.Configuration;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Hoodie Flink config options.
 *
 * <p>It has the options for Hoodie table read and write. It also defines some utilities.
 */
public class FlinkOptions {
  private FlinkOptions() {
  }

  // ------------------------------------------------------------------------
  //  Base Options
  // ------------------------------------------------------------------------
  public static final ConfigOption<String> PATH = ConfigOptions
      .key("path")
      .stringType()
      .noDefaultValue()
      .withDescription("Base path for the target hoodie table.\n"
          + "The path would be created if it does not exist,\n"
          + "otherwise a Hoodie table expects to be initialized successfully");

  // ------------------------------------------------------------------------
  //  Common Options
  // ------------------------------------------------------------------------

  public static final ConfigOption<String> PARTITION_DEFAULT_NAME = ConfigOptions
      .key("partition.default_name")
      .stringType()
      .defaultValue("__DEFAULT_PARTITION__")
      .withDescription("The default partition name in case the dynamic partition"
          + " column value is null/empty string");

  // ------------------------------------------------------------------------
  //  Index Options
  // ------------------------------------------------------------------------
  public static final ConfigOption<Boolean> INDEX_BOOTSTRAP_ENABLED = ConfigOptions
      .key("index.bootstrap.enabled")
      .booleanType()
      .defaultValue(false)
      .withDescription("Whether to bootstrap the index state from existing hoodie table, default false");

  // ------------------------------------------------------------------------
  //  Read Options
  // ------------------------------------------------------------------------
  public static final ConfigOption<Integer> READ_TASKS = ConfigOptions
      .key("read.tasks")
      .intType()
      .defaultValue(4)
      .withDescription("Parallelism of tasks that do actual read, default is 4");

  public static final ConfigOption<String> READ_AVRO_SCHEMA_PATH = ConfigOptions
      .key("read.avro-schema.path")
      .stringType()
      .noDefaultValue()
      .withDescription("Avro schema file path, the parsed schema is used for deserialization");

  public static final ConfigOption<String> READ_AVRO_SCHEMA = ConfigOptions
      .key("read.avro-schema")
      .stringType()
      .noDefaultValue()
      .withDescription("Avro schema string, the parsed schema is used for deserialization");

  public static final String QUERY_TYPE_SNAPSHOT = "snapshot";
  public static final String QUERY_TYPE_READ_OPTIMIZED = "read_optimized";
  public static final String QUERY_TYPE_INCREMENTAL = "incremental";
  public static final ConfigOption<String> QUERY_TYPE = ConfigOptions
      .key("hoodie.datasource.query.type")
      .stringType()
      .defaultValue(QUERY_TYPE_SNAPSHOT)
      .withDescription("Decides how data files need to be read, in\n"
          + "1) Snapshot mode (obtain latest view, based on row & columnar data);\n"
          + "2) incremental mode (new data since an instantTime);\n"
          + "3) Read Optimized mode (obtain latest view, based on columnar data)\n."
          + "Default: snapshot");

  public static final String REALTIME_SKIP_MERGE = "skip_merge";
  public static final String REALTIME_PAYLOAD_COMBINE = "payload_combine";
  public static final ConfigOption<String> MERGE_TYPE = ConfigOptions
      .key("hoodie.datasource.merge.type")
      .stringType()
      .defaultValue(REALTIME_PAYLOAD_COMBINE)
      .withDescription("For Snapshot query on merge on read table. Use this key to define how the payloads are merged, in\n"
          + "1) skip_merge: read the base file records plus the log file records;\n"
          + "2) payload_combine: read the base file records first, for each record in base file, checks whether the key is in the\n"
          + "   log file records(combines the two records with same key for base and log file records), then read the left log file records");

  public static final ConfigOption<Boolean> HIVE_STYLE_PARTITION = ConfigOptions
      .key("hoodie.datasource.hive_style_partition")
      .booleanType()
      .defaultValue(false)
      .withDescription("Whether the partition path is with Hive style, e.g. '{partition key}={partition value}', default false");

  public static final ConfigOption<Boolean> UTC_TIMEZONE = ConfigOptions
      .key("read.utc-timezone")
      .booleanType()
      .defaultValue(true)
      .withDescription("Use UTC timezone or local timezone to the conversion between epoch"
          + " time and LocalDateTime. Hive 0.x/1.x/2.x use local timezone. But Hive 3.x"
          + " use UTC timezone, by default true");

  public static final ConfigOption<Boolean> READ_AS_STREAMING = ConfigOptions
      .key("read.streaming.enabled")
      .booleanType()
      .defaultValue(false)// default read as batch
      .withDescription("Whether to read as streaming source, default false");

  public static final ConfigOption<Integer> READ_STREAMING_CHECK_INTERVAL = ConfigOptions
      .key("read.streaming.check-interval")
      .intType()
      .defaultValue(60)// default 1 minute
      .withDescription("Check interval for streaming read of SECOND, default 1 minute");

  public static final ConfigOption<String> READ_STREAMING_START_COMMIT = ConfigOptions
      .key("read.streaming.start-commit")
      .stringType()
      .noDefaultValue()
      .withDescription("Start commit instant for streaming read, the commit time format should be 'yyyyMMddHHmmss', "
          + "by default reading from the latest instant");

  // ------------------------------------------------------------------------
  //  Write Options
  // ------------------------------------------------------------------------
  public static final ConfigOption<String> TABLE_NAME = ConfigOptions
      .key(HoodieWriteConfig.TABLE_NAME)
      .stringType()
      .noDefaultValue()
      .withDescription("Table name to register to Hive metastore");

  public static final String TABLE_TYPE_COPY_ON_WRITE = HoodieTableType.COPY_ON_WRITE.name();
  public static final String TABLE_TYPE_MERGE_ON_READ = HoodieTableType.MERGE_ON_READ.name();
  public static final ConfigOption<String> TABLE_TYPE = ConfigOptions
      .key("table.type")
      .stringType()
      .defaultValue(TABLE_TYPE_COPY_ON_WRITE)
      .withDescription("Type of table to write. COPY_ON_WRITE (or) MERGE_ON_READ");

  public static final ConfigOption<String> OPERATION = ConfigOptions
      .key("write.operation")
      .stringType()
      .defaultValue("upsert")
      .withDescription("The write operation, that this write should do");

  public static final ConfigOption<String> PRECOMBINE_FIELD = ConfigOptions
      .key("write.precombine.field")
      .stringType()
      .defaultValue("ts")
      .withDescription("Field used in preCombining before actual write. When two records have the same\n"
          + "key value, we will pick the one with the largest value for the precombine field,\n"
          + "determined by Object.compareTo(..)");

  public static final ConfigOption<String> PAYLOAD_CLASS = ConfigOptions
      .key("write.payload.class")
      .stringType()
      .defaultValue(OverwriteWithLatestAvroPayload.class.getName())
      .withDescription("Payload class used. Override this, if you like to roll your own merge logic, when upserting/inserting.\n"
          + "This will render any value set for the option in-effective");

  /**
   * Flag to indicate whether to drop duplicates upon insert.
   * By default insert will accept duplicates, to gain extra performance.
   */
  public static final ConfigOption<Boolean> INSERT_DROP_DUPS = ConfigOptions
      .key("write.insert.drop.duplicates")
      .booleanType()
      .defaultValue(false)
      .withDescription("Flag to indicate whether to drop duplicates upon insert.\n"
          + "By default insert will accept duplicates, to gain extra performance");

  public static final ConfigOption<Integer> RETRY_TIMES = ConfigOptions
      .key("write.retry.times")
      .intType()
      .defaultValue(3)
      .withDescription("Flag to indicate how many times streaming job should retry for a failed checkpoint batch.\n"
          + "By default 3");

  public static final ConfigOption<Long> RETRY_INTERVAL_MS = ConfigOptions
      .key("write.retry.interval.ms")
      .longType()
      .defaultValue(2000L)
      .withDescription("Flag to indicate how long (by millisecond) before a retry should issued for failed checkpoint batch.\n"
          + "By default 2000 and it will be doubled by every retry");

  public static final ConfigOption<Boolean> IGNORE_FAILED = ConfigOptions
      .key("write.ignore.failed")
      .booleanType()
      .defaultValue(true)
      .withDescription("Flag to indicate whether to ignore any non exception error (e.g. writestatus error). within a checkpoint batch.\n"
          + "By default true (in favor of streaming progressing over data integrity)");

  public static final ConfigOption<String> RECORD_KEY_FIELD = ConfigOptions
      .key(KeyGeneratorOptions.RECORDKEY_FIELD_OPT_KEY)
      .stringType()
      .defaultValue("uuid")
      .withDescription("Record key field. Value to be used as the `recordKey` component of `HoodieKey`.\n"
          + "Actual value will be obtained by invoking .toString() on the field value. Nested fields can be specified using "
          + "the dot notation eg: `a.b.c`");

  public static final ConfigOption<String> PARTITION_PATH_FIELD = ConfigOptions
      .key(KeyGeneratorOptions.PARTITIONPATH_FIELD_OPT_KEY)
      .stringType()
      .defaultValue("partition-path")
      .withDescription("Partition path field. Value to be used at the `partitionPath` component of `HoodieKey`.\n"
          + "Actual value obtained by invoking .toString()");

  public static final ConfigOption<Boolean> PARTITION_PATH_URL_ENCODE = ConfigOptions
      .key("write.partition.url_encode")
      .booleanType()
      .defaultValue(false)
      .withDescription("Whether to encode the partition path url, default false");

  public static final ConfigOption<String> KEYGEN_CLASS = ConfigOptions
      .key(HoodieWriteConfig.KEYGENERATOR_CLASS_PROP)
      .stringType()
      .defaultValue(SimpleAvroKeyGenerator.class.getName())
      .withDescription("Key generator class, that implements will extract the key out of incoming record");

  public static final ConfigOption<Integer> WRITE_TASKS = ConfigOptions
      .key("write.tasks")
      .intType()
      .defaultValue(4)
      .withDescription("Parallelism of tasks that do actual write, default is 4");

  public static final ConfigOption<Double> WRITE_BATCH_SIZE = ConfigOptions
      .key("write.batch.size.MB")
      .doubleType()
      .defaultValue(64D) // 64MB
      .withDescription("Batch buffer size in MB to flush data into the underneath filesystem, default 64MB");

  public static final ConfigOption<Integer> WRITE_LOG_BLOCK_SIZE = ConfigOptions
      .key("write.log_block.size.MB")
      .intType()
      .defaultValue(128)
      .withDescription("Max log block size in MB for log file, default 128MB");

  public static final ConfigOption<Integer> WRITE_MERGE_MAX_MEMORY = ConfigOptions
      .key("write.merge.max_memory")
      .intType()
      .defaultValue(100) // default 100 MB
      .withDescription("Max memory in MB for merge, default 100MB");

  // ------------------------------------------------------------------------
  //  Compaction Options
  // ------------------------------------------------------------------------

  public static final ConfigOption<Boolean> COMPACTION_ASYNC_ENABLED = ConfigOptions
      .key("compaction.async.enabled")
      .booleanType()
      .defaultValue(true) // default true for MOR write
      .withDescription("Async Compaction, enabled by default for MOR");

  public static final ConfigOption<Integer> COMPACTION_TASKS = ConfigOptions
      .key("compaction.tasks")
      .intType()
      .defaultValue(10) // default WRITE_TASKS * COMPACTION_DELTA_COMMITS * 0.5 (assumes two commits generate one bucket)
      .withDescription("Parallelism of tasks that do actual compaction, default is 10");

  public static final String NUM_COMMITS = "num_commits";
  public static final String TIME_ELAPSED = "time_elapsed";
  public static final String NUM_AND_TIME = "num_and_time";
  public static final String NUM_OR_TIME = "num_or_time";
  public static final ConfigOption<String> COMPACTION_TRIGGER_STRATEGY = ConfigOptions
      .key("compaction.trigger.strategy")
      .stringType()
      .defaultValue(NUM_COMMITS) // default true for MOR write
      .withDescription("Strategy to trigger compaction, options are 'num_commits': trigger compaction when reach N delta commits;\n"
          + "'time_elapsed': trigger compaction when time elapsed > N seconds since last compaction;\n"
          + "'num_and_time': trigger compaction when both NUM_COMMITS and TIME_ELAPSED are satisfied;\n"
          + "'num_or_time': trigger compaction when NUM_COMMITS or TIME_ELAPSED is satisfied.\n"
          + "Default is 'num_commits'");

  public static final ConfigOption<Integer> COMPACTION_DELTA_COMMITS = ConfigOptions
      .key("compaction.delta_commits")
      .intType()
      .defaultValue(5)
      .withDescription("Max delta commits needed to trigger compaction, default 5 commits");

  public static final ConfigOption<Integer> COMPACTION_DELTA_SECONDS = ConfigOptions
      .key("compaction.delta_seconds")
      .intType()
      .defaultValue(3600) // default 1 hour
      .withDescription("Max delta seconds time needed to trigger compaction, default 1 hour");

  public static final ConfigOption<Integer> COMPACTION_MAX_MEMORY = ConfigOptions
      .key("compaction.max_memory")
      .intType()
      .defaultValue(100) // default 100 MB
      .withDescription("Max memory in MB for compaction spillable map, default 100MB");

  public static final ConfigOption<Boolean> CLEAN_ASYNC_ENABLED = ConfigOptions
      .key("clean.async.enabled")
      .booleanType()
      .defaultValue(true)
      .withDescription("Whether to cleanup the old commits immediately on new commits, enabled by default");

  public static final ConfigOption<Integer> CLEAN_RETAIN_COMMITS = ConfigOptions
      .key("clean.retain_commits")
      .intType()
      .defaultValue(10)// default 10 commits
      .withDescription("Number of commits to retain. So data will be retained for num_of_commits * time_between_commits (scheduled).\n"
          + "This also directly translates into how much you can incrementally pull on this table, default 10");

  // ------------------------------------------------------------------------
  //  Hive Sync Options
  // ------------------------------------------------------------------------
  public static final ConfigOption<Boolean> HIVE_SYNC_ENABLED = ConfigOptions
      .key("hive_sync.enable")
      .booleanType()
      .defaultValue(false)
      .withDescription("Asynchronously sync Hive meta to HMS, default false");

  public static final ConfigOption<String> HIVE_SYNC_DB = ConfigOptions
      .key("hive_sync.db")
      .stringType()
      .defaultValue("default")
      .withDescription("Database name for hive sync, default 'default'");

  public static final ConfigOption<String> HIVE_SYNC_TABLE = ConfigOptions
      .key("hive_sync.table")
      .stringType()
      .defaultValue("unknown")
      .withDescription("Table name for hive sync, default 'unknown'");

  public static final ConfigOption<String> HIVE_SYNC_FILE_FORMAT = ConfigOptions
      .key("hive_sync.file_format")
      .stringType()
      .defaultValue("PARQUET")
      .withDescription("File format for hive sync, default 'PARQUET'");

  public static final ConfigOption<String> HIVE_SYNC_USERNAME = ConfigOptions
      .key("hive_sync.username")
      .stringType()
      .defaultValue("hive")
      .withDescription("Username for hive sync, default 'hive'");

  public static final ConfigOption<String> HIVE_SYNC_PASSWORD = ConfigOptions
      .key("hive_sync.password")
      .stringType()
      .defaultValue("hive")
      .withDescription("Password for hive sync, default 'hive'");

  public static final ConfigOption<String> HIVE_SYNC_JDBC_URL = ConfigOptions
      .key("hive_sync.jdbc_url")
      .stringType()
      .defaultValue("jdbc:hive2://localhost:10000")
      .withDescription("Jdbc URL for hive sync, default 'jdbc:hive2://localhost:10000'");

  public static final ConfigOption<String> HIVE_SYNC_PARTITION_FIELDS = ConfigOptions
      .key("hive_sync.partition_fields")
      .stringType()
      .defaultValue("")
      .withDescription("Partition fields for hive sync, default ''");

  public static final ConfigOption<String> HIVE_SYNC_PARTITION_EXTRACTOR_CLASS = ConfigOptions
      .key("hive_sync.partition_extractor_class")
      .stringType()
      .defaultValue(SlashEncodedDayPartitionValueExtractor.class.getCanonicalName())
      .withDescription("Tool to extract the partition value from HDFS path, "
          + "default 'SlashEncodedDayPartitionValueExtractor'");

  public static final ConfigOption<Boolean> HIVE_SYNC_ASSUME_DATE_PARTITION = ConfigOptions
      .key("hive_sync.assume_date_partitioning")
      .booleanType()
      .defaultValue(false)
      .withDescription("Assume partitioning is yyyy/mm/dd, default false");

  public static final ConfigOption<Boolean> HIVE_SYNC_USE_JDBC = ConfigOptions
      .key("hive_sync.use_jdbc")
      .booleanType()
      .defaultValue(true)
      .withDescription("Use JDBC when hive synchronization is enabled, default true");

  public static final ConfigOption<Boolean> HIVE_SYNC_AUTO_CREATE_DB = ConfigOptions
      .key("hive_sync.auto_create_db")
      .booleanType()
      .defaultValue(true)
      .withDescription("Auto create hive database if it does not exists, default true");

  public static final ConfigOption<Boolean> HIVE_SYNC_IGNORE_EXCEPTIONS = ConfigOptions
      .key("hive_sync.ignore_exceptions")
      .booleanType()
      .defaultValue(false)
      .withDescription("Ignore exceptions during hive synchronization, default false");

  public static final ConfigOption<Boolean> HIVE_SYNC_SKIP_RO_SUFFIX = ConfigOptions
      .key("hive_sync.skip_ro_suffix")
      .booleanType()
      .defaultValue(false)
      .withDescription("Skip the _ro suffix for Read optimized table when registering, default false");

  public static final ConfigOption<Boolean> HIVE_SYNC_SUPPORT_TIMESTAMP = ConfigOptions
      .key("hive_sync.support_timestamp")
      .booleanType()
      .defaultValue(false)
      .withDescription("INT64 with original type TIMESTAMP_MICROS is converted to hive timestamp type.\n"
          + "Disabled by default for backward compatibility.");

  // -------------------------------------------------------------------------
  //  Utilities
  // -------------------------------------------------------------------------

  // Prefix for Hoodie specific properties.
  private static final String PROPERTIES_PREFIX = "properties.";

  /**
   * Transforms a {@code HoodieFlinkStreamer.Config} into {@code Configuration}.
   * The latter is more suitable for the table APIs. It reads all the properties
   * in the properties file (set by `--props` option) and cmd line options
   * (set by `--hoodie-conf` option).
   */
  @SuppressWarnings("unchecked, rawtypes")
  public static org.apache.flink.configuration.Configuration fromStreamerConfig(FlinkStreamerConfig config) {
    Map<String, String> propsMap = new HashMap<String, String>((Map) StreamerUtil.getProps(config));
    org.apache.flink.configuration.Configuration conf = fromMap(propsMap);

    conf.setString(FlinkOptions.PATH, config.targetBasePath);
    conf.setString(READ_AVRO_SCHEMA_PATH, config.readSchemaFilePath);
    conf.setString(FlinkOptions.TABLE_NAME, config.targetTableName);
    // copy_on_write works same as COPY_ON_WRITE
    conf.setString(FlinkOptions.TABLE_TYPE, config.tableType.toUpperCase());
    conf.setString(FlinkOptions.OPERATION, config.operation.value());
    conf.setString(FlinkOptions.PRECOMBINE_FIELD, config.sourceOrderingField);
    conf.setString(FlinkOptions.PAYLOAD_CLASS, config.payloadClassName);
    conf.setBoolean(FlinkOptions.INSERT_DROP_DUPS, config.filterDupes);
    conf.setInteger(FlinkOptions.RETRY_TIMES, Integer.parseInt(config.instantRetryTimes));
    conf.setLong(FlinkOptions.RETRY_INTERVAL_MS, Long.parseLong(config.instantRetryInterval));
    conf.setBoolean(FlinkOptions.IGNORE_FAILED, config.commitOnErrors);
    conf.setString(FlinkOptions.RECORD_KEY_FIELD, config.recordKeyField);
    conf.setString(FlinkOptions.PARTITION_PATH_FIELD, config.partitionPathField);
    conf.setString(FlinkOptions.KEYGEN_CLASS, config.keygenClass);
    conf.setInteger(FlinkOptions.WRITE_TASKS, config.writeTaskNum);

    return conf;
  }

  /**
   * Collects the config options that start with 'properties.' into a 'key'='value' list.
   */
  public static Map<String, String> getHoodieProperties(Map<String, String> options) {
    return getHoodiePropertiesWithPrefix(options, PROPERTIES_PREFIX);
  }

  /**
   * Collects the config options that start with specified prefix {@code prefix} into a 'key'='value' list.
   */
  public static Map<String, String> getHoodiePropertiesWithPrefix(Map<String, String> options, String prefix) {
    final Map<String, String> hoodieProperties = new HashMap<>();

    if (hasPropertyOptions(options)) {
      options.keySet().stream()
          .filter(key -> key.startsWith(PROPERTIES_PREFIX))
          .forEach(key -> {
            final String value = options.get(key);
            final String subKey = key.substring((prefix).length());
            hoodieProperties.put(subKey, value);
          });
    }
    return hoodieProperties;
  }

  /**
   * Collects all the config options, the 'properties.' prefix would be removed if the option key starts with it.
   */
  public static Configuration flatOptions(Configuration conf) {
    final Map<String, String> propsMap = new HashMap<>();

    conf.toMap().forEach((key, value) -> {
      final String subKey = key.startsWith(PROPERTIES_PREFIX)
          ? key.substring((PROPERTIES_PREFIX).length())
          : key;
      propsMap.put(subKey, value);
    });
    return fromMap(propsMap);
  }

  private static boolean hasPropertyOptions(Map<String, String> options) {
    return options.keySet().stream().anyMatch(k -> k.startsWith(PROPERTIES_PREFIX));
  }

  /** Creates a new configuration that is initialized with the options of the given map. */
  public static Configuration fromMap(Map<String, String> map) {
    final Configuration configuration = new Configuration();
    map.forEach(configuration::setString);
    return configuration;
  }

  /**
   * Returns whether the given conf defines default value for the option {@code option}.
   */
  public static <T> boolean isDefaultValueDefined(Configuration conf, ConfigOption<T> option) {
    return !conf.getOptional(option).isPresent()
        || conf.get(option).equals(option.defaultValue());
  }

  /**
   * Returns all the optional config options.
   */
  public static Set<ConfigOption<?>> optionalOptions() {
    Set<ConfigOption<?>> options = new HashSet<>(allOptions());
    options.remove(PATH);
    return options;
  }

  /**
   * Returns all the config options.
   */
  public static List<ConfigOption<?>> allOptions() {
    Field[] declaredFields = FlinkOptions.class.getDeclaredFields();
    List<ConfigOption<?>> options = new ArrayList<>();
    for (Field field : declaredFields) {
      if (java.lang.reflect.Modifier.isStatic(field.getModifiers())
          && field.getType().equals(ConfigOption.class)) {
        try {
          options.add((ConfigOption<?>) field.get(ConfigOption.class));
        } catch (IllegalAccessException e) {
          throw new HoodieException("Error while fetching static config option", e);
        }
      }
    }
    return options;
  }
}

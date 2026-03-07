package com.example.shortstop.database;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomOpenHelper;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class ShortStopDatabase_Impl extends ShortStopDatabase {
  private volatile ShortStopDao _shortStopDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(4) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `user_stats` (`id` INTEGER NOT NULL, `points` INTEGER NOT NULL, `currentStreak` INTEGER NOT NULL, `lastInterventionDate` TEXT NOT NULL, `totalInterventions` INTEGER NOT NULL, `totalTimeSaved` INTEGER NOT NULL, `successfulStudySessions` INTEGER NOT NULL, `totalPointsEarned` INTEGER NOT NULL, `dailyExitCount` INTEGER NOT NULL, `lastRewardDate` TEXT NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `blocked_apps` (`packageName` TEXT NOT NULL, `isBlocked` INTEGER NOT NULL, `lastExitTime` INTEGER NOT NULL, `totalInterventions` INTEGER NOT NULL, `totalTimeSaved` INTEGER NOT NULL, `isStudyMode` INTEGER NOT NULL, `studyStartTime` INTEGER NOT NULL, PRIMARY KEY(`packageName`))");
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_blocked_apps_packageName` ON `blocked_apps` (`packageName`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `app_usage` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `packageName` TEXT NOT NULL, `date` TEXT NOT NULL, `interventions` INTEGER NOT NULL, `timeSaved` INTEGER NOT NULL, `studySessions` INTEGER NOT NULL)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_app_usage_packageName` ON `app_usage` (`packageName`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_app_usage_date` ON `app_usage` (`date`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `hourly_interventions` (`hourKey` TEXT NOT NULL, `interventionCount` INTEGER NOT NULL, PRIMARY KEY(`hourKey`))");
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_hourly_interventions_hourKey` ON `hourly_interventions` (`hourKey`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '192883db803e66a2c67e5c89717aa49d')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `user_stats`");
        db.execSQL("DROP TABLE IF EXISTS `blocked_apps`");
        db.execSQL("DROP TABLE IF EXISTS `app_usage`");
        db.execSQL("DROP TABLE IF EXISTS `hourly_interventions`");
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onDestructiveMigration(db);
          }
        }
      }

      @Override
      public void onCreate(@NonNull final SupportSQLiteDatabase db) {
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onCreate(db);
          }
        }
      }

      @Override
      public void onOpen(@NonNull final SupportSQLiteDatabase db) {
        mDatabase = db;
        internalInitInvalidationTracker(db);
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onOpen(db);
          }
        }
      }

      @Override
      public void onPreMigrate(@NonNull final SupportSQLiteDatabase db) {
        DBUtil.dropFtsSyncTriggers(db);
      }

      @Override
      public void onPostMigrate(@NonNull final SupportSQLiteDatabase db) {
      }

      @Override
      @NonNull
      public RoomOpenHelper.ValidationResult onValidateSchema(
          @NonNull final SupportSQLiteDatabase db) {
        final HashMap<String, TableInfo.Column> _columnsUserStats = new HashMap<String, TableInfo.Column>(10);
        _columnsUserStats.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUserStats.put("points", new TableInfo.Column("points", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUserStats.put("currentStreak", new TableInfo.Column("currentStreak", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUserStats.put("lastInterventionDate", new TableInfo.Column("lastInterventionDate", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUserStats.put("totalInterventions", new TableInfo.Column("totalInterventions", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUserStats.put("totalTimeSaved", new TableInfo.Column("totalTimeSaved", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUserStats.put("successfulStudySessions", new TableInfo.Column("successfulStudySessions", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUserStats.put("totalPointsEarned", new TableInfo.Column("totalPointsEarned", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUserStats.put("dailyExitCount", new TableInfo.Column("dailyExitCount", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUserStats.put("lastRewardDate", new TableInfo.Column("lastRewardDate", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysUserStats = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesUserStats = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoUserStats = new TableInfo("user_stats", _columnsUserStats, _foreignKeysUserStats, _indicesUserStats);
        final TableInfo _existingUserStats = TableInfo.read(db, "user_stats");
        if (!_infoUserStats.equals(_existingUserStats)) {
          return new RoomOpenHelper.ValidationResult(false, "user_stats(com.example.shortstop.database.UserStatsEntity).\n"
                  + " Expected:\n" + _infoUserStats + "\n"
                  + " Found:\n" + _existingUserStats);
        }
        final HashMap<String, TableInfo.Column> _columnsBlockedApps = new HashMap<String, TableInfo.Column>(7);
        _columnsBlockedApps.put("packageName", new TableInfo.Column("packageName", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBlockedApps.put("isBlocked", new TableInfo.Column("isBlocked", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBlockedApps.put("lastExitTime", new TableInfo.Column("lastExitTime", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBlockedApps.put("totalInterventions", new TableInfo.Column("totalInterventions", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBlockedApps.put("totalTimeSaved", new TableInfo.Column("totalTimeSaved", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBlockedApps.put("isStudyMode", new TableInfo.Column("isStudyMode", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBlockedApps.put("studyStartTime", new TableInfo.Column("studyStartTime", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysBlockedApps = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesBlockedApps = new HashSet<TableInfo.Index>(1);
        _indicesBlockedApps.add(new TableInfo.Index("index_blocked_apps_packageName", true, Arrays.asList("packageName"), Arrays.asList("ASC")));
        final TableInfo _infoBlockedApps = new TableInfo("blocked_apps", _columnsBlockedApps, _foreignKeysBlockedApps, _indicesBlockedApps);
        final TableInfo _existingBlockedApps = TableInfo.read(db, "blocked_apps");
        if (!_infoBlockedApps.equals(_existingBlockedApps)) {
          return new RoomOpenHelper.ValidationResult(false, "blocked_apps(com.example.shortstop.database.BlockedAppEntity).\n"
                  + " Expected:\n" + _infoBlockedApps + "\n"
                  + " Found:\n" + _existingBlockedApps);
        }
        final HashMap<String, TableInfo.Column> _columnsAppUsage = new HashMap<String, TableInfo.Column>(6);
        _columnsAppUsage.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAppUsage.put("packageName", new TableInfo.Column("packageName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAppUsage.put("date", new TableInfo.Column("date", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAppUsage.put("interventions", new TableInfo.Column("interventions", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAppUsage.put("timeSaved", new TableInfo.Column("timeSaved", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAppUsage.put("studySessions", new TableInfo.Column("studySessions", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysAppUsage = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesAppUsage = new HashSet<TableInfo.Index>(2);
        _indicesAppUsage.add(new TableInfo.Index("index_app_usage_packageName", false, Arrays.asList("packageName"), Arrays.asList("ASC")));
        _indicesAppUsage.add(new TableInfo.Index("index_app_usage_date", false, Arrays.asList("date"), Arrays.asList("ASC")));
        final TableInfo _infoAppUsage = new TableInfo("app_usage", _columnsAppUsage, _foreignKeysAppUsage, _indicesAppUsage);
        final TableInfo _existingAppUsage = TableInfo.read(db, "app_usage");
        if (!_infoAppUsage.equals(_existingAppUsage)) {
          return new RoomOpenHelper.ValidationResult(false, "app_usage(com.example.shortstop.database.AppUsageEntity).\n"
                  + " Expected:\n" + _infoAppUsage + "\n"
                  + " Found:\n" + _existingAppUsage);
        }
        final HashMap<String, TableInfo.Column> _columnsHourlyInterventions = new HashMap<String, TableInfo.Column>(2);
        _columnsHourlyInterventions.put("hourKey", new TableInfo.Column("hourKey", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHourlyInterventions.put("interventionCount", new TableInfo.Column("interventionCount", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysHourlyInterventions = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesHourlyInterventions = new HashSet<TableInfo.Index>(1);
        _indicesHourlyInterventions.add(new TableInfo.Index("index_hourly_interventions_hourKey", true, Arrays.asList("hourKey"), Arrays.asList("ASC")));
        final TableInfo _infoHourlyInterventions = new TableInfo("hourly_interventions", _columnsHourlyInterventions, _foreignKeysHourlyInterventions, _indicesHourlyInterventions);
        final TableInfo _existingHourlyInterventions = TableInfo.read(db, "hourly_interventions");
        if (!_infoHourlyInterventions.equals(_existingHourlyInterventions)) {
          return new RoomOpenHelper.ValidationResult(false, "hourly_interventions(com.example.shortstop.database.HourlyInterventionEntity).\n"
                  + " Expected:\n" + _infoHourlyInterventions + "\n"
                  + " Found:\n" + _existingHourlyInterventions);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "192883db803e66a2c67e5c89717aa49d", "3cf00f250f7f749950b1e70ecbab6a2c");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "user_stats","blocked_apps","app_usage","hourly_interventions");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    try {
      super.beginTransaction();
      _db.execSQL("DELETE FROM `user_stats`");
      _db.execSQL("DELETE FROM `blocked_apps`");
      _db.execSQL("DELETE FROM `app_usage`");
      _db.execSQL("DELETE FROM `hourly_interventions`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      _db.query("PRAGMA wal_checkpoint(FULL)").close();
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM");
      }
    }
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final HashMap<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(ShortStopDao.class, ShortStopDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  @NonNull
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final HashSet<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  @NonNull
  public List<Migration> getAutoMigrations(
      @NonNull final Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
    final List<Migration> _autoMigrations = new ArrayList<Migration>();
    return _autoMigrations;
  }

  @Override
  public ShortStopDao dao() {
    if (_shortStopDao != null) {
      return _shortStopDao;
    } else {
      synchronized(this) {
        if(_shortStopDao == null) {
          _shortStopDao = new ShortStopDao_Impl(this);
        }
        return _shortStopDao;
      }
    }
  }
}

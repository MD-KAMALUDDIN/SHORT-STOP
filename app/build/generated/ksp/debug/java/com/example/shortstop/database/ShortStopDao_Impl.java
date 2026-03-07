package com.example.shortstop.database;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Integer;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class ShortStopDao_Impl implements ShortStopDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<UserStatsEntity> __insertionAdapterOfUserStatsEntity;

  private final EntityInsertionAdapter<BlockedAppEntity> __insertionAdapterOfBlockedAppEntity;

  private final EntityInsertionAdapter<AppUsageEntity> __insertionAdapterOfAppUsageEntity;

  private final EntityInsertionAdapter<HourlyInterventionEntity> __insertionAdapterOfHourlyInterventionEntity;

  private final SharedSQLiteStatement __preparedStmtOfDeleteBlockedApp;

  private final SharedSQLiteStatement __preparedStmtOfUpdateStudyMode;

  public ShortStopDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfUserStatsEntity = new EntityInsertionAdapter<UserStatsEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `user_stats` (`id`,`points`,`currentStreak`,`lastInterventionDate`,`totalInterventions`,`totalTimeSaved`,`successfulStudySessions`,`totalPointsEarned`,`dailyExitCount`,`lastRewardDate`) VALUES (?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final UserStatsEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindLong(2, entity.getPoints());
        statement.bindLong(3, entity.getCurrentStreak());
        statement.bindString(4, entity.getLastInterventionDate());
        statement.bindLong(5, entity.getTotalInterventions());
        statement.bindLong(6, entity.getTotalTimeSaved());
        statement.bindLong(7, entity.getSuccessfulStudySessions());
        statement.bindLong(8, entity.getTotalPointsEarned());
        statement.bindLong(9, entity.getDailyExitCount());
        statement.bindString(10, entity.getLastRewardDate());
      }
    };
    this.__insertionAdapterOfBlockedAppEntity = new EntityInsertionAdapter<BlockedAppEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `blocked_apps` (`packageName`,`isBlocked`,`lastExitTime`,`totalInterventions`,`totalTimeSaved`,`isStudyMode`,`studyStartTime`) VALUES (?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final BlockedAppEntity entity) {
        statement.bindString(1, entity.getPackageName());
        final int _tmp = entity.isBlocked() ? 1 : 0;
        statement.bindLong(2, _tmp);
        statement.bindLong(3, entity.getLastExitTime());
        statement.bindLong(4, entity.getTotalInterventions());
        statement.bindLong(5, entity.getTotalTimeSaved());
        final int _tmp_1 = entity.isStudyMode() ? 1 : 0;
        statement.bindLong(6, _tmp_1);
        statement.bindLong(7, entity.getStudyStartTime());
      }
    };
    this.__insertionAdapterOfAppUsageEntity = new EntityInsertionAdapter<AppUsageEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `app_usage` (`id`,`packageName`,`date`,`interventions`,`timeSaved`,`studySessions`) VALUES (nullif(?, 0),?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final AppUsageEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getPackageName());
        statement.bindString(3, entity.getDate());
        statement.bindLong(4, entity.getInterventions());
        statement.bindLong(5, entity.getTimeSaved());
        statement.bindLong(6, entity.getStudySessions());
      }
    };
    this.__insertionAdapterOfHourlyInterventionEntity = new EntityInsertionAdapter<HourlyInterventionEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `hourly_interventions` (`hourKey`,`interventionCount`) VALUES (?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final HourlyInterventionEntity entity) {
        statement.bindString(1, entity.getHourKey());
        statement.bindLong(2, entity.getInterventionCount());
      }
    };
    this.__preparedStmtOfDeleteBlockedApp = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM blocked_apps WHERE packageName = ?";
        return _query;
      }
    };
    this.__preparedStmtOfUpdateStudyMode = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE blocked_apps SET isStudyMode = ?, studyStartTime = ? WHERE packageName = ?";
        return _query;
      }
    };
  }

  @Override
  public Object updateUserStats(final UserStatsEntity stats,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfUserStatsEntity.insert(stats);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertBlockedApp(final BlockedAppEntity app,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfBlockedAppEntity.insert(app);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertAppUsage(final AppUsageEntity usage,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfAppUsageEntity.insert(usage);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertHourlyIntervention(final HourlyInterventionEntity intervention,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfHourlyInterventionEntity.insert(intervention);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteBlockedApp(final String packageName,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteBlockedApp.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, packageName);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteBlockedApp.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object updateStudyMode(final String packageName, final boolean isStudy,
      final long startTime, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfUpdateStudyMode.acquire();
        int _argIndex = 1;
        final int _tmp = isStudy ? 1 : 0;
        _stmt.bindLong(_argIndex, _tmp);
        _argIndex = 2;
        _stmt.bindLong(_argIndex, startTime);
        _argIndex = 3;
        _stmt.bindString(_argIndex, packageName);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfUpdateStudyMode.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<UserStatsEntity> getUserStats() {
    final String _sql = "SELECT * FROM user_stats WHERE id = 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"user_stats"}, new Callable<UserStatsEntity>() {
      @Override
      @Nullable
      public UserStatsEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfPoints = CursorUtil.getColumnIndexOrThrow(_cursor, "points");
          final int _cursorIndexOfCurrentStreak = CursorUtil.getColumnIndexOrThrow(_cursor, "currentStreak");
          final int _cursorIndexOfLastInterventionDate = CursorUtil.getColumnIndexOrThrow(_cursor, "lastInterventionDate");
          final int _cursorIndexOfTotalInterventions = CursorUtil.getColumnIndexOrThrow(_cursor, "totalInterventions");
          final int _cursorIndexOfTotalTimeSaved = CursorUtil.getColumnIndexOrThrow(_cursor, "totalTimeSaved");
          final int _cursorIndexOfSuccessfulStudySessions = CursorUtil.getColumnIndexOrThrow(_cursor, "successfulStudySessions");
          final int _cursorIndexOfTotalPointsEarned = CursorUtil.getColumnIndexOrThrow(_cursor, "totalPointsEarned");
          final int _cursorIndexOfDailyExitCount = CursorUtil.getColumnIndexOrThrow(_cursor, "dailyExitCount");
          final int _cursorIndexOfLastRewardDate = CursorUtil.getColumnIndexOrThrow(_cursor, "lastRewardDate");
          final UserStatsEntity _result;
          if (_cursor.moveToFirst()) {
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final int _tmpPoints;
            _tmpPoints = _cursor.getInt(_cursorIndexOfPoints);
            final int _tmpCurrentStreak;
            _tmpCurrentStreak = _cursor.getInt(_cursorIndexOfCurrentStreak);
            final String _tmpLastInterventionDate;
            _tmpLastInterventionDate = _cursor.getString(_cursorIndexOfLastInterventionDate);
            final int _tmpTotalInterventions;
            _tmpTotalInterventions = _cursor.getInt(_cursorIndexOfTotalInterventions);
            final long _tmpTotalTimeSaved;
            _tmpTotalTimeSaved = _cursor.getLong(_cursorIndexOfTotalTimeSaved);
            final int _tmpSuccessfulStudySessions;
            _tmpSuccessfulStudySessions = _cursor.getInt(_cursorIndexOfSuccessfulStudySessions);
            final int _tmpTotalPointsEarned;
            _tmpTotalPointsEarned = _cursor.getInt(_cursorIndexOfTotalPointsEarned);
            final int _tmpDailyExitCount;
            _tmpDailyExitCount = _cursor.getInt(_cursorIndexOfDailyExitCount);
            final String _tmpLastRewardDate;
            _tmpLastRewardDate = _cursor.getString(_cursorIndexOfLastRewardDate);
            _result = new UserStatsEntity(_tmpId,_tmpPoints,_tmpCurrentStreak,_tmpLastInterventionDate,_tmpTotalInterventions,_tmpTotalTimeSaved,_tmpSuccessfulStudySessions,_tmpTotalPointsEarned,_tmpDailyExitCount,_tmpLastRewardDate);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<BlockedAppEntity>> getBlockedApps() {
    final String _sql = "SELECT * FROM blocked_apps WHERE isBlocked = 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"blocked_apps"}, new Callable<List<BlockedAppEntity>>() {
      @Override
      @NonNull
      public List<BlockedAppEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfPackageName = CursorUtil.getColumnIndexOrThrow(_cursor, "packageName");
          final int _cursorIndexOfIsBlocked = CursorUtil.getColumnIndexOrThrow(_cursor, "isBlocked");
          final int _cursorIndexOfLastExitTime = CursorUtil.getColumnIndexOrThrow(_cursor, "lastExitTime");
          final int _cursorIndexOfTotalInterventions = CursorUtil.getColumnIndexOrThrow(_cursor, "totalInterventions");
          final int _cursorIndexOfTotalTimeSaved = CursorUtil.getColumnIndexOrThrow(_cursor, "totalTimeSaved");
          final int _cursorIndexOfIsStudyMode = CursorUtil.getColumnIndexOrThrow(_cursor, "isStudyMode");
          final int _cursorIndexOfStudyStartTime = CursorUtil.getColumnIndexOrThrow(_cursor, "studyStartTime");
          final List<BlockedAppEntity> _result = new ArrayList<BlockedAppEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final BlockedAppEntity _item;
            final String _tmpPackageName;
            _tmpPackageName = _cursor.getString(_cursorIndexOfPackageName);
            final boolean _tmpIsBlocked;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsBlocked);
            _tmpIsBlocked = _tmp != 0;
            final long _tmpLastExitTime;
            _tmpLastExitTime = _cursor.getLong(_cursorIndexOfLastExitTime);
            final int _tmpTotalInterventions;
            _tmpTotalInterventions = _cursor.getInt(_cursorIndexOfTotalInterventions);
            final long _tmpTotalTimeSaved;
            _tmpTotalTimeSaved = _cursor.getLong(_cursorIndexOfTotalTimeSaved);
            final boolean _tmpIsStudyMode;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsStudyMode);
            _tmpIsStudyMode = _tmp_1 != 0;
            final long _tmpStudyStartTime;
            _tmpStudyStartTime = _cursor.getLong(_cursorIndexOfStudyStartTime);
            _item = new BlockedAppEntity(_tmpPackageName,_tmpIsBlocked,_tmpLastExitTime,_tmpTotalInterventions,_tmpTotalTimeSaved,_tmpIsStudyMode,_tmpStudyStartTime);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<AppUsageEntity>> getUsageHistory(final String startDate) {
    final String _sql = "SELECT * FROM app_usage WHERE date >= ? ORDER BY date DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, startDate);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"app_usage"}, new Callable<List<AppUsageEntity>>() {
      @Override
      @NonNull
      public List<AppUsageEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfPackageName = CursorUtil.getColumnIndexOrThrow(_cursor, "packageName");
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfInterventions = CursorUtil.getColumnIndexOrThrow(_cursor, "interventions");
          final int _cursorIndexOfTimeSaved = CursorUtil.getColumnIndexOrThrow(_cursor, "timeSaved");
          final int _cursorIndexOfStudySessions = CursorUtil.getColumnIndexOrThrow(_cursor, "studySessions");
          final List<AppUsageEntity> _result = new ArrayList<AppUsageEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final AppUsageEntity _item;
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final String _tmpPackageName;
            _tmpPackageName = _cursor.getString(_cursorIndexOfPackageName);
            final String _tmpDate;
            _tmpDate = _cursor.getString(_cursorIndexOfDate);
            final int _tmpInterventions;
            _tmpInterventions = _cursor.getInt(_cursorIndexOfInterventions);
            final long _tmpTimeSaved;
            _tmpTimeSaved = _cursor.getLong(_cursorIndexOfTimeSaved);
            final int _tmpStudySessions;
            _tmpStudySessions = _cursor.getInt(_cursorIndexOfStudySessions);
            _item = new AppUsageEntity(_tmpId,_tmpPackageName,_tmpDate,_tmpInterventions,_tmpTimeSaved,_tmpStudySessions);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getTotalInterventions(final String pkg, final String startDate,
      final Continuation<? super Integer> $completion) {
    final String _sql = "SELECT SUM(interventions) FROM app_usage WHERE packageName = ? AND date >= ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindString(_argIndex, pkg);
    _argIndex = 2;
    _statement.bindString(_argIndex, startDate);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Integer>() {
      @Override
      @Nullable
      public Integer call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Integer _result;
          if (_cursor.moveToFirst()) {
            final Integer _tmp;
            if (_cursor.isNull(0)) {
              _tmp = null;
            } else {
              _tmp = _cursor.getInt(0);
            }
            _result = _tmp;
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getBlockedApp(final String packageName,
      final Continuation<? super BlockedAppEntity> $completion) {
    final String _sql = "SELECT * FROM blocked_apps WHERE packageName = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, packageName);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<BlockedAppEntity>() {
      @Override
      @Nullable
      public BlockedAppEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfPackageName = CursorUtil.getColumnIndexOrThrow(_cursor, "packageName");
          final int _cursorIndexOfIsBlocked = CursorUtil.getColumnIndexOrThrow(_cursor, "isBlocked");
          final int _cursorIndexOfLastExitTime = CursorUtil.getColumnIndexOrThrow(_cursor, "lastExitTime");
          final int _cursorIndexOfTotalInterventions = CursorUtil.getColumnIndexOrThrow(_cursor, "totalInterventions");
          final int _cursorIndexOfTotalTimeSaved = CursorUtil.getColumnIndexOrThrow(_cursor, "totalTimeSaved");
          final int _cursorIndexOfIsStudyMode = CursorUtil.getColumnIndexOrThrow(_cursor, "isStudyMode");
          final int _cursorIndexOfStudyStartTime = CursorUtil.getColumnIndexOrThrow(_cursor, "studyStartTime");
          final BlockedAppEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpPackageName;
            _tmpPackageName = _cursor.getString(_cursorIndexOfPackageName);
            final boolean _tmpIsBlocked;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsBlocked);
            _tmpIsBlocked = _tmp != 0;
            final long _tmpLastExitTime;
            _tmpLastExitTime = _cursor.getLong(_cursorIndexOfLastExitTime);
            final int _tmpTotalInterventions;
            _tmpTotalInterventions = _cursor.getInt(_cursorIndexOfTotalInterventions);
            final long _tmpTotalTimeSaved;
            _tmpTotalTimeSaved = _cursor.getLong(_cursorIndexOfTotalTimeSaved);
            final boolean _tmpIsStudyMode;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsStudyMode);
            _tmpIsStudyMode = _tmp_1 != 0;
            final long _tmpStudyStartTime;
            _tmpStudyStartTime = _cursor.getLong(_cursorIndexOfStudyStartTime);
            _result = new BlockedAppEntity(_tmpPackageName,_tmpIsBlocked,_tmpLastExitTime,_tmpTotalInterventions,_tmpTotalTimeSaved,_tmpIsStudyMode,_tmpStudyStartTime);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<BlockedAppEntity>> getStudyApps() {
    final String _sql = "SELECT * FROM blocked_apps WHERE isStudyMode = 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"blocked_apps"}, new Callable<List<BlockedAppEntity>>() {
      @Override
      @NonNull
      public List<BlockedAppEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfPackageName = CursorUtil.getColumnIndexOrThrow(_cursor, "packageName");
          final int _cursorIndexOfIsBlocked = CursorUtil.getColumnIndexOrThrow(_cursor, "isBlocked");
          final int _cursorIndexOfLastExitTime = CursorUtil.getColumnIndexOrThrow(_cursor, "lastExitTime");
          final int _cursorIndexOfTotalInterventions = CursorUtil.getColumnIndexOrThrow(_cursor, "totalInterventions");
          final int _cursorIndexOfTotalTimeSaved = CursorUtil.getColumnIndexOrThrow(_cursor, "totalTimeSaved");
          final int _cursorIndexOfIsStudyMode = CursorUtil.getColumnIndexOrThrow(_cursor, "isStudyMode");
          final int _cursorIndexOfStudyStartTime = CursorUtil.getColumnIndexOrThrow(_cursor, "studyStartTime");
          final List<BlockedAppEntity> _result = new ArrayList<BlockedAppEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final BlockedAppEntity _item;
            final String _tmpPackageName;
            _tmpPackageName = _cursor.getString(_cursorIndexOfPackageName);
            final boolean _tmpIsBlocked;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsBlocked);
            _tmpIsBlocked = _tmp != 0;
            final long _tmpLastExitTime;
            _tmpLastExitTime = _cursor.getLong(_cursorIndexOfLastExitTime);
            final int _tmpTotalInterventions;
            _tmpTotalInterventions = _cursor.getInt(_cursorIndexOfTotalInterventions);
            final long _tmpTotalTimeSaved;
            _tmpTotalTimeSaved = _cursor.getLong(_cursorIndexOfTotalTimeSaved);
            final boolean _tmpIsStudyMode;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsStudyMode);
            _tmpIsStudyMode = _tmp_1 != 0;
            final long _tmpStudyStartTime;
            _tmpStudyStartTime = _cursor.getLong(_cursorIndexOfStudyStartTime);
            _item = new BlockedAppEntity(_tmpPackageName,_tmpIsBlocked,_tmpLastExitTime,_tmpTotalInterventions,_tmpTotalTimeSaved,_tmpIsStudyMode,_tmpStudyStartTime);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getHourlyIntervention(final String hourKey,
      final Continuation<? super HourlyInterventionEntity> $completion) {
    final String _sql = "SELECT * FROM hourly_interventions WHERE hourKey = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, hourKey);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<HourlyInterventionEntity>() {
      @Override
      @Nullable
      public HourlyInterventionEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfHourKey = CursorUtil.getColumnIndexOrThrow(_cursor, "hourKey");
          final int _cursorIndexOfInterventionCount = CursorUtil.getColumnIndexOrThrow(_cursor, "interventionCount");
          final HourlyInterventionEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpHourKey;
            _tmpHourKey = _cursor.getString(_cursorIndexOfHourKey);
            final int _tmpInterventionCount;
            _tmpInterventionCount = _cursor.getInt(_cursorIndexOfInterventionCount);
            _result = new HourlyInterventionEntity(_tmpHourKey,_tmpInterventionCount);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}

package com.example.tasktracker.data.local;

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
public final class TaskDao_Impl implements TaskDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<TaskEntity> __insertionAdapterOfTaskEntity;

  private final Converters __converters = new Converters();

  private final SharedSQLiteStatement __preparedStmtOfMarkSynced;

  private final SharedSQLiteStatement __preparedStmtOfClear;

  public TaskDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfTaskEntity = new EntityInsertionAdapter<TaskEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `tasks` (`id`,`name`,`category`,`subcategory`,`priority`,`due`,`repeatType`,`weeklyMode`,`weeklyDays`,`monthlyMode`,`monthlyDates`,`customRulesJson`,`reminder`,`reminderTime`,`purpose`,`notes`,`createdAt`,`updatedAt`,`deleted`,`endDate`,`dirty`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final TaskEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getName());
        statement.bindString(3, entity.getCategory());
        statement.bindString(4, entity.getSubcategory());
        statement.bindString(5, entity.getPriority());
        statement.bindString(6, entity.getDue());
        statement.bindString(7, entity.getRepeatType());
        statement.bindString(8, entity.getWeeklyMode());
        final String _tmp = __converters.fromInts(entity.getWeeklyDays());
        statement.bindString(9, _tmp);
        statement.bindString(10, entity.getMonthlyMode());
        final String _tmp_1 = __converters.fromInts(entity.getMonthlyDates());
        statement.bindString(11, _tmp_1);
        statement.bindString(12, entity.getCustomRulesJson());
        final int _tmp_2 = entity.getReminder() ? 1 : 0;
        statement.bindLong(13, _tmp_2);
        statement.bindString(14, entity.getReminderTime());
        statement.bindString(15, entity.getPurpose());
        statement.bindString(16, entity.getNotes());
        statement.bindLong(17, entity.getCreatedAt());
        statement.bindLong(18, entity.getUpdatedAt());
        final int _tmp_3 = entity.getDeleted() ? 1 : 0;
        statement.bindLong(19, _tmp_3);
        statement.bindString(20, entity.getEndDate());
        final int _tmp_4 = entity.getDirty() ? 1 : 0;
        statement.bindLong(21, _tmp_4);
      }
    };
    this.__preparedStmtOfMarkSynced = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE tasks SET dirty = 0 WHERE id = ? AND updatedAt = ?";
        return _query;
      }
    };
    this.__preparedStmtOfClear = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM tasks";
        return _query;
      }
    };
  }

  @Override
  public Object upsert(final TaskEntity task, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfTaskEntity.insert(task);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object upsertAll(final List<TaskEntity> tasks,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfTaskEntity.insert(tasks);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object markSynced(final String id, final long updatedAt,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfMarkSynced.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, id);
        _argIndex = 2;
        _stmt.bindLong(_argIndex, updatedAt);
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
          __preparedStmtOfMarkSynced.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object clear(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfClear.acquire();
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
          __preparedStmtOfClear.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<TaskEntity>> observeActive() {
    final String _sql = "SELECT * FROM tasks WHERE deleted = 0 ORDER BY due, createdAt";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"tasks"}, new Callable<List<TaskEntity>>() {
      @Override
      @NonNull
      public List<TaskEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfCategory = CursorUtil.getColumnIndexOrThrow(_cursor, "category");
          final int _cursorIndexOfSubcategory = CursorUtil.getColumnIndexOrThrow(_cursor, "subcategory");
          final int _cursorIndexOfPriority = CursorUtil.getColumnIndexOrThrow(_cursor, "priority");
          final int _cursorIndexOfDue = CursorUtil.getColumnIndexOrThrow(_cursor, "due");
          final int _cursorIndexOfRepeatType = CursorUtil.getColumnIndexOrThrow(_cursor, "repeatType");
          final int _cursorIndexOfWeeklyMode = CursorUtil.getColumnIndexOrThrow(_cursor, "weeklyMode");
          final int _cursorIndexOfWeeklyDays = CursorUtil.getColumnIndexOrThrow(_cursor, "weeklyDays");
          final int _cursorIndexOfMonthlyMode = CursorUtil.getColumnIndexOrThrow(_cursor, "monthlyMode");
          final int _cursorIndexOfMonthlyDates = CursorUtil.getColumnIndexOrThrow(_cursor, "monthlyDates");
          final int _cursorIndexOfCustomRulesJson = CursorUtil.getColumnIndexOrThrow(_cursor, "customRulesJson");
          final int _cursorIndexOfReminder = CursorUtil.getColumnIndexOrThrow(_cursor, "reminder");
          final int _cursorIndexOfReminderTime = CursorUtil.getColumnIndexOrThrow(_cursor, "reminderTime");
          final int _cursorIndexOfPurpose = CursorUtil.getColumnIndexOrThrow(_cursor, "purpose");
          final int _cursorIndexOfNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "notes");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final int _cursorIndexOfDeleted = CursorUtil.getColumnIndexOrThrow(_cursor, "deleted");
          final int _cursorIndexOfEndDate = CursorUtil.getColumnIndexOrThrow(_cursor, "endDate");
          final int _cursorIndexOfDirty = CursorUtil.getColumnIndexOrThrow(_cursor, "dirty");
          final List<TaskEntity> _result = new ArrayList<TaskEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final TaskEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpCategory;
            _tmpCategory = _cursor.getString(_cursorIndexOfCategory);
            final String _tmpSubcategory;
            _tmpSubcategory = _cursor.getString(_cursorIndexOfSubcategory);
            final String _tmpPriority;
            _tmpPriority = _cursor.getString(_cursorIndexOfPriority);
            final String _tmpDue;
            _tmpDue = _cursor.getString(_cursorIndexOfDue);
            final String _tmpRepeatType;
            _tmpRepeatType = _cursor.getString(_cursorIndexOfRepeatType);
            final String _tmpWeeklyMode;
            _tmpWeeklyMode = _cursor.getString(_cursorIndexOfWeeklyMode);
            final List<Integer> _tmpWeeklyDays;
            final String _tmp;
            _tmp = _cursor.getString(_cursorIndexOfWeeklyDays);
            _tmpWeeklyDays = __converters.toInts(_tmp);
            final String _tmpMonthlyMode;
            _tmpMonthlyMode = _cursor.getString(_cursorIndexOfMonthlyMode);
            final List<Integer> _tmpMonthlyDates;
            final String _tmp_1;
            _tmp_1 = _cursor.getString(_cursorIndexOfMonthlyDates);
            _tmpMonthlyDates = __converters.toInts(_tmp_1);
            final String _tmpCustomRulesJson;
            _tmpCustomRulesJson = _cursor.getString(_cursorIndexOfCustomRulesJson);
            final boolean _tmpReminder;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfReminder);
            _tmpReminder = _tmp_2 != 0;
            final String _tmpReminderTime;
            _tmpReminderTime = _cursor.getString(_cursorIndexOfReminderTime);
            final String _tmpPurpose;
            _tmpPurpose = _cursor.getString(_cursorIndexOfPurpose);
            final String _tmpNotes;
            _tmpNotes = _cursor.getString(_cursorIndexOfNotes);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            final boolean _tmpDeleted;
            final int _tmp_3;
            _tmp_3 = _cursor.getInt(_cursorIndexOfDeleted);
            _tmpDeleted = _tmp_3 != 0;
            final String _tmpEndDate;
            _tmpEndDate = _cursor.getString(_cursorIndexOfEndDate);
            final boolean _tmpDirty;
            final int _tmp_4;
            _tmp_4 = _cursor.getInt(_cursorIndexOfDirty);
            _tmpDirty = _tmp_4 != 0;
            _item = new TaskEntity(_tmpId,_tmpName,_tmpCategory,_tmpSubcategory,_tmpPriority,_tmpDue,_tmpRepeatType,_tmpWeeklyMode,_tmpWeeklyDays,_tmpMonthlyMode,_tmpMonthlyDates,_tmpCustomRulesJson,_tmpReminder,_tmpReminderTime,_tmpPurpose,_tmpNotes,_tmpCreatedAt,_tmpUpdatedAt,_tmpDeleted,_tmpEndDate,_tmpDirty);
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
  public Flow<List<TaskEntity>> observeHistory() {
    final String _sql = "SELECT * FROM tasks WHERE deleted = 0 OR endDate != '' ORDER BY due, createdAt";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"tasks"}, new Callable<List<TaskEntity>>() {
      @Override
      @NonNull
      public List<TaskEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfCategory = CursorUtil.getColumnIndexOrThrow(_cursor, "category");
          final int _cursorIndexOfSubcategory = CursorUtil.getColumnIndexOrThrow(_cursor, "subcategory");
          final int _cursorIndexOfPriority = CursorUtil.getColumnIndexOrThrow(_cursor, "priority");
          final int _cursorIndexOfDue = CursorUtil.getColumnIndexOrThrow(_cursor, "due");
          final int _cursorIndexOfRepeatType = CursorUtil.getColumnIndexOrThrow(_cursor, "repeatType");
          final int _cursorIndexOfWeeklyMode = CursorUtil.getColumnIndexOrThrow(_cursor, "weeklyMode");
          final int _cursorIndexOfWeeklyDays = CursorUtil.getColumnIndexOrThrow(_cursor, "weeklyDays");
          final int _cursorIndexOfMonthlyMode = CursorUtil.getColumnIndexOrThrow(_cursor, "monthlyMode");
          final int _cursorIndexOfMonthlyDates = CursorUtil.getColumnIndexOrThrow(_cursor, "monthlyDates");
          final int _cursorIndexOfCustomRulesJson = CursorUtil.getColumnIndexOrThrow(_cursor, "customRulesJson");
          final int _cursorIndexOfReminder = CursorUtil.getColumnIndexOrThrow(_cursor, "reminder");
          final int _cursorIndexOfReminderTime = CursorUtil.getColumnIndexOrThrow(_cursor, "reminderTime");
          final int _cursorIndexOfPurpose = CursorUtil.getColumnIndexOrThrow(_cursor, "purpose");
          final int _cursorIndexOfNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "notes");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final int _cursorIndexOfDeleted = CursorUtil.getColumnIndexOrThrow(_cursor, "deleted");
          final int _cursorIndexOfEndDate = CursorUtil.getColumnIndexOrThrow(_cursor, "endDate");
          final int _cursorIndexOfDirty = CursorUtil.getColumnIndexOrThrow(_cursor, "dirty");
          final List<TaskEntity> _result = new ArrayList<TaskEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final TaskEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpCategory;
            _tmpCategory = _cursor.getString(_cursorIndexOfCategory);
            final String _tmpSubcategory;
            _tmpSubcategory = _cursor.getString(_cursorIndexOfSubcategory);
            final String _tmpPriority;
            _tmpPriority = _cursor.getString(_cursorIndexOfPriority);
            final String _tmpDue;
            _tmpDue = _cursor.getString(_cursorIndexOfDue);
            final String _tmpRepeatType;
            _tmpRepeatType = _cursor.getString(_cursorIndexOfRepeatType);
            final String _tmpWeeklyMode;
            _tmpWeeklyMode = _cursor.getString(_cursorIndexOfWeeklyMode);
            final List<Integer> _tmpWeeklyDays;
            final String _tmp;
            _tmp = _cursor.getString(_cursorIndexOfWeeklyDays);
            _tmpWeeklyDays = __converters.toInts(_tmp);
            final String _tmpMonthlyMode;
            _tmpMonthlyMode = _cursor.getString(_cursorIndexOfMonthlyMode);
            final List<Integer> _tmpMonthlyDates;
            final String _tmp_1;
            _tmp_1 = _cursor.getString(_cursorIndexOfMonthlyDates);
            _tmpMonthlyDates = __converters.toInts(_tmp_1);
            final String _tmpCustomRulesJson;
            _tmpCustomRulesJson = _cursor.getString(_cursorIndexOfCustomRulesJson);
            final boolean _tmpReminder;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfReminder);
            _tmpReminder = _tmp_2 != 0;
            final String _tmpReminderTime;
            _tmpReminderTime = _cursor.getString(_cursorIndexOfReminderTime);
            final String _tmpPurpose;
            _tmpPurpose = _cursor.getString(_cursorIndexOfPurpose);
            final String _tmpNotes;
            _tmpNotes = _cursor.getString(_cursorIndexOfNotes);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            final boolean _tmpDeleted;
            final int _tmp_3;
            _tmp_3 = _cursor.getInt(_cursorIndexOfDeleted);
            _tmpDeleted = _tmp_3 != 0;
            final String _tmpEndDate;
            _tmpEndDate = _cursor.getString(_cursorIndexOfEndDate);
            final boolean _tmpDirty;
            final int _tmp_4;
            _tmp_4 = _cursor.getInt(_cursorIndexOfDirty);
            _tmpDirty = _tmp_4 != 0;
            _item = new TaskEntity(_tmpId,_tmpName,_tmpCategory,_tmpSubcategory,_tmpPriority,_tmpDue,_tmpRepeatType,_tmpWeeklyMode,_tmpWeeklyDays,_tmpMonthlyMode,_tmpMonthlyDates,_tmpCustomRulesJson,_tmpReminder,_tmpReminderTime,_tmpPurpose,_tmpNotes,_tmpCreatedAt,_tmpUpdatedAt,_tmpDeleted,_tmpEndDate,_tmpDirty);
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
  public Object activeList(final Continuation<? super List<TaskEntity>> $completion) {
    final String _sql = "SELECT * FROM tasks WHERE deleted = 0";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<TaskEntity>>() {
      @Override
      @NonNull
      public List<TaskEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfCategory = CursorUtil.getColumnIndexOrThrow(_cursor, "category");
          final int _cursorIndexOfSubcategory = CursorUtil.getColumnIndexOrThrow(_cursor, "subcategory");
          final int _cursorIndexOfPriority = CursorUtil.getColumnIndexOrThrow(_cursor, "priority");
          final int _cursorIndexOfDue = CursorUtil.getColumnIndexOrThrow(_cursor, "due");
          final int _cursorIndexOfRepeatType = CursorUtil.getColumnIndexOrThrow(_cursor, "repeatType");
          final int _cursorIndexOfWeeklyMode = CursorUtil.getColumnIndexOrThrow(_cursor, "weeklyMode");
          final int _cursorIndexOfWeeklyDays = CursorUtil.getColumnIndexOrThrow(_cursor, "weeklyDays");
          final int _cursorIndexOfMonthlyMode = CursorUtil.getColumnIndexOrThrow(_cursor, "monthlyMode");
          final int _cursorIndexOfMonthlyDates = CursorUtil.getColumnIndexOrThrow(_cursor, "monthlyDates");
          final int _cursorIndexOfCustomRulesJson = CursorUtil.getColumnIndexOrThrow(_cursor, "customRulesJson");
          final int _cursorIndexOfReminder = CursorUtil.getColumnIndexOrThrow(_cursor, "reminder");
          final int _cursorIndexOfReminderTime = CursorUtil.getColumnIndexOrThrow(_cursor, "reminderTime");
          final int _cursorIndexOfPurpose = CursorUtil.getColumnIndexOrThrow(_cursor, "purpose");
          final int _cursorIndexOfNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "notes");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final int _cursorIndexOfDeleted = CursorUtil.getColumnIndexOrThrow(_cursor, "deleted");
          final int _cursorIndexOfEndDate = CursorUtil.getColumnIndexOrThrow(_cursor, "endDate");
          final int _cursorIndexOfDirty = CursorUtil.getColumnIndexOrThrow(_cursor, "dirty");
          final List<TaskEntity> _result = new ArrayList<TaskEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final TaskEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpCategory;
            _tmpCategory = _cursor.getString(_cursorIndexOfCategory);
            final String _tmpSubcategory;
            _tmpSubcategory = _cursor.getString(_cursorIndexOfSubcategory);
            final String _tmpPriority;
            _tmpPriority = _cursor.getString(_cursorIndexOfPriority);
            final String _tmpDue;
            _tmpDue = _cursor.getString(_cursorIndexOfDue);
            final String _tmpRepeatType;
            _tmpRepeatType = _cursor.getString(_cursorIndexOfRepeatType);
            final String _tmpWeeklyMode;
            _tmpWeeklyMode = _cursor.getString(_cursorIndexOfWeeklyMode);
            final List<Integer> _tmpWeeklyDays;
            final String _tmp;
            _tmp = _cursor.getString(_cursorIndexOfWeeklyDays);
            _tmpWeeklyDays = __converters.toInts(_tmp);
            final String _tmpMonthlyMode;
            _tmpMonthlyMode = _cursor.getString(_cursorIndexOfMonthlyMode);
            final List<Integer> _tmpMonthlyDates;
            final String _tmp_1;
            _tmp_1 = _cursor.getString(_cursorIndexOfMonthlyDates);
            _tmpMonthlyDates = __converters.toInts(_tmp_1);
            final String _tmpCustomRulesJson;
            _tmpCustomRulesJson = _cursor.getString(_cursorIndexOfCustomRulesJson);
            final boolean _tmpReminder;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfReminder);
            _tmpReminder = _tmp_2 != 0;
            final String _tmpReminderTime;
            _tmpReminderTime = _cursor.getString(_cursorIndexOfReminderTime);
            final String _tmpPurpose;
            _tmpPurpose = _cursor.getString(_cursorIndexOfPurpose);
            final String _tmpNotes;
            _tmpNotes = _cursor.getString(_cursorIndexOfNotes);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            final boolean _tmpDeleted;
            final int _tmp_3;
            _tmp_3 = _cursor.getInt(_cursorIndexOfDeleted);
            _tmpDeleted = _tmp_3 != 0;
            final String _tmpEndDate;
            _tmpEndDate = _cursor.getString(_cursorIndexOfEndDate);
            final boolean _tmpDirty;
            final int _tmp_4;
            _tmp_4 = _cursor.getInt(_cursorIndexOfDirty);
            _tmpDirty = _tmp_4 != 0;
            _item = new TaskEntity(_tmpId,_tmpName,_tmpCategory,_tmpSubcategory,_tmpPriority,_tmpDue,_tmpRepeatType,_tmpWeeklyMode,_tmpWeeklyDays,_tmpMonthlyMode,_tmpMonthlyDates,_tmpCustomRulesJson,_tmpReminder,_tmpReminderTime,_tmpPurpose,_tmpNotes,_tmpCreatedAt,_tmpUpdatedAt,_tmpDeleted,_tmpEndDate,_tmpDirty);
            _result.add(_item);
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
  public Object get(final String id, final Continuation<? super TaskEntity> $completion) {
    final String _sql = "SELECT * FROM tasks WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<TaskEntity>() {
      @Override
      @Nullable
      public TaskEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfCategory = CursorUtil.getColumnIndexOrThrow(_cursor, "category");
          final int _cursorIndexOfSubcategory = CursorUtil.getColumnIndexOrThrow(_cursor, "subcategory");
          final int _cursorIndexOfPriority = CursorUtil.getColumnIndexOrThrow(_cursor, "priority");
          final int _cursorIndexOfDue = CursorUtil.getColumnIndexOrThrow(_cursor, "due");
          final int _cursorIndexOfRepeatType = CursorUtil.getColumnIndexOrThrow(_cursor, "repeatType");
          final int _cursorIndexOfWeeklyMode = CursorUtil.getColumnIndexOrThrow(_cursor, "weeklyMode");
          final int _cursorIndexOfWeeklyDays = CursorUtil.getColumnIndexOrThrow(_cursor, "weeklyDays");
          final int _cursorIndexOfMonthlyMode = CursorUtil.getColumnIndexOrThrow(_cursor, "monthlyMode");
          final int _cursorIndexOfMonthlyDates = CursorUtil.getColumnIndexOrThrow(_cursor, "monthlyDates");
          final int _cursorIndexOfCustomRulesJson = CursorUtil.getColumnIndexOrThrow(_cursor, "customRulesJson");
          final int _cursorIndexOfReminder = CursorUtil.getColumnIndexOrThrow(_cursor, "reminder");
          final int _cursorIndexOfReminderTime = CursorUtil.getColumnIndexOrThrow(_cursor, "reminderTime");
          final int _cursorIndexOfPurpose = CursorUtil.getColumnIndexOrThrow(_cursor, "purpose");
          final int _cursorIndexOfNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "notes");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final int _cursorIndexOfDeleted = CursorUtil.getColumnIndexOrThrow(_cursor, "deleted");
          final int _cursorIndexOfEndDate = CursorUtil.getColumnIndexOrThrow(_cursor, "endDate");
          final int _cursorIndexOfDirty = CursorUtil.getColumnIndexOrThrow(_cursor, "dirty");
          final TaskEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpCategory;
            _tmpCategory = _cursor.getString(_cursorIndexOfCategory);
            final String _tmpSubcategory;
            _tmpSubcategory = _cursor.getString(_cursorIndexOfSubcategory);
            final String _tmpPriority;
            _tmpPriority = _cursor.getString(_cursorIndexOfPriority);
            final String _tmpDue;
            _tmpDue = _cursor.getString(_cursorIndexOfDue);
            final String _tmpRepeatType;
            _tmpRepeatType = _cursor.getString(_cursorIndexOfRepeatType);
            final String _tmpWeeklyMode;
            _tmpWeeklyMode = _cursor.getString(_cursorIndexOfWeeklyMode);
            final List<Integer> _tmpWeeklyDays;
            final String _tmp;
            _tmp = _cursor.getString(_cursorIndexOfWeeklyDays);
            _tmpWeeklyDays = __converters.toInts(_tmp);
            final String _tmpMonthlyMode;
            _tmpMonthlyMode = _cursor.getString(_cursorIndexOfMonthlyMode);
            final List<Integer> _tmpMonthlyDates;
            final String _tmp_1;
            _tmp_1 = _cursor.getString(_cursorIndexOfMonthlyDates);
            _tmpMonthlyDates = __converters.toInts(_tmp_1);
            final String _tmpCustomRulesJson;
            _tmpCustomRulesJson = _cursor.getString(_cursorIndexOfCustomRulesJson);
            final boolean _tmpReminder;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfReminder);
            _tmpReminder = _tmp_2 != 0;
            final String _tmpReminderTime;
            _tmpReminderTime = _cursor.getString(_cursorIndexOfReminderTime);
            final String _tmpPurpose;
            _tmpPurpose = _cursor.getString(_cursorIndexOfPurpose);
            final String _tmpNotes;
            _tmpNotes = _cursor.getString(_cursorIndexOfNotes);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            final boolean _tmpDeleted;
            final int _tmp_3;
            _tmp_3 = _cursor.getInt(_cursorIndexOfDeleted);
            _tmpDeleted = _tmp_3 != 0;
            final String _tmpEndDate;
            _tmpEndDate = _cursor.getString(_cursorIndexOfEndDate);
            final boolean _tmpDirty;
            final int _tmp_4;
            _tmp_4 = _cursor.getInt(_cursorIndexOfDirty);
            _tmpDirty = _tmp_4 != 0;
            _result = new TaskEntity(_tmpId,_tmpName,_tmpCategory,_tmpSubcategory,_tmpPriority,_tmpDue,_tmpRepeatType,_tmpWeeklyMode,_tmpWeeklyDays,_tmpMonthlyMode,_tmpMonthlyDates,_tmpCustomRulesJson,_tmpReminder,_tmpReminderTime,_tmpPurpose,_tmpNotes,_tmpCreatedAt,_tmpUpdatedAt,_tmpDeleted,_tmpEndDate,_tmpDirty);
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
  public Object dirtyRows(final Continuation<? super List<TaskEntity>> $completion) {
    final String _sql = "SELECT * FROM tasks WHERE dirty = 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<TaskEntity>>() {
      @Override
      @NonNull
      public List<TaskEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfCategory = CursorUtil.getColumnIndexOrThrow(_cursor, "category");
          final int _cursorIndexOfSubcategory = CursorUtil.getColumnIndexOrThrow(_cursor, "subcategory");
          final int _cursorIndexOfPriority = CursorUtil.getColumnIndexOrThrow(_cursor, "priority");
          final int _cursorIndexOfDue = CursorUtil.getColumnIndexOrThrow(_cursor, "due");
          final int _cursorIndexOfRepeatType = CursorUtil.getColumnIndexOrThrow(_cursor, "repeatType");
          final int _cursorIndexOfWeeklyMode = CursorUtil.getColumnIndexOrThrow(_cursor, "weeklyMode");
          final int _cursorIndexOfWeeklyDays = CursorUtil.getColumnIndexOrThrow(_cursor, "weeklyDays");
          final int _cursorIndexOfMonthlyMode = CursorUtil.getColumnIndexOrThrow(_cursor, "monthlyMode");
          final int _cursorIndexOfMonthlyDates = CursorUtil.getColumnIndexOrThrow(_cursor, "monthlyDates");
          final int _cursorIndexOfCustomRulesJson = CursorUtil.getColumnIndexOrThrow(_cursor, "customRulesJson");
          final int _cursorIndexOfReminder = CursorUtil.getColumnIndexOrThrow(_cursor, "reminder");
          final int _cursorIndexOfReminderTime = CursorUtil.getColumnIndexOrThrow(_cursor, "reminderTime");
          final int _cursorIndexOfPurpose = CursorUtil.getColumnIndexOrThrow(_cursor, "purpose");
          final int _cursorIndexOfNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "notes");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final int _cursorIndexOfDeleted = CursorUtil.getColumnIndexOrThrow(_cursor, "deleted");
          final int _cursorIndexOfEndDate = CursorUtil.getColumnIndexOrThrow(_cursor, "endDate");
          final int _cursorIndexOfDirty = CursorUtil.getColumnIndexOrThrow(_cursor, "dirty");
          final List<TaskEntity> _result = new ArrayList<TaskEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final TaskEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpCategory;
            _tmpCategory = _cursor.getString(_cursorIndexOfCategory);
            final String _tmpSubcategory;
            _tmpSubcategory = _cursor.getString(_cursorIndexOfSubcategory);
            final String _tmpPriority;
            _tmpPriority = _cursor.getString(_cursorIndexOfPriority);
            final String _tmpDue;
            _tmpDue = _cursor.getString(_cursorIndexOfDue);
            final String _tmpRepeatType;
            _tmpRepeatType = _cursor.getString(_cursorIndexOfRepeatType);
            final String _tmpWeeklyMode;
            _tmpWeeklyMode = _cursor.getString(_cursorIndexOfWeeklyMode);
            final List<Integer> _tmpWeeklyDays;
            final String _tmp;
            _tmp = _cursor.getString(_cursorIndexOfWeeklyDays);
            _tmpWeeklyDays = __converters.toInts(_tmp);
            final String _tmpMonthlyMode;
            _tmpMonthlyMode = _cursor.getString(_cursorIndexOfMonthlyMode);
            final List<Integer> _tmpMonthlyDates;
            final String _tmp_1;
            _tmp_1 = _cursor.getString(_cursorIndexOfMonthlyDates);
            _tmpMonthlyDates = __converters.toInts(_tmp_1);
            final String _tmpCustomRulesJson;
            _tmpCustomRulesJson = _cursor.getString(_cursorIndexOfCustomRulesJson);
            final boolean _tmpReminder;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfReminder);
            _tmpReminder = _tmp_2 != 0;
            final String _tmpReminderTime;
            _tmpReminderTime = _cursor.getString(_cursorIndexOfReminderTime);
            final String _tmpPurpose;
            _tmpPurpose = _cursor.getString(_cursorIndexOfPurpose);
            final String _tmpNotes;
            _tmpNotes = _cursor.getString(_cursorIndexOfNotes);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            final boolean _tmpDeleted;
            final int _tmp_3;
            _tmp_3 = _cursor.getInt(_cursorIndexOfDeleted);
            _tmpDeleted = _tmp_3 != 0;
            final String _tmpEndDate;
            _tmpEndDate = _cursor.getString(_cursorIndexOfEndDate);
            final boolean _tmpDirty;
            final int _tmp_4;
            _tmp_4 = _cursor.getInt(_cursorIndexOfDirty);
            _tmpDirty = _tmp_4 != 0;
            _item = new TaskEntity(_tmpId,_tmpName,_tmpCategory,_tmpSubcategory,_tmpPriority,_tmpDue,_tmpRepeatType,_tmpWeeklyMode,_tmpWeeklyDays,_tmpMonthlyMode,_tmpMonthlyDates,_tmpCustomRulesJson,_tmpReminder,_tmpReminderTime,_tmpPurpose,_tmpNotes,_tmpCreatedAt,_tmpUpdatedAt,_tmpDeleted,_tmpEndDate,_tmpDirty);
            _result.add(_item);
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

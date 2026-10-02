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
public final class CompletionDao_Impl implements CompletionDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<CompletionEntity> __insertionAdapterOfCompletionEntity;

  private final SharedSQLiteStatement __preparedStmtOfClearForTask;

  private final SharedSQLiteStatement __preparedStmtOfClearForTaskFrom;

  private final SharedSQLiteStatement __preparedStmtOfMarkSynced;

  private final SharedSQLiteStatement __preparedStmtOfClear;

  public CompletionDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfCompletionEntity = new EntityInsertionAdapter<CompletionEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `completions` (`id`,`taskId`,`date`,`done`,`updatedAt`,`dirty`) VALUES (?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final CompletionEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getTaskId());
        statement.bindString(3, entity.getDate());
        final int _tmp = entity.getDone() ? 1 : 0;
        statement.bindLong(4, _tmp);
        statement.bindLong(5, entity.getUpdatedAt());
        final int _tmp_1 = entity.getDirty() ? 1 : 0;
        statement.bindLong(6, _tmp_1);
      }
    };
    this.__preparedStmtOfClearForTask = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE completions SET done = 0, dirty = 1, updatedAt = ? WHERE taskId = ? AND done = 1";
        return _query;
      }
    };
    this.__preparedStmtOfClearForTaskFrom = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE completions SET done = 0, dirty = 1, updatedAt = ? WHERE taskId = ? AND done = 1 AND date >= ?";
        return _query;
      }
    };
    this.__preparedStmtOfMarkSynced = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE completions SET dirty = 0 WHERE id = ? AND updatedAt = ?";
        return _query;
      }
    };
    this.__preparedStmtOfClear = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM completions";
        return _query;
      }
    };
  }

  @Override
  public Object upsert(final CompletionEntity c, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfCompletionEntity.insert(c);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object upsertAll(final List<CompletionEntity> list,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfCompletionEntity.insert(list);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object clearForTask(final String taskId, final long now,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfClearForTask.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, now);
        _argIndex = 2;
        _stmt.bindString(_argIndex, taskId);
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
          __preparedStmtOfClearForTask.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object clearForTaskFrom(final String taskId, final String fromDate, final long now,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfClearForTaskFrom.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, now);
        _argIndex = 2;
        _stmt.bindString(_argIndex, taskId);
        _argIndex = 3;
        _stmt.bindString(_argIndex, fromDate);
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
          __preparedStmtOfClearForTaskFrom.release(_stmt);
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
  public Flow<List<CompletionEntity>> observeDone() {
    final String _sql = "SELECT * FROM completions WHERE done = 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"completions"}, new Callable<List<CompletionEntity>>() {
      @Override
      @NonNull
      public List<CompletionEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTaskId = CursorUtil.getColumnIndexOrThrow(_cursor, "taskId");
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfDone = CursorUtil.getColumnIndexOrThrow(_cursor, "done");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final int _cursorIndexOfDirty = CursorUtil.getColumnIndexOrThrow(_cursor, "dirty");
          final List<CompletionEntity> _result = new ArrayList<CompletionEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final CompletionEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpTaskId;
            _tmpTaskId = _cursor.getString(_cursorIndexOfTaskId);
            final String _tmpDate;
            _tmpDate = _cursor.getString(_cursorIndexOfDate);
            final boolean _tmpDone;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfDone);
            _tmpDone = _tmp != 0;
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            final boolean _tmpDirty;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfDirty);
            _tmpDirty = _tmp_1 != 0;
            _item = new CompletionEntity(_tmpId,_tmpTaskId,_tmpDate,_tmpDone,_tmpUpdatedAt,_tmpDirty);
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
  public Object get(final String id, final Continuation<? super CompletionEntity> $completion) {
    final String _sql = "SELECT * FROM completions WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<CompletionEntity>() {
      @Override
      @Nullable
      public CompletionEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTaskId = CursorUtil.getColumnIndexOrThrow(_cursor, "taskId");
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfDone = CursorUtil.getColumnIndexOrThrow(_cursor, "done");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final int _cursorIndexOfDirty = CursorUtil.getColumnIndexOrThrow(_cursor, "dirty");
          final CompletionEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpTaskId;
            _tmpTaskId = _cursor.getString(_cursorIndexOfTaskId);
            final String _tmpDate;
            _tmpDate = _cursor.getString(_cursorIndexOfDate);
            final boolean _tmpDone;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfDone);
            _tmpDone = _tmp != 0;
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            final boolean _tmpDirty;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfDirty);
            _tmpDirty = _tmp_1 != 0;
            _result = new CompletionEntity(_tmpId,_tmpTaskId,_tmpDate,_tmpDone,_tmpUpdatedAt,_tmpDirty);
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
  public Object dirtyRows(final Continuation<? super List<CompletionEntity>> $completion) {
    final String _sql = "SELECT * FROM completions WHERE dirty = 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<CompletionEntity>>() {
      @Override
      @NonNull
      public List<CompletionEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTaskId = CursorUtil.getColumnIndexOrThrow(_cursor, "taskId");
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfDone = CursorUtil.getColumnIndexOrThrow(_cursor, "done");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final int _cursorIndexOfDirty = CursorUtil.getColumnIndexOrThrow(_cursor, "dirty");
          final List<CompletionEntity> _result = new ArrayList<CompletionEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final CompletionEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpTaskId;
            _tmpTaskId = _cursor.getString(_cursorIndexOfTaskId);
            final String _tmpDate;
            _tmpDate = _cursor.getString(_cursorIndexOfDate);
            final boolean _tmpDone;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfDone);
            _tmpDone = _tmp != 0;
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            final boolean _tmpDirty;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfDirty);
            _tmpDirty = _tmp_1 != 0;
            _item = new CompletionEntity(_tmpId,_tmpTaskId,_tmpDate,_tmpDone,_tmpUpdatedAt,_tmpDirty);
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
  public Object doneFrom(final String taskId, final String fromDate,
      final Continuation<? super List<CompletionEntity>> $completion) {
    final String _sql = "SELECT * FROM completions WHERE taskId = ? AND done = 1 AND date >= ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindString(_argIndex, taskId);
    _argIndex = 2;
    _statement.bindString(_argIndex, fromDate);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<CompletionEntity>>() {
      @Override
      @NonNull
      public List<CompletionEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTaskId = CursorUtil.getColumnIndexOrThrow(_cursor, "taskId");
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfDone = CursorUtil.getColumnIndexOrThrow(_cursor, "done");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final int _cursorIndexOfDirty = CursorUtil.getColumnIndexOrThrow(_cursor, "dirty");
          final List<CompletionEntity> _result = new ArrayList<CompletionEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final CompletionEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpTaskId;
            _tmpTaskId = _cursor.getString(_cursorIndexOfTaskId);
            final String _tmpDate;
            _tmpDate = _cursor.getString(_cursorIndexOfDate);
            final boolean _tmpDone;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfDone);
            _tmpDone = _tmp != 0;
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            final boolean _tmpDirty;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfDirty);
            _tmpDirty = _tmp_1 != 0;
            _item = new CompletionEntity(_tmpId,_tmpTaskId,_tmpDate,_tmpDone,_tmpUpdatedAt,_tmpDirty);
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

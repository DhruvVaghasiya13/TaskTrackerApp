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
public final class SelectionDao_Impl implements SelectionDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<TaskSelectionEntity> __insertionAdapterOfTaskSelectionEntity;

  private final SharedSQLiteStatement __preparedStmtOfMarkSynced;

  private final SharedSQLiteStatement __preparedStmtOfClear;

  public SelectionDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfTaskSelectionEntity = new EntityInsertionAdapter<TaskSelectionEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `task_selections` (`id`,`taskId`,`date`,`selected`,`updatedAt`,`dirty`) VALUES (?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final TaskSelectionEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getTaskId());
        statement.bindString(3, entity.getDate());
        final int _tmp = entity.getSelected() ? 1 : 0;
        statement.bindLong(4, _tmp);
        statement.bindLong(5, entity.getUpdatedAt());
        final int _tmp_1 = entity.getDirty() ? 1 : 0;
        statement.bindLong(6, _tmp_1);
      }
    };
    this.__preparedStmtOfMarkSynced = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE task_selections SET dirty = 0 WHERE id = ? AND updatedAt = ?";
        return _query;
      }
    };
    this.__preparedStmtOfClear = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM task_selections";
        return _query;
      }
    };
  }

  @Override
  public Object upsert(final TaskSelectionEntity s, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfTaskSelectionEntity.insert(s);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object upsertAll(final List<TaskSelectionEntity> list,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfTaskSelectionEntity.insert(list);
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
  public Flow<List<TaskSelectionEntity>> observeAll() {
    final String _sql = "SELECT * FROM task_selections";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"task_selections"}, new Callable<List<TaskSelectionEntity>>() {
      @Override
      @NonNull
      public List<TaskSelectionEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTaskId = CursorUtil.getColumnIndexOrThrow(_cursor, "taskId");
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfSelected = CursorUtil.getColumnIndexOrThrow(_cursor, "selected");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final int _cursorIndexOfDirty = CursorUtil.getColumnIndexOrThrow(_cursor, "dirty");
          final List<TaskSelectionEntity> _result = new ArrayList<TaskSelectionEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final TaskSelectionEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpTaskId;
            _tmpTaskId = _cursor.getString(_cursorIndexOfTaskId);
            final String _tmpDate;
            _tmpDate = _cursor.getString(_cursorIndexOfDate);
            final boolean _tmpSelected;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfSelected);
            _tmpSelected = _tmp != 0;
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            final boolean _tmpDirty;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfDirty);
            _tmpDirty = _tmp_1 != 0;
            _item = new TaskSelectionEntity(_tmpId,_tmpTaskId,_tmpDate,_tmpSelected,_tmpUpdatedAt,_tmpDirty);
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
  public Object get(final String id, final Continuation<? super TaskSelectionEntity> $completion) {
    final String _sql = "SELECT * FROM task_selections WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<TaskSelectionEntity>() {
      @Override
      @Nullable
      public TaskSelectionEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTaskId = CursorUtil.getColumnIndexOrThrow(_cursor, "taskId");
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfSelected = CursorUtil.getColumnIndexOrThrow(_cursor, "selected");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final int _cursorIndexOfDirty = CursorUtil.getColumnIndexOrThrow(_cursor, "dirty");
          final TaskSelectionEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpTaskId;
            _tmpTaskId = _cursor.getString(_cursorIndexOfTaskId);
            final String _tmpDate;
            _tmpDate = _cursor.getString(_cursorIndexOfDate);
            final boolean _tmpSelected;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfSelected);
            _tmpSelected = _tmp != 0;
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            final boolean _tmpDirty;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfDirty);
            _tmpDirty = _tmp_1 != 0;
            _result = new TaskSelectionEntity(_tmpId,_tmpTaskId,_tmpDate,_tmpSelected,_tmpUpdatedAt,_tmpDirty);
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
  public Object dirtyRows(final Continuation<? super List<TaskSelectionEntity>> $completion) {
    final String _sql = "SELECT * FROM task_selections WHERE dirty = 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<TaskSelectionEntity>>() {
      @Override
      @NonNull
      public List<TaskSelectionEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTaskId = CursorUtil.getColumnIndexOrThrow(_cursor, "taskId");
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfSelected = CursorUtil.getColumnIndexOrThrow(_cursor, "selected");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final int _cursorIndexOfDirty = CursorUtil.getColumnIndexOrThrow(_cursor, "dirty");
          final List<TaskSelectionEntity> _result = new ArrayList<TaskSelectionEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final TaskSelectionEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpTaskId;
            _tmpTaskId = _cursor.getString(_cursorIndexOfTaskId);
            final String _tmpDate;
            _tmpDate = _cursor.getString(_cursorIndexOfDate);
            final boolean _tmpSelected;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfSelected);
            _tmpSelected = _tmp != 0;
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            final boolean _tmpDirty;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfDirty);
            _tmpDirty = _tmp_1 != 0;
            _item = new TaskSelectionEntity(_tmpId,_tmpTaskId,_tmpDate,_tmpSelected,_tmpUpdatedAt,_tmpDirty);
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
  public Object fromDate(final String taskId, final String fromDate,
      final Continuation<? super List<TaskSelectionEntity>> $completion) {
    final String _sql = "SELECT * FROM task_selections WHERE taskId = ? AND date >= ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindString(_argIndex, taskId);
    _argIndex = 2;
    _statement.bindString(_argIndex, fromDate);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<TaskSelectionEntity>>() {
      @Override
      @NonNull
      public List<TaskSelectionEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTaskId = CursorUtil.getColumnIndexOrThrow(_cursor, "taskId");
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfSelected = CursorUtil.getColumnIndexOrThrow(_cursor, "selected");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final int _cursorIndexOfDirty = CursorUtil.getColumnIndexOrThrow(_cursor, "dirty");
          final List<TaskSelectionEntity> _result = new ArrayList<TaskSelectionEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final TaskSelectionEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpTaskId;
            _tmpTaskId = _cursor.getString(_cursorIndexOfTaskId);
            final String _tmpDate;
            _tmpDate = _cursor.getString(_cursorIndexOfDate);
            final boolean _tmpSelected;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfSelected);
            _tmpSelected = _tmp != 0;
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            final boolean _tmpDirty;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfDirty);
            _tmpDirty = _tmp_1 != 0;
            _item = new TaskSelectionEntity(_tmpId,_tmpTaskId,_tmpDate,_tmpSelected,_tmpUpdatedAt,_tmpDirty);
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

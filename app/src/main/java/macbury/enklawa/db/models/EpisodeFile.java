package macbury.enklawa.db.models;

import android.util.Log;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import macbury.enklawa.db.DBCallbacks;

/**
 * Created by macbury on 11.09.14.
 */
@DatabaseTable(tableName = "episode_files")
public class EpisodeFile extends BaseModel implements DBCallbacks {
  public enum Status {
    Pending, Downloading, Ready, Failed
  }

  @DatabaseField(canBeNull = false, generatedId = true)
  public int      id;
  @DatabaseField(dataType = DataType.ENUM_STRING, defaultValue = "Pending")
  public Status   status;
  @DatabaseField(columnName = "retry_count", defaultValue = "0")
  public int retryCount = 0;
  @DatabaseField(foreign=true, foreignAutoRefresh=true, columnName = "episode_id")
  public Episode  episode;

  @Override
  public void afterCreate() {

  }

  @Override
  public void afterDestroy() {
    Log.wtf("EpisodeFile", "Remove file here!");
  }

  @Override
  public void afterSave() {

  }
}
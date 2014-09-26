package macbury.enklawa.db.models;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

/**
 * Created by macbury on 23.09.14.
 */
@DatabaseTable(tableName = "enqueue_episodes")
public class EnqueueEpisode {
  public enum Status {
    Pending, Played, Playing, Paused, Finished
  }
  @DatabaseField(canBeNull = false, generatedId = true)
  public int      id;
  @DatabaseField(canBeNull = false, defaultValue = "0")
  public int      position;
  @DatabaseField(canBeNull = false, defaultValue = "0")
  public int      time;
  @DatabaseField(dataType = DataType.ENUM_STRING, defaultValue = "Pending")
  public Status   status;
  @DatabaseField(foreign=true, foreignAutoRefresh=true)
  public Episode  episode;
}

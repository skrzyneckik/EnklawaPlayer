package macbury.enklawa.services;

import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.RemoteControlClient;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Toast;

import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;

import java.util.ArrayList;

import macbury.enklawa.R;
import macbury.enklawa.db.models.EnqueueEpisode;
import macbury.enklawa.db.models.Episode;
import macbury.enklawa.managers.Enklawa;
import macbury.enklawa.managers.player.PlayerManager;
import macbury.enklawa.managers.player.PlayerManagerListener;
import macbury.enklawa.managers.player.sources.AbstractMediaSource;
import macbury.enklawa.managers.player.sources.EpisodeMediaSource;
import macbury.enklawa.receivers.MediaButtonReceiver;

public class PlayerService extends Service implements PlayerManagerListener {
  private static final String TAG = "PlayerService";
  private static final int NOTIFICATION_PLAYED_ID = 689;
  private static final String WIFI_LOCK_TAG = "EnklawaPlayerService";
  private RemoteControlClient remoteControlClient;
  private Enklawa app;
  private PlayerManager playerManager;
  private final IBinder playerManagerBinder = new PlayerBinder();
  private Bitmap currentBitmapArt;
  private WifiManager.WifiLock wifiLock;
  private static boolean running;
  private AudioManager audioManager;
  private ComponentName mediaButtonEventReceiver;

  public PlayerService() {
  }

  @Override
  public void onCreate() {
    super.onCreate();
    this.audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
    this.wifiLock     = ((WifiManager) getSystemService(Context.WIFI_SERVICE)) .createWifiLock(WifiManager.WIFI_MODE_FULL, WIFI_LOCK_TAG);

    this.app           = Enklawa.current();
    this.playerManager = new PlayerManager(getApplicationContext());
    playerManager.addListener(this);

    wifiLock.acquire();
    running = true;
    app.broadcasts.playerStatusChanged();

    setupRemoteControlClient();

    registerReceiver(headsetDisconnected, new IntentFilter(Intent.ACTION_HEADSET_PLUG));
    registerReceiver(audioBecomingNoisy, new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY));
  }

  private void setupRemoteControlClient() {
    Intent mediaButtonIntent         = new Intent(Intent.ACTION_MEDIA_BUTTON);
    this.mediaButtonEventReceiver    = new ComponentName(getPackageName(), MediaButtonReceiver.class.getName());
    mediaButtonIntent.setComponent(mediaButtonEventReceiver);

    PendingIntent mediaPendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, mediaButtonIntent, 0);
    remoteControlClient              = new RemoteControlClient(mediaPendingIntent);
    remoteControlClient.setTransportControlFlags(RemoteControlClient.FLAG_KEY_MEDIA_PLAY_PAUSE);

    audioManager.registerMediaButtonEventReceiver(mediaButtonEventReceiver);
    audioManager.registerRemoteControlClient(remoteControlClient);
  }

  private void refreshRemoteControlClient(int playstate) {
    if (playerManager.isRunning()) {
      remoteControlClient.setPlaybackState(playstate);
      RemoteControlClient.MetadataEditor editor = remoteControlClient.editMetadata(false);

      AbstractMediaSource ams = playerManager.getCurrentMediaSource();

      editor.putString(MediaMetadataRetriever.METADATA_KEY_TITLE, ams.getTitle());
      editor.putString(MediaMetadataRetriever.METADATA_KEY_ALBUM, ams.getSummary());
      editor.putLong(MediaMetadataRetriever.METADATA_KEY_DURATION, ams.getDuration());
      //editor.putLong(MediaMetadataRetriever.METADATA_KEY_LOCATION, ams.getPosition());
      editor.apply();
    }

  }

  @Override
  public void onDestroy() {
    playerManager.removeListener(this);
    playerManager.destroy();
    wifiLock.release();
    running = false;
    unregisterReceiver(headsetDisconnected);
    unregisterReceiver(audioBecomingNoisy);
    audioManager.unregisterMediaButtonEventReceiver(mediaButtonEventReceiver);
    super.onDestroy();
  }

  @Override
  public IBinder onBind(Intent intent) {
    return playerManagerBinder;
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    if (app.intents.haveKeyCode(intent)) {
      int keycode = app.intents.getKeyCode(intent);
      handleKeycode(keycode);
    } else if (app.intents.havePauseExtra(intent)) {
      playerManager.pause();
    } else if (app.intents.haveCancelExtra(intent)) {
      playerManager.cancel();
    } else if (app.intents.haveEpisode(intent)) {
      Episode episode               = app.db.episodes.find(app.intents.getEpisodeId(intent));
      EnqueueEpisode enqueueEpisode = app.db.queue.createFromEpisode(episode);
      Log.i(TAG, "Recived episode to play:" + episode.name);
      if (playerManager.is(enqueueEpisode)) {
        playerManager.play();
      } else {
        app.db.queue.moveToBegining(enqueueEpisode);
        playerManager.restart();
      }
    } else {
      Log.i(TAG, "Play all enqeued episodes");
      playerManager.start();
    }
    return super.onStartCommand(intent, flags, startId);
  }

  private void handleKeycode(int keycode) {
    switch (keycode) {
      case KeyEvent.KEYCODE_MEDIA_PLAY:
        playerManager.start();
      break;

      case KeyEvent.KEYCODE_MEDIA_PAUSE:
        playerManager.pause();
      break;

      default:
        Toast.makeText(this, getString(R.string.unknown_media_key), Toast.LENGTH_SHORT).show();
      break;
    }
  }

  @Override
  public void onInitialize(PlayerManager manager, AbstractMediaSource mediaSource) {
    refreshRemoteControlClient(RemoteControlClient.PLAYSTATE_BUFFERING);
    Ion.with(this).load(mediaSource.getPreviewArtUri().toString()).asBitmap().setCallback(new FutureCallback<Bitmap>() {
      @Override
      public void onCompleted(Exception e, Bitmap result) {
        PlayerService.this.currentBitmapArt = result;
        updateNotification();
      }
    });
    updateNotification();
  }

  private void updateNotification() {
    EpisodeMediaSource ems = (EpisodeMediaSource) playerManager.getCurrentMediaSource();
    startForeground(NOTIFICATION_PLAYED_ID, Enklawa.current().notifications.playEpisode(currentBitmapArt, ems.getEnqueueEpisode()));
  }

  @Override
  public void onFinishAll(PlayerManager manager) {
    stopSelf();
  }

  @Override
  public void onPlay(PlayerManager manager, AbstractMediaSource mediaSource) {
    refreshRemoteControlClient(RemoteControlClient.PLAYSTATE_PLAYING);
    updateNotification();
  }

  @Override
  public void onPause(PlayerManager manager, AbstractMediaSource mediaSource) {
    refreshRemoteControlClient(RemoteControlClient.PLAYSTATE_PAUSED);
    updateNotification();
  }

  @Override
  public void onFinish(PlayerManager manager, AbstractMediaSource mediaSource) {
    updateNotification();
  }

  @Override
  public void onBufferMedia(PlayerManager manager, AbstractMediaSource mediaSource) {

  }

  @Override
  public void onMediaUpdate(PlayerManager playerManager, AbstractMediaSource currentMediaSource) {

  }

  public class PlayerBinder extends Binder {
    public PlayerManager getPlayerManager() {
      return PlayerService.this.playerManager;
    }

    public AbstractMediaSource getCurrentMediaSource() {
      return playerManager.getCurrentMediaSource();
    }

    public void addListener(PlayerManagerListener listener) {
      playerManager.addListener(listener);
    }

    public void removeListener(PlayerManagerListener listener) {
      playerManager.removeListener(listener);
    }

    public boolean isPlaying() {
      return playerManager.isPlaying();
    }
  }

  public static boolean isRunning() {
    return running;
  }

  private BroadcastReceiver headsetDisconnected = new BroadcastReceiver() {
    private static final String TAG = "headsetDisconnected";
    private static final int UNPLUGGED = 0;
    @Override
    public void onReceive(Context context, Intent intent) {
      if (intent.getAction() == Intent.ACTION_HEADSET_PLUG) {
        int state = intent.getIntExtra("state", -1);
        if (state != -1) {
          Log.d(TAG, "Headset plug event. State is " + state);
          if (state == UNPLUGGED) {
            Log.d(TAG, "Headset was unplugged during playback.");
            playerManager.pause();
          }
        } else {
          Log.e(TAG, "Received invalid ACTION_HEADSET_PLUG intent");
        }
      }
    }
  };

  private BroadcastReceiver audioBecomingNoisy = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
      Log.d(TAG, "Pausing playback because audio is becoming noisy");
      playerManager.pause();
    }
  };

}

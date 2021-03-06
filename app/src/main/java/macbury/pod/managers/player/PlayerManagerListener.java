package macbury.pod.managers.player;

import macbury.pod.managers.player.sources.AbstractMediaSource;

/**
 * Created by macbury on 25.09.14.
 */
public interface PlayerManagerListener {
  public void onInitialize(PlayerManager manager, AbstractMediaSource mediaSource);
  public void onFinishAll(PlayerManager manager);
  public void onPlay(PlayerManager manager, AbstractMediaSource mediaSource);
  public void onPause(PlayerManager manager, AbstractMediaSource mediaSource);
  public void onFinish(PlayerManager manager, AbstractMediaSource mediaSource);
  public void onBufferMedia(PlayerManager manager, AbstractMediaSource mediaSource);
  public void onMediaUpdate(PlayerManager playerManager, AbstractMediaSource currentMediaSource);

  public void onMediaError(PlayerManager playerManager, int extra);
}

package com.antsapps.triples;

import java.util.concurrent.TimeUnit;

import android.os.Bundle;
import android.text.format.DateUtils;
import android.view.ViewStub;
import android.widget.TextView;
import android.widget.Toast;

import com.antsapps.triples.backend.Application;
import com.antsapps.triples.backend.ArcadeGame;
import com.antsapps.triples.backend.Card;
import com.antsapps.triples.backend.Game;
import com.antsapps.triples.backend.OnTimerTickListener;
import com.google.android.gms.games.GamesClient;
import com.google.android.gms.games.leaderboard.LeaderboardVariant;
import com.google.android.gms.games.leaderboard.OnScoreSubmittedListener;
import com.google.android.gms.games.leaderboard.SubmitScoreResult;
import com.google.common.collect.ImmutableList;

/**
 * Arcade Game
 */
public class ArcadeGameActivity extends BaseGameActivity implements OnTimerTickListener,
    Game.OnUpdateCardsInPlayListener {

  private ArcadeGame mGame;
  private Application mApplication;

  @Override
  protected void init(Bundle savedInstanceState) {
    mApplication = Application.getInstance(this);

    if (getIntent().hasExtra(Game.ID_TAG)) {
      // We are being created from the game list.
      mGame = mApplication.getArcadeGame(getIntent().getLongExtra(Game.ID_TAG, 0));
    } else if (savedInstanceState != null) {
      // We are being restored
      mGame = mApplication.getArcadeGame(savedInstanceState.getLong(Game.ID_TAG));
    } else {
      throw new IllegalArgumentException(
          "No savedInstanceState or intent containing key");
    }

    ViewStub stub = (ViewStub) findViewById(R.id.status_bar);
    stub.setLayoutResource(R.layout.arcade_statusbar);
    stub.inflate();
    mGame.addOnTimerTickListener(this);
    mGame.addOnUpdateCardsInPlayListener(this);
  }

  @Override
  protected Game getGame() {
    return mGame;
  }

  @Override
  protected void saveGame() {
    mApplication.saveArcadeGame(mGame);
  }

  @Override
  protected void onDestroy() {
    mGame.removeOnUpdateCardsInPlayListener(this);
    mGame.removeOnTimerTickListener(this);
    super.onDestroy();
  }

  @Override
  public void onTimerTick(final long elapsedTime) {
    TextView timer = (TextView) findViewById(R.id.timer_value_text);
    timer.setText(DateUtils.formatElapsedTime(TimeUnit
        .MILLISECONDS
        .toSeconds(ArcadeGame.TIME_LIMIT_MS - elapsedTime)));
  }

  @Override
  public void onUpdateCardsInPlay(ImmutableList<Card> newCards,
                                  ImmutableList<Card> oldCards,
                                  int numRemaining,
                                  int numTriplesFound) {
    TextView triplesFound = (TextView) findViewById(R.id.triples_found_text);
    triplesFound.setText(String.valueOf(numTriplesFound));
  }

  protected Class<? extends BaseGameListActivity> getParentClass() {
    return ArcadeGameListActivity.class;
  }

  protected void submitScore() {
    if (mGame.getGameState() != Game.GameState.COMPLETED) {
      return;
    }
    mHelper.getGamesClient().submitScoreImmediate(new OnScoreSubmittedListener() {
      @Override
      public void onScoreSubmitted(int status, SubmitScoreResult submitScoreResult) {
        String message = null;
        switch (status) {
          case GamesClient.STATUS_OK:
            if (submitScoreResult.getScoreResult(LeaderboardVariant.TIME_SPAN_ALL_TIME).newBest) {
              message = "Congratulations! That's your best score ever.";
            } else if (submitScoreResult.getScoreResult(LeaderboardVariant.TIME_SPAN_WEEKLY).newBest) {
              message = "Well Done! That's your best score this week.";
            } else if (submitScoreResult.getScoreResult(LeaderboardVariant.TIME_SPAN_DAILY).newBest) {
              message = "Nice! That's your best score today.";
            } else {
              message = "You've done better today - keep trying!";
            }
            break;
          case GamesClient.STATUS_NETWORK_ERROR_OPERATION_DEFERRED :
            message = "Score will be submitted when next connected.";
            break;
          default:
            message = "Score could not be submitted";
            break;
        }
        Toast.makeText(ArcadeGameActivity.this, message, Toast.LENGTH_LONG).show();
      }
    }, GamesServices.Leaderboard.ARCADE, mGame.getNumTriplesFound());
  }
}

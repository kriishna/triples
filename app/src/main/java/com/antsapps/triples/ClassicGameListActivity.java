package com.antsapps.triples;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import com.antsapps.triples.backend.Application;
import com.antsapps.triples.backend.ClassicGame;
import com.antsapps.triples.backend.ClassicStatistics;
import com.antsapps.triples.backend.Game;
import com.antsapps.triples.backend.Period;
import com.antsapps.triples.stats.BaseStatisticsFragment;
import com.antsapps.triples.stats.ClassicStatisticsFragment;

public class ClassicGameListActivity extends BaseGameListActivity {

  private Application mApplication;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    mApplication = Application.getInstance(getApplication());
  }

  @Override
  protected Class<? extends BaseStatisticsFragment> getStatisticsFragmentClass() {
    return ClassicStatisticsFragment.class;
  }

  @Override
  protected Class<? extends BaseCurrentGameListFragment> getCurrentGamesFragment() {
    return ClassicCurrentGameListFragment.class;
  }

  @Override
  protected Intent createNewGame() {
    ClassicGame game = ClassicGame.createFromSeed(System.currentTimeMillis());
    mApplication.addClassicGame(game);
    Intent newGameIntent = new Intent(getBaseContext(), ClassicGameActivity.class);
    newGameIntent.putExtra(Game.ID_TAG, game.getId());
    return newGameIntent;
  }
}
package com.antsapps.triples.backend;

import java.util.List;

import com.google.common.collect.Lists;

public class Statistics {

  protected final List<Game> mGamesInPeriod;

  Statistics(Iterable<? extends Game> iterable, Period period) {
    mGamesInPeriod = period.filter(iterable);
  }

  public int getNumGames() {
    return mGamesInPeriod.size();
  }

  public List<Game> getData() {
    return Lists.newArrayList(mGamesInPeriod);
  }
}

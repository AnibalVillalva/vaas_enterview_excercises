package com.getvaas.excercises.core.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public class Match {

    public Long id;
    public LocalDate diaDelPartido;
    public List<Team> teamList;
    public Map<Long, Team> matchMap; //Key = team's score, value  = team
    public Map<Long, Player> playersYellowCardsMap;//Key = player's quantity of yellow cards, player in cuestion
    public List<Player> playersRedCardList; //Players with red card

}

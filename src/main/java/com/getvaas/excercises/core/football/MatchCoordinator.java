package com.getvaas.excercises.core.football;

import com.getvaas.excercises.core.domain.Fixture;
import com.getvaas.excercises.core.domain.Match;
import com.getvaas.excercises.core.domain.Player;
import com.getvaas.excercises.core.domain.Team;
import com.getvaas.excercises.repository.login.FixtureRepository;
import com.getvaas.excercises.repository.login.MatchRepository;
import com.getvaas.excercises.repository.login.PlayerRepository;
import com.getvaas.excercises.repository.login.TeamRepository;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.*;

import static org.springframework.beans.factory.config.BeanDefinition.SCOPE_SINGLETON;

@Component
@Scope(SCOPE_SINGLETON)
public class MatchCoordinator {
    public List<Match> matchesList = new ArrayList<>();

    private FixtureRepository fixtureRepository;
    private MatchRepository matchRepository;
    private PlayerRepository playerRepository;
    private TeamRepository teamRepository;

    Team winnerTeam = null;

    public boolean saveInfo(Fixture fixture, Match match, List<Player> playerList, List<Team> teamList) {

        fixtureRepository.save(fixture);
        matchRepository.save(match);
        playerRepository.saveAll(playerList);
        teamRepository.saveAll(teamList);

        matchesList.add(match);

        return Boolean.TRUE;

    }
    public List<Match> retrieveAllMatches() {

        if(matchesList.size() > 0) {
            return matchesList;
        }
        return matchRepository.findAll();

    }

    public boolean saveMatch(Match match) {
        
        matchRepository.save(match);
        return  Boolean.TRUE;
        
    }

    public Optional<Team> retrieveFirstTeam(Fixture fixture) {
        
        List<Match> matchList = new ArrayList<>(fixtureRepository.findById(fixture.id.intValue())
                .get()
                .listaDePartidos);

        Map<Team, Long> teamPointsMap = new HashMap<>();

        for (Match match: matchList) {
            
            Long winnerScore = 0L;
            boolean isScoreEven = Boolean.FALSE;

            for (Map.Entry<Long, Team> mapEntries: match.matchMap.entrySet()) {

                if(winnerScore == mapEntries.getKey()) {
                    isScoreEven = Boolean.TRUE;
                } else if(mapEntries.getKey() > winnerScore) {
                    winnerScore = mapEntries.getKey();
                    isScoreEven = Boolean.FALSE;
                }
            }

            if(isScoreEven) {
                Long team1Score = teamPointsMap.get(match.teamList.get(0)) + 1;
                Long team2Score = teamPointsMap.get(match.teamList.get(1)) + 1;
                teamPointsMap.put(match.teamList.get(0), team1Score);
                teamPointsMap.put(match.teamList.get(1), team2Score);
            } else {
                Long winnerTeamScore = teamPointsMap.get(winnerTeam) + 3;
                teamPointsMap.put(winnerTeam, winnerTeamScore);
            }
        }

        Map.Entry<Team, Long> winnerTeam = teamPointsMap.entrySet()
                .stream()
                .max((t1, t2) -> t1.getValue() > t2.getValue() ? t1.getValue().intValue() : t2.getValue().intValue())
                .get();

        return Optional.of(winnerTeam.getKey());

    }


}

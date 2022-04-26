package com.getvaas.excercises.core.domain;

import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode
public class Team {

    public Long id;
    public List<Player> playerList;

}

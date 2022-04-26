package com.getvaas.excercises.repository.login;

import com.getvaas.excercises.core.domain.Player;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlayerRepository extends JpaRepository<Player, Integer> {
}

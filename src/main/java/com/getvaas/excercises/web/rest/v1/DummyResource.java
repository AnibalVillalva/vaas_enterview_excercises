package com.getvaas.excercises.web.rest.v1;

import com.getvaas.excercises.core.football.MatchCoordinator;
import com.getvaas.excercises.repository.login.MatchRepository;
import com.getvaas.excercises.repository.login.TeamRepository;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@AllArgsConstructor
@RestController
@RequestMapping("/api/v1/dummy")
public class DummyResource {
    private MatchCoordinator matchCoordinator;
    private TeamRepository teamRepository;

    @GetMapping("/get-first-team")
    @ApiOperation(value = "Hello World", produces = "application/json")
    public ResponseEntity<?> getFirstTeam() {

        return ResponseEntity.ok(matchCoordinator.retrieveAllMatches());

    }

    @GetMapping("/get-team")
    @ApiOperation(value = "Hello World", produces = "application/json")
    public ResponseEntity<?> getTeam(@RequestAttribute(name = "teamId") Integer teamId) {

        return ResponseEntity.ok(teamRepository.findById(teamId));

    }

}

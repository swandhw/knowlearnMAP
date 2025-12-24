package com.knowlearnmap.ontologyToArango.runner;

import com.knowlearnmap.ontologyToArango.service.OntologyToArangoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("sync")
@RequiredArgsConstructor
@Slf4j
public class OntologySyncRunner implements CommandLineRunner {

    private final OntologyToArangoService ontologyToArangoService;

    @Override
    public void run(String... args) throws Exception {
        log.info("Starting Ontology Sync for Workspace 4...");
        try {
            // dropExist = true濡??ㅼ젙?섏뿬 留ㅻ쾲 珥덇린?????숆린??(?꾩슂???곕씪 蹂寃?媛??
            ontologyToArangoService.syncOntologyToArango(4L, true);
            log.info("Ontology Sync for Workspace 4 completed successfully.");
        } catch (Exception e) {
            log.error("Ontology Sync for Workspace 4 failed.", e);
        }
    }
}

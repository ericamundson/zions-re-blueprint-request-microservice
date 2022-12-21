package com.zions.pipeline.services.blueprint.endpoint

import org.springframework.web.bind.annotation.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity;
import com.zions.pipeline.services.blueprint.model.*
import com.zions.pipeline.services.blueprint.BlueprintRepoService
import com.zions.pipeline.services.blueprint.BlueprintExecutorService
import com.zions.pipeline.services.feedback.model.LogNode
import groovy.json.JsonBuilder
import groovy.util.logging.Slf4j


@CrossOrigin
@Slf4j
@RestController
@RequestMapping('blueprint')
class BlueprintRequestController {
	@Autowired
	BlueprintRepoService blueprintRepoService
	
	@Autowired
	BlueprintExecutorService blueprintExecutorService
	
	@GetMapping('repositories')
	List<Folder> getRepositories() {
		log.info("Calling getAllRepos ...")
		def repositories = blueprintRepoService.getAllRepos()
		List<Folder> bpRepos = []
		for (def orepo in repositories) {
			log.info("Adding repo ${orepo.name}")
			//Folder aRepo = (Folder)orepo.repo
			bpRepos.add((Folder)orepo.repo)
		}
		return bpRepos
	}
	
	@PostMapping('execute')
	Object executeBlueprint(@RequestBody BlueprintExecuteDto bpExecuteData) {
		// do I need to start a separate thread here for the bluprint execution
		blueprintExecutorService.executeBlueprint(bpExecuteData)
		return null
	}

	@GetMapping('repos/{repoName}')
	Folder getBlueprintRepoByName(@PathVariable String name) {
		Folder aRepo = null
		def bpRepo = blueprintRepoService.getRepoByName(name)
		if (bpRepo != null) {
			aRepo = (Folder)bpRepo.repo
		}
		return aRepo
	}

	@GetMapping('executionLogs/{executionId}')
	List<LogNode> getExecutionLogs(@PathVariable String executionId) {
		log.info("Calling getBlueprintExecutionLogs ...")
		LogNode[] logNodes = blueprintRepoService.getBlueprintExecutionLogs(executionId)
		log.info("Returning blueprint execution logs: \n ${logNodes}")
		return logNodes
	}
	
	@GetMapping('checkStatus/{executionId}')
	Object getExecutionStatus(@PathVariable String executionId) {
		log.info("Getting BlueprintExecutionLogs to check status ...")
		LogNode[] logNodes = blueprintRepoService.getBlueprintExecutionLogs(executionId)
		// check for logNode type / context
		log.info("Returning blueprint execution status ...")
		return ["status": "Running", "result": "Succeeded"]
	}

}

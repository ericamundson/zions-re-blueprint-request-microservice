package com.zions.pipeline.services.blueprint.endpoint

import org.springframework.web.bind.annotation.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity;
import com.zions.pipeline.services.blueprint.model.*
import com.zions.pipeline.services.blueprint.BlueprintRepoService
import com.zions.pipeline.services.mixins.FeedbackTrait
import com.zions.pipeline.services.yaml.template.RemoteBlueprintTemplateInterpretorService
import groovy.json.JsonBuilder
import groovy.util.logging.Slf4j


@CrossOrigin
@Slf4j
@RestController
@RequestMapping('blueprint')
class BlueprintRequestController implements FeedbackTrait {
	@Autowired
	BlueprintRepoService blueprintRepoService
	
	@Autowired
	RemoteBlueprintTemplateInterpretorService remoteBlueprintTemplateInterpretorService
	
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
		String executionId = "${bpExecuteData.executionId}"
		logContextStart(executionId, "Process blueprint")

		logInfo(pipelineId, bpExecuteData)
		//def answers = bpExecuteData.answers
		//println "Answers:  ${answers}"
		remoteBlueprintTemplateInterpretorService.outputPipeline(bpExecuteData)
		logContextComplete(executionId, "Blueprint generated from templates")
		// Probably don't need anything after this
		if (runViaMicroservice) {
			def result = remoteBlueprintTemplateInterpretorService.runExecutableYaml(true)

			remoteBlueprintTemplateInterpretorService.runPullRequestOnChanges()
			if (result && result.isComplete) {
				logContextStart(executionId, "Completed")
				logCompleted(executionId, "Blueprint processing is complete!")
				logContextComplete(executionId, "Completed")
			}
		} else {
			remoteBlueprintTemplateInterpretorService.runExecutableYaml()
		}

		return null
	}

	@GetMapping('repos/{repoName}')
	Folder getBlueprintRepoByName(@PathVariable String name) {
		Folder aRepo = null
		def bpRepo = blueprintRepoService.getRepoByName(name)
		if (repo != null) {
			aRepo = new Folder(bpRepo.json)
		}
		return aRepo
	}
	
}

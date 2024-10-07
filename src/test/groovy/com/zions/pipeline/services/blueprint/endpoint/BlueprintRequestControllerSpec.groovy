package com.zions.pipeline.services.blueprint.endpoint;

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Profile
import org.springframework.context.annotation.PropertySource
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.test.context.ContextConfiguration
import org.springframework.mail.javamail.JavaMailSenderImpl
import org.spockframework.spring.*

import com.zions.common.services.cache.ICacheManagementService
import com.zions.common.services.cache.CacheManagementService
import com.zions.common.services.command.CommandManagementService
import com.zions.common.services.rest.IGenericRestClient
import com.zions.common.services.test.DataGenerationService
import com.zions.vsts.services.tfs.rest.GenericRestClient
import com.zions.xld.services.ci.CiService
import com.zions.xld.services.deployment.DeploymentService
import com.zions.xld.services.rest.client.XldGenericRestClient
import spock.lang.Ignore
import spock.lang.Specification
import spock.mock.DetachedMockFactory
import com.zions.pipeline.services.blueprint.BlueprintRepoService
import com.zions.pipeline.services.blueprint.db.BlueprintRepo
import com.zions.pipeline.services.blueprint.model.*
import com.zions.pipeline.services.db.PipelineLogItem
import com.zions.pipeline.services.feedback.FeedbackService
import com.zions.pipeline.services.feedback.model.LogNode
import com.mongodb.MongoCredential
import com.mongodb.ServerAddress
import spock.lang.Ignore

import groovy.json.JsonSlurper


@ContextConfiguration(classes=[BlueprintRequestControlConfig])
class BlueprintRequestControllerSpec extends Specification {
	@SpringBean
	BlueprintRepoService blueprintRepoService = Stub()
	
	@SpringBean
	BlueprintRequestController underTest = new BlueprintRequestController()
	
	@SpringBean
	FeedbackService feedbackService = new FeedbackService()

	@Ignore
	def 'Get list of all repositories'() {
		setup: 'test data for stubs'
		String json = this.getClass().getResource('/testdata/repos.json').text
		def repos =  new JsonSlurper().parseText(json)
		//def repos = dataGenerationService.generate('/testdata/repos.json')
		//def repos = dataGenerationService.generate('/testdata/repos.json', true)
		underTest.blueprintRepoService = blueprintRepoService
		
		List<BlueprintRepo> orepos = []
		for (def r in repos) {
			println("Adding ${r}")
			BlueprintRepo or = new BlueprintRepo(r)
			orepos.add(or)
		}
		println("BlueprintRepo list: \n${orepos}")
		
		and: 'stub getAllRepos'
		blueprintRepoService.getAllRepos() >> { args ->
			return orepos
		}
		
		when: 'run web request for getRepositories'
		List<Folder> trepos = underTest.getRepositories()
		
		then:
		trepos.size() > 0 
		println "Blueprint repo count: ${trepos.size()}"
		
	}
	
	@Ignore
	def 'Get blueprint repository by name'() {
		setup: 'test data for stubs'
		String json = this.getClass().getResource('/testdata/repo_by_name.json').text
		def repo =  new JsonSlurper().parseText(json)
		underTest.blueprintRepoService = blueprintRepoService
		
		Folder repoAsFolder = new Folder(repo.repo.name)
		repoAsFolder.folders = repo.repo.folders
		repoAsFolder.blueprints = repo.repo.blueprints
		BlueprintRepo orepo = new BlueprintRepo(repo.name, repo.url, repoAsFolder)
		println("BlueprintRepo: \n${orepo}")
		
		and: 'stub getAllRepos'
		blueprintRepoService.getRepoByName(_) >> { args ->
			return orepo
		}
		
		when: 'run web request for getRepositories'
		Folder trepo = underTest.getBlueprintRepoByName('zions-blueprints')
		
		then:
		trepo.name.equals("zions-blueprints") 
		println "Blueprint repo name: ${trepo.name}"
		
	}
	
	@Ignore
	def 'Get logs for blueprint execution'() {
		setup: 'test data for stubs'
		String json = this.getClass().getResource('/testdata/execution_logs.json').text
		def logs =  new JsonSlurper().parseText(json)
		//def repos = dataGenerationService.generate('/testdata/repos.json')
		//def repos = dataGenerationService.generate('/testdata/repos.json', true)
		underTest.blueprintRepoService = blueprintRepoService
		
		List<PipelineLogItem> ilogs = []
		for (def l in logs) {
			//println("Adding ${l}")
			PipelineLogItem oln = new PipelineLogItem(l)
			ilogs.add(oln)
		}

		List<LogNode> ologs = feedbackService.buildLogNodes(logs)
		
		and: 'stub getBlueprintExecutionLogs'
		blueprintRepoService.getBlueprintExecutionLogs(_) >> { args ->
			return ologs
		}
		
		when: 'run web request for getExecutionLogs'
		List<LogNode> elogs = underTest.getExecutionLogs()
		
		then:
		elogs.size() > 0 
		println "Blueprint execution logs: ${elogs.size()}"
		
	}
	
}

@TestConfiguration
@ComponentScan(["com.zions.pipeline.services", "com.zions.vsts.services","com.zions.common.services.notification","com.zions.xld.services","com.zions.xlr.services","com.zions.common.services.test", "com.zions.common.services.vault"])
@PropertySource("classpath:test.properties")
@Profile('unittest')
class BlueprintRequestControlConfig {
	def factory = new DetachedMockFactory()
	
	
}


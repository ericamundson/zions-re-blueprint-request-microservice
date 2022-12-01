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
import com.mongodb.MongoClient
import com.mongodb.MongoClientOptions
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
import com.zions.blueprint.pipeline.services.blueprint.model.*
import com.mongodb.MongoCredential
import com.mongodb.ServerAddress

import groovy.json.JsonSlurper


@ContextConfiguration(classes=[BlueprintRequestControlConfig])
class BlueprintRequestControllerSpec extends Specification {
	@SpringBean
	BlueprintRepoService blueprintRepoService = Stub()
	
	@SpringBean
	DataGenerationService dataGenerationService = new DataGenerationService()

	def 'Query for repositories'() {
		setup: 'test data for stubs'
		def repos = dataGenerationService.generate('/testdata/repos.json')
		
		underTest.blueprintRepoService = blueprintRepoService
		
		List<Folder> orepos = []
		for (def r in repos) {
			Folder or = new Folder(r)
			orepos.add(op)
		}
		
		and: 'stub getProjects'
		blueprintRepoService.getAllRepos(_) >> { args ->
			return orepos
		}
		
		when: 'run web request for getRepositories'
		List<Folder> trepos = underTest.getRepositories()
		
		then:
		trepos.size() > 0 
		println "Blueprint repo count: ${trepos.size()}"
		
	}
	
}

@TestConfiguration
@ComponentScan(["com.zions.pipeline.services", "com.zions.vsts.services","com.zions.common.services.notification","com.zions.xld.services","com.zions.xlr.services","com.zions.common.services.test", "com.zions.common.services.vault"])
@PropertySource("classpath:test.properties")
@Profile('unittest')
class BlueprintRequestControlConfig {
	def factory = new DetachedMockFactory()
	
	
}


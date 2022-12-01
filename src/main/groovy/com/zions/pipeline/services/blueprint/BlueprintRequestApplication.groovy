package com.zions.pipeline.services.blueprint;

import com.zions.common.services.cli.action.CliAction
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.Banner
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration
import org.springframework.boot.autoconfigure.ldap.LdapAutoConfiguration
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration
import org.springframework.boot.autoconfigure.mongo.embedded.EmbeddedMongoAutoConfiguration


/* 
 * Provide API for blueprint loading and execution. 
 */

@SpringBootApplication(exclude=[LdapAutoConfiguration,RabbitAutoConfiguration,MongoAutoConfiguration,MongoDataAutoConfiguration,EmbeddedMongoAutoConfiguration])
public class BlueprintRequestApplication {


	public static void main(String[] args) {
		
        /*Call AppConfigTest.class to define custom resource path*/
		
		SpringApplication app = new SpringApplication(BlueprintRequestApplication.class);
		app.run(args);
				

	}
}

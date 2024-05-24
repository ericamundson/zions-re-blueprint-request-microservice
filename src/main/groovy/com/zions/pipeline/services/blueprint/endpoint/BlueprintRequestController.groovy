package com.zions.pipeline.services.blueprint.endpoint

import org.springframework.web.bind.annotation.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity;
import com.zions.pipeline.services.blueprint.model.*
import com.zions.pipeline.services.blueprint.utils.ChartRunner
import com.zions.pipeline.services.blueprint.BlueprintRepoService
import com.zions.pipeline.services.blueprint.BlueprintExecutorService
import com.zions.pipeline.services.blueprint.chart.model.*
import com.zions.pipeline.services.feedback.model.LogNode
import com.zions.pipeline.services.re.rest.ReGenericRestClient
import com.zions.vsts.services.code.CodeManagementService
import com.zions.vsts.services.admin.project.ProjectManagementService
import com.zions.nexus.services.search.SearchService
import com.zions.nexus.services.store.RepositoryService
import groovy.json.JsonBuilder
import groovy.json.JsonOutput
import groovy.yaml.YamlSlurper
import groovy.yaml.YamlBuilder
import groovy.util.logging.Slf4j
import groovy.io.FileType
import groovyx.net.http.ContentType
import org.springframework.http.HttpStatus
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import static com.fasterxml.jackson.dataformat.yaml.YAMLGenerator.Feature.LITERAL_BLOCK_STYLE


@CrossOrigin
@Slf4j
@RestController
@RequestMapping('blueprint')
class BlueprintRequestController {
	@Value('${values.options.folder:values_options}')
	String valuesOptionsFolder
	
	@Value('${pipeline.chart.cache.dir:/pipeline/chart-cache}')
	File pipelineChartCacheDir
	
	@Value('${pipeline.chart.download.dir:/temp/chart-download}')
	File pipelineChartDownloadDir
	
	@Value('${nexus.url:}')
	String nexusUrl;


	@Autowired
	BlueprintRepoService blueprintRepoService
	
	@Autowired
	BlueprintExecutorService blueprintExecutorService
	
	@Autowired
	SearchService searchService
	
	@Autowired
	RepositoryService repositoryService
	
	@Autowired
	ReGenericRestClient reGenericRestClient


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
	
	@GetMapping('basecharts')
	List<ChartValuesData> getBaseCharts() {
		def chartData = searchService.getCiCDChartList('latest')
		List<ChartValuesData> allValuesData = []
		if (chartData && chartData.items) {
			for (def chartItem in chartData.items) {
				File cLocation = loadChart(chartItem)
				File values_options = new File(cLocation, valuesOptionsFolder)
				if (values_options.exists()) {
					List<ChartValuesData> valuesDataList = generateChartValuesData(cLocation)
					allValuesData.addAll(valuesDataList)
				}
			}
		}
		return allValuesData
	}
	
	@PostMapping('runChart')
	def runChart(@RequestBody ChartValuesData valuesData) {
		
		File cLocation = loadChartForRun(valuesData)
		File vFile = new File(cLocation, "${valuesOptionsFolder}/${valuesData.valuesFileName}")
		if (vFile.exists()) {
			String outStr = vFile.text
			outStr = outStr.replaceAll(/(#)( |\S)*$/, '')
			def vals = [:]
			try {
				vals = new YamlSlurper().parseText(outStr)
			} catch (e) {
				e.printStackTrace()
				//log.error(e.message )
			}
			List<String> oVals = []
			if (vals.'interface' && vals.'interface'.overrideValues && vals.'interface'.overrideValues.size() > 0) {
				if (valuesData.valueOverrideSettings.size() > 0) {
					vals.'interface'.overrideValues = []
					for (def ov in valuesData.valueOverrideSettings) {
						vals.'interface'.overrideValues.add([key: ov.key, value: ov.value])
					}
				}
			}
			if (valuesData.applyChartArguments.size() > 0) {
				vals.'interface'.applyArgs = []
				for (def arg in valuesData.applyChartArguments) {
					vals.'interface'.applyArgs.add([key: arg.key, value: arg.value])
				}
			}
			def yb = new YamlBuilder()
		
			yb(vals)
		
			String valsYaml = new ObjectMapper(new YAMLFactory().configure(LITERAL_BLOCK_STYLE, true)).writeValueAsString(yb.content)

			def result = runCLIApplyChart(valsYaml, valuesData.chartName, valuesData.repoUrl, valuesData.valuesFileName)

			if (result || result.exitValue != 0) {
				return new ResponseEntity<String>(result.logs, HttpStatus.EXPECTATION_FAILED)
				
			} else {
				return new ResponseEntity<String>(result.logs, HttpStatus.OK)
				
			}		
		}
		return new ResponseEntity<String>("Values selection doesn't exist", HttpStatus.EXPECTATION_FAILED)
		
	}
	
	File loadChartForRun(ChartValuesData valuesData) {
		String pipelineChartName = valuesData.chartName
		if (!pipelineChartCacheDir.exists()) {
			pipelineChartCacheDir.mkdirs()
		}
		File cOutDir = new File(pipelineChartCacheDir, pipelineChartName)
		if (!cOutDir.exists()) {
			cOutDir.mkdirs()
		}
		if (!pipelineChartDownloadDir.exists()) {
			pipelineChartDownloadDir.mkdirs()
		}
		
		AntBuilder ant = new AntBuilder()
		// https://nexus.cs.zionsbank.com/repository/zions-cicd-charts/ADO.Build.Deploy/latest/ADO.Build.Deploy-latest.tgz
		ant.get( dest: "${pipelineChartDownloadDir}/${pipelineChartName}-latest.tgz",  src: "${nexusUrl}/zions-cicd-charts/${pipelineChartName}/latest/${pipelineChartName}-latest.tgz")
		
		File cLocation = new File(pipelineChartDownloadDir, pipelineChartName)
		if (!cLocation.exists()) {
			cLocation.mkdirs()
		}
		ant.untar( src: "${pipelineChartDownloadDir}/${pipelineChartName}-latest.tgz", compression: 'gzip', dest: "${cOutDir.absolutePath}")
		return cOutDir

	}
	
	List<ChartValuesData> generateChartValuesData(File cLocation) {
		List<ChartValuesData> valuesDataList = []
		File packageYaml = new File(cLocation,'Package.yaml')
		String outStr = packageYaml.text
		outStr = outStr.replaceAll(/(#)( |\S)*$/, '')
		def chartYaml = [:]
		try {
			chartYaml = new YamlSlurper().parseText(outStr)
		} catch (e) {
			e.printStackTrace()
			//log.error(e.message )
		}
		
		String chartName = chartYaml.name
		String chartDescription = chartYaml.description
		String chartVersion = chartYaml.version
		File oDir = new File(cLocation, valuesOptionsFolder)
		oDir.eachFileRecurse(FileType.FILES) { File t ->
			if (t.name.endsWith('.yaml')) {				
				String vStr = t.text
				outStr = vStr.replaceAll(/(#)( |\S)*$/, '')
				def valuesYaml = [:]
				try {
					valuesYaml = new YamlSlurper().parseText(vStr)
				} catch (e) {
					e.printStackTrace()
					//log.error(e.message )
				}
				if (valuesYaml.'interface' && valuesYaml.'interface'.name) {
					ChartValuesData cvd = new ChartValuesData()
					cvd.chartDescription = chartDescription
					cvd.chartName = chartName
					cvd.chartVersion = chartVersion
					cvd.valuesFileName = t.absolutePath.substring(oDir.absolutePath.length()+1)
					cvd.valuesName = valuesYaml.'interface'.name
					if (valuesYaml.'interface'.description) {
						cvd.valuesDescription = valuesYaml.'interface'.description
					}
					if (valuesYaml.'interface'.overrideValues && valuesYaml.'interface'.overrideValues.size()>0) {
						cvd.valueOverrideSettings = []
						for (def overrideValue in valuesYaml.'interface'.overrideValues) {
							ValueOverrideSetting setting = new ValueOverrideSetting()
							setting.key = overrideValue.key
							if (overrideValue.description) {
								setting.description = overrideValue.description								
							} else {
								setting.description = 'Tip is coming'
							}
							if (overrideValue.label) {
								setting.label = overrideValue.label
							} else {
								setting.label = setting.key
							}
							if (overrideValue.validationRegex) {
								setting.validationRegex = overrideValue.validationRegex
							} else {
								setting.validationRegex = '[^]*'
							}
							if (overrideValue.valueOptions) {
								setting.valueOptions = overrideValue.valueOptions as String[]
							} else {
								setting.valueOptions = []
							}
							setting.value = overrideValue.'value'
							cvd.valueOverrideSettings.add(setting)
						}
					}
					if (valuesYaml.'interface'.applyArgs && valuesYaml.'interface'.applyArgs.size() > 0) {
						cvd.applyChartArguments = []
						for (def arg in valuesYaml.'interface'.applyArgs) {
							ApplyChartArgument applyArg = new ApplyChartArgument()
							applyArg.key = arg.key
							if (arg.description) {
								applyArg.description = arg.description
							} else {
								applyArg.description = 'Tip is coming'
							}
							if (arg.label) {
								applyArg.label = arg.label
							} else {
								applyArg.label = arg.key
							}
							if (arg.validationRegex) {
								applyArg.validationRegex = arg.validationRegex
							} else {
								applyArg.validationRegex = '[^]*'
							}
							if (arg.valueOptions) {
								applyArg.valueOptions = arg.valueOptions as String[]
							} else {
								applyArg.valueOptions = []
							}
							applyArg.value = arg.'value'
							cvd.applyChartArguments.add(applyArg)
						}
					}
					valuesDataList.add(cvd)
				}
			}
		}
		
		return valuesDataList
	}
	
	def runCLIApplyChart(String valsYaml, String chartName, String repoUrl, String fileName) {
		def body = [actionName: 'applyChart', profile: 'default', project: 'DTS', repoName: chartName, loadRepo: true, arguments: []] 
		body.arguments.add([name: 'repo.url', value: repoUrl, argType: 'String'])
		body.arguments.add([name: 'pipeline.chart.name', value: chartName, argType: 'String'])
		body.arguments.add([name: 'override.values.file', value: fileName, outData: valsYaml.bytes.encodeBase64() as String, argType: 'File'])
		String sbody = new JsonOutput().toJson( body )
		def result = reGenericRestClient.post(
			requestContentType: ContentType.JSON,
			contentType: ContentType.JSON,
			uri:  "${reGenericRestClient.reUrl}/pipelinecli/action",
			//header: ['content-type': 'application/json'],
			body: body
		)
		return result
	}
	
	File loadChart(def chartData) {
		String chartName = chartData.name as String
		String pipelineChartName = chartName.substring(0, chartName.indexOf('/'))
		if (!pipelineChartCacheDir.exists()) {
			pipelineChartCacheDir.mkdirs()
		}
		File cOutDir = new File(pipelineChartCacheDir, pipelineChartName)
		if (!cOutDir.exists()) {
			cOutDir.mkdirs()
		}
		if (!pipelineChartDownloadDir.exists()) {
			pipelineChartDownloadDir.mkdirs()
		}
		byte[] cContent = repositoryService.getCiCdChartContent(pipelineChartName, 'latest', "${pipelineChartName}-latest.tgz")
		File cFileZip = new File(pipelineChartDownloadDir, "${pipelineChartName}-latest.tgz")
		def cos = cFileZip.newDataOutputStream()
		cos << cContent
		cos.close()
		
		File cLocation = new File(pipelineChartDownloadDir, pipelineChartName)
		if (!cLocation.exists()) {
			cLocation.mkdirs()
		}
		AntBuilder ant = new AntBuilder()
		ant.untar( src: "${cFileZip.absoluteFile}", compression: 'gzip', dest: "${cOutDir.absolutePath}")
		return cOutDir
	}
	
	
}

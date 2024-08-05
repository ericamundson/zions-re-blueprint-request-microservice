package com.zions.pipeline.services.blueprint.chart.model

import groovy.transform.Canonical

@Canonical
class ChartValuesData {
	String chartName
	String chartDescription
	String chartVersion
	
	String valuesName
	String valuesDescription
	String valuesFileName
	
	Boolean editApplyArguments
	
	String repoUrl
	
	List<ValueOverrideSetting> valueOverrideSettings
	List<ApplyChartArgument> applyChartArguments
	
	
}

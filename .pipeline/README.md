# <no value>

## Introduction

This blueprint deploys a project artifact built from an ADO pipeline, to a K8s environment.    
XL Deploy does the provisioning and deployment, while XL Release orchestrates everything.

## Before you get started

If you're new to XebiaLabs blueprints, check out:

* [Get started with DevOps as Code](https://docs.xebialabs.com/xl-platform/concept/get-started-with-devops-as-code.html)
* [Get started with blueprints](https://docs.xebialabs.com/xl-platform/concept/get-started-with-blueprints.html)

## Prerequisites

* XebiaLabs Release Orchestration and Deployment Automation up and running

## Usage

To use this blueprint, run `xl blueprint` in an empty directory and select:

```plain
webcms/project-aem
```

Explain how the user will execute your blueprint (especially if it needs to be used offline)

## Tools and technologies

This blueprint includes the following tools and technologies:

* Target:
  * K8s environment
* Tools:
  * [XebiaLabs Release Orchestration](https://xebialabs.com/products/xl-release/)
  * [XebiaLabs Deployment Automation](https://xebialabs.com/products/xl-deploy/)

## Minimum required versions

This blueprint version requires at least the following versions of the specified tools to work properly:

* XL Deploy: Version 9.0.0
* XL Release: Version 9.0.0
* XL CLI: Version 9.0.0

Remove whatever is not applicable

## Prerequisites

To run the YAML that this blueprint generates, you need:

* K8s cloud environment accessible
* XebiaLabs Deployment Automation up and running

## Information required

What important data will your blueprint be asking for
* The name of the project
* The name of the ADO repository
* The name of the ADO repository
* If this AEM project has an application Component
* If this AEM project has an httpd Component

## Output

This blueprint will output:

* ADO yaml
* XL Deploy & XL Release yaml

## Labels

* K8s
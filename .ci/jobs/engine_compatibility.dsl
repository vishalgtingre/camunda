pipelineJob('engine_compatibility') {

  displayName 'Integration Tests against previous CamBPM Versions'
  description 'Runs integration tests against the previous two CamBPM versions to check compatibility.'

  // By default, this job is disabled in non-prod envs.
  if (binding.variables.get("ENVIRONMENT") != "prod") {
    disabled()
  }

  definition {
    cps {
      script(readFileFromWorkspace('.ci/pipelines/engine_compatibility.groovy'))
      sandbox()
    }
  }

  parameters {
    stringParam('BRANCH', 'master', 'Branch to use for Engine compatibility tests.')
  }

  properties {
    pipelineTriggers {
      triggers {
        cron {
          spec('H 22 * * *')
        }
      }
    }
  }
}

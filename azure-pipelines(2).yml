# Starter pipeline
# Start with a minimal pipeline that you can customize to build and deploy your code.
# Add steps that build, run tests, deploy, and more:
# https://aka.ms/yaml


variables:
   - name: acrRepositoryName
     value: 'fasdas'
   - name: acrName
     value: 'sadas'
   - name: helmReleaseName
     value: 'helm'
   - name: newRelicName
     value: 'asdas'
   - name: releaseName
     value: 'release123'  #do not use '.' or '-' in the release name.
   - name: major
     value: '2'
   - name: minor
     value: '1'
     
name: ${{variables.major}}.${{variables.minor}}$(Rev:.r)

pool:
  vmImage: 'ubuntu-latest'


#Stages of pipeline
stages:
  #Validated Git Check
    - stage: 'DockerizeDeploy'
      #condition: and(succeeded(), ${{ containsValue(parameters.onlyForBranches, variables['Build.SourceBranch']) }})
      displayName: 'Image Creation'
      jobs:
      - job: 'CreateImage'
        variables:
          patch:  $[counter(variables['Build.SourceBranchName'].variables['major'].variables['minor'], 0)]
        steps:
          - template: stages/dockerbuild.yml
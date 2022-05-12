{{/*
Common helper templates/"functions" for use in the other files in this directory.
/*}}

{{/*
Expand the name of the chart.
*/}}
{{- define "name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{/*
Create a default fully qualified app name.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
If the chart name is the same as the release name then don't include both
*/}}
If the chart name is the same as the release name then don't include both
*/}}
{{- define "fullname" -}}
{{- $name := default .Chart.Name .Values.app -}}
{{- $name -}}
{{- end -}}
{{/*
Build full docker image name, including sha reference or tag
*/}}
{{- define "docker-image" -}}
{{- if hasKey .Values "dockerSha" -}}
  {{- printf "%s@%s" .Values.docker.image .Values.dockerSha | quote -}}
{{- else if hasKey .Values "dockerTag" -}}
  {{- printf "%s:%s" .Values.docker.image .Values.dockerTag | quote -}}
{{- else }}
  {{- printf "%s:latest" .Values.docker.image | quote -}}
{{- end -}}
{{- end -}}
{{/*
Generate random ingress name, if needed
*/}}
{{- define "ingress-name" -}}
{{- if .Values.ingress.name -}}
  {{- .Values.ingress.name -}}
{{- else -}}
  {{- randAlphaNum 12 | lower }}
{{- end -}}
{{- end -}}

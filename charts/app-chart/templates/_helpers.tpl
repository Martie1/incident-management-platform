{{/*
chart name
*/}}
{{- define "app-chart.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
fully qualified app name
*/}}
{{- define "app-chart.fullname" -}}
{{- if .Values.fullnameOverride }}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- $name := default .Chart.Name .Values.nameOverride }}
{{- if contains $name .Release.Name }}
{{- .Release.Name | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" }}
{{- end }}
{{- end }}
{{- end }}

{{/*
chart label
*/}}
{{- define "app-chart.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
common labels - without component, every resource adds its own
(including the backend selector labels here duplicated the component key on frontend/db resources)
*/}}
{{- define "app-chart.labels" -}}
helm.sh/chart: {{ include "app-chart.chart" . }}
{{ include "app-chart.baseSelectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{/*
labels shared by all components
*/}}
{{- define "app-chart.baseSelectorLabels" -}}
app.kubernetes.io/name: {{ include "app-chart.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
backend selector labels
*/}}
{{- define "app-chart.selectorLabels" -}}
{{ include "app-chart.baseSelectorLabels" . }}
app.kubernetes.io/component: backend
{{- end }}

{{/*
frontend selector labels
*/}}
{{- define "app-chart.frontendSelectorLabels" -}}
{{ include "app-chart.baseSelectorLabels" . }}
app.kubernetes.io/component: frontend
{{- end }}

{{/*
database selector labels
*/}}
{{- define "app-chart.dbSelectorLabels" -}}
{{ include "app-chart.baseSelectorLabels" . }}
app.kubernetes.io/component: database
{{- end }}

{{/*
sql_exporter selector labels
*/}}
{{- define "app-chart.sqlExporterSelectorLabels" -}}
{{ include "app-chart.baseSelectorLabels" . }}
app.kubernetes.io/component: sql-exporter
{{- end }}

{{/*
alert logger selector labels
*/}}
{{- define "app-chart.alertLoggerSelectorLabels" -}}
{{ include "app-chart.baseSelectorLabels" . }}
app.kubernetes.io/component: alert-logger
{{- end }}

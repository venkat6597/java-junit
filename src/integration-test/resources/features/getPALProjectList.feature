#Author: Bharath.KM@mnscorp.net
#Feature: GET service to get the PAL Project List
#StoryDescription : GET service to  the PAL Project List
#Adding validation to check the response whether it has been returning the details correctly
@getPALProjectList


Feature: Get List getPALProjectList Functionality


  @getPALProjectList_verifyjsonschema_verifyresponse_
  Scenario Outline: Positive Test to get list of Project for PAL service for valid userrole.
    When the service call for PAL Project List with POST request with "/v1/pal/PALProject/ProjectList" with payload  with PageObject "FSP PAL Projects", X-operation "Read", Origin "localhost"
     """
  {
  "userRole":<userRole>
  }

  """
    Then service should return 200 status code
    Then response must match with json schema "schema/getProjectList.json".
    Examples:
      |userRole           |
      |"projectManager"   |
      |"productDeveloper" |
      |"foodTechnologist" |
      |"buyer"            |
      |"supplier"         |
      |"ocado"            |
      |"international"    |
      |"vat"              |
      |"design"           |
      |"commercialPlanner"|


  @getPALProjectList_verifyprojecttype
  Scenario Outline: Positive Test to get list of Project for PAL service for valid projecttype.
    When the service call for PAL Project List with POST request with "/v1/pal/PALProject/ProjectList" with payload  with PageObject "FSP PAL Projects", X-operation "Read", Origin "localhost"
     """
  {
  "projectType": [
    <projectType>
  ],
  "userRole":<userRole>

  }

  """
    Then service should return 200 status code
    Then cross verify if the response contains projectType <projectType>
    Then response must match with json schema "schema/getProjectList.json".
    Examples:
      |projectType    |userRole        |
      |"Rolling"      |"projectManager"|
      |"Standard NPD" |"projectManager"|
  @getPALProjectList_verifyerrormessage
  Scenario Outline: Negative Test to get list of Project for PAL service for invalid projecttype
    When the service call for PAL Project List with POST request with "/v1/pal/PALProject/ProjectList" with payload with filter for negative scenarios  with PageObject "FSP PAL Projects", X-operation "Read", Origin "localhost"
     """
  {
  "projectType": [
    "<projectType>"
  ],
  "userRole":<userRole>

  }

  """
    Then service should return 204 status code
    Examples:
      |projectType|userRole        |
      |invalid    |"projectManager"|
      |           |"projectManager"|

  @getPALProjectList_verifyprojecttypes
  Scenario Outline: Positive Test to get list of Project for PAL service for valid projecttypes.
When the service call for PAL Project List with POST request with "/v1/pal/PALProject/ProjectList" with payload  with PageObject "FSP PAL Projects", X-operation "Read", Origin "localhost"
     """
  {
  "projectType": [
    <projectTypes>
  ],
   "userRole":<userRole>

  }

  """
    Then service should return 200 status code
    Then cross verify if the response contains projectTypes <projectTypes>
    Then response must match with json schema "schema/getProjectList.json".
    Examples:
      |projectTypes            |userRole        |
      |"Rolling","Standard NPD"|"projectManager"|

  @getPALProjectList_verifysearchText
  Scenario Outline: Positive Test to get list of Project for PAL service for valid searchText.
When the service call for PAL Project List with POST request with "/v1/pal/PALProject/ProjectList" with payload  with PageObject "FSP PAL Projects", X-operation "Read", Origin "localhost"
     """
  {
  "searchText": "<searchText>",
   "userRole":<userRole>
}
  """
    Then service should return 200 status code
    Then cross verify if the response contains searchText "<searchText>"
    Then response must match with json schema "schema/getProjectList.json".
    Examples:
      |searchText          |userRole        |
      |2021-2022 April Sale|"projectManager"|
      |ssss                |"projectManager"|
      |CHRISTMAS           |"projectManager"|
      |christmas           |"projectManager"|
      |2021-2022           |"projectManager"|

  @getPALProjectList_verifyemptysearchText
  Scenario Outline: Positive Test to get list of Project for PAL service for empty searchText.
When the service call for PAL Project List with POST request with "/v1/pal/PALProject/ProjectList" with payload  with PageObject "FSP PAL Projects", X-operation "Read", Origin "localhost"
     """
  {
  "searchText": "<searchText>",
   "userRole":<userRole>
}
  """
    Then service should return 200 status code
    Then cross verify if the response contains searchText "<searchText>"
    Then response must match with json schema "schema/getProjectList.json".
    Examples:
      |searchText|userRole        |
      |          |"projectManager"|

  @getPALProjectList_verifyinvalidsearchText
  Scenario Outline: Negative Test to get list of Project for PAL service for invalid searchText
    When the service call for PAL Project List with POST request with "/v1/pal/PALProject/ProjectList" with payload with filter for negative scenarios  with PageObject "FSP PAL Projects", X-operation "Read", Origin "localhost"
     """
  {
  "searchText": "<searchText>",
   "userRole":<userRole>
}
  """
    Then service should return 204 status code
    Examples:
      |searchText|userRole        |
      |invalid   |"projectManager"|

  @getPALProjectList_verifyfinancialYear
  Scenario Outline: Positive Test to get list of Project for PAL service for valid financialYear.
When the service call for PAL Project List with POST request with "/v1/pal/PALProject/ProjectList" with payload  with PageObject "FSP PAL Projects", X-operation "Read", Origin "localhost"
     """
  {

  "financialYear": [
    "<financialYear>"
  ],
   "userRole":<userRole>
}
  """
    Then service should return 200 status code
    Then cross verify if the response contains financialYear "<financialYear>"
    Then response must match with json schema "schema/getProjectList.json".
    Examples:
      |financialYear|userRole        |
      |2021-2022    |"projectManager"|
      |2020-2021    |"projectManager"|
      |2024-2025    |"projectManager"|

  @getPALProjectList_verifyprojectType_financialYear
  Scenario Outline: Positive Test to get list of Project for PAL service for valid projecttype and valid financialYear.
When the service call for PAL Project List with POST request with "/v1/pal/PALProject/ProjectList" with payload  with PageObject "FSP PAL Projects", X-operation "Read", Origin "localhost"
     """
  {
"projectType": [
    <projectType>
  ],
  "financialYear": [
    <financialYear>
  ],
   "userRole":<userRole>
}
  """
    Then service should return 200 status code
    Then cross verify if the response contains financialYear <financialYear>
    Then cross verify if the response contains projectType <projectType>
    Then response must match with json schema "schema/getProjectList.json".
    Examples:
      |financialYear|projectType   |userRole        |
      |"2021-2022"  |"Standard NPD"|"projectManager"|
      |"2021-2022"  |"Rolling"     |"projectManager"|




  @getPALProjectList_verifyprojectTypes_financialYears
  Scenario Outline: Positive Test to get list of Project for PAL service for valid projecttypes and valid financialYears.
When the service call for PAL Project List with POST request with "/v1/pal/PALProject/ProjectList" with payload  with PageObject "FSP PAL Projects", X-operation "Read", Origin "localhost"
     """
  {
"projectType": [
    <projectTypes>
  ],
  "financialYear": [
    <financialYears>
  ],
   "userRole":<userRole>
}
  """
    Then service should return 200 status code
    Then cross verify if the response contains financialYears <financialYears>
    Then cross verify if the response contains projectTypes <projectTypes>
    Then response must match with json schema "schema/getProjectList.json".
    Examples:
      |financialYears         |projectTypes            |userRole        |
      |"2021-2022","2020-2021"|"Rolling","Standard NPD"|"projectManager"|

  @getPALProjectList_verifyprojectTypes_financialYears
  Scenario Outline: Positive Test to get list of Project for PAL service for valid projecttypes and valid financialYears.
When the service call for PAL Project List with POST request with "/v1/pal/PALProject/ProjectList" with payload  with PageObject "FSP PAL Projects", X-operation "Read", Origin "localhost"
     """
  {
"projectType": [
    <projectTypes>
  ],
  "financialYear": [
    <financialYear>
  ],
   "userRole":<userRole>
}
  """
    Then service should return 200 status code
    Then cross verify if the response contains financialYear <financialYear>
    Then cross verify if the response contains projectTypes <projectTypes>
    Then response must match with json schema "schema/getProjectList.json".
    Examples:
      |financialYear|projectTypes            |userRole        |
      |"2020-2021"  |"Rolling","Standard NPD"|"projectManager"|

  @getPALProjectList_verifyprojectType_financialYears
  Scenario Outline: Positive Test to get list of Project for PAL service for valid projecttype and valid financialYears.
When the service call for PAL Project List with POST request with "/v1/pal/PALProject/ProjectList" with payload  with PageObject "FSP PAL Projects", X-operation "Read", Origin "localhost"
     """
  {
"projectType": [
    "<projectType>"
  ],
  "financialYear": [
    <financialYears>
  ],
   "userRole":<userRole>
}
  """
    Then service should return 200 status code
    Then cross verify if the response contains financialYears <financialYears>
    Then cross verify if the response contains projectType "<projectType>"
    Then response must match with json schema "schema/getProjectList.json".
    Examples:
      |financialYears         |projectType|userRole        |
      |"2021-2022","2020-2021"|Rolling    |"projectManager"|

  @getPALProjectList_verifyprojectTypes_financialYear
  Scenario Outline: Positive Test to get list of Project for PAL service for valid projecttypes and valid financialYear.
When the service call for PAL Project List with POST request with "/v1/pal/PALProject/ProjectList" with payload  with PageObject "FSP PAL Projects", X-operation "Read", Origin "localhost"
     """
  {
"projectType": [
    <projectTypes>
  ],
  "financialYear": [
    "<financialYear>"
  ],
   "userRole":<userRole>
}
  """
    Then service should return 200 status code
    Then cross verify if the response contains financialYear "<financialYear>"
    Then cross verify if the response contains projectTypes <projectTypes>
    Then response must match with json schema "schema/getProjectList.json".
    Examples:
      |financialYear|projectTypes            |userRole        |
      |2021-2022    |"Rolling","Standard NPD"|"projectManager"|

  @getPALProjectList_verifyprojectType_date
  Scenario Outline: Positive Test to get list of Project for PAL service for valid projecttype and valid financialYear.
When the service call for PAL Project List with POST request with "/v1/pal/PALProject/ProjectList" with payload  with PageObject "FSP PAL Projects", X-operation "Read", Origin "localhost"
     """
  {
"projectType": [
    "<projectType>"
  ],
"fromDate": "<fromDate>",
 "toDate": "<toDate>",
  "userRole":<userRole>
}
  """
    Then service should return 200 status code
    Then cross verify if the response contains projects between "<fromDate>" and "<toDate>"
    Then cross verify if the response contains projectType "<projectType>"
    Then response must match with json schema "schema/getProjectList.json".
    Examples:
      |projectType |fromDate  |toDate    |userRole        |
      |Standard NPD|23/11/2021|25/11/2021|"projectManager"|
      |Rolling    |22/11/2021|25/11/2021|"projectManager"|

  @getPALProjectList_verifyprojectType_date
  Scenario Outline: Positive Test to get list of Project for PAL service for valid projecttype and valid financialYear.
When the service call for PAL Project List with POST request with "/v1/pal/PALProject/ProjectList" with payload  with PageObject "FSP PAL Projects", X-operation "Read", Origin "localhost"
     """
  {
"projectType": [
    <projectTypes>
  ],
"fromDate": "<fromDate>",
 "toDate": "<toDate>",
  "userRole":<userRole>
}
  """
    Then service should return 200 status code
    Then cross verify if the response contains projects between "<fromDate>" and "<toDate>"
    Then cross verify if the response contains projectTypes <projectTypes>
    Then response must match with json schema "schema/getProjectList.json".
    Examples:
      |projectTypes            |fromDate  |toDate    |userRole        |
      |"Rolling","Standard NPD"|23/11/2021|25/11/2021|"projectManager"|
  @getPALProjectList_verifyprojectType_financialYear_date
  Scenario Outline: Positive Test to get list of Project for PAL service for valid projecttype and valid financialYear.
When the service call for PAL Project List with POST request with "/v1/pal/PALProject/ProjectList" with payload  with PageObject "FSP PAL Projects", X-operation "Read", Origin "localhost"
     """
  {
"projectType": [
    "<projectType>"
  ],
  "financialYear": [
    "<financialYear>"
  ],
  "fromDate": "<fromDate>",
 "toDate": "<toDate>",
  "userRole":<userRole>
}
  """
    Then service should return 200 status code
    Then cross verify if the response contains financialYear "<financialYear>"
    Then cross verify if the response contains projectType "<projectType>"
    Then cross verify if the response contains projects between "<fromDate>" and "<toDate>"
    Then response must match with json schema "schema/getProjectList.json".
    Examples:
      |financialYear|projectType |fromDate  |toDate    |userRole        |
      |2021-2022    |Standard NPD|23/11/2021|25/11/2021|"projectManager"|


  @getPALProjectList_verifyprojectType_financialYear_date_searchtext
  Scenario Outline: Positive Test to get list of Project for PAL service for valid projecttype and valid financialYear.
When the service call for PAL Project List with POST request with "/v1/pal/PALProject/ProjectList" with payload  with PageObject "FSP PAL Projects", X-operation "Read", Origin "localhost"
     """
  {
   "searchText": "<searchText>",
"projectType": [
    "<projectType>"
  ],
  "financialYear": [
    "<financialYear>"
  ],
  "fromDate": "<fromDate>",
 "toDate": "<toDate>",
  "userRole":<userRole>
}
  """
    Then service should return 200 status code
    Then cross verify if the response contains searchText "<searchText>"
    Then cross verify if the response contains financialYear "<financialYear>"
    Then cross verify if the response contains projectType "<projectType>"
    Then cross verify if the response contains projects between "<fromDate>" and "<toDate>"
    Then response must match with json schema "schema/getProjectList.json".
    Examples:
      |searchText       |financialYear|projectType |fromDate  |toDate    |userRole        |
      |Christmas Project|2021-2022    |Standard NPD|23/11/2021|25/11/2021|"projectManager"|



  @getPALProjectList_verifyjsonschema_verifyresponse
  Scenario Outline: Negative Test to get list of Project for PAL service for invalid financialYear.
    When the service call for PAL Project List with POST request with "/v1/pal/PALProject/ProjectList" with payload with filter for negative scenarios  with PageObject "FSP PAL Projects", X-operation "Read", Origin "localhost"
     """
  {

  "financialYear": [
    "<financialYear>"
  ],
   "userRole":<userRole>
}
  """
    Then service should return 204 status code
    Examples:
      |financialYear|userRole        |
      |invalidyear  |"projectManager"|
      |             |"projectManager"|

  @getPALProjectList_verifyfinancialYears
  Scenario Outline: Positive Test to get list of Project for PAL service for valid financialYears.
When the service call for PAL Project List with POST request with "/v1/pal/PALProject/ProjectList" with payload  with PageObject "FSP PAL Projects", X-operation "Read", Origin "localhost"
     """
  {

  "financialYear": [
    <financialYears>
  ],
   "userRole":<userRole>
}
  """
    Then service should return 200 status code
    Then cross verify if the response contains financialYears <financialYears>
    Then response must match with json schema "schema/getProjectList.json".
    Examples:
      |financialYears         |userRole        |
      |"2021-2022","2020-2021"|"projectManager"|


  @getPALProjectList_verifyfinancialYear_date
  Scenario Outline: Positive Test to get list of Project for PAL service for valid financialYear.
When the service call for PAL Project List with POST request with "/v1/pal/PALProject/ProjectList" with payload  with PageObject "FSP PAL Projects", X-operation "Read", Origin "localhost"
     """
  {

  "financialYear": [
    "<financialYear>"
  ],
  "fromDate": "<fromDate>",
 "toDate": "<toDate>",
  "userRole":<userRole>
}
  """
    Then service should return 200 status code
    Then cross verify if the response contains financialYear "<financialYear>"
    Then cross verify if the response contains projects between "<fromDate>" and "<toDate>"
    Then response must match with json schema "schema/getProjectList.json".
    Examples:
      |financialYear|fromDate  |toDate    |userRole        |
      |2021-2022    |23/11/2021|25/11/2021|"projectManager"|


  @getPALProjectList_verifydate
  Scenario Outline: Positive Test to get list of Project for PAL service for valid fromDate and toDate.
When the service call for PAL Project List with POST request with "/v1/pal/PALProject/ProjectList" with payload  with PageObject "FSP PAL Projects", X-operation "Read", Origin "localhost"
     """
  {
 "fromDate": "<fromDate>",
 "toDate": "<toDate>",
  "userRole":<userRole>
}
  """
    Then service should return 200 status code
    Then cross verify if the response contains projects between "<fromDate>" and "<toDate>"
    Then response must match with json schema "schema/getProjectList.json".
    Examples:
      |fromDate  |toDate    |userRole        |
      |23/11/2021|25/11/2021|"projectManager"|

  @getPALProjectList_verifydate
  Scenario Outline: Negative Test to get list of Project for PAL service for valid fromDate and toDate.
    When the service call for PAL Project List with POST request with "/v1/pal/PALProject/ProjectList" with payload with filter for negative scenarios  with PageObject "FSP PAL Projects", X-operation "Read", Origin "localhost"
     """
  {
 "fromDate": "<fromDate>",
 "toDate": "<toDate>",
  "userRole":<userRole>
}
  """
    Then service should return 204 status code
    Examples:
      |fromDate  |toDate    |userRole        |
      |23/11/2022|25/11/2022|"projectManager"|


  @getPALProjectList_verify_invalidate
  Scenario Outline: Positive Test to get list of Project for PAL service for invalid fromDate and toDate.
    When the service call for PAL Project List with POST request with "/v1/pal/PALProject/ProjectList" with payload with filter for negative scenarios  with PageObject "FSP PAL Projects", X-operation "Read", Origin "localhost"
     """
  {
 "fromDate": "<fromDate>",
 "toDate": "<toDate>",
  "userRole":<userRole>
}
  """
    Then service should return 200 status code
    Then response must match with json schema "schema/getProjectList.json".
    Examples:
      |fromDate  |toDate    |userRole        |
      |23/11/2021|          |"projectManager"|
      |          |25/11/2021|"projectManager"|
      |          |          |"projectManager"|



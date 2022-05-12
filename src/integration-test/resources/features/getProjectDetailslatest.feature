#Author: MadhuShreeVarshini.Chandrasekar@mnscorp.net
#Feature:Able to call the spring boot service and fetch the project details
#StoryDiscription  : This endpoint is to fetch the PAL Project Details by giving the projectId and userRole
#Adding validation to check the response whether it has the respective projectId and error message
@GetProjectDetails
Feature:Able to call the spring boot service and fetch the project details

###################################################Story/FSP-1271#######################################################
  @getProjectDetails
  Scenario Outline: Positive scenario that verify the response from the get project details
    When the service call with POST request to fetch PAL project response with "/v1/pal/PALProject/ProjectDetails" with PageObject "FSP PAL Project Details", X-operation "Read", Origin "localhost" and payload
  """
  {
  "projectId": "<projectId>",
  "userRole": "<userRole>"
  }
  """
    Then service should return 200 status code
    Then response must match with json schema "schema/getProjectDetails.json".
    Then assert the response for getProjectDetails
    Examples:
      | projectId                | userRole         |
      | 615a60905952b2dd44437e2b | projectManager   |
      | 615a60905952b2dd44437e2b | productDeveloper |
      | 6164618ea34ae47bf7072788 | foodTechnologist |
      | 6164618ea34ae47bf7072788 | buyer            |
      | 6164618ea34ae47bf7072788 | supplier         |
###################################################Story/FSP-1271#######################################################
  @getProjectDetails_BadRequest
  Scenario Outline: Negative scenario that verify the response from the get project details against the Bad Request error message
    When the service call with POST request to fetch PAL project response with "/v1/pal/PALProject/ProjectDetails" for negative scenario with PageObject "FSP PAL Project Details", X-operation "Read", Origin "localhost" and payload
  """
  {
  "projectId": <projectId>,
  "userRole": <userRole>
  }
  """
    Then service should return 400 status code
    Then response must match with json schema "schema/errorResponse.json".
    Then cross verify with the response if the error message is "Bad Request".
    Examples:
      | projectId                  | userRole         |
      | "615a60905952b2dd44437e2b" | null             |
      | "InvalidId"                | null             |
      | null                       | "projectManager" |
      | null                       | null             |

###################################################Story/FSP-1271#######################################################
  @getProjectDetails_No_Data_Available
  Scenario Outline: Negative scenario that verify the response from the get project details against the No Data Available error message
    When the service call with POST request to fetch PAL project response with "/v1/pal/PALProject/ProjectDetails" for negative scenario with PageObject "FSP PAL Project Details", X-operation "Read", Origin "localhost" and payload
  """
  {
  "projectId": <projectId>,
  "userRole": <userRole>
  }
  """
    Then service should return 204 status code
    Examples:
      | projectId   | userRole         |
      | "InvalidId" | "projectManager" |

#####################################################Story/FSP-1271#####################################################"
#Author: MadhuShreeVarshini.Chandrasekar@mnscorp.net
#Feature: PAL Service to get the PAL Project Progress
#StoryDescription: Automation for the getPALProjectProgress endpoint
@GetPALProjectProgress
Feature: Able to call the spring boot service and fetch the PAL project progress

#####################################################Story/FSP-825#####################################################
  @getPALProjectProgress_validdetails
  Scenario Outline: Positive scenario that verify the response from the get PAL Project Progress for valid projectId and valid userRole
    When the service call with POST request to fetch PAL project response with "/v1/pal/PALProject/Progress" with PageObject "FSP PAL Project Details", X-operation "Read", Origin "localhost" and payload
  """
  {
  "projectId": "<projectId>",
  "userRole": "<userRole>"
  }
  """
    Then service should return 200 status code
    Then response must match with json schema "schema/getPALProjectProgress.json".
    Then assert the PALProjectProgress response

    Examples:
      | projectId                | userRole          |
      | 60ff64a1476c45bf24a684d4 | projectManager    |
      | 60ff64a1476c45bf24a684d4 | productDeveloper  |
      | 60ff64a1476c45bf24a684d4 | foodTechnologist  |
      | 60ff64a1476c45bf24a684d4 | international     |
      | 60ff64a1476c45bf24a684d4 | buyer             |
      | 60ff64a1476c45bf24a684d4 | ocado             |
      | 60ff64a1476c45bf24a684d4 | vat               |
      | 60ff64a1476c45bf24a684d4 | commercialPlanner |

########################################################Story/FSP-825####################################################
  @getPALProjectProgress_Not_authorized_to_access
  Scenario Outline: Negative scenario that verify the response from the get PAL Project Progress for valid projectId and invalid userRole
    When the service call with POST request to fetch PAL project response with "/v1/pal/PALProject/Progress" for negative scenario with PageObject "FSP PAL Project Details", X-operation "Read", Origin "localhost" and payload
  """
  {
  "projectId": "<projectId>",
  "userRole": "<userRole>"
  }
  """
    Then service should return 400 status code
    Then response must match with json schema "schema/errorResponse.json".
    Then cross verify with the response if the error message is "Not authorized to access".
    Examples:
      | projectId                | userRole        |
      | 60ff64a1476c45bf24a684d4 | supplier        |
      | 60ff64a1476c45bf24a684d4 | InvalidUserRole |

####################################################Story/FSP-825#######################################################
  @getPALProjectProgress_BadRequest
  Scenario Outline: Negative scenario that verify the response from the get PAL Project Progress against the error message Bad Request
    When the service call with POST request to fetch PAL project response with "/v1/pal/PALProject/Progress" for negative scenario with PageObject "FSP PAL Project Details", X-operation "Read", Origin "localhost" and payload
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
      | projectId                  | userRole |
      | "60ff64a1476c45bf24a684d4" | ""       |
      | "60ff64a1476c45bf24a684d4" | null     |
      | null                       | null     |

#################################################Story/FSP-825##########################################################
  @getPALProjectProgress_NoDataAvailable
  Scenario Outline: Negative scenario that verify the response from the get PAL Project Progress against the error message No Data Available
    When the service call with POST request to fetch PAL project response with "/v1/pal/PALProject/Progress" for negative scenario with PageObject "FSP PAL Project Details", X-operation "Read", Origin "localhost" and payload
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
############################################Story/FSP-825###############################################################
  @getPALProjectProgress_removedprojectId
  Scenario Outline: Negative scenario that verify the response from the get PAL Project Progress after removing the projectId field in request
    When the service call with POST request to fetch PAL project response with "/v1/pal/PALProject/Progress" for negative scenario with PageObject "FSP PAL Project Details", X-operation "Read", Origin "localhost" and payload
  """
  {
  "userRole": <userRole>
  }
  """
    Then service should return 400 status code
    Then response must match with json schema "schema/errorResponse.json".
    Then cross verify with the response if the error message is "Bad Request".
    Examples:
      | userRole         |
      | "projectManager" |
################################################Story/FSP-825###########################################################
  @getPALProjectProgress_removeduserRole
  Scenario Outline: Negative scenario that verify the response from the get PAL Project Progress after removing the userRole field in request
    When the service call with POST request to fetch PAL project response with "/v1/pal/PALProject/Progress" for negative scenario with PageObject "FSP PAL Project Details", X-operation "Read", Origin "localhost" and payload
  """
  {
  "projectId": <projectId>
  }
  """
    Then service should return 400 status code
    Then response must match with json schema "schema/errorResponse.json".
    Then cross verify with the response if the error message is "Bad Request".
    Examples:
      | projectId                  |
      | "60ff64a1476c45bf24a684d4" |
##########################################Story/FSP-825#################################################################
  @getPALProjectProgress_NotAuthorized
  Scenario Outline: Negative scenario that verify the response from the get PAL Project Progress against the error message Not authorized to access
    When the service call with POST request to fetch PAL project response with "/v1/pal/PALProject/Progress" for negative scenario with PageObject "FSP PAL Project Details", X-operation "Read", Origin "localhost" and payload
  """
  {
  "projectId": <projectId>,
  "userRole": <userRole>
  }
  """
    Then service should return 400 status code
    Then response must match with json schema "schema/errorResponse.json".
    Then cross verify with the response if the error message is "Not authorized to access".
    Examples:
      | projectId   | userRole          |
      | "InvalidId" | "InvalidUserRole" |
###################################################Story/FSP-825########################################################
  @getPALProjectProgress_AccessDenied
  Scenario Outline: Negative scenario that verify the response from the get PAL Project Progress against the error message Access Denied
    When the service call with POST request to fetch PAL project response with "/v1/pal/PALProject/Progress" for negative scenario with PageObject "FSP PAL Project Details", X-operation "Read", Origin "localhost" and payload
  """
  {
  "projectId": <projectId>,
  "userRole": <userRole>
  }
  """
    Then service should return 403 status code
    Then response must match with json schema "schema/errorResponse.json".
    Then cross verify with the response if the error message is "Access denied".
    Examples:
      | projectId                  | userRole   |
      | "60ff64a1476c45bf24a684d4" | "supplier" |
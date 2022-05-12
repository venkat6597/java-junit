#Author: MadhuShreeVarshini.Chandrasekar@mnscorp.net
#Feature: PAL Service to update the PAL Project Details
#StoryDescription: Automation for the updatePALProject endpoint
@UpdatePALProject
Feature: Able to call the spring boot service and update the project details

#####################################################Story/FSP-1005#####################################################
  @updateProjectDetailsForStatus
  Scenario Outline: Positive scenario that verify the response from the update PAL Project for status field against success message
    When the service call with PUT request to update project service "/v1/pal/PALProject/updateProject" with PageObject "FSP PAL Project Details", X-operation "Update", Origin "localhost" payload
  """
  {
  "id": "<projectId>",
  "information":
   {
       "status": "<status>"
   },

    "userRole": "<role>",
    "user": "<user>"

  }
  """
    Then service should return 200 status code
    Then response must match with json schema "schema/updatePALProject.json".
    Then cross verify with the response if the updated field is "status" "<status>".
    Examples:
      | projectId                | status   | role             | user                     |
      | 60ff64a1476c45bf24a684d4 | Archived | projectManager   | projectManager@xyz.com   |
      | 615a60905952b2dd44437e2b | Archived | productDeveloper | productDeveloper@xyz.com |


#####################################################Story/FSP-1005#####################################################

  @updateProjectDetailsForStatus_errorstatus400
  Scenario Outline: Negative scenario that verify the response from the update PAL Project for status field by passing unauthorized user and role
    When the service call with PUT request to update project service "/v1/pal/PALProject/updateProject" with PageObject "FSP PAL Project Details", X-operation "Update", Origin "localhost" payload
  """
  {
  "id": "<projectId>",
  "information":
   {
       "status": "<status>"
   },

    "userRole": "<role>",
    "user": "<user>"

  }
  """
    Then service should return 400 status code
    Then response must match with json schema "schema/errorResponse.json".
    Then cross verify with the response if the error message is "Not authorized to access".
    Examples:
      | projectId                | status   | role              | user                      |
      | 60ff64a1476c45bf24a684d4 | Archived | buyer             | buyer@xyz.com             |
      | 615a60905952b2dd44437e2b | Archived | foodTechnologist  | foodTechnologist@xyz.com  |
      | 615a60905952b2dd44437e2b | Archived | commercialPlanner | commercialPlanner@xyz.com |

############################################################Story/FSP-1005##############################################

  @updateProjectDetailsForProjectName
  Scenario Outline: Positive scenario that verify the response from the update PAL Project for projectName field against success message
    When the service call with PUT request to update project service "/v1/pal/PALProject/updateProject" with PageObject "FSP PAL Project Details", X-operation "Update", Origin "localhost" payload
  """
  {
  "id": "<projectId>",
  "information":
   {
       "projectName": "<projectName>"
   },

    "userRole": "<role>",
    "user": "<user>"

  }
  """
    Then service should return 200 status code
    Then response must match with json schema "schema/updatePALProject.json".
    Then cross verify with the response if the updated field is "projectName" "<projectName>".
    Examples:
      | projectId                | projectName  | role             | user                     |
      | 60ff64a1476c45bf24a684d4 | projectName1 | projectManager   | projectManager@xyz.com   |
      | 615a60905952b2dd44437e2b | projectName2 | productDeveloper | productDeveloper@xyz.com |

#####################################################Story/FSP-1005#####################################################

  @updateProjectDetailsForProjectName_errorstatus400
  Scenario Outline:Negative scenario that verify the response from the update PAL Project for projectName field against Not authorized to access error message
    When the service call with PUT request to update project service "/v1/pal/PALProject/updateProject" with PageObject "FSP PAL Project Details", X-operation "Update", Origin "localhost" payload
  """
  {
  "id": "<projectId>",
  "information":
   {
       "projectName": "<projectName>"
   },

    "userRole": "<role>",
    "user": "<user>"

  }
  """
    Then service should return 400 status code
    Then response must match with json schema "schema/errorResponse.json".
    Then cross verify with the response if the error message is "Not authorized to access".
    Examples:
      | projectId                | projectName  | role              | user                      |
      | 60ff64a1476c45bf24a684d4 | projectName1 | buyer             | buyer@xyz.com             |
      | 615a60905952b2dd44437e2b | projectName2 | foodTechnologist  | foodTechnologist@xyz.com  |
      | 615a60905952b2dd44437e2b | projectName2 | commercialPlanner | commercialPlanner@xyz.com |
############################################################Story/FSP-1005##############################################
  @updateProjectDetailsForProjectType
  Scenario Outline: Negative scenario that verify the response from the update PAL Project for projectType field against BadRequest
    When the service call with PUT request to update project service "/v1/pal/PALProject/updateProject" with PageObject "FSP PAL Project Details", X-operation "Update", Origin "localhost" payload
  """
  {
  "id": "<projectId>",
  "information":
   {
       "projectType": "<projectType>"
   },
   "userRole": "<role>",
   "user": "<user>"

  }
  """
    Then service should return 400 status code
    Then response must match with json schema "schema/errorResponse.json".
    Then cross verify with the response if the error message is "Bad Request".
    Examples:
      | projectId                | projectType  | role             | user                     |
      | 615a60905952b2dd44437e2b | Rolling      | projectManager   | projectManager@xyz.com   |
      | 615a60905952b2dd44437e2b | projectType2 | productDeveloper | productDeveloper@xyz.com |

#################################################Story/FSP-1005#########################################################
  @updateProjectDetailsForProjectType_errorstatus400
  Scenario Outline: Negative scenario that verify the response from the update PAL Project for projectType field against Not authorized to access error message
    When the service call with PUT request to update project service "/v1/pal/PALProject/updateProject" with PageObject "FSP PAL Project Details", X-operation "Update", Origin "localhost" payload
  """
  {
  "id": "<projectId>",
  "information":
   {
       "projectType": "<projectType>"
   },

    "userRole": "<role>",
    "user": "<user>"

  }
  """
    Then service should return 400 status code
    Then response must match with json schema "schema/errorResponse.json".
    Then cross verify with the response if the error message is "Not authorized to access".
    Examples:
      | projectId                | projectType  | role              | user                      |
      | 60ff64a1476c45bf24a684d4 | projectType1 | buyer             | buyer@xyz.com             |
      | 615a60905952b2dd44437e2b | projectType2 | foodTechnologist  | foodTechnologist@xyz.com  |
      | 615a60905952b2dd44437e2b | projectType2 | commercialPlanner | commercialPlanner@xyz.com |
############################################################Story/FSP-1005##############################################
  @updateProjectDetailsForComments
  Scenario Outline:Positive scenario that verify the response from the update PAL Project for comments field against success message
    When the service call with PUT request to update project service "/v1/pal/PALProject/updateProject" with PageObject "FSP PAL Project Details", X-operation "Update", Origin "localhost" payload
  """
  {
  "id": "<projectId>",
  "information":
   {
       "comments": "<comments>"
   },

    "userRole": "<role>",
    "user": "<user>"

  }
  """
    Then service should return 200 status code
    Then response must match with json schema "schema/updatePALProject.json".
    Then cross verify with the response if the updated field is "comments" "<comments>".
    Examples:
      | projectId                | comments  | role             | user                     |
      | 60ff64a1476c45bf24a684d4 | comments1 | projectManager   | projectManager@xyz.com   |
      | 615a60905952b2dd44437e2b | comments2 | productDeveloper | productDeveloper@xyz.com |

#################################################Story/FSP-1005#########################################################
  @updateProjectDetailsForComments_errorstatus400
  Scenario Outline:Negative scenario that verify the response from the update PAL Project for comments field against Not authorized to access error message
    When the service call with PUT request to update project service "/v1/pal/PALProject/updateProject" with PageObject "FSP PAL Project Details", X-operation "Update", Origin "localhost" payload
  """
  {
  "id": "<projectId>",
  "information":
   {
       "comments": "<comments>"
   },

    "userRole": "<role>",
    "user": "<user>"
   }
  }
  """
    Then service should return 400 status code
    Then response must match with json schema "schema/errorResponse.json".
    Then cross verify with the response if the error message is "Not authorized to access".
    Examples:
      | projectId                | comments  | role              | user                      |
      | 60ff64a1476c45bf24a684d4 | comments1 | buyer             | buyer@xyz.com             |
      | 615a60905952b2dd44437e2b | comments2 | foodTechnologist  | foodTechnologist@xyz.com  |
      | 615a60905952b2dd44437e2b | comments3 | commercialPlanner | commercialPlanner@xyz.com |
############################################################Story/FSP-1005##############################################
  @updateProjectDetails_MissingMandatoryFields
  Scenario Outline:Negative scenario that verify the response from the update PAL Project by removing the mandatory fields
    When the service call with PUT request to update project service "/v1/pal/PALProject/updateProject" with PageObject "FSP PAL Project Details", X-operation "Update", Origin "localhost" payload
  """
  {
   "id":<projectId>,
   "information":
   {
       "status": <status>
   },

    "userRole": <role>,
    "user": <user>

  }
  """
    Then service should return 400 status code
    Then response must match with json schema "schema/errorResponse.json".
    Then cross verify with the response if the error message is "Missing mandatory fields".
    Examples:
      | projectId                  | status     | role               | user                       |
      | null                       | "Archived" | "projectManager"   | "projectManager@xyz.com"   |
      | null                       | "Archived" | "productDeveloper" | "productDeveloper@xyz.com" |
      | "60ff64a1476c45bf24a684d4" | null       | "projectManager"   | "projectManager@xyz.com"   |
      | "615a60905952b2dd44437e2b" | null       | "productDeveloper" | "productDeveloper@xyz.com" |
      | "60ff64a1476c45bf24a684d4" | "Archived" | null               | null                       |
      | "615a60905952b2dd44437e2b" | "Archived" | "projectManager"   | null                       |
      | "615a60905952b2dd44437e2b" | "Archived" | null               | "productDeveloper@xyz.com" |
############################################################Story/FSP-1005##############################################
  @updateProjectDetils_InvalidProjectId
  Scenario Outline:Negative scenario that verify the response from the update PAL Project by passing invalid projectId
    When the service call with PUT request to update project service "/v1/pal/PALProject/updateProject" with PageObject "FSP PAL Project Details", X-operation "Update", Origin "localhost" payload
  """
  {
   "id": <projectId>,
   "information":
   {
       "status": "<status>"
   },

    "userRole": "<role>",
    "user": "<user>"

  }
  """
    Then service should return 400 status code
    Then response must match with json schema "schema/errorResponse.json".
    Then cross verify with the response if the error message is "Invalid Id".
    Examples:
      | projectId          | status   | role             | user                     |
      | "InvalidProjectID" | Archived | projectManager   | projectManager@xyz.com   |
      | "xyz123"           | Archived | productDeveloper | productDeveloper@xyz.com |

#################################################Story/FSP-1005#########################################################
  @updateProjectDetailsForTemplateName
  Scenario Outline:Negative scenario that verify the response from the update PAL Project by changing the template name
    When the service call with PUT request to update project service "/v1/pal/PALProject/updateProject" with PageObject "FSP PAL Project Details", X-operation "Update", Origin "localhost" payload
  """
  {
   "id":"<projectId>",
   "information":
   {
      "templateId": "<templateId>",
      "templateName": "<templateName>"
   },

    "userRole": "<role>",
    "user": "<user>"

  }
  """
    Then service should return 400 status code
    Then response must match with json schema "schema/errorResponse.json".
    Then cross verify with the response if the error message is "Incorrect template".
    Examples:
      | projectId                | templateId               | templateName | role           | user                   |
      | 619d103a0de1321aa091bd63 | 611d11f7cde71e991b217579 | Standard123  | projectManager | projectManager@xyz.com |
###################################################Story/FSP-1005#######################################################
  @updateProjectDetailsForFinancialyear
  Scenario Outline:Negative scenario that verify the response from the update PAL Project for financialYear field against Bad Request message
    When the service call with PUT request to update project service "/v1/pal/PALProject/updateProject" with PageObject "FSP PAL Project Details", X-operation "Update", Origin "localhost" payload
  """
  {
  "id": "<projectId>",
  "information":
   {
       "financialYear": "<financialYear>"
   },

    "userRole": "<role>",
    "user": "<user>"

  }
  """
    Then service should return 400 status code
    Then response must match with json schema "schema/errorResponse.json".
    Then cross verify with the response if the error message is "Bad Request".
    Examples:
      | projectId                | financialYear | role             | user                     |
      | 60ff64a1476c45bf24a684d4 | 2018-2020     | projectManager   | projectManager@xyz.com   |
      | 615a60905952b2dd44437e2b | 2019-2021     | productDeveloper | productDeveloper@xyz.com |
#################################################Story/FSP-1005#########################################################
  @updateProjectDetailsForFinancialyear_errorstatus400
  Scenario Outline:Negative scenario that verify the response from the update PAL Project for financialYear field against Not authorized to access error message
    When the service call with PUT request to update project service "/v1/pal/PALProject/updateProject" with PageObject "FSP PAL Project Details", X-operation "Update", Origin "localhost" payload
  """
  {
  "id": "<projectId>",
  "information":
   {
       "financialYear": "<financialYear>"
   },

    "userRole": "<role>",
    "user": "<user>"

  }
  """
    Then service should return 400 status code
    Then response must match with json schema "schema/errorResponse.json".
    Then cross verify with the response if the error message is "Not authorized to access".
    Examples:
      | projectId                | financialYear | role              | user                      |
      | 60ff64a1476c45bf24a684d4 | 2018-2020     | buyer             | buyer@xyz.com             |
      | 615a60905952b2dd44437e2b | 2019-2021     | foodTechnologist  | foodTechnologist@xyz.com  |
      | 615a60905952b2dd44437e2b | 2019-2021     | commercialPlanner | commercialPlanner@xyz.com |

############################################################Story/FSP-1005##############################################



#Author: Bharath.KM@mnscorp.net
#Feature: PUT service to get the Update PAL Project Personnel Details
#StoryDescription : PUT service to  the PAL Project Personnel Details
#Adding validation to check the response whether it has been updating the details correctly
@UpdatePAlProjectPersonnel
Feature: Get List palProjectUpdatePersonnel Functionality

  @UpdatePAlProjectPersonnel_verifyschema_response
  Scenario Outline: Positive Test to get list of ProjectUpdatePersonnel for PAL service for valid projectId,user and userrole.
    When the service call with PUT request with "/v1/pal/PALProject/Personnel" with payload with PageObject "FSP PAL Project Details", X-operation "Update", Origin "localhost"
  """
  {
  "personnelUpdates": [
  {
  "newValue": ["<newValue>"],
  "oldValue": ["<oldValue>"],
  "field": "<field>"
  }
  ],
  "projectId": "<projectId>",
  "user": "<user>",
  "userRole": "<userRole>"
  }
  """
    Then service should return 200 status code
    Then cross verify if the response contains "<projectId>"
    Then cross verify if the personnel is updated to "<newValue>"
    Then response must match with json schema "schema/projectUpdatePersonnel.json".
    Examples:
      |projectId	           |user                  |userRole      |newValue                |oldValue              |section |field         |
      |6164618ea34ae47bf7072788|projectManager@xyz.com|projectManager|projectManager1@xyz.com |projectManager@xyz.com|internal|projectManager|
      |6164618ea34ae47bf7072788|projectManager@xyz.com|projectManager|productDeveloper@xyz.com|projectManager@xyz.com|internal|projectManager|
      |6164618ea34ae47bf7072788|invalid user          |projectManager|projectManager1@xyz.com |projectManager@xyz.com|internal|projectManager|
      |6164618ea34ae47bf7072788|projectManager@xyz.com|projectManager|projectManager1@xyz.com |                      |internal|projectManager|


  @UpdatePAlProjectPersonnel_error
  Scenario Outline: Negative Test to get list of ProjectUpdatePersonnel for PAL service for valid projectId,user and userrole.
    When the service call with PUT request with "/v1/pal/PALProject/Personnel" for invalid cases with payload with PageObject "FSP PAL Project Details", X-operation "Update", Origin "localhost"
  """
  {
  "personnelUpdates": [
  {
  "newValue": ["<newValue>"],
  "oldValue": ["<oldValue>"],
  "field": "<field>"
  }
  ],
  "projectId": "<projectId>",
  "user": "<user>",
  "userRole": "<userRole>"
  }
  """
    Then service should return 400 status code
    Then cross verify with the response if the error message is "<errorMessage>".
    Then response must match with json schema "schema/errorResponse.json".
    Examples:
      |projectId	           |user                    |userRole         |newValue                 |oldValue                |field           |errorMessage            |
      |60ff64a1476c45bf24a684d4|                        |projectManager   |projectManager1@xyz.com  |projectManager@xyz.com  |projectManager  |Bad Request             |
      |6164618ea34ae47bf7072788|productDeveloper@xyz.com|productDeveloper |productDeveloper1@xyz.com|productDeveloper@xyz.com|productDeveloper|Not authorized to access|
      |6164618ea34ae47bf7072788|productDeveloper@xyz.com|                 |productDeveloper1@xyz.com|productDeveloper@xyz.com|productDeveloper|Bad Request             |
      |6164618ea34ae47bf7072788|productDeveloper@xyz.com|invalid userRole |productDeveloper1@xyz.com|productDeveloper@xyz.com|productDeveloper|Not authorized to access|
      |                        |productManager@xyz.com  |projectManager   |productDeveloper1@xyz.com|productDeveloper@xyz.com|productDeveloper|Bad Request             |
      |                        |productManager@xyz.com  |                 |productDeveloper1@xyz.com|productDeveloper@xyz.com|productDeveloper|Bad Request             |
      |                        |productManager@xyz.com  |projectManager   |                         |                        |               |Bad Request             |
      |6164618ea34ae47bf707288 |productManager@xyz.com  |                 |                         |                        |                |Bad Request             |
      |                        |productManager@xyz.com  |                 |                         |                        |                |Bad Request             |
      |6164618ea34ae47bf7072788|productDeveloper@xyz.com|foodTechnologist |productDeveloper1@xyz.com|productDeveloper@xyz.com|productDeveloper|Not authorized to access|
      |6164618ea34ae47bf7072788|productDeveloper@xyz.com|buyer            |productDeveloper1@xyz.com|productDeveloper@xyz.com|productDeveloper|Not authorized to access|
      |6164618ea34ae47bf7072788|productDeveloper@xyz.com|supplier         |productDeveloper1@xyz.com|productDeveloper@xyz.com|productDeveloper|Not authorized to access|
      |6164618ea34ae47bf7072788|productDeveloper@xyz.com|ocado            |productDeveloper1@xyz.com|productDeveloper@xyz.com|productDeveloper|Not authorized to access|
      |6164618ea34ae47bf7072788|productDeveloper@xyz.com|international    |productDeveloper1@xyz.com|productDeveloper@xyz.com|productDeveloper|Not authorized to access|
      |6164618ea34ae47bf7072788|productDeveloper@xyz.com|design           |productDeveloper1@xyz.com|productDeveloper@xyz.com|productDeveloper|Not authorized to access|
      |6164618ea34ae47bf7072788|productDeveloper@xyz.com|commercialPlanner|productDeveloper1@xyz.com|productDeveloper@xyz.com|productDeveloper|Not authorized to access|

  @UpdatePAlProjectPersonnel_errortest
  Scenario Outline: Negative Test to get list of ProjectUpdatePersonnel for PAL service for valid projectId,user and userrole.
    When the service call with PUT request with "/v1/pal/PALProject/Personnel" for invalid cases with payload with PageObject "FSP PAL Project Details", X-operation "Update", Origin "localhost"
  """
  {
  "personnelUpdates": [
  {
  "newValue": ["<newValue>"],
  "oldValue": ["<oldValue>"],
  "field": "<field>"
  }
  ],
  "projectId": "<projectId>",
  "user": "<user>",
  "userRole": "<userRole>"
  }
  """
    Then service should return 204 status code
    Examples:
      |projectId	          |user                    |userRole        |newValue                 |oldValue                |field           |
      |6164618ea34ae47bf707288|productManager@xyz.com  |projectManager  |productDeveloper1@xyz.com|productDeveloper@xyz.com|productDeveloper|



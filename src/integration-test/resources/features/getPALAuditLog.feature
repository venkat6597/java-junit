#Author: Bharath.KM@mnscorp.net
#Feature: GET service to get the PAL Audit Log Details
#StoryDescription : GET service to  the PAL Audit Log Details
#Adding validation to check the response whether it has been returning the details correctly
@GetPALAuditLog
Feature: Get List palAuditLog Functionality

  @GetPALAuditLog_verifyjsonschema_response
  Scenario Outline: Positive Test to get list of ProductAuditLog for PAL service by passing valid productId and userRole
    When the service call with POST request to fetch the product response "/v1/pal/PALProduct/Auditlogs" with PageObject "FSP PAL Product", X-operation "Read", Origin "localhost" and payload
    """
  {
  "productId":"<productId>",
  "userRole":"<userRole>"
  }

  """
    Then service should return 200 status code
    Then cross verify if the response contains productId "<productId>"
    Then response must match with json schema "<schema>".
    Examples:
      |productId               |userRole         |schema                 |
      |61f290df1d58391d76c70e01|projectManager   |schema/palAuditLog.json|
      |61f290df1d58391d76c70e01|productDeveloper |schema/palAuditLog.json|
      |61f290df1d58391d76c70e01|foodTechnologist |schema/palAuditLog.json|
      |61f290df1d58391d76c70e01|buyer            |schema/palAuditLog.json|
      |61f290df1d58391d76c70e01|supplier         |schema/palAuditLog.json|
      |61f290df1d58391d76c70e01|ocado            |schema/palAuditLog.json|
      |61f290df1d58391d76c70e01|international    |schema/palAuditLog.json|
      |61f290df1d58391d76c70e01|vat              |schema/palAuditLog.json|
      |61f290df1d58391d76c70e01|design           |schema/palAuditLog.json|
      |61f290df1d58391d76c70e01|commercialPlanner|schema/palAuditLog.json|

  @GetPALAuditLog_response
  Scenario Outline: Positive Test to get list of ProductAuditLog for PAL service by passing  valid productId,role,sectionname and userRole
    When the service call with POST request to fetch the product response "/v1/pal/PALProduct/Auditlogs" with PageObject "FSP PAL Product", X-operation "Read", Origin "localhost" and payload
  """
  {
  "productId":  "<productId>",
  "role":"<role>" ,
  "sectionName":"<sectionName>",
  "userRole": "<userRole>"
  }
  """
    Then service should return 200 status code
    Then cross verify if the response contains productId "<productId>"
    Then response must match with json schema "schema/palAuditLog.json".
    Examples:
      |productId               |userRole        |role |sectionName       |
      |61f290df1d58391d76c70e01|projectManager  |buyer|productInformation|
      |61f290df1d58391d76c70e01|projectManager  |     |productInformation|
      |61f290df1d58391d76c70e01|projectManager  |buyer|                  |
      |61f290df1d58391d76c70e01|projectManager  |     |                  |



  @GetPALAuditLog_filter
  Scenario Outline: Positive Test to get list of ProductAuditLog for PAL service by passing  valid productId,userRole and valid filter values(Search by Author and Date filter)
    When the service call with POST request to fetch the product response "/v1/pal/PALProduct/Auditlogs" with PageObject "FSP PAL Product", X-operation "Read", Origin "localhost" and payload
  """
  {
  "productId":  "<productId>",
  "userRole": "<userRole>",
  "filter": {
  "searchText": "<searchText>",
  "fromDate": "<fromDate>",
  "toDate": "<toDate>"
  }
  }
  """
    Then service should return 200 status code
    Then cross verify if the response contains productId "<productId>"
    Then auditlogs in response must be between "<fromDate>" and "<toDate>"
    Then response must match with json schema "schema/palAuditLog.json".
    Examples:
      |productId               |userRole        |searchText      |fromDate  |toDate    |
      |61f290df1d58391d76c70e01|projectManager  |sathishkumar    |02/11/2021|03/11/2021|
      |61f290df1d58391d76c70e01|productDeveloper|sathishkumar    |02/11/2021|03/11/2021|
      |61f290df1d58391d76c70e01|projectManager  |productDeveloper|02/11/2021|03/11/2021|
      |61f290df1d58391d76c70e01|projectManager  |projectManager  |02/11/2021|03/11/2021|
      |61f290df1d58391d76c70e01|projectManager  |buyer           |02/11/2021|03/11/2021|
      |61f290df1d58391d76c70e01|projectManager  |foodTechnologist|02/11/2021|03/11/2021|
      |61f290df1d58391d76c70e01|projectManager  |PRODUCT TITLE   |02/11/2021|03/11/2021|

  @GetPALAuditLog_filter_searchtext
  Scenario Outline: Positive Test to get list of ProductAuditLog for PAL service by passing  valid productId,userRole and valid filter values(Search by Author and Date filter)
    When the service call with POST request to fetch the product response "/v1/pal/PALProduct/Auditlogs" with PageObject "FSP PAL Product", X-operation "Read", Origin "localhost" and payload
  """
  {
  "productId":  "<productId>",
  "userRole": "<userRole>",
  "filter": {
  "searchText": "<searchText>"
  }
  }
  """
    Then service should return 200 status code
    Then cross verify if the response contains productId "<productId>"
    Then response must match with json schema "schema/palAuditLog.json".
    Examples:
      |productId               |userRole        |searchText      |
      |61f290df1d58391d76c70e01|projectManager  |sathishkumar    |
      |61f290df1d58391d76c70e01|productDeveloper|sathishkumar    |
      |61f290df1d58391d76c70e01|projectManager  |productDeveloper|
      |61f290df1d58391d76c70e01|projectManager  |projectManager  |
      |61f290df1d58391d76c70e01|projectManager  |buyer           |
      |61f290df1d58391d76c70e01|projectManager  |foodTechnologist|

  @GetPALAuditLog_filterwithchildproducts
  Scenario Outline: Positive Test to get list of ProductAuditLog for PAL service by passing  valid productId,userRole and valid filter values(empty Search by Role and valid Date filter)
    When the service call with POST request to fetch the product response "/v1/pal/PALProduct/Auditlogs" with PageObject "FSP PAL Product", X-operation "Read", Origin "localhost" and payload
  """
  {
  "productId":  "<productId>",
  "userRole": "<userRole>",
  "filter": {
  "searchText": "<searchText>",
  "fromDate": "<fromDate>",
  "toDate": "<toDate>"
  }
  }
  """
    Then service should return 200 status code
    Then cross verify if the response contains productId "<productId>"
    Then response must match with json schema "schema/palAuditLog.json".
    Examples:
      |productId               |userRole      |searchText|fromDate  |toDate    |
      |61f290df1d58391d76c70e01|projectManager|          |02/11/2021|03/11/2021|

  @GetPALAuditLog_verifyerrormessage
  Scenario Outline: Negative Test to get list of ProductAuditLog for PAL service by passing  invalid productId,valid userRole and valid filter values(Search by Role and Date filter)
    When the service call with POST request to fetch the product response "/v1/pal/PALProduct/Auditlogs" for negative scenario with PageObject "FSP PAL Product", X-operation "Read", Origin "localhost" and payload
  """
  {
  "productId":  "<productId>",
  "userRole": "<userRole>",
  "filter": {
  "searchText": "<searchText>",
  "fromDate": "<fromDate>",
  "toDate": "<toDate>"
  }
  }
  """
    Then service should return 400 status code
    Then cross verify with the response if the error message is "<errorMessage>".
    Then response must match with json schema "schema/errorResponse.json".
    Examples:
      |productId               |userRole      |searchText      |fromDate  |toDate    |errorMessage     |
      |                        |projectManager|productDeveloper|02/11/2021|03/11/2021|Bad Request      |
      |61f290df1d58391d76c70e01|              |productDeveloper|02/11/2021|03/11/2021|Bad Request      |
      |                        |              |productDeveloper|02/11/2021|03/11/2021|Bad Request      |
      |                        |projectManager|                |02/11/2021|03/11/2021|Bad Request      |
      |                        |              |                |02/11/2021|03/11/2021|Bad Request      |
      |                        |              |                |          |          |Bad Request      |


  @GetPALAuditLog_verifyerrormessage
  Scenario Outline: Negative Test to get list of ProductAuditLog for PAL service by passing  invalid productId,valid userRole and valid filter values(Search by Role and Date filter)
    When the service call with POST request to fetch the product response "/v1/pal/PALProduct/Auditlogs" for negative scenario with PageObject "FSP PAL Product", X-operation "Read", Origin "localhost" and payload
  """
  {
  "productId":  "<productId>",
  "userRole": "<userRole>",
  "filter": {
  "searchText": "<searchText>",
  "fromDate": "<fromDate>",
  "toDate": "<toDate>"
  }
  }
  """
    Then service should return 204 status code
    Examples:
      |productId               |userRole      |searchText      |fromDate  |toDate    |
      |61357305cde71e991bd8eb  |projectManager|productDeveloper|02/11/2021|03/11/2021|
      |61357305cde71e991bd8eb  |projectManager|                |02/11/2021|03/11/2021|


  @GetPALAuditLog_verifyerrormessage
  Scenario Outline: Negative Test for ProductAuditLog for PAL service by passing Invalid productId
    When the service call with POST request to fetch the product response "/v1/pal/PALProduct/Auditlogs" for negative scenario with PageObject "FSP PAL Product", X-operation "Read", Origin "localhost" and payload
    """
  {
  "productId":"<productId>",
  "userRole":"<userRole>"
  }

  """
    Then service should return 400 status code
    Then cross verify with the response if the error message is "<errorMessage>".
    Then response must match with json schema "schema/errorResponse.json".
    Examples:
      |productId             |userRole      |errorMessage     |
      |                      |projectManager|Bad Request      |
      |61357305cde71e991bd8e |              |Bad Request      |
      |                      |              |Bad Request      |

  @GetPALAuditLog_verifyerrormessage
  Scenario Outline: Negative Test for ProductAuditLog for PAL service by passing Invalid productId
    When the service call with POST request to fetch the product response "/v1/pal/PALProduct/Auditlogs" for negative scenario with PageObject "FSP PAL Product", X-operation "Read", Origin "localhost" and payload
    """
  {
  "productId":"<productId>",
  "userRole":"<userRole>"
  }

  """
    Then service should return 204 status code
    Examples:
      |productId             |userRole      |
      |61357305cde71e991bd8e |projectManager|



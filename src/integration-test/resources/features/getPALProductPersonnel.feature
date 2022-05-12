#Author: Bharath.KM@mnscorp.net
#Feature: GET service to get the PAL Product Personnel Details
#StoryDescription : GET service to  the PAL Product Personnel Details
#Adding validation to check the response whether it has been returning the details correctly
@getPALProductPersonnel


Feature: Get List getpalProductPersonnel Functionality

 ############################################## Story-1360 ##############################################
  @getPALProductPersonnel_verifyjsonschema_verifyresponse
  Scenario Outline: Positive Test to get list of Product Personnel for PAL service for valid productId and valid userrole.
    When the service call with POST request to fetch the product response "/v1/pal/PALProduct/Personnel" with PageObject "FSP PAL Product", X-operation "Read", Origin "localhost" and payload
     """
  {
  "productId":"<productId>",
  "userRole":"<userRole>"
  }

  """
    Then service should return 200 status code
    Then cross verify if the response contains "<productId>","projectManager"
    Then response must match with json schema "schema/getProductPersonnel.json".
    Examples:
      |productId	           |userRole          |
      |61fcef5bea4f25640d71fc83|projectManager    |
      |61fbe8c81a759535f34d8ab6|projectManager    |
      |61fcef5bea4f25640d71fc83|productDeveloper  |
      |61fcef5bea4f25640d71fc83|foodTechnologist  |
      |61fcef5bea4f25640d71fc83|buyer             |
      |61fcef5bea4f25640d71fc83|supplier          |
      |61fcef5bea4f25640d71fc83|ocado             |
      |61fcef5bea4f25640d71fc83|international     |
      |61fcef5bea4f25640d71fc83|vat               |
      |61fcef5bea4f25640d71fc83|design            |
      |61fcef5bea4f25640d71fc83|commercialPlanner |

      ############################################## Story-1360 ##############################################
  @getPALProductPersonnel_verifyerrormessage
  Scenario Outline: Negative Test to get list of Product Personnel for PAL service
    When the service call with POST request to fetch the product response "/v1/pal/PALProduct/Personnel" for negative scenario with PageObject "FSP PAL Product", X-operation "Read", Origin "localhost" and payload
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
      |productId	           |userRole      |errorMessage            |
      |                        |projectManager|Bad Request             |
      |61fcef5bea4f25640d71fc83|              |Bad Request             |
      |                        |              |Bad Request             |
      |61fcef5bea4f25640d71fc83|invalid       |Not authorized to access|

  @getPALProductPersonnel_verifyerrormessage
  Scenario Outline: Negative Test to get list of Product Personnel for PAL service
    When the service call with POST request to fetch the product response "/v1/pal/PALProduct/Personnel" for negative scenario with PageObject "FSP PAL Product", X-operation "Read", Origin "localhost" and payload
     """
  {
  "productId":"<productId>",
  "userRole":"<userRole>"
  }

  """
    Then service should return 204 status code
    Examples:
      |productId	           |userRole      |
      |618ee6eae2df165bcd5940  |projectManager|

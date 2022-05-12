#Author: MadhuShreeVarshini.Chandrasekar@mnscorp.net
#Feature: PAL Service to fetch the PAL Product Information
#StoryDescription: Automation for the getPALProductInformation endpoint

@GetPALProductInformation
Feature: Able to call the spring boot service and fetch the PAL Product Information

#####################################################Story/FSP-1271#####################################################
  @getPALProductInformation_success
  Scenario Outline: Positive scenario that verify the response from the get PAL product information against success message
    When the service call with POST request to fetch the product response "/v1/pal/PALProduct/Information" with PageObject "FSP PAL Product", X-operation "Read", Origin "localhost" and payload
  """
  {
  "productId":"<productId>",
  "role":<role>,
  "sectionName":<sectionName>,
  "userRole":"<userRole>"
  }
  """
    Then service should return 200 status code
    Then response must match with json schema "schema/getPALproductinfo.json".
    Then cross verify with the response for productId.
    Examples:
      | productId                | role          | sectionName          | userRole       |
      | 616d8c8479e4d44111202fcb | "buyer"       | "productInformation" | projectManager |
      | 616d8c8479e4d44111202fcb | "buyer"       | null                 | projectManager |
      | 616d8c8479e4d44111202fcb | null          | "productInformation" | projectManager |
      | 616d8c8479e4d44111202fcb | null          | null                 | projectManager |
      | 6149dbf63535de17d5ad6b80 | "buyer"       | "InvalidSectionName" | projectManager |
      | 6149dbf63535de17d5ad6b80 | "InvalidRole" | "productInformation" | projectManager |
##########################################################Story/FSP-1271################################################
  @getPALProductInformation_errorstatus400
  Scenario Outline: Negative scenario that verify the response from the get PAL product information against Bad Request
    When the service call with POST request to fetch the product response "/v1/pal/PALProduct/Information" for negative scenario with PageObject "FSP PAL Product", X-operation "Read", Origin "localhost" and payload
  """
  {
  "productId":<productId>,
  "role":<role>,
  "sectionName":<sectionName>,
  "userRole":<userRole>
  }
  """
    Then service should return 400 status code
    Then response must match with json schema "schema/errorResponse.json".
    Then cross verify with the response if the error message is "Bad Request".
    Examples:
      | productId                  | role    | sectionName          | userRole         |
      | "616d8c8479e4d44111202fcb" | "buyer" | "productInformation" | null             |
      | "616d8c8479e4d44111202fcb" | "buyer" | null                 | null             |
      | "616d8c8479e4d44111202fcb" | null    | "productInformation" | null             |
      | "616d8c8479e4d44111202fcb" | null    | null                 | null             |
      | null                       | "buyer" | "productInformation" | "projectManager" |
      | null                       | "buyer" | "productInformation" | null             |
      | null                       | "buyer" | null                 | "projectManager" |
      | null                       | "buyer" | null                 | null             |
      | null                       | null    | "productInformation" | "projectManager" |
      | null                       | null    | "productInformation" | null             |
      | null                       | null    | null                 | "projectManager" |
      | null                       | null    | null                 | null             |

##########################################################Story/FSP-1271################################################
  @getPALProductInformation_errorstatus400
  Scenario Outline: Negative scenario that verify the response from the get PAL product information against Not authorized to access
    When the service call with POST request to fetch the product response "/v1/pal/PALProduct/Information" for negative scenario with PageObject "FSP PAL Product", X-operation "Read", Origin "localhost" and payload
  """
  {
  "productId":"<productId>",
  "role":"<role>",
  "sectionName":"<sectionName>",
  "userRole":"<userRole>"
  }
  """
    Then service should return 400 status code
    Then response must match with json schema "schema/errorResponse.json".
    Then cross verify with the response if the error message is "Not authorized to access".
    Examples:
      | productId                | role  | sectionName        | userRole         |
      | 6149dbf63535de17d5ad6b80 | buyer | productInformation | InvalidUserRole  |
      | InvalidId                | buyer | productInformation | productDeveloper |
###########################################################Story/FSP-1271###############################################
  @getPALProductInformation_errorstatus204
  Scenario Outline: Negative scenario that verify the response from the get PAL product information against No Data Available
    When the service call with POST request to fetch the product response "/v1/pal/PALProduct/Information" for negative scenario with PageObject "FSP PAL Product", X-operation "Read", Origin "localhost" and payload
  """
  {
  "productId":"<productId>",
  "role":"<role>",
  "sectionName":"<sectionName>",
  "userRole":"<userRole>"
  }
  """
    Then service should return 204 status code
    Examples:
      | productId | role  | sectionName        | userRole       |
      | InvalidId | buyer | productInformation | projectManager |
###########################################################Story/FSP-1271###############################################
  @getPALProductInformation
  Scenario Outline: Positive scenario that verify the response from the get PAL product information against success message
    When the service call with POST request to fetch the product response "/v1/pal/PALProduct/Information" for negative scenario with PageObject "FSP PAL Product", X-operation "Read", Origin "localhost" and payload
  """
  {
  "productId":"<productId>",
  "userRole":"<userRole>"
  }
  """
    Then service should return 200 status code
    Then response must match with json schema "schema/getPALproductinfo.json".
    Then cross verify with the response if the productId is "<productId>".
    Examples:
      | productId                | userRole         |
      | 6149dbf63535de17d5ad6b80 | productDeveloper |
###########################################################Story/FSP-1271###############################################
  @getPALProductInformation_errorstatus204
  Scenario Outline: Negative scenario that verify the response from the get PAL product information against No Data Available
    When the service call with POST request to fetch the product response "/v1/pal/PALProduct/Information" for negative scenario with PageObject "FSP PAL Product", X-operation "Read", Origin "localhost" and payload
  """
  {
  "productId":"<productId>",
  "userRole":"<userRole>"
  }
  """
    Then service should return 204 status code
    Examples:
      | productId | userRole       |
      | InvalidId | projectManager |

###########################################################Story/FSP-1271###############################################
  @getPALProductInformation_errorstatus400
  Scenario Outline: Negative scenario that verify the response from the get PAL product information against Bad Request
    When the service call with POST request to fetch the product response "/v1/pal/PALProduct/Information" for negative scenario with PageObject "FSP PAL Product", X-operation "Read", Origin "localhost" and payload
  """
  {
  "productId":<productId>,
  "userRole":<userRole>
  }
  """
    Then service should return 400 status code
    Then response must match with json schema "schema/errorResponse.json".
    Then cross verify with the response if the error message is "Bad Request".
    Examples:
      | productId                  | userRole           |
      | null                       | "foodTechnologist" |
      | "6149dbf63535de17d5ad6b80" | null               |
##########################################################Story/FSP-1271################################################
  @Role_ProjectManager
  Scenario Outline: Positive scenario that verify the response from the get PAL product information for userRole ProjectManager with different roles
    When the service call with POST request to fetch the product information service "/v1/pal/PALProduct/Information" with PageObject "FSP PAL Product", X-operation "Read", Origin "localhost" and payload
  """
  {
  "productId":"<productId>",
  "role":"<role>",
  "userRole":"<userRole>"
  }
  """
    Then service should return 200 status code
    Then response must match with json schema "schema/getPALproductinfo.json".
    Then cross verify with the response for the role "<role>"
    Examples:
      | productId                | role              | userRole       |
      | 616d8c8479e4d44111202fcb | projectManager    | projectManager |
      | 616d8c8479e4d44111202fcb | productDeveloper  | projectManager |
      | 616d8c8479e4d44111202fcb | buyer             | projectManager |
      | 616d8c8479e4d44111202fcb | foodTechnologist  | projectManager |
      | 616d8c8479e4d44111202fcb | international     | projectManager |
      | 616d8c8479e4d44111202fcb | commercialPlanner | projectManager |
      | 616d8c8479e4d44111202fcb | ocado             | projectManager |
      | 616d8c8479e4d44111202fcb | finance           | projectManager |
      | 616d8c8479e4d44111202fcb | supplier          | projectManager |
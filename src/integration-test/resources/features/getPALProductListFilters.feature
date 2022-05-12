#Author: MadhuShreeVarshini.Chandrasekar@mnscorp.net
#Feature: PAL Service to fetch the PAL Product List using filters
#StoryDescription: Automation for the getPALProductList endpoint
@GetPALProductList
Feature: Able to call the spring boot service and fetch the PAL Product using filters

#############################################Story/FSP-810##############################################################
  @getPALProductList_Filters_success
  Scenario Outline: Positive scenario that verify the response from the getPALProductList using filters and searchText
    When the service call with POST request to fetch PAL project response with "/v1/pal/PALProject/ProductList" with PageObject "FSP PAL Project Details", X-operation "Read", Origin "localhost" and payload
  """
  {
    "projectId": <projectId>,
    "userRole": <userRole>,
    "filter": {
        "searchText": <searchText>,
        "status": <status>,
        "suppliers": <suppliers>,
        "progressRange": <progress>,
        "type": <type>,
        "launchByDate": <launchByDate>,
       "intoDepotDate": <intoDepotDate>,
       "ocadoProduct": <ocadoProduct>
    }
  }
  """
    Then service should return 200 status code
    Then response must match with json schema "schema/getPALProductListFilters.json".
    Then assert the response
    Examples:
      | projectId                  | userRole            | searchText              | status                  | suppliers            | progress         | type                       | launchByDate   | intoDepotDate | ocadoProduct |
      | "618ea6165952b2dd4453cdd4" | "projectManager"    | "Cak"                   | null                    | null                 | null             | null                       | null           | null          | null         |
      | "618ea6165952b2dd4453cdd4" | "projectManager"    | "1213432"               | null                    | null                 | null             | null                       | null           | null          | null         |
      | "618ea6165952b2dd4453cdd4" | "projectManager"    | "Blue Earth Foods LTD." | null                    | null                 | null             | null                       | null           | null          | null         |
      | "618ea6165952b2dd4453cdd4" | "projectManager"    | null                    | ["In Progress"]         | null                 | null             | null                       | null           | null          | null         |
      | "618ea6165952b2dd4453cdd4" | "projectManager"    | null                    | ["In Progress", "Hold"] | null                 | null             | null                       | null           | null          | null         |
      | "618ea6165952b2dd4453cdd4" | "projectManager"    | null                    | null                    | ["F01901"]           | null             | null                       | null           | null          | null         |
      | "618ea6165952b2dd4453cdd4" | "projectManager"    | null                    | null                    | ["F01902", "F01901"] | null             | null                       | null           | null          | null         |
      | "618ea6165952b2dd4453cdd4" | "projectManager"    | null                    | null                    | null                 | ["0-24"]         | null                       | null           | null          | null         |
      | "618ea6165952b2dd4453cdd4" | "projectManager"    | null                    | null                    | null                 | ["100"]          | null                       | null           | null          | null         |
      | "618ea6165952b2dd4453cdd4" | "projectManager"    | null                    | null                    | null                 | ["0-24","25-49"] | null                       | null           | null          | null         |
      | "618ea6165952b2dd4453cdd4" | "projectManager"    | null                    | null                    | null                 | null             | ["New Product"]            | null           | null          | null         |
      | "618ea6165952b2dd4453cdd4" | "projectManager"    | null                    | null                    | null                 | null             | ["New Product", "Upgrade"] | null           | null          | null         |
      | "618ea6165952b2dd4453cdd4" | "projectManager"    | null                    | null                    | null                 | null             | null                       | "P05 AUG 2020" | null          | null         |
      | "618ea6165952b2dd4453cdd4" | "projectManager"    | null                    | null                    | null                 | null             | null                       | null           | "31/12/2021"  | null         |
      | "618ea6165952b2dd4453cdd4" | "projectManager"    | null                    | null                    | null                 | null             | null                       | null           | null          | true         |
      | "618ea6165952b2dd4453cdd4" | "projectManager"    | null                    | null                    | null                 | null             | null                       | "P05 AUG 2020" | "31/12/2021"  | null         |
      | "618ea6165952b2dd4453cdd4" | "projectManager"    | null                    | null                    | null                 | null             | null                       | "P05 AUG 2020" | null          | true         |
      | "618ea6165952b2dd4453cdd4" | "projectManager"    | null                    | null                    | null                 | null             | null                       | null           | "31/12/2021"  | true         |
      | "618ea6165952b2dd4453cdd4" | "projectManager"    | null                    | null                    | null                 | null             | null                       | "P05 AUG 2020" | "31/12/2021"  | true         |
      | "618ea6165952b2dd4453cdd4" | "projectManager"    | null                    | null                    | null                 | null             | ["New Product", "Upgrade"] | "P05 AUG 2020" | null          | null         |
      | "618ea6165952b2dd4453cdd4" | "projectManager"    | null                    | null                    | null                 | null             | ["New Product", "Upgrade"] | null           | "31/12/2021"  | null         |
      | "618ea6165952b2dd4453cdd4" | "projectManager"    | null                    | null                    | null                 | null             | ["New Product", "Upgrade"] | null           | null          | true         |
      | "618ea6165952b2dd4453cdd4" | "projectManager"    | null                    | null                    | null                 | null             | ["New Product", "Upgrade"] | "P05 AUG 2020" | "31/12/2021"  | null         |
      | "618ea6165952b2dd4453cdd4" | "projectManager"    | null                    | null                    | null                 | null             | ["New Product", "Upgrade"] | "P05 AUG 2020" | null          | true         |
      | "618ea6165952b2dd4453cdd4" | "projectManager"    | null                    | null                    | null                 | null             | ["New Product", "Upgrade"] | null           | "31/12/2021"  | true         |
      | "618ea6165952b2dd4453cdd4" | "projectManager"    | null                    | null                    | null                 | null             | ["New Product", "Upgrade"] | "P05 AUG 2020" | "31/12/2021"  | true         |
      | "618ea6165952b2dd4453cdd4" | "projectManager"    | null                    | null                    | null                 | ["0-24","25-49"] | ["New Product", "Upgrade"] | null           | null          | null         |
      | "618ea6165952b2dd4453cdd4" | "projectManager"    | null                    | null                    | null                 | ["0-24","25-49"] | null                       | "P05 AUG 2020" | null          | null         |
      | "618ea6165952b2dd4453cdd4" | "projectManager"    | null                    | null                    | null                 | ["0-24","25-49"] | null                       | null           | "31/12/2021"  | null         |
      | "618ea6165952b2dd4453cdd4" | "projectManager"    | null                    | null                    | null                 | ["0-24","25-49"] | null                       | null           | null          | true         |
      | "618ea6165952b2dd4453cdd4" | "projectManager"    | null                    | null                    | null                 | ["0-24","25-49"] | null                       | "P05 AUG 2020" | "31/12/2021"  | null         |
      | "618ea6165952b2dd4453cdd4" | "projectManager"    | null                    | null                    | null                 | ["0-24","25-49"] | null                       | "P05 AUG 2020" | null          | true         |
      | "618ea6165952b2dd4453cdd4" | "projectManager"    | null                    | null                    | null                 | ["0-24","25-49"] | null                       | null           | "31/12/2021"  | true         |
      | "618ea6165952b2dd4453cdd4" | "projectManager"    | null                    | null                    | null                 | ["0-24","25-49"] | null                       | "P05 AUG 2020" | "31/12/2021"  | true         |
      | "618ea6165952b2dd4453cdd4" | "projectManager"    | null                    | null                    | null                 | ["0-24","25-49"] | ["New Product", "Upgrade"] | "P05 AUG 2020" | null          | null         |
      | "618ea6165952b2dd4453cdd4" | "projectManager"    | null                    | null                    | null                 | ["0-24","25-49"] | ["New Product", "Upgrade"] | null           | "31/12/2021"  | null         |
      | "618ea6165952b2dd4453cdd4" | "projectManager"    | null                    | null                    | null                 | ["0-24","25-49"] | ["New Product", "Upgrade"] | null           | null          | true         |
      | "618ea6165952b2dd4453cdd4" | "projectManager"    | null                    | null                    | null                 | ["0-24","25-49"] | ["New Product", "Upgrade"] | "P05 AUG 2020" | "31/12/2021"  | null         |
      | "618ea6165952b2dd4453cdd4" | "projectManager"    | null                    | null                    | null                 | ["0-24","25-49"] | ["New Product", "Upgrade"] | "P05 AUG 2020" | null          | true         |
      | "618ea6165952b2dd4453cdd4" | "projectManager"    | null                    | null                    | null                 | ["0-24","25-49"] | ["New Product", "Upgrade"] | null           | "31/12/2021"  | true         |
      | "618ea6165952b2dd4453cdd4" | "projectManager"    | null                    | null                    | null                 | ["0-24","25-49"] | ["New Product", "Upgrade"] | "P05 AUG 2020" | "31/12/2021"  | true         |
      | "618ea6165952b2dd4453cdd4" | "projectManager"    | null                    | null                    | ["F01902", "F01901"] | ["25-49"]        | null                       | null           | null          | null         |
      | "618ea6165952b2dd4453cdd4" | "projectManager"    | null                    | null                    | ["F01902", "F01901"] | null             | ["Upgrade"]                | null           | null          | null         |
      | "618ea6165952b2dd4453cdd4" | "projectManager"    | null                    | null                    | ["F01902", "F01901"] | null             | null                       | "P05 AUG 2020" | null          | null         |
      | "618ea6165952b2dd4453cdd4" | "projectManager"    | null                    | null                    | ["F01902", "F01901"] | null             | null                       | null           | "28/10/2021"  | null         |
      | "618ea6165952b2dd4453cdd4" | "projectManager"    | null                    | null                    | ["F01901"]           | null             | null                       | null           | null          | true         |
      | "618ea6165952b2dd4453cdd4" | "projectManager"    | null                    | null                    | ["F01902", "F01901"] | null             | null                       | "P05 AUG 2020" | "31/12/2021"  | null         |
      | "618ea6165952b2dd4453cdd4" | "projectManager"    | null                    | null                    | ["F01902", "F01901"] | null             | null                       | "P05 AUG 2020" | null          | true         |
      | "618ea6165952b2dd4453cdd4" | "projectManager"    | null                    | null                    | ["F01902", "F01901"] | null             | null                       | null           | "31/12/2021"  | true         |
      | "618ea6165952b2dd4453cdd4" | "projectManager"    | null                    | null                    | ["F01902", "F01901"] | null             | null                       | "P05 AUG 2020" | "31/12/2021"  | true         |
      | "618ea6165952b2dd4453cdd4" | "projectManager"    | null                    | null                    | ["F01902", "F01901"] | null             | ["New Product", "Upgrade"] | "P05 AUG 2020" | null          | null         |
      | "618ea6165952b2dd4453cdd4" | "projectManager"    | null                    | null                    | ["F01902", "F01901"] | null             | ["New Product", "Upgrade"] | null           | "31/12/2021"  | null         |
      | "618ea6165952b2dd4453cdd4" | "projectManager"    | null                    | null                    | ["F01902", "F01901"] | null             | ["New Product", "Upgrade"] | null           | null          | true         |
      | "618ea6165952b2dd4453cdd4" | "projectManager"    | null                    | null                    | ["F01902", "F01901"] | null             | ["New Product", "Upgrade"] | "P05 AUG 2020" | "31/12/2021"  | null         |
      | "618ea6165952b2dd4453cdd4" | "projectManager"    | null                    | null                    | ["F01902", "F01901"] | null             | ["New Product", "Upgrade"] | "P05 AUG 2020" | null          | true         |
      | "618ea6165952b2dd4453cdd4" | "projectManager"    | null                    | null                    | ["F01902", "F01901"] | null             | ["New Product", "Upgrade"] | null           | "31/12/2021"  | true         |
      | "618ea6165952b2dd4453cdd4" | "projectManager"    | null                    | null                    | ["F01902", "F01901"] | null             | ["New Product", "Upgrade"] | "P05 AUG 2020" | "31/12/2021"  | true         |
      | "618ea6165952b2dd4453cdd4" | "projectManager"    | null                    | null                    | ["F01902", "F01901"] | ["25-49"]        | ["New Product", "Upgrade"] | null           | null          | null         |
      | "618ea6165952b2dd4453cdd4" | "projectManager"    | null                    | null                    | ["F01902", "F01901"] | ["25-49"]        | null                       | "P05 AUG 2020" | null          | null         |
      | "618ea6165952b2dd4453cdd4" | "projectManager"    | null                    | null                    | ["F01902", "F01901"] | ["25-49"]        | null                       | null           | "31/12/2021"  | null         |
      | "618ea6165952b2dd4453cdd4" | "projectManager"    | null                    | null                    | ["F01902", "F01901"] | ["25-49"]        | null                       | null           | null          | true         |
      | "618ea6165952b2dd4453cdd4" | "projectManager"    | null                    | null                    | ["F01902", "F01901"] | ["25-49"]        | null                       | "P05 AUG 2020" | "31/12/2021"  | null         |
      | "618ea6165952b2dd4453cdd4" | "projectManager"    | null                    | null                    | ["F01902", "F01901"] | ["25-49"]        | null                       | "P05 AUG 2020" | null          | true         |
      | "618ea6165952b2dd4453cdd4" | "projectManager"    | null                    | null                    | ["F01902", "F01901"] | ["25-49"]        | null                       | null           | "31/12/2021"  | true         |
      | "618ea6165952b2dd4453cdd4" | "projectManager"    | null                    | null                    | ["F01902", "F01901"] | ["25-49"]        | null                       | "P05 AUG 2020" | "31/12/2021"  | true         |
      | "618ea6165952b2dd4453cdd4" | "projectManager"    | null                    | null                    | ["F01902", "F01901"] | ["25-49"]        | ["New Product", "Upgrade"] | "P05 AUG 2020" | null          | null         |
      | "618ea6165952b2dd4453cdd4" | "projectManager"    | null                    | null                    | ["F01902", "F01901"] | ["25-49"]        | ["New Product", "Upgrade"] | null           | "31/12/2021"  | null         |
      | "618ea6165952b2dd4453cdd4" | "projectManager"    | null                    | null                    | ["F01902", "F01901"] | ["25-49"]        | ["New Product", "Upgrade"] | null           | null          | true         |
      | "618ea6165952b2dd4453cdd4" | "projectManager"    | null                    | null                    | ["F01902", "F01901"] | ["25-49"]        | ["New Product", "Upgrade"] | "P05 AUG 2020" | "31/12/2021"  | null         |
      | "618ea6165952b2dd4453cdd4" | "projectManager"    | null                    | null                    | ["F01902", "F01901"] | ["25-49"]        | ["New Product", "Upgrade"] | "P05 AUG 2020" | null          | true         |
      | "618ea6165952b2dd4453cdd4" | "projectManager"    | null                    | null                    | ["F01902", "F01901"] | ["25-49"]        | ["New Product", "Upgrade"] | null           | "31/12/2021"  | true         |
      | "618ea6165952b2dd4453cdd4" | "projectManager"    | null                    | null                    | ["F01902", "F01901"] | ["25-49"]        | ["New Product", "Upgrade"] | "P05 AUG 2020" | "31/12/2021"  | true         |
      | "618ea6165952b2dd4453cdd4" | "projectManager"    | null                    | ["In Progress", "Hold"] | ["F01902", "F01901"] | null             | null                       | null           | null          | null         |
      | "618ea6165952b2dd4453cdd4" | "projectManager"    | null                    | ["In Progress", "Hold"] | null                 | ["25-49"]        | null                       | null           | null          | null         |
      | "618ea6165952b2dd4453cdd4" | "projectManager"    | null                    | ["In Progress", "Hold"] | null                 | null             | ["Upgrade"]                | null           | null          | null         |
      | "618ea6165952b2dd4453cdd4" | "projectManager"    | null                    | ["In Progress", "Hold"] | null                 | null             | null                       | "P05 AUG 2020" | null          | null         |
      | "618ea6165952b2dd4453cdd4" | "projectManager"    | null                    | ["In Progress", "Hold"] | null                 | null             | null                       | null           | "31/12/2021"  | null         |
      | "618ea6165952b2dd4453cdd4" | "projectManager"    | null                    | ["In Progress", "Hold"] | null                 | null             | null                       | null           | null          | true         |
      | "618ea6165952b2dd4453cdd4" | "projectManager"    | null                    | ["In Progress", "Hold"] | null                 | null             | null                       | "P05 AUG 2020" | "31/12/2021"  | null         |
      | "618ea6165952b2dd4453cdd4" | "projectManager"    | null                    | ["In Progress", "Hold"] | null                 | null             | null                       | "P05 AUG 2020" | null          | true         |
      | "618ea6165952b2dd4453cdd4" | "projectManager"    | null                    | ["In Progress", "Hold"] | null                 | null             | null                       | null           | "31/12/2021"  | true         |
      | "618ea6165952b2dd4453cdd4" | "projectManager"    | null                    | ["In Progress", "Hold"] | null                 | null             | null                       | "P05 AUG 2020" | "31/12/2021"  | true         |
      | "618ea6165952b2dd4453cdd4" | "projectManager"    | null                    | ["In Progress", "Hold"] | null                 | null             | ["New Product", "Upgrade"] | "P05 AUG 2020" | null          | null         |
      | "618ea6165952b2dd4453cdd4" | "projectManager"    | null                    | ["In Progress", "Hold"] | null                 | null             | ["New Product", "Upgrade"] | null           | "31/12/2021"  | null         |
      | "618ea6165952b2dd4453cdd4" | "projectManager"    | null                    | ["In Progress", "Hold"] | null                 | null             | ["New Product", "Upgrade"] | null           | null          | true         |
      | "618ea6165952b2dd4453cdd4" | "projectManager"    | null                    | ["In Progress", "Hold"] | null                 | null             | ["New Product", "Upgrade"] | "P05 AUG 2020" | "31/12/2021"  | null         |
      | "618ea6165952b2dd4453cdd4" | "projectManager"    | null                    | ["In Progress", "Hold"] | null                 | null             | ["New Product", "Upgrade"] | "P05 AUG 2020" | null          | true         |
      | "618ea6165952b2dd4453cdd4" | "projectManager"    | null                    | ["In Progress", "Hold"] | null                 | null             | ["New Product", "Upgrade"] | null           | "31/12/2021"  | true         |
      | "618ea6165952b2dd4453cdd4" | "projectManager"    | null                    | ["In Progress", "Hold"] | null                 | null             | ["New Product", "Upgrade"] | "P05 AUG 2020" | "31/12/2021"  | true         |
      | "618ea6165952b2dd4453cdd4" | "projectManager"    | null                    | ["In Progress", "Hold"] | null                 | ["25-49"]        | ["Upgrade"]                | null           | null          | null         |
      | "618ea6165952b2dd4453cdd4" | "projectManager"    | null                    | ["In Progress", "Hold"] | null                 | ["25-49"]        | null                       | "P05 AUG 2020" | null          | null         |
      | "618ea6165952b2dd4453cdd4" | "projectManager"    | null                    | ["In Progress", "Hold"] | null                 | ["25-49"]        | null                       | null           | "31/12/2021"  | null         |
      | "618ea6165952b2dd4453cdd4" | "projectManager"    | null                    | ["In Progress", "Hold"] | null                 | ["25-49"]        | null                       | null           | null          | true         |
      | "618ea6165952b2dd4453cdd4" | "projectManager"    | null                    | ["In Progress", "Hold"] | null                 | ["25-49"]        | null                       | "P05 AUG 2020" | "31/12/2021"  | null         |
      | "618ea6165952b2dd4453cdd4" | "projectManager"    | null                    | ["In Progress", "Hold"] | null                 | ["25-49"]        | null                       | "P05 AUG 2020" | null          | true         |
      | "618ea6165952b2dd4453cdd4" | "projectManager"    | null                    | ["In Progress", "Hold"] | null                 | ["25-49"]        | null                       | null           | "31/12/2021"  | true         |
      | "618ea6165952b2dd4453cdd4" | "projectManager"    | null                    | ["In Progress", "Hold"] | null                 | ["25-49"]        | null                       | "P05 AUG 2020" | "31/12/2021"  | true         |
      | "618ea6165952b2dd4453cdd4" | "projectManager"    | null                    | ["In Progress", "Hold"] | null                 | ["25-49"]        | ["New Product", "Upgrade"] | "P05 AUG 2020" | null          | null         |
      | "618ea6165952b2dd4453cdd4" | "projectManager"    | null                    | ["In Progress", "Hold"] | null                 | ["25-49"]        | ["New Product", "Upgrade"] | null           | "31/12/2021"  | null         |
      | "618ea6165952b2dd4453cdd4" | "projectManager"    | null                    | ["In Progress", "Hold"] | null                 | ["25-49"]        | ["New Product", "Upgrade"] | null           | null          | true         |
      | "618ea6165952b2dd4453cdd4" | "projectManager"    | null                    | ["In Progress", "Hold"] | null                 | ["25-49"]        | ["New Product", "Upgrade"] | "P05 AUG 2020" | "31/12/2021"  | null         |
      | "618ea6165952b2dd4453cdd4" | "projectManager"    | null                    | ["In Progress", "Hold"] | null                 | ["25-49"]        | ["New Product", "Upgrade"] | "P05 AUG 2020" | null          | true         |
      | "618ea6165952b2dd4453cdd4" | "projectManager"    | null                    | ["In Progress", "Hold"] | null                 | ["25-49"]        | ["New Product", "Upgrade"] | null           | "31/12/2021"  | true         |
      | "618ea6165952b2dd4453cdd4" | "projectManager"    | null                    | ["In Progress", "Hold"] | null                 | ["25-49"]        | ["New Product", "Upgrade"] | "P05 AUG 2020" | "31/12/2021"  | true         |
      | "618ea6165952b2dd4453cdd4" | "projectManager"    | null                    | ["In Progress", "Hold"] | ["F01902", "F01901"] | ["25-49"]        | null                       | null           | null          | null         |
      | "618ea6165952b2dd4453cdd4" | "projectManager"    | null                    | ["In Progress", "Hold"] | ["F01902", "F01901"] | null             | ["New Product", "Upgrade"] | null           | null          | null         |
      | "618ea6165952b2dd4453cdd4" | "projectManager"    | null                    | ["In Progress", "Hold"] | ["F01902", "F01901"] | null             | null                       | "P05 AUG 2020" | null          | null         |
      | "618ea6165952b2dd4453cdd4" | "projectManager"    | null                    | ["In Progress", "Hold"] | ["F01902", "F01901"] | null             | null                       | null           | "31/12/2021"  | null         |
      | "618ea6165952b2dd4453cdd4" | "projectManager"    | null                    | ["In Progress", "Hold"] | ["F01902", "F01901"] | null             | null                       | null           | null          | true         |
      | "618ea6165952b2dd4453cdd4" | "projectManager"    | null                    | ["In Progress", "Hold"] | ["F01902", "F01901"] | null             | null                       | "P05 AUG 2020" | "31/12/2021"  | null         |
      | "618ea6165952b2dd4453cdd4" | "projectManager"    | null                    | ["In Progress", "Hold"] | ["F01902", "F01901"] | null             | null                       | "P05 AUG 2020" | null          | true         |
      | "618ea6165952b2dd4453cdd4" | "projectManager"    | null                    | ["In Progress", "Hold"] | ["F01902", "F01901"] | null             | null                       | null           | "31/12/2021"  | true         |
      | "618ea6165952b2dd4453cdd4" | "projectManager"    | null                    | ["In Progress", "Hold"] | ["F01902", "F01901"] | null             | null                       | "P05 AUG 2020" | "31/12/2021"  | true         |
      | "618ea6165952b2dd4453cdd4" | "projectManager"    | null                    | ["In Progress", "Hold"] | ["F01902", "F01901"] | null             | ["New Product", "Upgrade"] | "P05 AUG 2020" | null          | null         |
      | "618ea6165952b2dd4453cdd4" | "projectManager"    | null                    | ["In Progress", "Hold"] | ["F01902", "F01901"] | null             | ["New Product", "Upgrade"] | null           | "31/12/2021"  | null         |
      | "618ea6165952b2dd4453cdd4" | "projectManager"    | null                    | ["In Progress", "Hold"] | ["F01902", "F01901"] | null             | ["New Product", "Upgrade"] | null           | null          | true         |
      | "618ea6165952b2dd4453cdd4" | "projectManager"    | null                    | ["In Progress", "Hold"] | ["F01902", "F01901"] | null             | ["New Product", "Upgrade"] | "P05 AUG 2020" | "31/12/2021"  | null         |
      | "618ea6165952b2dd4453cdd4" | "projectManager"    | null                    | ["In Progress", "Hold"] | ["F01902", "F01901"] | null             | ["New Product", "Upgrade"] | "P05 AUG 2020" | null          | true         |
      | "618ea6165952b2dd4453cdd4" | "projectManager"    | null                    | ["In Progress", "Hold"] | ["F01902", "F01901"] | null             | ["New Product", "Upgrade"] | null           | "31/12/2021"  | true         |
      | "618ea6165952b2dd4453cdd4" | "projectManager"    | null                    | ["In Progress", "Hold"] | ["F01902", "F01901"] | null             | ["New Product", "Upgrade"] | "P05 AUG 2020" | "31/12/2021"  | true         |
      | "618ea6165952b2dd4453cdd4" | "projectManager"    | null                    | ["In Progress", "Hold"] | ["F01902", "F01901"] | ["25-49"]        | ["New Product", "Upgrade"] | null           | null          | null         |
      | "618ea6165952b2dd4453cdd4" | "projectManager"    | null                    | ["In Progress", "Hold"] | ["F01902", "F01901"] | ["25-49"]        | null                       | "P05 AUG 2020" | null          | null         |
      | "618ea6165952b2dd4453cdd4" | "projectManager"    | null                    | ["In Progress", "Hold"] | ["F01902", "F01901"] | ["25-49"]        | null                       | null           | "31/12/2021"  | null         |
      | "618ea6165952b2dd4453cdd4" | "projectManager"    | null                    | ["In Progress", "Hold"] | ["F01902", "F01901"] | ["25-49"]        | null                       | null           | null          | true         |
      | "618ea6165952b2dd4453cdd4" | "projectManager"    | null                    | ["In Progress", "Hold"] | ["F01902", "F01901"] | ["25-49"]        | null                       | "P05 AUG 2020" | "31/12/2021"  | null         |
      | "618ea6165952b2dd4453cdd4" | "projectManager"    | null                    | ["In Progress", "Hold"] | ["F01902", "F01901"] | ["25-49"]        | null                       | "P05 AUG 2020" | null          | true         |
      | "618ea6165952b2dd4453cdd4" | "projectManager"    | null                    | ["In Progress", "Hold"] | ["F01902", "F01901"] | ["25-49"]        | null                       | null           | "31/12/2021"  | true         |
      | "618ea6165952b2dd4453cdd4" | "projectManager"    | null                    | ["In Progress", "Hold"] | ["F01902", "F01901"] | ["25-49"]        | null                       | "P05 AUG 2020" | "31/12/2021"  | true         |
      | "618ea6165952b2dd4453cdd4" | "projectManager"    | null                    | ["In Progress", "Hold"] | ["F01902", "F01901"] | ["25-49"]        | ["New Product", "Upgrade"] | "P05 AUG 2020" | null          | null         |
      | "618ea6165952b2dd4453cdd4" | "projectManager"    | null                    | ["In Progress", "Hold"] | ["F01902", "F01901"] | ["25-49"]        | ["New Product", "Upgrade"] | null           | "31/12/2021"  | null         |
      | "618ea6165952b2dd4453cdd4" | "projectManager"    | null                    | ["In Progress", "Hold"] | ["F01902", "F01901"] | ["25-49"]        | ["New Product", "Upgrade"] | null           | null          | true         |
      | "618ea6165952b2dd4453cdd4" | "projectManager"    | null                    | ["In Progress", "Hold"] | ["F01902", "F01901"] | ["25-49"]        | ["New Product", "Upgrade"] | "P05 AUG 2020" | "31/12/2021"  | null         |
      | "618ea6165952b2dd4453cdd4" | "projectManager"    | null                    | ["In Progress", "Hold"] | ["F01902", "F01901"] | ["25-49"]        | ["New Product", "Upgrade"] | "P05 AUG 2020" | null          | true         |
      | "618ea6165952b2dd4453cdd4" | "projectManager"    | null                    | ["In Progress", "Hold"] | ["F01902", "F01901"] | ["25-49"]        | ["New Product", "Upgrade"] | null           | "31/12/2021"  | true         |
      | "618ea6165952b2dd4453cdd4" | "projectManager"    | null                    | ["In Progress", "Hold"] | ["F01902", "F01901"] | ["25-49"]        | ["New Product", "Upgrade"] | "P05 AUG 2020" | "31/12/2021"  | true         |
      | "618ea6165952b2dd4453cdd4" | "projectManager"    | "Sandwich"              | ["In Progress", "Hold"] | ["F01902", "F01901"] | ["25-49"]        | ["New Product", "Upgrade"] | "P04 JUL 2020" | "28/10/2021"  | null         |
      | "618ea6165952b2dd4453cdd4" | "commercialPlanner" | "Cak"                   | null                    | null                 | null             | null                       | null           | null          | null         |
      | "618ea6165952b2dd4453cdd4" | "vat"               | "Cak"                   | null                    | null                 | null             | null                       | null           | null          | null         |
      | "618ea6165952b2dd4453cdd4" | "buyer"             | "Cak"                   | null                    | null                 | null             | null                       | null           | null          | null         |
      | "618ea6165952b2dd4453cdd4" | "international"     | "Cak"                   | null                    | null                 | null             | null                       | null           | null          | null         |
      | "618ea6165952b2dd4453cdd4" | "ocado"             | "Cak"                   | null                    | null                 | null             | null                       | null           | null          | null         |
      | "618ea6165952b2dd4453cdd4" | "foodTechnologist"  | "Cak"                   | null                    | null                 | null             | null                       | null           | null          | null         |

#############################################################################Story/FSP-810#########################################################################################################################################
  @getPALProductList_BadRequest
  Scenario Outline: Negative Scenario that verify the response from the getPALProductList using filters and searchText
    When the service call with POST request to fetch PAL project response with "/v1/pal/PALProject/ProductList" for negative scenario with PageObject "FSP PAL Project Details", X-operation "Read", Origin "localhost" and payload
  """
  {
    "projectId": <projectId>,
    "userRole": <userRole>,
    "filter": {
        "searchText": <searchText>
    }
  }
  """
    Then service should return 400 status code
    Then response must match with json schema "schema/errorResponse.json".
    Then cross verify with the response if the error message is "Bad Request".
    Examples:
      | projectId                  | userRole         | searchText |
      | "618ea6165952b2dd4453cdd4" | ""               | "Cakes"    |
      | ""                         | "projectManager" | "Cakes"    |
      | null                       | "projectManager" | "Cakes"    |
      | "618ea6165952b2dd4453cdd4" | null             | "Cakes"    |

 ########################################################################Story/FSP-810########################################################################################################################

  @getPALProductList_NoDataAvailable
  Scenario Outline: Negative Scenario that verify the response from the getPALProductList using filters and searchText
    When the service call with POST request to fetch PAL project response with "/v1/pal/PALProject/ProductList" for negative scenario with PageObject "FSP PAL Project Details", X-operation "Read", Origin "localhost" and payload
  """
  {
    "projectId": <projectId>,
    "userRole": <userRole>,
    "filter": {
        "searchText": <searchText>
    }
  }
  """
    Then service should return 204 status code
    Examples:
      | projectId   | userRole         | searchText |
      | "InvalidId" | "projectManager" | "Cakes"    |

#######################################################################Story/FSP-810#####################################################################################
  @getPALProductList_NotAuthorized
  Scenario Outline: Negative Scenario that verify the response from the getPALProductList using filters and searchText
    When the service call with POST request to fetch PAL project response with "/v1/pal/PALProject/ProductList" for negative scenario with PageObject "FSP PAL Project Details", X-operation "Read", Origin "localhost" and payload
  """
  {
    "projectId": <projectId>,
    "userRole": <userRole>,
    "filter": {
        "searchText": <searchText>
    }
  }
  """
    Then service should return 400 status code
    Then response must match with json schema "schema/errorResponse.json".
    Then cross verify with the response if the error message is "Not authorized to access".
    Examples:
      | projectId                  | userRole          | searchText |
      | "618ea6165952b2dd4453cdd4" | "InvalidUserRole" | "Cakes"    |

#########################################################################Story/FSP-810##################################################################################














#Author: Bharath.KM@mnscorp.net
#Feature: POST service to get the PAL Configuration
#StoryDescription : POST service to  the PAL Configuration
#Adding validation to check the response whether it has been returning the details correctly
@getListPALConfiguration
Feature: Post Configuration for PAL Functionality



  @getListPALConfiguration_verifyjsonschema_response
  Scenario Outline: Positive Test to get list of PAL Configurations for PAL service by passing valid configurationID
    When service call with POST Request for PAL Configurations endpoint with "/v1/pal/PALConfigurations" for following payload
        """
[<configurationId>]

  """
    Then service should return 200 status code
    Then Assert the response for post configuration
    Then response must match with json schema "schema/palConfig.json".
    Examples:
      |configurationId                                                                                |
      |"AGREED COST PRICE CURRENCY"                                                                   |
      |"PROJECT TYPE"                                                                                 |
      |"LAUNCH PHASE"                                                                                 |
      |"AGREED COST PRICE CURRENCY","PROJECT TYPE"                                                    |
      |"LAUNCH PHASE","PROJECT TYPE" ,"AGREED COST PRICE CURRENCY"                                    |
      |"PROJECT TYPE","STATUS","LAUNCH PHASE","AGREED COST PRICE CURRENCY"                            |
      |"PROJECT TYPE","STATUS","LAUNCH PHASE","PRODUCT TYPE","FIND PRODUCT FILE TYPE","PROJECT STATUS"|

  @getListPALConfiguration_invalid_configurationid
  Scenario Outline: Positive Test to get list of PAL Configurations for PAL service by passing valid configurationID
    When service call with POST Request for PAL Configurations endpoint with "/v1/pal/PALConfigurations" for following payload for invalid configurations
        """
[<configurationId>]

  """
    Then service should return 200 status code
    Then assert the response for invalid configurationid
    Then response must match with json schema "schema/palConfig.json".
    Examples:
      |configurationId|
      |""             |
      |"invalid"      |
      |null           |

  @getListPALConfiguration_empty_configurationid
  Scenario Outline: Positive Test to get list of PAL Configurations for PAL service by passing valid configurationID
    When service call with POST Request for PAL Configurations endpoint with "/v1/pal/PALConfigurations" for following payload for invalid configurations
        """
[<configurationId>]

  """
    Then service should return 200 status code
    Then response must match with json schema "schema/palConfig.json".
    Examples:
      |configurationId|
      |               |

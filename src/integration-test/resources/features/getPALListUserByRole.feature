#Author: fariha.parveensa@mnscorp.net
#Feature: Get List user by role for PAL functionality
#Story Description: Automation PAL Service for List User by role
@getListUserByRole
Feature: Get list user by role for PAL Functionality

########################################Story/FSP-1275#############################################################
  @getListUserByRole_valid_objectId
  Scenario Outline: Positive Test to get list user by role for PAL service for valid objectId
    When the service call with GET request is made to url "/v1/users/listUserByRoles" with "<role>" as queryParam
    Then service should return 200 status code
    Then response must match with json schema "schema/getPalListUserByRole.json".
    Examples:
      | role                                 |
      | 654fd403-b97b-4cfe-9ba1-78f14c302b9e |
      | 71dad8df-bf7e-4dbe-be66-ce9c3d7c5af8 |
      | a0315d39-d00e-4f6e-8c3b-63565057db44 |

#######################################################Story/FSP-1275###############################################
  @getListUserByRole_inValid_ObjectId
  Scenario Outline: Negative Test to get list user by role for PAL service for invalid objectId
    When the service call with GET request is made to url "/v1/users/listUserByRoles" with "<invalidRole>" as queryParam
    Then service should return 400 status code
    Then cross verify with the response if the error message is "Invalid role".
    Then response must match with json schema "schema/errorResponse.json".
    Examples:
      | invalidRole |
      | 67ab35      |
      | 67@f        |

#############################################################Story/FSP-1275##########################################
  @getListUserByRole_null_ObjectId
  Scenario: Negative Test to get list user by role for PAL service for empty objectId
    When the service call with GET request is made to url "/v1/users/listUserByRoles" with " " as queryParam
    Then service should return 400 status code
    Then cross verify with the response if the error message is "Something went wrong, please try again".
    Then response must match with json schema "schema/errorResponse.json".

#############################################################Story/FSP-1275##########################################
